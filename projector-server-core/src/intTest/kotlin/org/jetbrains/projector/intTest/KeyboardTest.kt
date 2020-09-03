/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
package org.jetbrains.projector.intTest

import com.codeborne.selenide.Condition.appear
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import org.jetbrains.projector.server.core.convert.toAwt.toAwtKeyEvent
import java.awt.event.KeyEvent
import javax.swing.JLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KeyboardTest {

  private companion object {

    private fun withReadableException(events: List<KeyEvent?>, checks: (events: List<KeyEvent?>) -> Unit) {
      try {
        checks(events)
      }
      catch (e: AssertionError) {
        throw AssertionError("exception when checking the following events (see cause): $events", e)
      }
    }

    private fun checkEvent(actual: KeyEvent?, id: Int, keyCode: Int, keyChar: Char, keyLocation: Int) {
      assertNotNull(actual)
      assertEquals(id, actual.id)
      assertEquals(keyCode, actual.keyCode)
      // keyText is generated from keyCode so no need to compare it
      assertEquals(keyChar, actual.keyChar)
      assertEquals(keyLocation, actual.keyLocation)
      // todo: check modifiers
    }
  }

  @Test
  fun testSimpleSymbol() {
    val keyEvents = Channel<List<KeyEvent?>>()

    val server = startServerAndDoHandshake { (sender, receiver) ->
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

      while (true) {
        val list = mutableListOf<KeyEvent?>()

        val events = receiver()
        events.forEach {
          when (it) {
            is ClientKeyPressEvent -> list.add(it.toAwtKeyEvent(0, JLabel()) { println(it()) })
            is ClientKeyEvent -> list.add(it.toAwtKeyEvent(0, JLabel()) { println(it()) })
            else -> Unit
          }
        }

        if (list.isNotEmpty()) {
          keyEvents.send(list)
        }
      }
    }
    server.start()

    open(clientUrl)
    element(".window").should(appear)

    element("body").sendKeys("h")  // test letter "h"

    val events = runBlocking { keyEvents.receive() }

    // expected (tested "h" click in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=72,keyText=H,keyChar='h',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='h',keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=72,keyText=H,keyChar='h',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0

    withReadableException(events) {
      assertEquals(3, events.size)
      checkEvent(it[0], KeyEvent.KEY_PRESSED, 72, 'h', KeyEvent.KEY_LOCATION_STANDARD)
      checkEvent(it[1], KeyEvent.KEY_TYPED, 0, 'h', KeyEvent.KEY_LOCATION_UNKNOWN)
      checkEvent(it[2], KeyEvent.KEY_RELEASED, 72, 'h', KeyEvent.KEY_LOCATION_STANDARD)
    }

    server.stop(500, 1000)
  }
}
