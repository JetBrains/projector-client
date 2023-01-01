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
package org.jetbrains.projector.intTest.lowLevelKeyboard

import com.codeborne.selenide.Condition.appear
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.ServerClipboardEvent
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.common.protocol.toServer.*
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import org.junit.jupiter.api.condition.OS
import org.openqa.selenium.Keys
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.Test
import kotlin.test.assertEquals

// todo: test not only IME
class LowLevelClipboardTest {

  private companion object {

    private fun createServerAndReceiveKeyEvents(keyEvents: Channel<List<Any>>): ApplicationEngine {
      return startServerAndDoHandshake { (sender, receiver) ->
        val window = WindowData(
          id = 1,
          isShowing = true,
          zOrder = 0,
          bounds = CommonRectangle(0.0, 0.0, 100.0, 100.0),
          resizable = true,
          modal = false,
          undecorated = false,
          windowType = WindowType.IDEA_WINDOW,
        )

        sender(listOf(ServerWindowSetChangedEvent(listOf(window))))

        while (true) {
          val list = mutableListOf<Any>()

          val events = receiver() ?: break

          events.forEach {
            when (it) {
              is ClientKeyPressEvent -> list.add(it)
              is ClientKeyEvent -> list.add(it)
              is ClientRawKeyEvent -> list.add(it)
              is ClientMouseEvent -> list.add(it)  // check that no mouse events are generated when typing
              is ClientClipboardEvent -> list.add(it)
              else -> Unit
            }
          }

          if (list.isNotEmpty()) {
            keyEvents.send(list)
          }
        }
      }
    }
  }

  @Test
  fun testPaste() {
    val toPaste = "projector-paste"
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(toPaste), null)

    val events = Channel<List<Any>>()

    val server = createServerAndReceiveKeyEvents(events)
    server.start()

    try {
      open("$clientUrl&inputMethod=ime")
      element(".window").should(appear)

      val modifierKey = when (OS.MAC.isCurrentOs) {
        true -> Keys.META
        false -> Keys.CONTROL
      }

      element("body").sendKeys(Keys.chord(modifierKey, "v"))

      runBlocking { delay(500) }

      val allEvents = mutableListOf<Any>()
      runBlocking {
        try {
          withTimeout(500) {
            while (true) {
              val next = events.receive()
              allEvents.addAll(next)
            }
          }
        }
        catch (e: TimeoutCancellationException) {
          // going out of the loop
        }
      }

      val expectedEvents = listOf(
        ClientClipboardEvent(toPaste),
        "${ClientKeyEvent::class.simpleName}(keyEventType=${ClientKeyEvent.KeyEventType.DOWN})",
        "${ClientKeyEvent::class.simpleName}(keyEventType=${ClientKeyEvent.KeyEventType.DOWN})",
        "${ClientKeyPressEvent::class.simpleName}(char=v)",
        "${ClientKeyEvent::class.simpleName}(keyEventType=${ClientKeyEvent.KeyEventType.UP})",
        "${ClientKeyEvent::class.simpleName}(keyEventType=${ClientKeyEvent.KeyEventType.UP})",
      )

      assertEquals(expectedEvents.size, allEvents.size, "Different sizes: expected $expectedEvents, all $allEvents")

      val comparableEvents = allEvents.map {
        when (it) {
          is ClientKeyEvent -> "${ClientKeyEvent::class.simpleName}(keyEventType=${it.keyEventType})"
          is ClientKeyPressEvent -> "${ClientKeyPressEvent::class.simpleName}(char=${it.char})"
          else -> it
        }
      }

      assertEquals(expectedEvents, comparableEvents)
    }
    finally {
      server.stop(500, 1000)
    }
  }

  @Test
  fun testCopy() {
    val initialClipboard = "projector-paste"
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(initialClipboard), null)

    val toCopy = "projector-copy"
    val server = startServerAndDoHandshake { (sender, receiver) ->
      val window = WindowData(
        id = 1,
        isShowing = true,
        zOrder = 0,
        bounds = CommonRectangle(0.0, 0.0, 100.0, 100.0),
        resizable = true,
        modal = false,
        undecorated = false,
        windowType = WindowType.IDEA_WINDOW,
      )

      sender(listOf(ServerWindowSetChangedEvent(listOf(window))))

      sender(listOf(ServerClipboardEvent(toCopy)))

      while (true) {
        receiver() ?: break
      }
    }
    server.start()

    try {
      open("$clientUrl&inputMethod=ime")
      element(".window").should(appear)

      runBlocking { delay(500) }

      val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getContents(null).getTransferData(DataFlavor.stringFlavor).toString()

      assertEquals(toCopy, clipboard)
    }
    finally {
      server.stop(500, 1000)
    }
  }
}
