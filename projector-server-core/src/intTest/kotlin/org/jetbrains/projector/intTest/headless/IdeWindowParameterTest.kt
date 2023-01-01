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

import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Selenide.elements
import com.codeborne.selenide.Selenide.open
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import kotlin.test.Test

class IdeWindowParameterTest {

  private companion object {

    private val windows = listOf(
      WindowData(
        id = 1,
        isShowing = true,
        zOrder = 0,
        bounds = CommonRectangle(10.0, 10.0, 100.0, 100.0),
        resizable = true,
        modal = false,
        undecorated = false,
        windowType = WindowType.IDEA_WINDOW
      ),
      WindowData(
        id = 2,
        isShowing = true,
        zOrder = 0,
        bounds = CommonRectangle(200.0, 20.0, 100.0, 100.0),
        resizable = true,
        modal = false,
        undecorated = false,
        windowType = WindowType.IDEA_WINDOW
      )
    )
  }

  @Test
  fun shouldShowAllWindowsWhenParameterIsNotPresented() {
    val clientLoadNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake { (sender, _) ->
      sender(listOf(ServerWindowSetChangedEvent(windows)))

      clientLoadNotifier.send(Unit)

      for (frame in incoming) {
        // maintaining connection
      }
    }
    server.start()

    open(clientUrl)

    runBlocking {
      clientLoadNotifier.receive()
    }
    elements("canvas.window").shouldHave(size(2))

    server.stop(500, 1000)
  }

  @Test
  fun shouldShowSelectedWindow0WhenParameterIsPresented() {
    val clientLoadNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake { (sender, _) ->
      sender(listOf(ServerWindowSetChangedEvent(windows)))

      clientLoadNotifier.send(Unit)

      for (frame in incoming) {
        // maintaining connection
      }
    }
    server.start()

    open("$clientUrl&ideWindow=0")

    runBlocking {
      clientLoadNotifier.receive()
    }
    elements("canvas.window").shouldHave(size(1))

    server.stop(500, 1000)
  }

  @Test
  fun shouldShowSelectedWindow1WhenParameterIsPresented() {
    val clientLoadNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake { (sender, _) ->
      sender(listOf(ServerWindowSetChangedEvent(windows)))

      clientLoadNotifier.send(Unit)

      for (frame in incoming) {
        // maintaining connection
      }
    }
    server.start()

    open("$clientUrl&ideWindow=1")

    runBlocking {
      clientLoadNotifier.receive()
    }
    elements("canvas.window").shouldHave(size(1))

    server.stop(500, 1000)
  }

  @Test
  fun shouldNotShowNotExistingWindow2WhenParameterIsPresented() {
    val clientLoadNotifier = Channel<Unit>()

    val server = startServerAndDoHandshake { (sender, _) ->
      sender(listOf(ServerWindowSetChangedEvent(windows)))

      clientLoadNotifier.send(Unit)

      for (frame in incoming) {
        // maintaining connection
      }
    }
    server.start()

    open("$clientUrl&ideWindow=2")

    runBlocking {
      clientLoadNotifier.receive()
    }
    elements("canvas.window").shouldHave(size(0))

    server.stop(500, 1000)
  }
}
