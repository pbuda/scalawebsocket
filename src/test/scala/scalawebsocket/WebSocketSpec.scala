/*
* Copyright 2012 Jeanfrancois Arcand
* Copyright 2013 Piotr Buda
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
*
* Changes are made to the test code to use different API of the WebSocket.
*/

package scalawebsocket

import java.io.IOException
import javax.servlet.http.HttpServletRequest
import com.typesafe.scalalogging.log4j.Logging
import java.util.concurrent.{TimeUnit, CountDownLatch}
import com.ning.http.client.{AsyncHttpClient, websocket}
import org.scalatest.matchers.{MatchResult, BeMatcher}


class WebSocketSpec extends BaseTest with Logging {

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

  def getWebSocketHandler: BaseTest#WebSocketHandler = {
    new WebSocketHandler {
      def doWebSocketConnect(httpServletRequest: HttpServletRequest, s: String): org.eclipse.jetty.websocket.WebSocket = {
        new EchoTextWebSocket
      }
    }
  }

  it should "call all onOpen handlers" in {
    val latch = new CountDownLatch(3)
    WebSocket()
      .onOpen(ws => latch.countDown())
      .onOpen(ws => latch.countDown())
      .onOpen(ws => latch.countDown())
      .open(getTargetUrl)
    waitForHandlersToExecute(latch)
  }

  def waitForHandlersToExecute(latch: CountDownLatch) {
    class CountDownLatchMatcher extends BeMatcher[Boolean] {
      def apply(left: Boolean): MatchResult = {
        MatchResult(left, "Not all handlers were executed", "All handlers were executed")
      }
    }
    val completed = new CountDownLatchMatcher
    latch.await(10, TimeUnit.SECONDS) should be(completed)
  }

  it should "remove specified onOpen handler" in {
    def openHandler(ws: websocket.WebSocket) {
      throw new IllegalStateException("This handler should be removed")
    }
    val handler = openHandler _
    WebSocket().onOpen(handler).removeOnOpen(handler).open(getTargetUrl)
  }

  it should "call all onClose handlers" in {
    val latch = new CountDownLatch(3)
    WebSocket()
      .onClose(ws => latch.countDown())
      .onClose(ws => latch.countDown())
      .onClose(ws => latch.countDown())
      .open(getTargetUrl)
      .close()
    waitForHandlersToExecute(latch)
  }

  it should "remove specified onClose handler" in {
    def closeHandler(ws: websocket.WebSocket) {
      throw new IllegalStateException("This handler should be removed")
    }
    val handler = closeHandler _
    WebSocket().onClose(handler).removeOnClose(handler).open(getTargetUrl).close()
  }

  it should "call all onTextMessage handlers" in {
    val latch = new CountDownLatch(3)
    WebSocket()
      .onTextMessage(message => latch.countDown())
      .onTextMessage(message => latch.countDown())
      .onTextMessage(message => latch.countDown())
      .open(getTargetUrl)
      .sendText("message")
    waitForHandlersToExecute(latch)
  }

  it should "remove specified onTextMessage handler" in {
    def textHandler(text: String) {
      throw new IllegalStateException("This handler should be removed")
    }
    val handler = textHandler _
    WebSocket().onTextMessage(handler).removeOnTextMessage(handler).open(getTargetUrl).sendText("some message")
  }

  it should "call all onBinaryMessage handlers" in {
    val latch = new CountDownLatch(3)
    WebSocket()
      .onBinaryMessage(msg => latch.countDown())
      .onBinaryMessage(msg => latch.countDown())
      .onBinaryMessage(msg => latch.countDown())
      .open(getTargetUrl)
      .send("binary".getBytes)
    waitForHandlersToExecute(latch)
  }

  it should "remove specified onBinaryMessage handler" in {
    def binaryHandler(message: Array[Byte]) {
      throw new IllegalStateException("This handler should be removed")
    }
    val handler = binaryHandler _
    WebSocket().onBinaryMessage(handler).removeOnBinaryMessage(handler).open(getTargetUrl).send("binary".getBytes)
  }

  it should "send text message" in {
    val latch = new CountDownLatch(1)
    var received = ""
    WebSocket().onTextMessage(
      message => {
        received = message
        latch.countDown()
      }).open(getTargetUrl).sendText("test message")
    waitForHandlersToExecute(latch)
    received should be("test message")
  }

  it should "send binary message" in {
    val message = Array[Byte](0, 1)
    val latch = new CountDownLatch(1)
    var received = Array[Byte]()
    WebSocket().onBinaryMessage(message => {
      received = message
      latch.countDown()
    }).open(getTargetUrl).send(message)
    waitForHandlersToExecute(latch)
    received should be(message)
  }

  it should "shut down correctly" in {
    val client = new AsyncHttpClient()
    val ws = new WebSocket(client)
    ws.shutdown()
    client should be('closed)
  }

  it should "allow new connection after closing" in {
    def errorHandler(error: Throwable) {
      throw new IllegalStateException("Problem reestablishing connection")
    }
    val latch = new CountDownLatch(1)
    WebSocket().onError(errorHandler).open(getTargetUrl).close().open(getTargetUrl)
      .onTextMessage(msg => latch.countDown()).sendText("echo")
    waitForHandlersToExecute(latch)
  }

  it should "not allow connection after shutdown" in {
    val ws = WebSocket()
    ws.shutdown()
    intercept[IllegalStateException] {
      ws.open(getTargetUrl)
    }
  }

  it should "not allow sending text message after close" in {
    val ws = WebSocket().open(getTargetUrl).close()
    intercept[IllegalStateException] {
      ws.sendText("text")
    }
  }

  it should "not allow sending binary message after close" in {
    val ws = WebSocket().open(getTargetUrl).close()
    intercept[IllegalStateException] {
      ws.send("text".getBytes)
    }
  }

  it should "not allow non ws/wss schemes connection" in {
    intercept[IllegalArgumentException] {
      WebSocket().open("https://localhost")
    }
  }

}
