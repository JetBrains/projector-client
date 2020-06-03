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
package org.jetbrains.projector.client.web

import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.RepaintAreaSetting
import org.jetbrains.projector.client.web.misc.ClientStats
import org.jetbrains.projector.client.web.misc.toScrollingMode
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.window.DragEventsInterceptor
import org.jetbrains.projector.client.web.window.WindowManager
import org.jetbrains.projector.common.protocol.toServer.*
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.roundToInt

class InputController(private val openingTimeStamp: Int,
                      private val stateMachine: ClientStateMachine,
                      private val windowManager: WindowManager) {

  private val mouseButtonsDown = mutableSetOf<Short>()

  private var eventsInterceptor: DragEventsInterceptor? = null

  private fun handleMouseMoveEvent(event: Event) {
    require(event is MouseEvent)

    if (mouseButtonsDown.isEmpty()) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.MOVE, event)
    }
    else {
      if (eventsInterceptor != null) {
        eventsInterceptor!!.onMouseMove(event.clientX, event.clientY)
      }
      else {
        fireMouseEvent(ClientMouseEvent.MouseEventType.DRAG, event)
      }
    }
  }

  private fun handleMouseDownEvent(event: Event) {
    require(event is MouseEvent)

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY)
    eventsInterceptor = topWindow?.onMouseDown(event)
    if (eventsInterceptor == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.DOWN, event)
    }
    else {
      windowManager.bringToFront(topWindow!!)
    }
    mouseButtonsDown.add(event.button)
  }

  private fun handleMouseUpEvent(event: Event) {
    require(event is MouseEvent)
    if (eventsInterceptor != null) {
      eventsInterceptor!!.onMouseUp(event.clientX, event.clientY)
      eventsInterceptor = null
    }
    else {
      fireMouseEvent(ClientMouseEvent.MouseEventType.UP, event)
    }
    mouseButtonsDown.remove(event.button)
  }

  private fun handleClickEvent(event: Event) {
    require(event is MouseEvent)
    if (windowManager.getTopWindow(event.clientX, event.clientY)?.onMouseClick(event) == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.CLICK, event)
    }
  }

  // This is extremely dangerous method, because it is called when mouse leave ANY canvas inside document!
  private fun handleMouseOutEvent(event: Event) {
    require(event is MouseEvent)
    fireMouseEvent(ClientMouseEvent.MouseEventType.OUT, event)
  }

  private fun handleKeyDownEvent(event: Event) {
    require(event is KeyboardEvent)
    fireKeyEvent(ClientKeyEvent.KeyEventType.DOWN, event)
  }

  private fun handleKeyUpEvent(event: Event) {
    require(event is KeyboardEvent)
    fireKeyEvent(ClientKeyEvent.KeyEventType.UP, event)
  }

  private fun handleClipboardChange(event: Event) {
    require(event is ClipboardEvent)

    val stringContent = event.clipboardData?.getData("text/plain") ?: return

    stateMachine.fire(ClientAction.AddEvent(ClientClipboardEvent(stringContent)))
  }

  private val documentActionListeners = mapOf<String, (Event) -> Unit>(
    "paste" to ::handleClipboardChange,
    "mousemove" to ::handleMouseMoveEvent,
    "mousedown" to ::handleMouseDownEvent,
    "mouseup" to ::handleMouseUpEvent,
    "click" to ::handleClickEvent,
    "mouseout" to ::handleMouseOutEvent,
    "wheel" to ::fireWheelEvent,
    "keydown" to ::handleKeyDownEvent,
    "keyup" to ::handleKeyUpEvent,
    "keypress" to ::fireKeyPressEvent
  )

  fun addListeners() {
    documentActionListeners.forEach { (type, handler) ->
      document.addEventListener(type, handler)
    }
  }

  fun removeListeners() {
    documentActionListeners.forEach { (type, handler) ->
      document.removeEventListener(type, handler)
    }
  }

  private fun fireKeyPressEvent(event: Event) {
    require(event is KeyboardEvent)

    val message = ClientKeyPressEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      key = event.key,
      modifiers = event.modifiers
    )

    if (message.key.toLowerCase() == "v" && KeyModifier.CTRL_KEY in message.modifiers) {
      // let "paste" event go to server and only after it send the keystroke
      window.setTimeout(
        handler = { stateMachine.fire(ClientAction.AddEvent(message)) },
        timeout = 5 * (ParamsProvider.FLUSH_DELAY ?: 10)
      )
    }
    else {
      stateMachine.fire(ClientAction.AddEvent(message))
    }
  }

  private fun fireWheelEvent(event: Event) {
    require(event is WheelEvent)

    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO

    val message = ClientWheelEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      modifiers = event.modifiers,
      mode = event.deltaMode.toScrollingMode(),
      x = (event.clientX / userScalingRatio).roundToInt(),
      y = (event.clientY / userScalingRatio).roundToInt(),
      deltaX = event.deltaX,
      deltaY = event.deltaY
    )

    stateMachine.fire(ClientAction.AddEvent(message))
  }

  private fun fireMouseEvent(type: ClientMouseEvent.MouseEventType, event: MouseEvent) {
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO

    val message = ClientMouseEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      x = (event.clientX / userScalingRatio).roundToInt(),
      y = (event.clientY / userScalingRatio).roundToInt(),
      button = event.button,
      clickCount = event.detail,
      modifiers = event.modifiers,
      mouseEventType = type
    )

    stateMachine.fire(ClientAction.AddEvent(message))
  }

  private fun fireKeyEvent(type: ClientKeyEvent.KeyEventType, event: KeyboardEvent) {
    val message = ClientKeyEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      key = event.key,
      code = event.code,
      location = event.location.toCommonKeyLocation(),
      modifiers = event.modifiers,
      keyEventType = type
    )

    val isBrowserSpecialKey = message.key.length > 1  // like Tab
    val isKeystroke = KeyModifier.CTRL_KEY in message.modifiers  // like Ctrl+S
    val isBrowserKeyStroke = isKeystroke && (message.code != "KeyV")  // don't block paste keystroke

    if (type == ClientKeyEvent.KeyEventType.DOWN && event.key == "F10") {  // todo: move to client state
      ClientStats.printStats()
    }

    if (type == ClientKeyEvent.KeyEventType.DOWN && event.key == "F11") {  // todo: move to client state
      (ParamsProvider.REPAINT_AREA as? RepaintAreaSetting.Enabled)?.let {
        it.show = !it.show
      }
    }

    if (isBrowserSpecialKey || isBrowserKeyStroke) {  // can't prevent all defaults because PRESS events won't be generated
      event.preventDefault()
    }

    if (isKeystroke && message.code == "KeyV") {
      // let "paste" event go to server and only after it send the keystroke
      window.setTimeout(
        handler = { stateMachine.fire(ClientAction.AddEvent(message)) },
        timeout = 5 * (ParamsProvider.FLUSH_DELAY ?: 10)
      )
    }
    else {
      stateMachine.fire(ClientAction.AddEvent(message))
    }
  }

  companion object {

    private val MouseEvent.modifiers: Set<MouseModifier>
      get() {
        val modifiers = mutableSetOf<MouseModifier>()

        if (shiftKey) {
          modifiers.add(MouseModifier.SHIFT_KEY)
        }
        if (ctrlKey) {
          modifiers.add(MouseModifier.CTRL_KEY)
        }
        if (altKey) {
          modifiers.add(MouseModifier.ALT_KEY)
        }
        if (metaKey) {
          modifiers.add(MouseModifier.META_KEY)
        }

        return modifiers
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

    private fun Int.toCommonKeyLocation() = when (this) {
      KeyboardEvent.DOM_KEY_LOCATION_STANDARD -> ClientKeyEvent.KeyLocation.STANDARD
      KeyboardEvent.DOM_KEY_LOCATION_LEFT -> ClientKeyEvent.KeyLocation.LEFT
      KeyboardEvent.DOM_KEY_LOCATION_RIGHT -> ClientKeyEvent.KeyLocation.RIGHT
      KeyboardEvent.DOM_KEY_LOCATION_NUMPAD -> ClientKeyEvent.KeyLocation.NUMPAD
      else -> throw IllegalArgumentException("Bad key location: $this")
    }
  }
}
