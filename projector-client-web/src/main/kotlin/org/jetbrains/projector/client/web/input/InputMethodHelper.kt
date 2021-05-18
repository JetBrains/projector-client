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
package org.jetbrains.projector.client.web.input

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.RepaintAreaSetting
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.web.input.InputController.Companion.codeToVk
import org.jetbrains.projector.client.web.input.InputController.Companion.keyToChar
import org.jetbrains.projector.client.web.input.InputController.Companion.orCtrlQ
import org.jetbrains.projector.client.web.input.InputController.Companion.toCommonKeyLocation
import org.jetbrains.projector.client.web.misc.ClientStats
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.jetbrains.projector.common.misc.isUpperCase
import org.jetbrains.projector.common.protocol.data.VK
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.DOWN
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyEventType.UP
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.LEFT
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.STANDARD
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.math.roundToInt

interface InputMethodHelper {

  fun dispose()
}

object NopInputMethodHelper : InputMethodHelper {

  override fun dispose() {}
}

class MobileKeyboardHelper(
  private val openingTimeStamp: Int,
  private val specialKeysState: SpecialKeysState,
  private val clientEventConsumer: (ClientEvent) -> Unit,
) : InputMethodHelper {

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
      val jsKey = JsKey("F$it")
      val code = VK.valueOf("F$it")

      SimpleButton(
        text = "F$it",
        parent = this,
        elementId = "pressF$it",
        onClick = {
          fireDownUp(jsKey, code)
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

  private var virtualKeyboardEnabled = false

  private val virtualKeyboardInput = (document.createElement("textarea") as HTMLTextAreaElement).apply {
    style.apply {
      position = "fixed"
      bottom = "-30%"
    }

    autocomplete = "off"
    asDynamic().autocapitalize = "none"

    onblur = {
      if (virtualKeyboardEnabled) {
        this.focus()
        this.click()
      }
    }

    onkeydown = {
      // handle Tabs: don't make default action "switch to the next input"
      if (it.key == "Tab") {  // don't use `it.code == "Tab"` here because `it.code` is empty on mobile devices
        it.preventDefault()
        it.stopPropagation()
        fireDownPressUp(VK.TAB)
      }
    }

    oninput = fun(event: InputEvent) {
      resetVirtualKeyboardInput()

      when (val inputType = event.asDynamic().inputType) {
        "insertText", "insertCompositionText" -> event.data.forEach { char ->
          val code = VK.codeMap[char] ?: VK.UNDEFINED  // todo: firing VK.UNDEFINED is not correct for example for cyrillic letters

          val shiftWasAlreadyEnabled = specialKeysState.isShiftEnabled
          if (!shiftWasAlreadyEnabled && char.isUpperCase()) {
            specialKeysState.isShiftEnabled = true
            fireKeyEvent(key = JsKey("ShiftLeft"), code = VK.SHIFT, location = LEFT, keyEventType = DOWN)
          }
          fireDownPressUp(code, char)
          if (!shiftWasAlreadyEnabled && char.isUpperCase()) {
            specialKeysState.isShiftEnabled = false
            fireKeyEvent(key = JsKey("ShiftLeft"), code = VK.SHIFT, location = LEFT, keyEventType = UP)
          }
        }

        "deleteContentBackward" -> fireDownPressUp(VK.BACK_SPACE)
        "deleteContentForward" -> fireDownPressUp(VK.DELETE)
        "insertLineBreak" -> fireDownPressUp(VK.ENTER)

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

  private var lastSelectionResetTimeStamp = TimeStamp.current

  private fun resetVirtualKeyboardInput() {
    lastSelectionResetTimeStamp = TimeStamp.current
    virtualKeyboardInput.value = TEXTAREA_VALUE
    virtualKeyboardInput.selectionStart = TEXTAREA_VALUE.length / 2
    virtualKeyboardInput.selectionEnd = TEXTAREA_VALUE.length / 2
  }

  @Suppress("UNUSED_PARAMETER")  // actually, parameter is used in function refs
  private fun handleVirtualKeyboardSelection(unused: Event) {
    if (
      document.activeElement == virtualKeyboardInput &&
      virtualKeyboardInput.value == TEXTAREA_VALUE &&
      (virtualKeyboardInput.selectionStart != TEXTAREA_VALUE.length / 2 || virtualKeyboardInput.selectionEnd != TEXTAREA_VALUE.length / 2)
    ) {
      // The following check is needed to fix behavior on Android Chrome with Gboard:
      // when Backspace is pressed there, the selection change to the left event is generated immediately after the input event,
      // and we shouldn't treat it as a command to move left.
      // So here we make sure that the selection event is received way after the last input event and execute cursor move only if it's true
      if (TimeStamp.current - lastSelectionResetTimeStamp > MINIMUM_MS_BETWEEN_INPUT_AND_SELECTION_CHANGE) {
        when (virtualKeyboardInput.selectionStart) {
          0 -> fireDownUp(JsKey("ArrowUp"), VK.UP)
          1 -> fireDownUp(JsKey("Home"), VK.HOME)
          2 -> fireDownUp(JsKey("ArrowLeft"), VK.LEFT)
          4 -> fireDownUp(JsKey("ArrowRight"), VK.RIGHT)
          5 -> fireDownUp(JsKey("End"), VK.END)
          6 -> fireDownUp(JsKey("ArrowDown"), VK.DOWN)
          else -> Unit
        }
      }

      virtualKeyboardInput.selectionStart = TEXTAREA_VALUE.length / 2
      virtualKeyboardInput.selectionEnd = TEXTAREA_VALUE.length / 2
    }
  }

  private fun fireDownPressUp(code: VK, char: Char = code.typedSymbols.single()) {
    val jsKey = JsKey(char.toString())
    fireKeyEvent(key = jsKey, code = code, location = STANDARD, keyEventType = DOWN)
    fireKeyPressEvent(char = char)
    fireKeyEvent(key = jsKey, code = code, location = STANDARD, keyEventType = UP)
  }

  private fun fireDownUp(key: JsKey, code: VK) {
    fireKeyEvent(key = key, code = code, location = STANDARD, keyEventType = DOWN)
    fireKeyEvent(key = key, code = code, location = STANDARD, keyEventType = UP)
  }

  private fun fireKeyEvent(
    key: JsKey,
    code: VK,
    location: ClientKeyEvent.KeyLocation,
    keyEventType: ClientKeyEvent.KeyEventType,
  ) {
    val char = InputController.keyToChar(key, code)

    clientEventConsumer(
      ClientKeyEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        char = if (specialKeysState.isShiftEnabled) char.toUpperCase() else char,
        code = code,
        location = location,
        modifiers = specialKeysState.keyModifiers,
        keyEventType = keyEventType
      )
    )
  }

  private fun fireKeyPressEvent(char: Char) {
    clientEventConsumer(
      ClientKeyPressEvent(
        timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
        char = if (specialKeysState.isShiftEnabled) char.toUpperCase() else char,
        modifiers = specialKeysState.keyModifiers
      )
    )
  }

  init {
    SimpleButton(
      text = "Esc",
      parent = panel,
      elementId = "pressEsc",
      onClick = {
        fireDownPressUp(VK.ESCAPE)
      }
    )

    fun Boolean.toKeyEventType() = when (this) {
      true -> DOWN
      false -> UP
    }

    ToggleButton(
      text = "Alt",
      parent = panel,
      elementId = "toggleAlt",
      onStateChange = { newState ->
        specialKeysState.isAltEnabled = newState

        val key = JsKey("Alt")
        val code = VK.ALT
        val type = newState.toKeyEventType()

        fireKeyEvent(key = key, code = code, location = LEFT, keyEventType = type)
      }
    )

    ToggleButton(
      text = "Ctrl",
      parent = panel,
      elementId = "toggleCtrl",
      onStateChange = { newState ->
        specialKeysState.isCtrlEnabled = newState

        val key = JsKey("Control")
        val code = VK.CONTROL
        val type = newState.toKeyEventType()

        fireKeyEvent(key = key, code = code, location = LEFT, keyEventType = type)
      }
    )

    ToggleButton(
      text = "Shift",
      parent = panel,
      elementId = "toggleShift",
      onStateChange = { newState ->
        specialKeysState.isShiftEnabled = newState

        val key = JsKey("Shift")
        val code = VK.SHIFT
        val type = newState.toKeyEventType()

        fireKeyEvent(key = key, code = code, location = LEFT, keyEventType = type)
      }
    )

    ToggleButton(
      text = "F*",
      parent = panel,
      elementId = "toggleFunctionalKeys",
      onStateChange = { newState ->
        functionalButtonsPanel.style.display = newState.toDisplayType()
      }
    )

    if (ParamsProvider.INPUT_METHOD_TYPE == ParamsProvider.InputMethodType.OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD) {
      ToggleButton(
        text = "⌨",  // Keyboard symbol
        parent = panel,
        elementId = "toggleInput",
        onStateChange = { newState ->
          when (newState) {
            true -> {
              virtualKeyboardEnabled = true
              virtualKeyboardInput.focus()
              virtualKeyboardInput.click()
            }
            false -> {
              virtualKeyboardEnabled = false
              virtualKeyboardInput.blur()
            }
          }
        }
      )

      document.addEventListener("selectionchange", this::handleVirtualKeyboardSelection)
    }
  }

  override fun dispose() {
    panel.remove()
    virtualKeyboardInput.remove()

    if (ParamsProvider.INPUT_METHOD_TYPE == ParamsProvider.InputMethodType.OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD) {
      document.removeEventListener("selectionchange", this::handleVirtualKeyboardSelection)
    }
  }

  private class ToggleButton(
    text: String,
    parent: Node,
    elementId: String,
    private val onStateChange: (newState: Boolean) -> Unit,
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

      id = elementId

      innerText = text

      onclick = { e ->
        flipFlop()
        e.stopPropagation()
      }
      onmousedown = { e -> e.stopPropagation() }
      onmouseup = { e -> e.stopPropagation() }
      asDynamic().ontouchstart = { e: TouchEvent ->
        e.stopPropagation()
        e.preventDefault()
      }
      asDynamic().ontouchmove = { e: TouchEvent ->
        e.stopPropagation()
        e.preventDefault()
      }
      asDynamic().ontouchend = { e: TouchEvent ->
        flipFlop()
        e.stopPropagation()
        e.preventDefault()
      }

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
    elementId: String,
    private val onClick: () -> Unit,
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

      id = elementId

      innerText = text

      onclick = { e ->
        animate()
        onClick()
        e.stopPropagation()
      }
      onmousedown = MouseEvent::stopPropagation
      onmouseup = MouseEvent::stopPropagation
      asDynamic().ontouchstart = { e: TouchEvent ->
        e.stopPropagation()
        e.preventDefault()
      }
      asDynamic().ontouchmove = { e: TouchEvent ->
        e.stopPropagation()
        e.preventDefault()
      }
      asDynamic().ontouchend = { e: TouchEvent ->
        animate()
        onClick()
        e.stopPropagation()
        e.preventDefault()
      }

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

    private const val TEXTAREA_VALUE = "\n----\n"

    private const val MINIMUM_MS_BETWEEN_INPUT_AND_SELECTION_CHANGE = 100

    private val logger = Logger<MobileKeyboardHelper>()
  }
}

class ImeHelper(
  private val openingTimeStamp: Int,
  private val clientEventConsumer: (ClientEvent) -> Unit,
) : InputMethodHelper {

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
      GlobalScope.launch {
        val message = ClientKeyPressEvent(
          timeStamp = TimeStamp.current.roundToInt() - openingTimeStamp,
          char = char,
          modifiers = if (char.isUpperCase()) setOf(KeyModifier.SHIFT_KEY) else setOf(),  // todo: use modifiers of the last KEY_UP
        )
        clientEventConsumer(message)
      }
    }

    var composing = false

    addEventListener("compositionstart", { composing = true })

    addEventListener(
      "compositionend",
      { event: Event ->
        clearInputField()
        composing = false

        event as CompositionEvent
        event.data.forEach { char ->
          fireKeyPress(char)
        }
      }
    )

    val invisibleCodes: Set<VK> = setOf(
      VK.SHIFT,
      VK.CONTROL,
      VK.META,
      VK.ALT,
      VK.ALT_GRAPH,
      VK.CAPS_LOCK,
      VK.LEFT,
      VK.HOME,
      VK.END,
      VK.PAGE_UP,
      VK.PAGE_DOWN,
      VK.RIGHT,
      VK.UP,
      VK.DOWN,
      VK.KP_LEFT,
      VK.KP_RIGHT,
      VK.KP_UP,
      VK.KP_DOWN,
      *(1..24).map { "F$it" }.map { VK.valueOf(it) }.toTypedArray(),
    )

    fun fireKeyEvent(type: ClientKeyEvent.KeyEventType, event: KeyboardEvent) {
      if (event.key == "Process") {
        return
      }

      val isKeystroke = event.ctrlKey ||  // like Ctrl+S
                        event.metaKey  // like Cmd+N
      val isPasteKeystroke = isKeystroke && (event.code == "KeyV")

      if (!isPasteKeystroke) {  // don't block paste keystroke
        event.preventDefault()  // prevent all defaults (because of this we need to generate PRESS events ourselves)
      }

      GlobalScope.launch {
        if (isPasteKeystroke) {
          // let "paste" event be generated by the browser, be caught by us, and go to server and only after it send the keystroke
          delay(5L * (ParamsProvider.FLUSH_DELAY ?: 10))
        }

        val vk = codeToVk(event.code)
        val char = keyToChar(JsKey(event.key), vk)

        val message = ClientKeyEvent(
          timeStamp = event.timeStamp.toInt() - openingTimeStamp,
          char = char,
          code = vk,
          location = toCommonKeyLocation(event.location, event.code),
          modifiers = event.modifiers,
          keyEventType = type
        ).orCtrlQ()

        if (type == ClientKeyEvent.KeyEventType.DOWN && vk == VK.F10 && KeyModifier.CTRL_KEY in message.modifiers) {  // todo: move to client state
          ClientStats.printStats()
        }

        if (type == ClientKeyEvent.KeyEventType.DOWN && vk == VK.F11 && KeyModifier.CTRL_KEY in message.modifiers) {  // todo: move to client state
          (ParamsProvider.REPAINT_AREA as? RepaintAreaSetting.Enabled)?.let {
            it.show = !it.show
          }
        }

        clientEventConsumer(message)

        if (type == ClientKeyEvent.KeyEventType.DOWN && message.code !in invisibleCodes) {
          // we've blocked special keys like Tab to stop the browser react
          // but we also disabled generation of PRESS events
          // so need to send them manually
          clientEventConsumer(ClientKeyPressEvent(
            timeStamp = message.timeStamp,
            char = message.char,
            modifiers = message.modifiers,
          ))
        }
      }
    }

    // Ctrl+* – prevent default (todo: Ctrl+V – don't prevent default, but ignore further input to the field)
    // Tab – prevent default
    // others – don't prevent default
    onkeydown = {
      fireKeyEvent(DOWN, it)
    }

    onkeyup = {
      fireKeyEvent(UP, it)
    }

    oninput = fun(event: InputEvent) {
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

  companion object {

    private val KeyboardEvent.modifiers: Set<KeyModifier>
      get() {
        val modifiers = mutableSetOf<KeyModifier>()

        if (shiftKey) {
          modifiers.add(KeyModifier.SHIFT_KEY)
        }
        if (ctrlKey) {
          modifiers.add(KeyModifier.CTRL_KEY)
        }
        if (altKey) {
          modifiers.add(KeyModifier.ALT_KEY)
        }
        if (metaKey) {
          modifiers.add(KeyModifier.META_KEY)
        }
        if (repeat) {
          modifiers.add(KeyModifier.REPEAT)
        }

        return modifiers
      }

    private val logger = Logger<ImeHelper>()
  }
}
