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
package org.jetbrains.projector.client.web.input.key

import kotlinx.browser.document
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.common.misc.isUpperCase
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.roundToInt

class ImeInputMethod(
  private val openingTimeStamp: Int,
  private val clientEventConsumer: (ClientEvent) -> Unit,
) : InputMethod {

  private val inputField = (document.createElement("textarea") as HTMLTextAreaElement).apply {
    style.apply {
      position = "fixed"
      bottom = "-30%"
      left = "50%"
    }

    autocomplete = "off"
    asDynamic().autocapitalize = "none"

    onblur = {
      this.focus()
      this.click()
    }

    fun fireKeyPress(char: Char) {
      val message = ClientKeyPressEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        char = char,
        modifiers = if (char.isUpperCase()) setOf(KeyModifier.SHIFT_KEY) else setOf(),  // todo: use modifiers of the last KEY_UP
      )
      clientEventConsumer(message)
    }

    var composing = false

    addEventListener(
      "compositionstart",
      {
        composing = true
      }
    )

    addEventListener(
      "compositionend",
      { event: Event ->
        require(event is CompositionEvent)

        clearInputField()
        composing = false

        event.data.forEach { char ->
          fireKeyPress(char)
        }
      }
    )

    onkeydown = fireKeyEvent(clientEventConsumer, openingTimeStamp, DOWN)
    onkeyup = fireKeyEvent(clientEventConsumer, openingTimeStamp, UP)

    oninput = {
      if (!composing) {
        clearInputField()
      }
    }

    document.body!!.appendChild(this)
  }

  init {
    clearInputField()
  }

  private fun clearInputField() {
    inputField.value = ""
  }

  init {
    inputField.focus()
    inputField.click()
  }

  override fun dispose() {
    inputField.remove()
  }

  private companion object {

    private fun fireKeyEvent(
      clientEventConsumer: (ClientEvent) -> Unit,
      openingTimeStamp: Int,
      type: ClientKeyEvent.KeyEventType,
    ) = lambda@{ event: KeyboardEvent ->
      if (event.key == "Process") {
        return@lambda
      }

      clientEventConsumer.fireKeyEvent(type, event, openingTimeStamp)
    }
  }
}
