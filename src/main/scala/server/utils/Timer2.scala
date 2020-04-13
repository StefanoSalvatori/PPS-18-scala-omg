package server.utils

import java.util.TimerTask

trait Timer2 {

  private var timer2: Option[java.util.Timer] = Option.empty

  def started2: Boolean = this.timer2.nonEmpty

  def scheduleAtFixedRate2(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer2 match {
      case Some(timer) => stop2(timer); schedule2(task, delay, period)
      case None => schedule2(task, delay, period)
    }
  }

  def stopTimer2(): Unit = {
    this.timer2 match {
      case Some(timer) => stop2(timer)
      case None =>
    }
  }

  private def schedule2(task: () => Unit, delay: Long, period: Long): Unit = {
    this.timer2 = Option(new java.util.Timer())
    this.timer2.get.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = task()
    }, delay, period)

  }

  private def stop2(timer: java.util.Timer) = {
    this.timer2 = Option.empty
    timer.cancel()
    timer.purge()
  }
}
