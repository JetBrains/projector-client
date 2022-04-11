/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import org.junit.jupiter.api.condition.OS
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTextArea

// todo: test not only IME
open class LowLevelKeyboardTest : AnnotationSpec() {

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

    private fun List<KeyEvent>.toPrettyString(): String = joinToString(",\n", "[\n", "\n]") {
      val type = when (it.id) {
        KeyEvent.KEY_PRESSED -> "PRESSED"
        KeyEvent.KEY_TYPED -> "TYPED"
        KeyEvent.KEY_RELEASED -> "RELEASED"
        else -> "${it.id}"
      }
      val keyText = KeyEvent.getKeyText(it.keyCode)
      val location = when (it.keyLocation) {
        KeyEvent.KEY_LOCATION_STANDARD -> "STANDARD"
        KeyEvent.KEY_LOCATION_NUMPAD -> "NUMPAD"
        KeyEvent.KEY_LOCATION_LEFT -> "LEFT"
        KeyEvent.KEY_LOCATION_UNKNOWN -> "UNKNOWN"
        KeyEvent.KEY_LOCATION_RIGHT -> "RIGHT"
        else -> "${it.keyLocation}"
      }
      val modifiersEx = KeyEvent.getModifiersExText(it.modifiersEx)
      "KeyEvent[$type,keyCode=${it.keyCode},keyText=$keyText,keyChar=${it.keyChar} (${it.keyChar.code}),keyLocation=$location,modifiersEx=$modifiersEx]"
    } + " (size: $size)"

    private fun waitABit() {
      runBlocking { delay(100) }
    }
  }

  @BeforeEach
  open fun setupBrowser() {
    Configuration.browserPosition = "200x200"
    Configuration.browserSize = "200x200"
  }

  @OptIn(ExperimentalStdlibApi::class)
  fun test(expectedInputText: String, input: Robot.() -> Unit) {
    val robot = Robot().apply {
      autoDelay = 20
    }

    val events = Channel<List<Any>>()

    val server = createServerAndReceiveKeyEvents(events)
    server.start()

    try {
      open("$clientUrl&inputMethod=ime")
      element(".window").should(appear)

      input(robot)

      waitABit()
      robot.waitForIdle()

      val allEvents = mutableListOf<Any>()
      runBlocking {
        try {
          withTimeout(100) {
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

      val keyEvents = allEvents.filterIsInstance<KeyEvent>()
      withClue("Different sizes: keys $keyEvents, all $allEvents") {
        keyEvents.shouldBeSameSizeAs(allEvents)
      }

      val expectedEvents = mutableListOf<KeyEvent>()

      val frame = JFrame("Input receiver")
      try {
        val textArea = JTextArea()  // JTextField doesn't work here since it doesn't support inputting symbols like Tabs
        frame.add(textArea)
        textArea.requestFocusInWindow()
        textArea.addKeyListener(object : KeyListener {
          override fun keyTyped(e: KeyEvent) {
            expectedEvents.add(e)
          }

          override fun keyPressed(e: KeyEvent) {
            expectedEvents.add(e)
          }

          override fun keyReleased(e: KeyEvent) {
            expectedEvents.add(e)
          }
        })

        frame.isVisible = true

        waitABit()

        if (OS.MAC.isCurrentOs) {
          // switch windows manually
          robot.mouseMove(frame.x, frame.y)
          robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK)
          robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK)

          waitABit()
        }

        input(robot)

        waitABit()
        robot.waitForIdle()

        if (OS.MAC.isCurrentOs) {
          // switch windows manually
          robot.mouseMove(300, 300)
          robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK)
          robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK)

          waitABit()
        }

        withClue("No expected events received (actual key events size: ${keyEvents.size})...") {
          expectedEvents.size shouldNotBe 0
        }
        withClue("Wrong text is input") {
          textArea.text shouldBe expectedInputText
        }
        withClue("Different sizes: actual ${keyEvents.toPrettyString()}, expected ${expectedEvents.toPrettyString()}") {
          keyEvents.size shouldBe expectedEvents.size
        }

        expectedEvents.zip(keyEvents).forEachIndexed { i, (expected, actual) ->
          try {
            actual.id shouldBe expected.id
            actual.modifiersEx shouldBe expected.modifiersEx
            actual.keyCode shouldBe expected.keyCode
            withClue("expected int: ${expected.keyChar.code} but was int: ${actual.keyChar.code}") {
              actual.keyChar shouldBe expected.keyChar
            }
            actual.keyLocation shouldBe expected.keyLocation
          }
          catch (e: AssertionError) {
            throw AssertionError(
              "exception when comparing the event #$i: actual ${keyEvents.toPrettyString()}, expected ${expectedEvents.toPrettyString()}",
              e)
          }
        }
      }
      finally {
        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
      }
    }
    finally {
      server.stop(500, 1000)
    }
  }

  @Test
  fun `'h' should be pressed and released`() = test("h") {
    keyPress(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_H)
  }

  @Test
  fun `'H' should be pressed and released`() = test("H") {
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_SHIFT)
  }

  @Test
  fun `Tab should be pressed and released`() = test("\t") {
    keyPress(KeyEvent.VK_TAB)
    keyRelease(KeyEvent.VK_TAB)
  }

  @Test
  fun `Enter should be pressed and released`() = test("\n") {
    keyPress(KeyEvent.VK_ENTER)
    keyRelease(KeyEvent.VK_ENTER)
  }

  @Test
  fun `Backspace should be pressed and released`() = test("") {
    keyPress(KeyEvent.VK_BACK_SPACE)
    keyRelease(KeyEvent.VK_BACK_SPACE)
  }

  @Test
  fun `Spase should be pressed and released`() = test(" ") {
    keyPress(KeyEvent.VK_SPACE)
    keyRelease(KeyEvent.VK_SPACE)
  }

  @Test
  fun `Escape should be pressed and released`() = test("") {
    keyPress(KeyEvent.VK_ESCAPE)
    keyRelease(KeyEvent.VK_ESCAPE)
  }

  @Test
  fun `Delete should be pressed and released`() = test("") {
    keyPress(KeyEvent.VK_DELETE)
    keyRelease(KeyEvent.VK_DELETE)
  }

  @Test
  fun `Letter should be pressed and released with Ctrl modifier`() = test("") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_Z)
    keyRelease(KeyEvent.VK_Z)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  @Test
  fun `functional key should be pressed and released`() = test("") {
    keyPress(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_F6)
  }

  @Test
  fun `functional key should be pressed and released with Shift modifier`() = test("") {
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_SHIFT)
  }

  @Test
  fun `letter should be pressed and released with Shift and Ctrl modifier`() = test("") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_K)
    keyRelease(KeyEvent.VK_K)
    keyRelease(KeyEvent.VK_SHIFT)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  @Test
  fun `arrow should be pressed and released`() = test("") {
    keyPress(KeyEvent.VK_RIGHT)
    keyRelease(KeyEvent.VK_RIGHT)
  }
}
