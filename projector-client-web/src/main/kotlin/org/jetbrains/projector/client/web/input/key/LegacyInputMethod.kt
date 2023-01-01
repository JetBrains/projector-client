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
package org.jetbrains.projector.client.web.input.key

import kotlinx.browser.document
import org.jetbrains.projector.client.web.input.SpecialKeysState
import org.jetbrains.projector.common.protocol.toClient.ServerCaretInfoChangedEvent
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP

class LegacyInputMethod(
  openingTimeStamp: Int,
  specialKeysState: SpecialKeysState,
  clientEventConsumer: (ClientEvent) -> Unit,
) : InputMethod {

  private val documentActionListeners = mapOf(
    "keydown" to handleKeyboardEvent(DOWN, clientEventConsumer, openingTimeStamp, specialKeysState),
    "keyup" to handleKeyboardEvent(UP, clientEventConsumer, openingTimeStamp, specialKeysState),
  )

  init {
    documentActionListeners.forEach { (type, handler) ->
      document.addEventListener(type, handler)
    }
  }

  override fun dispose() {
    documentActionListeners.forEach { (type, handler) ->
      document.removeEventListener(type, handler)
    }
  }

  override fun handleCaretInfoChange(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
    // nop
  }
}
