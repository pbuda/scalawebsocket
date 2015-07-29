/*
* Copyright 2012 Jeanfrancois Arcand
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package scalawebsocket

import org.eclipse.jetty.server.nio.SelectChannelConnector
import com.typesafe.scalalogging.slf4j._
import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.websocket.{ WebSocketFactory }
import java.io.IOException
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.ServerSocket

trait TestServer extends StrictLogging {
  val server = new Server()
  val connector = new SelectChannelConnector

  def setUpServer() {
    server.addConnector(connector)
    val _wsHandler = getWebSocketHandler
    server.setHandler(_wsHandler)
    server.start()
    logger.info("Local HTTP server started successfully")
  }

  def tearDownServer() {
    server.stop()
  }

  abstract class WebSocketHandler extends HandlerWrapper with WebSocketFactory.Acceptor {

    def getWebSocketFactory: WebSocketFactory = {
      _webSocketFactory
    }

    override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
      if (_webSocketFactory.acceptWebSocket(request, response) || response.isCommitted) return
      super.handle(target, baseRequest, request, response)
    }

    def checkOrigin(request: HttpServletRequest, origin: String): Boolean = {
      true
    }

    private final val _webSocketFactory: WebSocketFactory = new WebSocketFactory(this, 32 * 1024)
  }

  protected def getTargetUrl: String =
    "ws://127.0.0.1:" + connector.getLocalPort()

  private final class EchoTextWebSocket extends org.eclipse.jetty.websocket.WebSocket with org.eclipse.jetty.websocket.WebSocket.OnTextMessage with org.eclipse.jetty.websocket.WebSocket.OnBinaryMessage {
    private var connection: org.eclipse.jetty.websocket.WebSocket.Connection = null

    def onOpen(connection: org.eclipse.jetty.websocket.WebSocket.Connection) {
      this.connection = connection
      connection.setMaxTextMessageSize(1000)
    }

    def onClose(i: Int, s: String) {
      connection.close()
    }

    def onMessage(s: String) {
      try {
        connection.sendMessage(s)
      } catch {
        case e: IOException => {
          try {
            connection.sendMessage("FAIL")
          } catch {
            case e1: IOException => {
              e1.printStackTrace()
            }
          }
        }
      }
    }

    def onMessage(data: Array[Byte], offset: Int, length: Int) {
      try {
        connection.sendMessage(data, offset, length)
      } catch {
        case e: IOException => {
          try {
            connection.sendMessage("FAIL")
          } catch {
            case e1: IOException => {
              e1.printStackTrace()
            }
          }
        }
      }
    }
  }

  def getWebSocketHandler: WebSocketHandler = {
    new WebSocketHandler {
      def doWebSocketConnect(httpServletRequest: HttpServletRequest, s: String): org.eclipse.jetty.websocket.WebSocket = {
        new EchoTextWebSocket
      }
    }
  }
}
