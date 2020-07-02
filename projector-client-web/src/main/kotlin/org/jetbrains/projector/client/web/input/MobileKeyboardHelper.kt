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
package org.jetbrains.projector.client.web.input

import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.Node
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.roundToInt

interface MobileKeyboardHelper {

  fun dispose()
}

object NopMobileKeyboardHelper : MobileKeyboardHelper {

  override fun dispose() {}
}

class MobileKeyboardHelperImpl(
  private val openingTimeStamp: Int,
  private val specialKeysState: SpecialKeysState,
  private val keyEventConsumer: (ClientKeyEvent) -> Unit
) : MobileKeyboardHelper {

  private val panel = (document.createElement("div") as HTMLDivElement).apply {
    style.apply {
      zIndex = "1234"
      position = "absolute"
      bottom = "3%"
      left = "50%"
      transform = "translate(-50%, 0%)"
    }

    document.body!!.appendChild(this)
  }

  private fun fireKeyEvent(
    key: String,
    code: String,
    location: ClientKeyEvent.KeyLocation,
    keyEventType: ClientKeyEvent.KeyEventType
  ) {
    keyEventConsumer(
      ClientKeyEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        key = key,
        code = code,
        location = location,
        modifiers = specialKeysState.keyModifiers,
        keyEventType = keyEventType
      )
    )
  }

  init {
    SimpleButton(
      text = "Esc",
      parent = panel,
      onClick = {
        fireKeyEvent(key = "Escape", code = "Escape", location = STANDARD, keyEventType = DOWN)
        fireKeyEvent(key = "Escape", code = "Escape", location = STANDARD, keyEventType = UP)
      }
    )

    ToggleButton(
      text = "Alt",
      parent = panel,
      onStateChange = { newState ->
        specialKeysState.isAltEnabled = newState

        if (newState) {
          fireKeyEvent(key = "Alt", code = "AltLeft", location = LEFT, keyEventType = DOWN)
        }
        else {
          fireKeyEvent(key = "Alt", code = "AltLeft", location = LEFT, keyEventType = UP)
        }
      }
    )

    ToggleButton(
      text = "Ctrl",
      parent = panel,
      onStateChange = { newState ->
        specialKeysState.isCtrlEnabled = newState

        if (newState) {
          fireKeyEvent(key = "Control", code = "ControlLeft", location = LEFT, keyEventType = DOWN)
        }
        else {
          fireKeyEvent(key = "Control", code = "ControlLeft", location = LEFT, keyEventType = UP)
        }
      }
    )

    ToggleButton(
      text = "Shift",
      parent = panel,
      onStateChange = { newState ->
        specialKeysState.isShiftEnabled = newState

        if (newState) {
          fireKeyEvent(key = "Shift", code = "ShiftLeft", location = LEFT, keyEventType = DOWN)
        }
        else {
          fireKeyEvent(key = "Shift", code = "ShiftLeft", location = LEFT, keyEventType = UP)
        }
      }
    )
  }

  override fun dispose() {
    panel.remove()
  }

  private class ToggleButton(
    text: String,
    parent: Node,
    private val onStateChange: (newState: Boolean) -> Unit
  ) {

    private var isEnabled = false

    private val span = (document.createElement("span") as HTMLSpanElement).apply {
      style.apply {
        backgroundColor = DISABLED_COLOR
        padding = "5px"
        margin = "5px"
        cursor = "pointer"
        asDynamic().userSelect = "none"
      }

      innerText = text

      onclick = { e ->
        flipFlop()
        e.stopPropagation()
      }
      onmousedown = { e -> e.stopPropagation() }
      onmouseup = { e -> e.stopPropagation() }

      parent.appendChild(this)
    }

    private fun flipFlop() {
      if (isEnabled) {
        span.style.backgroundColor = DISABLED_COLOR
      }
      else {
        span.style.backgroundColor = ENABLED_COLOR
      }

      isEnabled = !isEnabled
      onStateChange(isEnabled)
    }
  }

  private class SimpleButton(
    text: String,
    parent: Node,
    private val onClick: () -> Unit
  ) {

    private val span = (document.createElement("span") as HTMLSpanElement).apply {
      style.apply {
        backgroundColor = DISABLED_COLOR
        padding = "5px"
        margin = "5px"
        cursor = "pointer"
        asDynamic().userSelect = "none"
      }

      innerText = text

      onclick = { e ->
        animate()
        onClick()
        e.stopPropagation()
      }
      onmousedown = MouseEvent::stopPropagation
      onmouseup = MouseEvent::stopPropagation

      parent.appendChild(this)
    }

    private fun animate() {
      span.style.backgroundColor = ENABLED_COLOR
      window.setTimeout(this::disableColor, timeout = 200)
    }

    private fun disableColor() {
      span.style.backgroundColor = DISABLED_COLOR
    }
  }

  companion object {

    private const val DISABLED_COLOR = "#ACA"
    private const val ENABLED_COLOR = "#8F8"
  }
}
