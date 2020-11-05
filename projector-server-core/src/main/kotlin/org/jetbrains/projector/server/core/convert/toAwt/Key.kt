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
package org.jetbrains.projector.server.core.convert.toAwt

import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.jetbrains.projector.util.logging.Logger
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

private val logger = Logger("KeyKt")

public fun ClientKeyPressEvent.toAwtKeyEvent(connectionMillis: Long, target: Component): KeyEvent? {
  @Suppress("MoveVariableDeclarationIntoWhen") val isKeystroke = KeyModifier.CTRL_KEY in this.modifiers
  val keyChar = when (isKeystroke) {
                  true -> this.key.toJavaCodeOrNull()?.toJavaControlCharOrNull()
                  false -> this.key.toJavaCharOrNull()
                } ?: run {
    logger.error { "$this.toAwtKeyEvent(...): unknown key, skipping" }
    return null
  }

  return KeyEvent(
    target,
    KeyEvent.KEY_TYPED,
    this.timeStamp + connectionMillis,
    this.modifiers.toInt(),
    KeyEvent.VK_UNDEFINED,
    keyChar,
    KeyEvent.KEY_LOCATION_UNKNOWN,
  )
}

public fun ClientKeyEvent.toAwtKeyEvent(connectionMillis: Long, target: Component): KeyEvent? {
  val keyEventType = this.keyEventType.toAwtKeyEventId()

  val code = this.code.toJavaCodeOrNull() ?: run {
    logger.error { "$this.toAwtKeyEvent(...): unknown code, skipping" }
    return null
  }

  @Suppress("MoveVariableDeclarationIntoWhen") val isKeystroke = KeyModifier.CTRL_KEY in this.modifiers
  val key = when (isKeystroke) {
              true -> code.toJavaControlCharOrNull()
              false -> this.key.toJavaCharOrNull()
            } ?: KeyEvent.CHAR_UNDEFINED

  return KeyEvent(
    target,
    keyEventType,
    this.timeStamp + connectionMillis,
    this.modifiers.toInt(),
    code,
    key,
    (this.code.extractLocationOrNull() ?: this.location).toJavaLocation(),
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

private fun ClientKeyEvent.KeyLocation.toJavaLocation() = when (this) {
  ClientKeyEvent.KeyLocation.STANDARD -> KeyEvent.KEY_LOCATION_STANDARD
  ClientKeyEvent.KeyLocation.LEFT -> KeyEvent.KEY_LOCATION_LEFT
  ClientKeyEvent.KeyLocation.RIGHT -> KeyEvent.KEY_LOCATION_RIGHT
  ClientKeyEvent.KeyLocation.NUMPAD -> KeyEvent.KEY_LOCATION_NUMPAD
}
