package server.utils

import java.util.TimerTask

/**
 * Utility trait to wrap a [[java.util.Timer]]
 */
sealed trait Timer {

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
   * @param task the task to be executed
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

object Timer {
  def apply(): Timer = TimerImpl()
}

private case class TimerImpl() extends Timer
