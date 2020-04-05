package client.room

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.SharedRoom.RoomId

import scala.util.{Failure, Success}

object ClientRoomActor {
  def apply(coreClient: ActorRef, serverUri: String, room: ClientRoom): Props =
    Props(classOf[ClientRoomActor], coreClient, serverUri, room)
}

/**
 * Handles the connection with the server side room.
 * Notify the coreClient if the associated room was left or joined
 */
case class ClientRoomActor(coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends BasicActor {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: String => Unit = x => {}

  override def receive: Receive = onReceive orElse fallbackReceive
  def waitSocketResponse(replyTo: ActorRef): Receive = onWaitSocketResponse(replyTo) orElse fallbackReceive
  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = onSocketOpened(outRef, replyTo) orElse fallbackReceive
  def roomJoined(outRef: ActorRef): Receive = onRoomJoined(outRef) orElse fallbackReceive


  def onReceive: Receive = {
    case JoinRoom(roomId: RoomId) =>
      coreClient ! ClientRoomJoined(room)//TODO: delete this
      httpClient ! HttpSocketRequest(roomId)
      context.become(waitSocketResponse(sender))
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) => replyTo ! Failure(new Exception(code.toString))
    case HttpSocketSuccess(outRef) =>
      /*context.become(socketOpened(outRef, replyTo))
      self ! SendMsg("join")*/ //TODO: protocol join
      replyTo ! Success()
      context.become(roomJoined(outRef))

    case OnMsg(callback) => onMessageCallback = callback
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case TextMessage.Strict("join ok") =>
      context.become(roomJoined(outRef))
      coreClient ! ClientRoomJoined(room)
      replyTo ! Success()
    case TextMessage.Strict("join fail") =>
      replyTo ! Failure(new Exception("Can't join"))

    case TextMessage.Strict(_) =>
      context.become(roomJoined(outRef))
      replyTo ! Success()

    case OnMsg(callback) => onMessageCallback = callback
  }

  def onRoomJoined(outRef: ActorRef): Receive = {
    case LeaveRoom() =>
      self ! SendMsg("leave")
      coreClient ! ClientRoomLeaved(room.roomId)
      context.become(receive)
    case SendMsg(msg) =>
      outRef ! msg
    case TextMessage.Strict(msg) =>
      onMessageCallback(msg)

    case OnMsg(callback) => onMessageCallback = callback
  }




}