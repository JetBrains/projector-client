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
package org.jetbrains.projector.intTest.headless.keyboard

import com.codeborne.selenide.Condition.appear
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.ClientMouseEvent
import org.jetbrains.projector.common.protocol.toServer.ClientRawKeyEvent
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import org.jetbrains.projector.server.core.convert.toAwt.toAwtKeyEvent
import org.jetbrains.projector.util.logging.loggerFactory
import org.openqa.selenium.Keys
import java.awt.event.KeyEvent
import javax.swing.JLabel
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Ignore
abstract class AbstractKeyboardTest(private val inputMethod: String) {

  private companion object {

    private fun testWithReadableException(events: List<Any>, tests: (events: List<Any>) -> Unit) {
      try {
        tests(events)
      }
      catch (e: AssertionError) {
        val eventsString = events.joinToString(separator = "\n", prefix = "[\n", postfix = "\n]")
        throw AssertionError("exception when checking the following events (see cause): $eventsString", e)
      }
    }

    private fun checkEvent(actual: Any?, id: Int, keyCode: Int, keyChar: Char, keyLocation: Int?, modifiersEx: Int) {
      check(actual is KeyEvent)  // todo: replace with assertIsType
      assertEquals(id, actual.id)
      assertEquals(keyCode, actual.keyCode)
      // keyText is generated from keyCode so no need to compare it
      assertEquals(keyChar, actual.keyChar, "expected int: ${keyChar.code} but was int: ${actual.keyChar.code}")
      keyLocation?.let { assertEquals(it, actual.keyLocation) }
      ?: loggerFactory("checkEvent").info { "Skipping keyLocation check for $actual" }
      if (modifiersEx >= 0) {
        assertEquals(modifiersEx, actual.modifiersEx)
      }
    }

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
          windowType = WindowType.IDEA_WINDOW
        )

        sender(listOf(ServerWindowSetChangedEvent(listOf(window))))

        while (true) {
          val list = mutableListOf<Any>()

          val events = receiver() ?: break

          events.forEach {
            when (it) {
              is ClientKeyPressEvent -> list.add(it.toAwtKeyEvent(0, JLabel()))
              is ClientKeyEvent -> list.add(it.toAwtKeyEvent(0, JLabel()))
              is ClientRawKeyEvent -> list.add(it.toAwtKeyEvent(0, JLabel()))
              is ClientMouseEvent -> list.add(it)  // check that no mouse events are generated when typing
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

  protected abstract fun input(
    vararg keysToSend: CharSequence,
    ctrl: Boolean,
    shift: Boolean,
    f: Keys?,
    esc: Boolean,
  )

  @OptIn(ExperimentalStdlibApi::class)
  private fun test(
    vararg keysToSend: CharSequence,
    ctrl: Boolean = false,
    shift: Boolean = false,
    expectedEvents: Int? = null,  // todo: get rid of expectedEvents and tester, pass a list with expected events
    f: Keys? = null,
    esc: Boolean = false,
    tester: (events: List<Any>) -> Unit,
  ) {
    val keyEvents = Channel<List<Any>>()

    val server = createServerAndReceiveKeyEvents(keyEvents)
    server.start()

    try {
      open("$clientUrl&inputMethod=$inputMethod")
      element(".window").should(appear)

      input(*keysToSend, ctrl = ctrl, shift = shift, f = f, esc = esc)

      val events = runBlocking {
        buildList {
          try {
            withTimeout(5_000L) {
              do {
                addAll(keyEvents.receive())
              }
              while (expectedEvents?.let { it > this@buildList.size } == true)
            }
          }
          catch (e: TimeoutCancellationException) {
            println("warning: Timeout exceeded but not all events are received!")
          }
        }
      }

      testWithReadableException(events, tester)
    }
    finally {
      server.stop(500, 1000)
    }
  }

  @Test
  open fun testSimpleSymbol() = test("h") {
    // expected (tested "h" press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=72,keyText=H,keyChar='h',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='h',keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=72,keyText=H,keyChar='h',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 72, 'h', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, 'h', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 72, 'h', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testShiftedSimpleSymbol() = test("h", shift = true, expectedEvents = 5) {
    // expected (tested "H" press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=72,keyText=H,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=72,keyText=H,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(5, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT,
               -64)  // todo: the modifier is wrong in WebDriver so skip the check for now by making it negative
    checkEvent(it[1], KeyEvent.KEY_PRESSED, 72, 'H', KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[2], KeyEvent.KEY_TYPED, 0, 'H', KeyEvent.KEY_LOCATION_UNKNOWN, 64)
    checkEvent(it[3], KeyEvent.KEY_RELEASED, 72, 'H', KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[4], KeyEvent.KEY_RELEASED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 0)
  }

  @Test
  open fun testAlreadyShiftedSimpleSymbol() = test("H") {
    // expected (tested "H" press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=72,keyText=H,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=72,keyText=H,keyChar='H',modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(5, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT,
               -64)  // todo: the modifier is wrong in WebDriver so skip the check for now by making it negative
    checkEvent(it[1], KeyEvent.KEY_PRESSED, 72, 'H', KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[2], KeyEvent.KEY_TYPED, 0, 'H', KeyEvent.KEY_LOCATION_UNKNOWN, 64)
    checkEvent(it[3], KeyEvent.KEY_RELEASED, 72, 'H', KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[4], KeyEvent.KEY_RELEASED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 0)
  }

  @Test
  open fun testTab() = test(Keys.TAB) {
    // expected (tested TAB press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=9,keyText=Tab,keyChar=Tab,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=Tab,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=9,keyText=Tab,keyChar=Tab,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 9, '\t', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '\t', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 9, '\t', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testEnter() = test(Keys.ENTER) {  // test ENTER
    // expected (tested Enter press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=10,keyText=Enter,keyChar=Enter,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=Enter,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=10,keyText=Enter,keyChar=Enter,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 10, '\n', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '\n', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 10, '\n', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testBackspace() = test(Keys.BACK_SPACE) {
    // expected (tested Backspace press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=8,keyText=Backspace,keyChar=Backspace,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=Backspace,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=8,keyText=Backspace,keyChar=Backspace,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 8, '\b', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '\b', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 8, '\b', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testSpace() = test(Keys.SPACE) {
    // expected (tested Space press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=32,keyText=Space,keyChar=' ',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=' ',keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=32,keyText=Space,keyChar=' ',keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 32, ' ', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, ' ', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 32, ' ', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testEscape() = test(esc = true) {
    // expected (tested Space press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=27,keyText=Escape,keyChar=Escape,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=Escape,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=27,keyText=Escape,keyChar=Escape,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 27, '\u001b', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '\u001b', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 27, '\u001b', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testDelete() = test(Keys.DELETE) {
    // expected (tested Space press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=127,keyText=Delete,keyChar=Delete,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=Delete,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=127,keyText=Delete,keyChar=Delete,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0

    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 127, '\u007f', KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '\u007f', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 127, '\u007f', KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testCtrlLetter() = test("z", ctrl = true, expectedEvents = 5) {
    // expected (tested Ctrl+Z press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=17,keyText=Ctrl,keyChar=Undefined keyChar,modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 128
    //java.awt.event.KeyEvent[KEY_PRESSED,keyCode=90,keyText=Z,keyChar='',modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 128
    //java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='',modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 128
    //java.awt.event.KeyEvent[KEY_RELEASED,keyCode=90,keyText=Z,keyChar='',modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 128
    //java.awt.event.KeyEvent[KEY_RELEASED,keyCode=17,keyText=Ctrl,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(5, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 17, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 128)
    checkEvent(it[1], KeyEvent.KEY_PRESSED, 90, '', KeyEvent.KEY_LOCATION_STANDARD, 128)
    checkEvent(it[2], KeyEvent.KEY_TYPED, 0, '', KeyEvent.KEY_LOCATION_UNKNOWN, 128)
    checkEvent(it[3], KeyEvent.KEY_RELEASED, 90, '', KeyEvent.KEY_LOCATION_STANDARD, 128)
    checkEvent(it[4], KeyEvent.KEY_RELEASED, 17, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 0)
  }

  @Test
  open fun testFunctionalKey() = test(f = Keys.F6) {
    // expected (tested F6 press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=117,keyText=F6,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=117,keyText=F6,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(2, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 117, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_RELEASED, 117, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testShiftedFunctionalKey() = test(f = Keys.F6, shift = true, expectedEvents = 4) {
    // expected (tested Shift+F6 press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=117,keyText=F6,keyChar=Undefined keyChar,modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=117,keyText=F6,keyChar=Undefined keyChar,modifiers=Shift,extModifiers=Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 64
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(4, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 64)
    checkEvent(it[1], KeyEvent.KEY_PRESSED, 117, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 117, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 64)
    checkEvent(it[3], KeyEvent.KEY_RELEASED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 0)
  }

  @Test
  open fun testCtrlShiftedLetter() = test("k", ctrl = true, shift = true, expectedEvents = 7) {
    // expected (tested Ctrl+Shift+K press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=17,keyText=Ctrl,keyChar=Undefined keyChar,modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 128
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,modifiers=Ctrl+Shift,extModifiers=Ctrl+Shift,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 192
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=75,keyText=K,keyChar='',modifiers=Ctrl+Shift,extModifiers=Ctrl+Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 192
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='',modifiers=Ctrl+Shift,extModifiers=Ctrl+Shift,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 192
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=75,keyText=K,keyChar='',modifiers=Ctrl+Shift,extModifiers=Ctrl+Shift,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on dialog0 192
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=16,keyText=Shift,keyChar=Undefined keyChar,modifiers=Ctrl,extModifiers=Ctrl,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on dialog0 128
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=17,keyText=Ctrl,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_LEFT,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on dialog0 0

    assertEquals(7, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 17, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 128)
    checkEvent(it[1], KeyEvent.KEY_PRESSED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 192)
    checkEvent(it[2], KeyEvent.KEY_PRESSED, 75, '', KeyEvent.KEY_LOCATION_STANDARD, 192)
    checkEvent(it[3], KeyEvent.KEY_TYPED, 0, '', KeyEvent.KEY_LOCATION_UNKNOWN, 192)
    checkEvent(it[4], KeyEvent.KEY_RELEASED, 75, '', KeyEvent.KEY_LOCATION_STANDARD, 192)
    checkEvent(it[5], KeyEvent.KEY_RELEASED, 16, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT,
               -128)  // todo: the modifier is wrong in WebDriver so skip the check for now by making it negative
    checkEvent(it[6], KeyEvent.KEY_RELEASED, 17, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_LEFT, 0)
  }

  @Test
  open fun testArrow() = test(Keys.ARROW_RIGHT) {
    // expected (tested Right Arrow press in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=39,keyText=Right,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=39,keyText=Right,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_STANDARD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0 0

    assertEquals(2, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 39, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 0)
    checkEvent(it[1], KeyEvent.KEY_RELEASED, 39, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD, 0)
  }

  @Test
  open fun testNumpadWithNumLock() = test(Keys.NUMPAD5) {
    // expected (tested NUMPAD5+numlock (5) press on virtual keyboard in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=101,keyText=NumPad-5,keyChar='5',modifiers=Button1,extModifiers=Button1,keyLocation=KEY_LOCATION_NUMPAD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar='5',modifiers=Button1,extModifiers=Button1,keyLocation=KEY_LOCATION_UNKNOWN,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=101,keyText=NumPad-5,keyChar='5',keyLocation=KEY_LOCATION_NUMPAD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0

    // todo: change key location from null to KeyEvent.KEY_LOCATION_NUMPAD when there is a chance to run this test on a pc with numpad.
    //       On a laptop without numpad, the browser sends standard location. If it sends the same even with numpad, we need to fix it
    //       on the client-side.
    assertEquals(3, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 101, '5', null, 0)
    checkEvent(it[1], KeyEvent.KEY_TYPED, 0, '5', KeyEvent.KEY_LOCATION_UNKNOWN, 0)
    checkEvent(it[2], KeyEvent.KEY_RELEASED, 101, '5', null, 0)
  }

  @Test
  open fun testNumpadWithoutNumLock() = test("\uE057") {  // Numpad Home code point: https://www.w3.org/TR/webdriver
    // expected (tested NUMPAD7-numlock (home) press on virtual keyboard in a headful app):
    // java.awt.event.KeyEvent[KEY_PRESSED,keyCode=36,keyText=Home,keyChar=Undefined keyChar,modifiers=Button1,extModifiers=Button1,keyLocation=KEY_LOCATION_NUMPAD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0
    // java.awt.event.KeyEvent[KEY_RELEASED,keyCode=36,keyText=Home,keyChar=Undefined keyChar,keyLocation=KEY_LOCATION_NUMPAD,rawCode=0,primaryLevelUnicode=0,scancode=0,extendedKeyCode=0x0] on frame0

    // todo: change key location from null to KeyEvent.KEY_LOCATION_NUMPAD when there is a chance to run this test on a pc with numpad.
    //       On a laptop without numpad, the browser sends standard location. If it sends the same even with numpad, we need to fix it
    //       on the client-side.
    assertEquals(2, it.size)
    checkEvent(it[0], KeyEvent.KEY_PRESSED, 36, KeyEvent.CHAR_UNDEFINED, null, 0)
    checkEvent(it[1], KeyEvent.KEY_RELEASED, 36, KeyEvent.CHAR_UNDEFINED, null, 0)
  }
}
