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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.RepaintAreaSetting
import org.jetbrains.projector.client.web.input.JsKey
import org.jetbrains.projector.client.web.input.SpecialKeysState
import org.jetbrains.projector.client.web.input.layout.FrAzerty
import org.jetbrains.projector.client.web.input.layout.KeyboardApiLayout
import org.jetbrains.projector.client.web.input.layout.UsQwerty
import org.jetbrains.projector.client.web.misc.ClientStats
import org.jetbrains.projector.common.protocol.data.VK
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyEvent.KeyLocation.*
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

// todo: try to move to VK field, rename to canBeTyped
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

private val jsCodeLocation = mapOf(
  "ControlLeft" to LEFT,
  "MetaLeft" to LEFT,
  "ShiftLeft" to LEFT,
  "AltLeft" to LEFT,
  "ControlRight" to RIGHT,
  "MetaRight" to RIGHT,
  "ShiftRight" to RIGHT,
  "AltRight" to RIGHT,
)

fun toCommonKeyLocation(location: Int, code: String) = jsCodeLocation[code] ?: when (location) {
  KeyboardEvent.DOM_KEY_LOCATION_STANDARD -> STANDARD
  KeyboardEvent.DOM_KEY_LOCATION_LEFT -> LEFT
  KeyboardEvent.DOM_KEY_LOCATION_RIGHT -> RIGHT
  KeyboardEvent.DOM_KEY_LOCATION_NUMPAD -> NUMPAD
  else -> throw IllegalArgumentException("Bad key location: $location")
}

fun keyToChar(key: JsKey, code: VK): Char {
  key.key.singleOrNull()?.let { return it }

  return code.typedSymbols.singleOrNull() ?: CHAR_UNDEFINED
}

private const val CHAR_UNDEFINED: Char = 0xFFFF.toChar()

fun ClientKeyEvent.orCtrlQ(): ClientKeyEvent {
  if (this@orCtrlQ.code != VK.F1) {
    return this@orCtrlQ
  }

  return this@orCtrlQ.copy(
    char = 'q',
    code = VK.Q,
    modifiers = setOf(KeyModifier.CTRL_KEY),
  )
}

suspend fun codeToVk(code: String): VK {
  KeyboardApiLayout.getVirtualKey(code)?.let {
    return it
  }

  when (ParamsProvider.LAYOUT_TYPE) {
    ParamsProvider.LayoutType.JS_DEFAULT -> UsQwerty
    ParamsProvider.LayoutType.FR_AZERTY -> FrAzerty
  }.getVirtualKey(code)?.let { return it }

  return VK.UNDEFINED
}

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

fun mergeModifiers(event: KeyboardEvent, specialKeysState: SpecialKeysState?): Set<KeyModifier> =
  event.modifiers.union(specialKeysState?.keyModifiers.orEmpty())

fun ((ClientEvent) -> Unit).fireKeyEvent(
  type: ClientKeyEvent.KeyEventType,
  event: KeyboardEvent,
  openingTimeStamp: Int,
  specialKeysState: SpecialKeysState? = null,
) {
  val isKeystroke = event.ctrlKey || event.metaKey || event.altKey || event.shiftKey
  val isPasteKeystroke = isKeystroke && (event.code == "KeyV")

  if (!isPasteKeystroke) {  // don't block paste keystroke
    event.preventDefault()  // prevent all defaults like Ctrl+S and Cmd+N (because of this we need to generate PRESS events ourselves)
  }

  GlobalScope.launch {
    val vk = codeToVk(event.code)
    val char = keyToChar(JsKey(event.key), vk)

    if (isKeystroke || vk in setOf(VK.CONTROL, VK.META, VK.ALT, VK.SHIFT)) {
      // delay all the typing with control because it can be paste:
      // let "paste" event be generated by the browser, be caught by us, and go to server and only after it send the keystroke
      delay(5L * (ParamsProvider.FLUSH_DELAY ?: 10))
    }

    val message = ClientKeyEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      char = if (specialKeysState?.isShiftEnabled == true) char.uppercaseChar() else char,
      code = vk,
      location = toCommonKeyLocation(event.location, event.code),
      modifiers = mergeModifiers(event, specialKeysState),
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

    this@fireKeyEvent(message)

    if (type == ClientKeyEvent.KeyEventType.DOWN && message.code !in invisibleCodes) {
      // we've blocked special keys like Tab to stop the browser react
      // but we also disabled generation of PRESS events
      // so need to send them manually
      this@fireKeyEvent(ClientKeyPressEvent(
        timeStamp = message.timeStamp,
        char = message.char,
        modifiers = message.modifiers,
      ))
    }
  }
}

fun handleKeyboardEvent(
  type: ClientKeyEvent.KeyEventType,
  clientEventConsumer: (ClientEvent) -> Unit,
  openingTimeStamp: Int,
  specialKeysState: SpecialKeysState,
) = { event: Event ->
  require(event is KeyboardEvent)
  clientEventConsumer.fireKeyEvent(type, event, openingTimeStamp, specialKeysState)
}
