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
package org.jetbrains.projector.intTest.lowLevelKeyboard

import com.codeborne.selenide.Condition.appear
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.data.VK
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.security.Key
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTextArea
import kotlin.test.*

// todo: test not only IME
class kLowLevelKeyboardTest {

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
  }

  @BeforeEach
  fun setupBrowser() {
    Configuration.browserPosition = "200x200"
    Configuration.browserSize = "200x200"
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun test(expectedInputText: String, input: Robot.() -> Unit) {
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

      runBlocking { delay(100) }
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
      assertEquals(allEvents.size, keyEvents.size, "Different sizes: keys $keyEvents, all $allEvents")

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

        runBlocking { delay(100) }

        if (OS.MAC.isCurrentOs) {
          // switch windows manually
          robot.mouseMove(frame.x, frame.y)
          robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK)
          robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK)

          runBlocking { delay(100) }
        }

        input(robot)

        runBlocking { delay(100) }
        robot.waitForIdle()

        if (OS.MAC.isCurrentOs) {
          // switch windows manually
          robot.mouseMove(300, 300)
          robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK)
          robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK)

          runBlocking { delay(100) }
        }

        assertNotEquals(0, expectedEvents.size, "No expected events received (actual key events size: ${keyEvents.size})...")
        assertEquals(expectedInputText, textArea.text, "Wrong text is input")
        assertEquals(expectedEvents.size, keyEvents.size,
                     "Different sizes: actual ${keyEvents.toPrettyString()}, expected ${expectedEvents.toPrettyString()}")

        expectedEvents.zip(keyEvents).forEachIndexed { i, (expected, actual) ->
          try {
            assertEquals(expected.id, actual.id)
            assertEquals(expected.modifiersEx, actual.modifiersEx)
            assertEquals(expected.keyCode, actual.keyCode)
            assertEquals(expected.keyChar, actual.keyChar, "expected int: ${expected.keyChar.code} but was int: ${actual.keyChar.code}")
            assertEquals(expected.keyLocation, actual.keyLocation)
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

  private var shiftedSymbols = arrayOf<Char>('!','@','#','$','%','^','&','*','(',')','_','+','{','}',':','"','|','<','>','?') //qwerty shifted symbols
  //all qwertz symbols
  private var qwertzSymbols = arrayOf<Char>('+','ě','š','č','ř','ž','ý','á','í','é','=','\'','ú',')','ů','§','¨',',','.','-', '1','2','3','4','5','6','7','8','9','0','%','ˇ','/','(','"','!','`','?',':','_')
  //qwertz shifted symbols
  private var qwertzShiftedSymbols = arrayOf<Char>('1','2','3','4','5','6','7','8','9','0','%','ˇ','/','(','"','!','`','?',':','_')
  //all azerty symbols
  private var azertySymbols = arrayOf<Char>('&','é','"','\'','(','§','è','!','ç','à',')','-','m','ù','`',';',':','=', '1','2','3','4','5','6','7','8','9','0','°','_','¨','*','M','%','£','.','/','+')
  //azerty shifted symbols
  private var azertyShiftedSymbols = arrayOf<Char>('1','2','3','4','5','6','7','8','9','0','°','_','¨','*','M','%','£','.','/','+')

  @Test
  fun shiftedLettersTest() {
    for (i in 'A'..'Z') {
      pressKeys(i.toString(), i, null, null)
    }
  }

  @Test
  fun lettersTest() {
    for (i in 'a'..'z') {
      pressKeys(i.toString(), i, null, null)
    }
  }

  @Test
  fun symbolsTest() {
    for (i in '!'..'@') {
      pressKeys(i.toString(), i, null, null)
    }
    for (i in '['..'_') {
      pressKeys(i.toString(), i, null, null)
    }
    for (i in '{'..'}') {
      pressKeys(i.toString(), i, null, null)
    }
  }

  @Test
  fun qwertzlettersTest() {
    for (i in 'a' .. 'x')
      pressKeys(i.toString(), i, null, null)
  }
  @Test
  fun qwertzShiftedlettersTest() {
    for (i in 'A' .. 'X')
      pressKeys(i.toString(), i, null, null)
  }

  /*todo fix this
  actual:   KeyEvent[PRESSED,keyCode=521,keyText=+,keyChar=+ (43),keyLocation=STANDARD,modifiersEx=],
  expected: KeyEvent[PRESSED,keyCode=49,keyText=1,keyChar=+ (43),keyLocation=STANDARD,modifiersEx=],
   */
  @Ignore
  @Test
  fun qwertzSymbols() {
    var j = 0
    for (i in '!'..'@') {
      pressKeys(qwertzSymbols[j].toString(), i, qwertzSymbols[j], null)
      j++
    }
    for (i in '['..'_') {
      pressKeys(qwertzSymbols[j].toString(), i, qwertzSymbols[j], null)
      j++
    }
    for (i in '{'..'}') {
      pressKeys(qwertzSymbols[j].toString(), i, qwertzSymbols[j], null)
      j++
    }
  }

  @Test
  fun azertyLettersTest() {
    for (i in 'b' .. 'l')
      pressKeys(i.toString(), i, null, null)
    for (i in 'n' .. 'p')
      pressKeys(i.toString(), i, null, null)
    for (i in 'r' .. 'v')
      pressKeys(i.toString(), i, null, null)
  }

  @Test
  fun azertyShiftedLettersTest() {
    for (i in 'B' .. 'L')
      pressKeys(i.toString(), i, null, null)
    for (i in 'N' .. 'P')
      pressKeys(i.toString(), i, null, null)
    for (i in 'R' .. 'V')
      pressKeys(i.toString(), i, null, null)
  }

  /* todo fix this
  actual:   KeyEvent[PRESSED,keyCode=150,keyText=&,keyChar=& (38),keyLocation=STANDARD,modifiersEx=],
  expected: KeyEvent[PRESSED,keyCode=49,keyText=1,keyChar=& (38),keyLocation=STANDARD,modifiersEx=],
   */
  @Ignore
  @Test
  fun azertySymbols() {
    var j = 0
    for (i in '!'..'@') {
      pressKeys(azertySymbols[j].toString(), i, null, azertySymbols[j])
      j++
    }
    for (i in '['..'_') {
      pressKeys(azertySymbols[j].toString(), i, null, azertySymbols[j])
      j++
    }
    for (i in '{'..'}') {
      pressKeys(azertySymbols[j].toString(), i, null, azertySymbols[j])
      j++
    }
  }

  private fun pressKeys(symbol : String, symbolChar: Char, qwertzChar: Char?, azertyChar: Char?) = test(symbol) {
    if (symbolChar in 'A'..'Z' || (symbolChar in shiftedSymbols && qwertzChar == null && azertyChar == null) || (qwertzChar in qwertzShiftedSymbols || azertyChar in azertyShiftedSymbols))
      keyPress(KeyEvent.VK_SHIFT)

    when(symbolChar.lowercaseChar() ) {
      in 'a' .. 'z' -> {
        var charOffset = symbolChar.lowercaseChar() - 'a'
        val vk = KeyEvent.VK_A + charOffset
        keyPress(vk)
        keyRelease(vk)
      }
    }

    when(symbolChar.toString()) {
      "1","!" -> {keyPress(KeyEvent.VK_1); keyRelease(KeyEvent.VK_1)}
      "2","@" -> {keyPress(KeyEvent.VK_2); keyRelease(KeyEvent.VK_2)}
      "3","#" -> {keyPress(KeyEvent.VK_3); keyRelease(KeyEvent.VK_3)}
      "4","$" -> {keyPress(KeyEvent.VK_4); keyRelease(KeyEvent.VK_4)}
      "5","%" -> {keyPress(KeyEvent.VK_5); keyRelease(KeyEvent.VK_5)}
      "6","^" -> {keyPress(KeyEvent.VK_6); keyRelease(KeyEvent.VK_6)}
      "7","&" -> {keyPress(KeyEvent.VK_7); keyRelease(KeyEvent.VK_7)}
      "8","*" -> {keyPress(KeyEvent.VK_8); keyRelease(KeyEvent.VK_8)}
      "9","(" -> {keyPress(KeyEvent.VK_9); keyRelease(KeyEvent.VK_9)}
      "0",")" -> {keyPress(KeyEvent.VK_0); keyRelease(KeyEvent.VK_0)}
      "-","_" -> {keyPress(KeyEvent.VK_MINUS); keyRelease(KeyEvent.VK_MINUS)}
      "=","+" -> {keyPress(KeyEvent.VK_EQUALS); keyRelease(KeyEvent.VK_EQUALS)}
      "[","{" -> {keyPress(KeyEvent.VK_OPEN_BRACKET); keyRelease(KeyEvent.VK_OPEN_BRACKET)}
      "]","}" -> {keyPress(KeyEvent.VK_CLOSE_BRACKET); keyRelease(KeyEvent.VK_CLOSE_BRACKET)}
      ";",":" -> {keyPress(KeyEvent.VK_SEMICOLON); keyRelease(KeyEvent.VK_SEMICOLON)}
      "'", "\"" -> {keyPress(KeyEvent.VK_QUOTE); keyRelease(KeyEvent.VK_QUOTE)}
      "\\" ,"|" -> {keyPress(KeyEvent.VK_BACK_SLASH); keyRelease(KeyEvent.VK_BACK_SLASH)}
      ",","<" -> {keyPress(KeyEvent.VK_COMMA); keyRelease(KeyEvent.VK_COMMA)}
      ".",">" -> {keyPress(KeyEvent.VK_PERIOD); keyRelease(KeyEvent.VK_PERIOD)}
      "/","?" -> {keyPress(KeyEvent.VK_SLASH); keyRelease(KeyEvent.VK_SLASH)}
    }
    if (symbolChar in 'A'..'Z' || (symbolChar in shiftedSymbols && qwertzChar == null && azertyChar == null) || (qwertzChar in qwertzShiftedSymbols || azertyChar in azertyShiftedSymbols))
      keyRelease(KeyEvent.VK_SHIFT)
  }

  @Test
  fun qwertzYTest() = test("z") {
    keyPress(KeyEvent.VK_Y)
    keyRelease(KeyEvent.VK_Y)
  }

  @Test
  fun qwertzZTest() = test("y") {
    keyPress(KeyEvent.VK_Z)
    keyRelease(KeyEvent.VK_Z)
  }

  /*todo fix this
  actual: KeyEvent[PRESSED,keyCode=521,keyText=+,keyChar=+ (43),keyLocation=STANDARD,modifiersEx=],
  expected: KeyEvent[PRESSED,keyCode=49,keyText=1,keyChar=+ (43),keyLocation=STANDARD,modifiersEx=],
   */
  @Ignore
  @Test
  fun qwertzSimpleSymbolTest() = test("+") {
    keyPress(KeyEvent.VK_1)
    keyRelease(KeyEvent.VK_1)
  }


  /*todo fix this
  actual:   KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=á (225),keyLocation=UNKNOWN,modifiersEx=]
            (size: 1)

  expected: KeyEvent[RELEASED,keyCode=61,keyText==,keyChar=' (39),keyLocation=STANDARD,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=á (225),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[RELEASED,keyCode=65,keyText=A,keyChar=a (97),keyLocation=STANDARD,modifiersEx=]
            (size: 3)
   */
  @Ignore
  @Test
  fun qwertzDeadKeyTest() = test("á")
  {
    keyPress(KeyEvent.VK_EQUALS)
    keyRelease(KeyEvent.VK_EQUALS)
    keyPress(KeyEvent.VK_A)
    keyRelease(KeyEvent.VK_A)
  }

  @Test
  fun azertyTest() = test("a") {
    keyPress(KeyEvent.VK_Q)
    keyRelease(KeyEvent.VK_Q)
  }

  /* todo fix this
  actual:   KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=請 (35531),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=問 (21839),keyLocation=UNKNOWN,modifiersEx=]
            (size: 2)

  expected: KeyEvent[RELEASED,keyCode=81,keyText=Q,keyChar=q (113),keyLocation=STANDARD,modifiersEx=],
            KeyEvent[RELEASED,keyCode=87,keyText=W,keyChar=w (119),keyLocation=STANDARD,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=請 (35531),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=問 (21839),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[RELEASED,keyCode=32,keyText=␣,keyChar=  (32),keyLocation=STANDARD,modifiersEx=]
            (size: 5)
   */
  @Ignore
  @Test
  fun chineseSimpleInputTest() = test("請問") {
    keyPress(KeyEvent.VK_Q)
    keyRelease(KeyEvent.VK_Q)
    keyPress(KeyEvent.VK_W)
    keyRelease(KeyEvent.VK_W)
    keyPress(KeyEvent.VK_SPACE)
    keyRelease(KeyEvent.VK_SPACE)
  }

  @Test
  fun funKeysTest() {
    for (j in 0 .. 1) {
      var vk = KeyEvent.VK_F2
      for (i in 2..10) {
        pressFunKey(vk, j)
        vk += 1
      }
      vk += 1
      pressFunKey(vk, j)
      //pressFunKey(vk - 11, j)   //Todo: fix this (f1)
    }
  }

  fun pressFunKey(vk: Int, shift: Int) = test("") {
    if (shift == 1)
      keyPress(KeyEvent.VK_SHIFT)
    keyPress(vk)
    keyRelease(vk)
    if (shift == 1)
      keyRelease(KeyEvent.VK_SHIFT)
  }


  /*todo fix this
  actual:   KeyEvent[PRESSED,keyCode=16,keyText=⇧,keyChar=￿ (65535),keyLocation=LEFT,modifiersEx=⇧],
            KeyEvent[RELEASED,keyCode=16,keyText=⇧,keyChar=￿ (65535),keyLocation=LEFT,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=š (353),keyLocation=UNKNOWN,modifiersEx=]
            (size: 3)

  expected: KeyEvent[PRESSED,keyCode=16,keyText=⇧,keyChar=￿ (65535),keyLocation=LEFT,modifiersEx=⇧],
            KeyEvent[PRESSED,keyCode=138,keyText=Dead Caron,keyChar=ˇ (711),keyLocation=UNKNOWN,modifiersEx=⇧],
            KeyEvent[RELEASED,keyCode=61,keyText==,keyChar=ˇ (711),keyLocation=STANDARD,modifiersEx=⇧],
            KeyEvent[RELEASED,keyCode=16,keyText=⇧,keyChar=￿ (65535),keyLocation=LEFT,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=š (353),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[RELEASED,keyCode=83,keyText=S,keyChar=s (115),keyLocation=STANDARD,modifiersEx=]
            (size: 6)
   */
  @Ignore
  @Test
  fun qwertzShiftedDeadKey() = test("š") {
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_EQUALS)
    keyRelease(KeyEvent.VK_EQUALS)
    keyRelease(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_S)
    keyRelease(KeyEvent.VK_S)
  }

  /*todo fix this
  actual:   KeyEvent[PRESSED,keyCode=65406,keyText=⌥,keyChar=￿ (65535),keyLocation=RIGHT,modifiersEx=⌥],
            KeyEvent[RELEASED,keyCode=65406,keyText=⌥,keyChar=￿ (65535),keyLocation=RIGHT,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=à (224),keyLocation=UNKNOWN,modifiersEx=]
            (size: 3)

  expected: KeyEvent[PRESSED,keyCode=65406,keyText=⌥,keyChar=￿ (65535),keyLocation=RIGHT,modifiersEx=⌥+⌥],
            KeyEvent[PRESSED,keyCode=128,keyText=Dead Grave,keyChar=` (96),keyLocation=UNKNOWN,modifiersEx=⌥+⌥],
            KeyEvent[RELEASED,keyCode=192,keyText=`,keyChar=` (96),keyLocation=STANDARD,modifiersEx=⌥+⌥],
            KeyEvent[RELEASED,keyCode=65406,keyText=⌥,keyChar=￿ (65535),keyLocation=RIGHT,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=à (224),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[RELEASED,keyCode=65,keyText=A,keyChar=a (97),keyLocation=STANDARD,modifiersEx=]
            (size: 6)
   */
  @Ignore
  @Test
  fun deadKeyTest() = test("à") {
    keyPress(KeyEvent.VK_ALT_GRAPH)
    keyPress(KeyEvent.VK_BACK_QUOTE)
    keyRelease(KeyEvent.VK_BACK_QUOTE)
    keyRelease(KeyEvent.VK_ALT_GRAPH)
    keyPress(KeyEvent.VK_A)
    keyRelease(KeyEvent.VK_A)
  }

  /*todo fix this
  actual:   KeyEvent[... ,modifiersEx=⌥]
  expected: KeyEvent[... ,modifiersEx=⌥+⌥]
   */
  @Ignore
  @Test
  fun altGraphTest() = test("å") {
    keyPress(KeyEvent.VK_ALT_GRAPH)
    keyPress(KeyEvent.VK_A)
    keyRelease(KeyEvent.VK_A)
    keyRelease(KeyEvent.VK_ALT_GRAPH)
  }

  @Test
  fun testCtrlD() = test("") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_D)
    keyRelease(KeyEvent.VK_D)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  /* todo fix this
  actual:   KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=ふ (12405),keyLocation=UNKNOWN,modifiersEx=]
            (size: 1)

  expected: KeyEvent[RELEASED,keyCode=72,keyText=H,keyChar=h (104),keyLocation=STANDARD,modifiersEx=],
            KeyEvent[RELEASED,keyCode=85,keyText=U,keyChar=u (117),keyLocation=STANDARD,modifiersEx=],
            KeyEvent[TYPED,keyCode=0,keyText=Unknown keyCode: 0x0,keyChar=ふ (12405),keyLocation=UNKNOWN,modifiersEx=],
            KeyEvent[RELEASED,keyCode=10,keyText=⏎,keyChar=(10),keyLocation=STANDARD,modifiersEx=]
            (size: 4)
   */
  @Ignore
  @Test
  fun japSimpleTest() = test("ふ") {
    keyPress(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_H)
    keyPress(KeyEvent.VK_U)
    keyRelease(KeyEvent.VK_U)
    keyPress(KeyEvent.VK_ENTER)
    keyRelease(KeyEvent.VK_ENTER)
  }

  @Test
  fun testSimpleSymbol() = test("h") {
    keyPress(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_H)
  }

  @Test
  fun testShiftedSimpleSymbol() = test("H") {
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_H)
    keyRelease(KeyEvent.VK_SHIFT)
  }

  @Test
  fun testTab() = test("\t") {
    keyPress(KeyEvent.VK_TAB)
    keyRelease(KeyEvent.VK_TAB)
  }

  @Test
  fun testEnter() = test("\n") {
    keyPress(KeyEvent.VK_ENTER)
    keyRelease(KeyEvent.VK_ENTER)
  }

  @Test
  fun testBackspace() = test("") {
    keyPress(KeyEvent.VK_BACK_SPACE)
    keyRelease(KeyEvent.VK_BACK_SPACE)
  }

  @Test
  fun testSpace() = test(" ") {
    keyPress(KeyEvent.VK_SPACE)
    keyRelease(KeyEvent.VK_SPACE)
  }

  @Test
  fun testEscape() = test("") {
    keyPress(KeyEvent.VK_ESCAPE)
    keyRelease(KeyEvent.VK_ESCAPE)
  }


  @Test
  fun testDelete() = test("") {
    keyPress(KeyEvent.VK_DELETE)
    keyRelease(KeyEvent.VK_DELETE)
  }

  @Test
  fun testCtrlLetter() = test("") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_Z)
    keyRelease(KeyEvent.VK_Z)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  @Test
  fun testFunctionalKey() = test("") {
    keyPress(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_F6)
  }

  @Test
  fun testShiftedFunctionalKey() = test("") {
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_F6)
    keyRelease(KeyEvent.VK_SHIFT)
  }

  @Test
  fun testCtrlShiftedLetter() = test("") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_K)
    keyRelease(KeyEvent.VK_K)
    keyRelease(KeyEvent.VK_SHIFT)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  @Test
  fun testArrow() = test("") {
    keyPress(KeyEvent.VK_RIGHT)
    keyRelease(KeyEvent.VK_RIGHT)
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  fun testNumpadEnterLinux() = test("\n") {
    Runtime.getRuntime().exec("xdotool key KP_Enter").waitFor()
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  fun testNumpadWithNumLockLinux() = test("5") {
    Runtime.getRuntime().exec("numlockx on").waitFor()
    keyPress(KeyEvent.VK_NUMPAD5)
    keyRelease(KeyEvent.VK_NUMPAD5)
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @Ignore  // todo: https://youtrack.jetbrains.com/issue/PRJ-301
  fun testNumpadWithoutNumLockLinux() = test("") {
    Runtime.getRuntime().exec("numlockx off").waitFor()
    keyPress(KeyEvent.VK_NUMPAD7)
    keyRelease(KeyEvent.VK_NUMPAD7)
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @Ignore  // todo: https://youtrack.jetbrains.com/issue/PRJ-194
  fun testLinuxAltCode() = test("–") {
    keyPress(KeyEvent.VK_CONTROL)
    keyPress(KeyEvent.VK_SHIFT)
    keyPress(KeyEvent.VK_U)
    keyRelease(KeyEvent.VK_U)
    keyPress(KeyEvent.VK_2)
    keyRelease(KeyEvent.VK_2)
    keyPress(KeyEvent.VK_0)
    keyRelease(KeyEvent.VK_0)
    keyPress(KeyEvent.VK_1)
    keyRelease(KeyEvent.VK_1)
    keyPress(KeyEvent.VK_3)
    keyRelease(KeyEvent.VK_3)
    keyRelease(KeyEvent.VK_SHIFT)
    keyRelease(KeyEvent.VK_CONTROL)
  }

  @Test
  @EnabledOnOs(OS.MAC)
  fun testMacAltCode() = test("–") {
    keyPress(KeyEvent.VK_ALT)
    keyPress(KeyEvent.VK_MINUS)
    keyRelease(KeyEvent.VK_MINUS)
    keyRelease(KeyEvent.VK_ALT)
  }
}
