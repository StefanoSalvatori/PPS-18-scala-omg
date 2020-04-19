package server.utils

import java.util.TimerTask
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

/**
 * Utility class used to wrap a [[java.util.Timer]]
 */
trait Timer {
  /**
   * Schedule a given task at a fixed rate. The task will be added to a task queue if other scheduled tasks are running
   *
   * @param task   the task to schedule
   * @param delay  the initial delay
   * @param period the period
   */
  def scheduleAtFixedRate(task: () => Unit, delay: Long, period: Long): Unit

  /**
   * Schedule the execution of the given task. The task will be added to a task queue if other scheduled tasks are running
   *
   * @param task  the task to execute
   * @param delay the initial delay
   */
  def scheduleOnce(task: () => Unit, delay: Long): Unit

  /**
   * Cancel the execution of all tasks and reset the timer
   */
  def stopTimer(): Unit
}

object Timer {
  def apply(): Timer = JavaUtilTimer()

  def withExecutor(): Timer = ExecutorTimer()
}

private case class JavaUtilTimer() extends Timer {
  /**
   * Utility trait to wrap a [[java.util.Timer]]
   */
  private var timer: Option[java.util.Timer] = None

  def started: Boolean = this.timer.nonEmpty

  def scheduleAtFixedRate(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer match {
      case Some(timer) => stop(timer); schedulePeriodic(task, delay, period)
      case None => schedulePeriodic(task, delay, period)
    }
  }

  /**
   * Schedules the specified task for execution after the specified delay.
   *
   * @param task  the task to be executed
   * @param delay millis
   */
  def scheduleOnce(task: () => Unit, delay: Long): Unit = {
    this.timer match {
      case Some(timer) => stop(timer); schedule(task, delay)
      case None => schedule(task, delay)
    }
  }

  def stopTimer(): Unit = {
    this.timer foreach stop
  }

  private def schedule(task: () => Unit, delay: Long): Unit = {
    this.timer = Option(new java.util.Timer())
    this.timer.get.schedule(new TimerTask {
      override def run(): Unit = task()
    }, delay)
  }

  private def schedulePeriodic(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer = Option(new java.util.Timer())
    this.timer.get.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = task()
    }, delay, period)
  }

  private def stop(timer: java.util.Timer) = {
    this.timer = None
    timer.cancel()
    timer.purge()
  }
}

private case class ExecutorTimer() extends Timer {
  private var executor: Option[ScheduledThreadPoolExecutor] = Some(createExecutor())

  override def scheduleAtFixedRate(task: () => Unit, delay: Long, period: Long): Unit =
    getOrCreate().scheduleAtFixedRate(() => task(), delay, period, TimeUnit.MILLISECONDS)

  override def scheduleOnce(task: () => Unit, delay: Long): Unit = {
    getOrCreate().schedule(new Runnable {
      override def run(): Unit = task()
    }, delay, TimeUnit.MILLISECONDS)
  }

  override def stopTimer(): Unit = {
    this.executor.foreach(_.shutdownNow())
    this.executor = None
  }

  private def getOrCreate(): ScheduledThreadPoolExecutor = {
    this.executor.getOrElse({
      this.executor = Some(createExecutor())
      this.executor.get
    })
  }

  private def createExecutor(): ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1)
}
