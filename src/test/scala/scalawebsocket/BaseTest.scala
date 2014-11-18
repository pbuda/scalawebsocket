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
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ConfigMap, Matchers, FlatSpec, BeforeAndAfterAllConfigMap}
import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.websocket.WebSocketFactory
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.ServerSocket

abstract class BaseTest extends FlatSpec with BeforeAndAfterAllConfigMap with Matchers with LazyLogging {
  private[this] val server = new Server()
  protected var port1: Int = 0
  private var _connector: SelectChannelConnector = null

  override def beforeAll(configMap: ConfigMap) {
    setUpGlobal()
  }

  override def afterAll(configMap: ConfigMap) {
    tearDownGlobal()
  }

  def setUpGlobal() {
    port1 = findFreePort
    _connector = new SelectChannelConnector
    _connector.setPort(port1)
    server.addConnector(_connector)
    val _wsHandler: BaseTest#WebSocketHandler = getWebSocketHandler
    server.setHandler(_wsHandler)
    server.start()
    logger.info("Local HTTP server started successfully")
  }

  def tearDownGlobal() {
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

  protected def findFreePort: Int = {
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      socket.getLocalPort
    } finally {
      if (socket != null) {
        socket.close()
      }
    }
  }

  protected def getTargetUrl: String = {
    "ws://127.0.0.1:" + port1
  }

  def getWebSocketHandler: BaseTest#WebSocketHandler
}
