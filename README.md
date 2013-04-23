ScalaWebSocket
==============

ScalaWebSocket provides Scala vocabulary for async-http-client's WebSocket support.

Currently compiled against Scala 2.10.0

Current version is **0.1.0**

[![Build Status](https://travis-ci.org/pbuda/scalawebsocket.png)](https://travis-ci.org/pbuda/scalawebsocket)

Installation
============

ScalaWebSocket lives in Sonatype's repositories and releases are synced to Maven Central. To use snapshot versions,
simply add Sonatype snapshot repository to resolvers.

```
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
```

#### Using SBT

```
libraryDependncies += "eu.piotrbuda" %% "scalawebsocket" % "0.1.0"
```

#### Using Maven
```
<dependency>
  <groupId>eu.piotrbuda</groupId>
  <artifactId>scalawebsocket_2.10</artifactId>
  <version>0.1.0</version>
</dependency>
```

Example
=======

#### Open connection and send message

```scala
    WebSocket().open("ws://echo.websocket.org/").sendText("text").close().shutdown()
```

#### Listen for text messages

```scala
    WebSocket().open("ws://echo.websocket.org/").onTextMessage(msg => doSomethingWithMessage(msg))
```

#### Add and then remove handler

To be able to remove a handler from WebSocket, it has to be a named handler.

```scala
    val handler = {
      msg: String => doSomethingWithMessage(msg)
    }
    WebSocket.open(url).onTextMessage(handler).sendText("text").removeOnTextMessage(handler)
```

Credits
=======
Credit has to go to @jfarcand as I used testing part of his [WCS project](https://github.com/jfarcand/WCS) project.

License
=======
    Copyright 2013 Piotr Buda

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.