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

import org.jetbrains.projector.client.common.misc.Logger
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
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
  private val clientEventConsumer: (ClientEvent) -> Unit
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

  private val functionalButtonsPanel = (document.createElement("div") as HTMLDivElement).apply {
    style.apply {
      display = "none"
    }

    (1..12).forEach {
      SimpleButton(
        text = "F$it",
        parent = this,
        onClick = {
          fireKeyEvent(key = "F$it", code = "F$it", location = STANDARD, keyEventType = DOWN)
          fireKeyEvent(key = "F$it", code = "F$it", location = STANDARD, keyEventType = UP)
        }
      )

      if (it % 4 == 0) {
        val separator = (document.createElement("div") as HTMLDivElement).apply {
          style.height = "15px"
        }
        this.appendChild(separator)
      }
    }

    panel.appendChild(this)
  }

  private val virtualKeyboardInput = (document.createElement("textarea") as HTMLTextAreaElement).apply {
    style.apply {
      position = "fixed"
      bottom = "-30%"
    }

    oninput = fun(event: InputEvent) {
      resetVirtualKeyboardInput()

      when (val inputType = event.asDynamic().inputType) {
        "insertText" -> event.data.forEach { char ->
          val key = char.toString()
          val code = "Key${char.toUpperCase()}"

          fireKeyEvent(key = key, code = code, location = STANDARD, keyEventType = DOWN)
          fireKeyPressEvent(key = key)
          fireKeyEvent(key = key, code = code, location = STANDARD, keyEventType = UP)
        }

        "deleteContentBackward" -> {
          fireKeyEvent(key = "Backspace", code = "Backspace", location = STANDARD, keyEventType = DOWN)
          fireKeyEvent(key = "Backspace", code = "Backspace", location = STANDARD, keyEventType = UP)
        }

        "deleteContentForward" -> {
          fireKeyEvent(key = "Delete", code = "Delete", location = STANDARD, keyEventType = DOWN)
          fireKeyEvent(key = "Delete", code = "Delete", location = STANDARD, keyEventType = UP)
        }

        "insertLineBreak" -> {
          fireKeyEvent(key = "Enter", code = "Enter", location = STANDARD, keyEventType = DOWN)
          fireKeyEvent(key = "Enter", code = "Enter", location = STANDARD, keyEventType = UP)
        }

        else -> {
          logger.info { "Unknown inputType=$inputType" }
          @Suppress("UnsafeCastFromDynamic")  // TODO: Remove suppress after KT-39975 is implemented
          console.log(event)
        }
      }
    }

    document.body!!.appendChild(this)
  }

  init {
    resetVirtualKeyboardInput()
  }

  private fun resetVirtualKeyboardInput() {
    virtualKeyboardInput.value = TEXTAREA_VALUE
    virtualKeyboardInput.selectionStart = TEXTAREA_VALUE.length / 2
    virtualKeyboardInput.selectionEnd = TEXTAREA_VALUE.length / 2
  }

  private fun handleVirtualKeyboardSelection(unused: Event) {
    fun sendArrow(direction: String) {
      fireKeyEvent(key = "Arrow$direction", code = "Arrow$direction", location = STANDARD, keyEventType = DOWN)
      fireKeyEvent(key = "Arrow$direction", code = "Arrow$direction", location = STANDARD, keyEventType = UP)
    }

    if (
      document.activeElement == virtualKeyboardInput &&
      virtualKeyboardInput.value == TEXTAREA_VALUE
    ) {
      when (virtualKeyboardInput.selectionStart) {
        0 -> sendArrow("Up")
        1 -> sendArrow("Left")
        3 -> sendArrow("Right")
        4 -> sendArrow("Down")
        else -> Unit
      }

      resetVirtualKeyboardInput()
    }
  }

  private fun fireKeyEvent(
    key: String,
    code: String,
    location: ClientKeyEvent.KeyLocation,
    keyEventType: ClientKeyEvent.KeyEventType
  ) {
    clientEventConsumer(
      ClientKeyEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        key = if (key.length == 1 && specialKeysState.isShiftEnabled) key.toUpperCase() else key,
        code = code,
        location = location,
        modifiers = specialKeysState.keyModifiers,
        keyEventType = keyEventType
      )
    )
  }

  private fun fireKeyPressEvent(key: String) {
    clientEventConsumer(
      ClientKeyPressEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        key = if (key.length == 1 && specialKeysState.isShiftEnabled) key.toUpperCase() else key,
        modifiers = specialKeysState.keyModifiers
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

    ToggleButton(
      text = "F*",
      parent = panel,
      onStateChange = { newState ->
        functionalButtonsPanel.style.display = newState.toDisplayType()
      }
    )

    if (ParamsProvider.MOBILE_SETTING == ParamsProvider.MobileSetting.ALL) {
      SimpleButton(
        text = "⌨",  // Keyboard symbol
        parent = panel,
        onClick = {
          virtualKeyboardInput.focus()
          virtualKeyboardInput.click()
        }
      )

      document.addEventListener("selectionchange", this::handleVirtualKeyboardSelection)
    }
  }

  override fun dispose() {
    panel.remove()
    virtualKeyboardInput.remove()

    if (ParamsProvider.MOBILE_SETTING == ParamsProvider.MobileSetting.ALL) {
      document.removeEventListener("selectionchange", this::handleVirtualKeyboardSelection)
    }
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
        borderRadius = "3px"
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
        borderRadius = "3px"
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

    private const val TEXTAREA_VALUE = "\n--\n"

    private val logger = Logger(MobileKeyboardHelperImpl::class.simpleName!!)
  }
}
