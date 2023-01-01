/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
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
package org.jetbrains.projector.intTest.headless

import com.codeborne.selenide.ClickOptions
import com.codeborne.selenide.Condition.appear
import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide.*
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloseBlockingTest {

  private companion object {

    private fun openClientAndActivatePage() {
      open(clientUrl)
      element("body").click(ClickOptions.usingDefaultMethod())  // enable onbeforeunload listener, can't click without arguments because of an exception
    }
  }

  @Test
  fun shouldBeAbleToCloseBefore() {
    openClientAndActivatePage()
    element("body").shouldHave(text("reconnect"))
    refresh()
    assertFalse(isAlertPresent())
  }

  @Test
  fun shouldBeUnableToCloseWhenConnected() {
    val clientLoadNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake { (sender, _) ->
      val window = WindowData(
        id = 1,
        isShowing = true,
        zOrder = 0,
        bounds = CommonRectangle(10.0, 10.0, 100.0, 100.0),
        resizable = true,
        modal = false,
        undecorated = false,
        windowType = WindowType.IDEA_WINDOW
      )

      sender(listOf(ServerWindowSetChangedEvent(listOf(window))))

      clientLoadNotifier.send(Unit)

      for (frame in incoming) {
        // maintaining connection
      }
    }
    server.start()

    openClientAndActivatePage()

    runBlocking {
      clientLoadNotifier.receive()
    }
    element("canvas.window").should(appear)

    refresh()
    assertTrue(isAlertPresent())
    confirm()

    server.stop(500, 1000)
  }

  @Test
  fun shouldBeAbleToCloseAfterConnectionEnds() {
    val connectionEndedNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake {
      close()
      connectionEndedNotifier.send(Unit)
    }
    server.start()

    openClientAndActivatePage()

    runBlocking {
      connectionEndedNotifier.receive()
    }
    element("body").shouldHave(text("ended"))

    refresh()
    assertFalse(isAlertPresent())

    server.stop(500, 1000)
  }
}
