package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.zeromq.ZMQMessage
import com.ibm.spark.client.CallbackMap
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json
/**
 * The client endpoint for IOPub messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class IOPubClient(socketFactory: SocketFactory) extends Actor with LogLike {
  val socket  = socketFactory.IOPubClient(context.system, self)

  override def receive: Receive = {
    case message: ZMQMessage =>
      // convert to KernelMessage using implicits in v5
      val kernelMessage: KernelMessage = message
      val id = kernelMessage.header.msg_id

      // look up callback in CallbackMap based on msg_id and invoke
      val callback = CallbackMap.get(id)

      if(callback != None) {
        val messageType: MessageType = MessageType.withName(kernelMessage.header.msg_type)
        messageType match {
          case MessageType.ExecuteResult =>
            //  Get the callback from the map and pass the ExecuteResult as a param
            callback.get { Json.parse(kernelMessage.contentString).as[ExecuteResult] }
            CallbackMap.remove(id)
        }
      }
  }
}
