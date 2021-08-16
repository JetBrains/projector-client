/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

class WebsocketKotlin extends Simulation {
  val httpProtocol: HttpProtocolBuilder =
    http.baseUrl("http://localhost:8887")
      .wsBaseUrl("ws://localhost:8887")

  val scn: ScenarioBuilder = scenario("test")
    .exec(http("first").get("/"))
    .pause(1)
    .exec(ws("openSocket")
      .connect("/projector/")
      .onConnected(
        exec(
          ws("some numbers")
            .sendText("456250626;0")
        )
          exec(
          ws("init")
            .sendText(
              """{"commonVersion":1580903519,
                |"commonVersionId":9,
                |"clientDoesWindowManagement":false,
                |"displays":[{"x":0,"y":0,"width":1048,"height":1233,"scaleFactor":1}],"supportedToClientCompressions":["NONE"],"supportedToClientProtocols":["KOTLINX_JSON"],"supportedToServerCompressions":["NONE"],"supportedToServerProtocols":["KOTLINX_JSON"]}""".stripMargin)
          )
          exec ws("fonts")
          .sendText("Unused string meaning fonts loading is done")
          exec ws("linux")
          .sendText(
            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientSetKeymapEvent",
              |"keymap":"LINUX"}]""".stripMargin)
          exec ws("EnterDown")
          .sendText(
            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent","timeStamp":391765,"char":"\n","code":"ENTER","location":"STANDARD","modifiers":[],"keyEventType":"DOWN"},{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent","timeStamp":391765,"char":"\n","modifiers":[]}]""")
          exec ws("EnterUp")
          .sendText(
            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent","timeStamp":391905,"char":"\n","code":"ENTER","location":"STANDARD","modifiers":[],"keyEventType":"UP"}]""")

        //          exec ws("p down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"p",
        //              |"code":"P",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"p",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("p up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"p",
        //              |"code":"P",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("r down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"r",
        //              |"code":"R",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"r",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("r up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"r",
        //              |"code":"R",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("i down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"i",
        //              |"code":"I",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":576818,
        //              |"char":"i",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("i up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"i",
        //              |"code":"I",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("n down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"code":"N",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("n up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"code":"N",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("t down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"t",
        //              |"code":"T",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"t",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("t up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"t",
        //              |"code":"T",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("l down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"l",
        //              |"code":"L",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"l",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("l up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"l",
        //              |"code":"L",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("n down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"code":"N",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("n up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"n",
        //              |"code":"N",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("SHIFT down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"}]""".stripMargin)
        //          exec ws("9 down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"(",
        //              |"code":"D9",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"(",
        //              |"modifiers":["SHIFT_KEY"]}]""".stripMargin)
        //          exec ws("9 up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":21146,
        //              |"char":"(",
        //              |"code":"D9",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //          exec ws("SHIFT Up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":21179,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("SHIFT down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"}]""".stripMargin)
        //          exec ws(" ' down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"\"",
        //              |"code":"QUOTE",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"\"",
        //              |"modifiers":["SHIFT_KEY"]}]""".stripMargin)
        //          exec ws(" ' up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"\"",
        //              |"code":"QUOTE",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //          exec ws("SHIFT Up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("o down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"o",
        //              |"code":"O",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"DOWN"},
        //              |{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent",
        //              |"timeStamp":0,
        //              |"char":"o",
        //              |"modifiers":[]}]""".stripMargin)
        //          exec ws("o up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"o",
        //              |"code":"O",
        //              |"location":"STANDARD",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //
        //          exec ws("SHIFT down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"}]""".stripMargin)
        //          exec ws("F10 down")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"F10",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"DOWN"}]""".stripMargin)
        //          exec ws("F10 up")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"F10",
        //              |"location":"STANDARD",
        //              |"modifiers":["SHIFT_KEY"],
        //              |"keyEventType":"UP"}]""".stripMargin)
        //          exec ws("SHIFT UP")
        //          .sendText(
        //            """[{"type":"org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent",
        //              |"timeStamp":0,
        //              |"char":"ï¿¿",
        //              |"code":"SHIFT",
        //              |"location":"LEFT",
        //              |"modifiers":[],
        //              |"keyEventType":"UP"}]""".stripMargin)
      )
    )
  exec(ws("close").close)

  setUp(scn
    .inject(atOnceUsers(1))
    .protocols(httpProtocol))
}
