package server.utils

import java.util.TimerTask

/**
 * Utility trait to wrap a [[java.util.Timer]]
 */
trait Timer {

  private var timer: Option[java.util.Timer] = Option.empty

  def started: Boolean = this.timer.nonEmpty

  def scheduleAtFixedRate(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer match {
      case Some(timer) => stop(timer); schedule(task, delay, period)
      case None => schedule(task, delay, period)
    }
  }

  def stopTimer(): Unit = {
    this.timer match {
      case Some(timer) => stop(timer)
      case None =>
    }
  }

  private def schedule(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer = Option(new java.util.Timer())
    this.timer.get.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = task()
    }, delay, period)

  }

  private def stop(timer: java.util.Timer) = {
    this.timer = Option.empty
    timer.cancel()
    timer.purge()
  }
}
