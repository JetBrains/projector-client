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
package org.jetbrains.projector.server.core.convert.toAwt

import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.ClientRawKeyEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

public fun ClientRawKeyEvent.toAwtKeyEvent(connectionMillis: Long, target: Component): KeyEvent {
  return KeyEvent(
    target,
    this.keyEventType.toAwtKeyEventId(),
    this.timeStamp + connectionMillis,
    this.modifiers,
    this.code,
    this.char,
    this.location
  )
}

private fun createAwtChar(char: Char, modifiers: Set<KeyModifier>): Char {
  return when (KeyModifier.CTRL_KEY in modifiers) {
    true -> controlCharMap[char.uppercaseChar().code] ?: KeyEvent.CHAR_UNDEFINED
    false -> char
  }
}

public fun ClientKeyPressEvent.toAwtKeyEvent(connectionMillis: Long, target: Component): KeyEvent {
  val char = createAwtChar(this.char, this.modifiers)

  return KeyEvent(
    target,
    KeyEvent.KEY_TYPED,
    this.timeStamp + connectionMillis,
    this.modifiers.toInt(),
    KeyEvent.VK_UNDEFINED,
    char,
    KeyEvent.KEY_LOCATION_UNKNOWN,
  )
}

public fun ClientKeyEvent.toAwtKeyEvent(connectionMillis: Long, target: Component): KeyEvent {
  val char = createAwtChar(this.char, this.modifiers)

  return KeyEvent(
    target,
    this.keyEventType.toAwtKeyEventId(),
    this.timeStamp + connectionMillis,
    this.modifiers.toInt(),
    codesMap.getValue(this.code),
    char,
    this.location.toJavaLocation(),
  )
}

private val keyModifierMask = mapOf(
  KeyModifier.ALT_KEY to InputEvent.ALT_DOWN_MASK,
  KeyModifier.CTRL_KEY to InputEvent.CTRL_DOWN_MASK,
  KeyModifier.SHIFT_KEY to InputEvent.SHIFT_DOWN_MASK,
  KeyModifier.META_KEY to InputEvent.META_DOWN_MASK,
  KeyModifier.REPEAT to 0 // todo: find a way to use this key
)

private fun Set<KeyModifier>.toInt(): Int {
  return map(keyModifierMask::getValue).fold(0, Int::or)
}

private fun ClientKeyEvent.KeyEventType.toAwtKeyEventId() = when (this) {
  ClientKeyEvent.KeyEventType.DOWN -> KeyEvent.KEY_PRESSED
  ClientKeyEvent.KeyEventType.UP -> KeyEvent.KEY_RELEASED
}

private fun ClientRawKeyEvent.RawKeyEventType.toAwtKeyEventId() = when (this) {
  ClientRawKeyEvent.RawKeyEventType.DOWN -> KeyEvent.KEY_PRESSED
  ClientRawKeyEvent.RawKeyEventType.UP -> KeyEvent.KEY_RELEASED
  ClientRawKeyEvent.RawKeyEventType.TYPED -> KeyEvent.KEY_TYPED
}

private fun ClientKeyEvent.KeyLocation.toJavaLocation() = when (this) {
  ClientKeyEvent.KeyLocation.STANDARD -> KeyEvent.KEY_LOCATION_STANDARD
  ClientKeyEvent.KeyLocation.LEFT -> KeyEvent.KEY_LOCATION_LEFT
  ClientKeyEvent.KeyLocation.RIGHT -> KeyEvent.KEY_LOCATION_RIGHT
  ClientKeyEvent.KeyLocation.NUMPAD -> KeyEvent.KEY_LOCATION_NUMPAD
}
