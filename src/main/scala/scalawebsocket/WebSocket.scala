/*
 * Copyright 2013 Piotr Buda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalawebsocket

import com.ning.http.client.{websocket, AsyncHttpClient}
import com.ning.http.client.websocket.{WebSocketTextListener, WebSocketByteListener, WebSocketUpgradeHandler}
import com.typesafe.scalalogging.log4j.Logging

object WebSocket {
  def apply() = {
    new WebSocket(new AsyncHttpClient())
  }
}

/** WebSocket wrapper for [[com.ning.http.client.AsyncHttpClient]]
  *
  * This is a thin wrapper for [[com.ning.http.client.AsyncHttpClient]] that exposes a functional API
  * for attaching handlers to events that [[com.ning.http.client.AsyncHttpClient]] exposes.
  *
  * You can chain nearly every method in this class. The single exception is the shutdown()
  * method, which ends the life of this websocket. After executing it, you need to create a new instance of [[scalawebsocket.WebSocket]]
  *
  * @param client preconfigured instance of the [[com.ning.http.client.AsyncHttpClient]]
  */
class WebSocket(client: AsyncHttpClient) extends Logging {

  type OnTextMessageHandler = String => Unit
  type OnBinaryMessageHandler = Array[Byte] => Unit
  type OnWebSocketOperationHandler = websocket.WebSocket => Unit
  type OnErrorHandler = Throwable => Unit

  private var ws: websocket.WebSocket = _
  private var textMessageHandlers = List[OnTextMessageHandler]()
  private var binaryMessageHandlers = List[OnBinaryMessageHandler]()
  private var openHandlers = List[OnWebSocketOperationHandler]()
  private var closeHandlers = List[OnWebSocketOperationHandler]()
  private var errorHandlers = List[OnErrorHandler]()

  /** Open a websocket to the specified url
    *
    * @param url url to connecto to
    * @return this [[scalawebsocket.WebSocket]]
    */
  def open(url: String): WebSocket = {
    if (client.isClosed) throw new IllegalStateException("Client is closed, please create a new scalawebsocket.WebSocket instance by calling WebSocket()")
    val handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(internalWebSocketListener).build()
    ws = client.prepareGet(url).execute(handler).get()
    this
  }

  /** Creates internal websocket listener.
    *
    * This method creates the [[scalawebsocket.WebSocketListener]] and runs proper handlers on messages sent by that created listener.
    *
    * @return an instance of [[scalawebsocket.WebSocketListener]]
    */
  protected def internalWebSocketListener = {
    new WebSocketListener {
      def onError(t: Throwable) {
        errorHandlers foreach (_(t))
      }

      def onMessage(message: String) {
        textMessageHandlers foreach (_(message))
      }

      def onMessage(message: Array[Byte]) {
        binaryMessageHandlers foreach (_(message))
      }

      def onClose(ws: websocket.WebSocket) {
        closeHandlers foreach (_(ws))
      }

      def onOpen(ws: websocket.WebSocket) {
        openHandlers foreach (_(ws))
      }

      def onFragment(fragment: String, last: Boolean) {
        logger.trace("noop")
      }

      def onFragment(fragment: Array[Byte], last: Boolean) {
        logger.trace("noop")
      }
    }
  }

  def onTextMessage(handler: OnTextMessageHandler): WebSocket = {
    textMessageHandlers ::= handler
    this
  }

  def removeOnTextMessage(handler: OnTextMessageHandler): WebSocket = {
    textMessageHandlers = textMessageHandlers filterNot (_ == handler)
    this
  }

  def onBinaryMessage(handler: OnBinaryMessageHandler): WebSocket = {
    binaryMessageHandlers ::= handler
    this
  }

  def removeOnBinaryMessage(handler: OnBinaryMessageHandler): WebSocket = {
    binaryMessageHandlers = binaryMessageHandlers filterNot (_ == handler)
    this
  }

  def onOpen(handler: OnWebSocketOperationHandler): WebSocket = {
    openHandlers ::= handler
    this
  }

  def removeOnOpen(handler: OnWebSocketOperationHandler): WebSocket = {
    openHandlers = openHandlers filterNot (_ == handler)
    this
  }

  def onClose(handler: OnWebSocketOperationHandler): WebSocket = {
    closeHandlers ::= handler
    this
  }

  def removeOnClose(handler: OnWebSocketOperationHandler): WebSocket = {
    closeHandlers = closeHandlers filterNot (_ == handler)
    this
  }

  def onError(handler: OnErrorHandler): WebSocket = {
    errorHandlers ::= handler
    this
  }

  def removeOnError(handler: OnErrorHandler): WebSocket = {
    errorHandlers = errorHandlers filterNot (_ == handler)
    this
  }

  def sendText(message: String): WebSocket = {
    if (ws.isOpen) ws.sendTextMessage(message)
    else throw new IllegalStateException("WebSocket is closed, use WebSocket.open(String) to reconnect)")
    this
  }

  def send(message: Array[Byte]): WebSocket = {
    if (ws.isOpen) ws.sendMessage(message)
    else throw new IllegalStateException("WebSocket is closed, use WebSocket.open(String) to reconnect)")
    this
  }

  def close(): WebSocket = {
    ws.close()
    this
  }

  /** Closes the underlying [[com.ning.http.client.AsyncHttpClient]] client.
    *
    * This method is terminating the chain. After calling it, this instance of [[scalawebsocket.WebSocket]] is no longer useable.
    */
  def shutdown() {
    client.close()
  }

}

/** Trait grouping all supported derivatives of [[com.ning.http.client.websocket.WebSocketListener]]
  *
  */
trait WebSocketListener extends WebSocketByteListener with WebSocketTextListener
