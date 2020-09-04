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

import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.RepaintAreaSetting
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.web.misc.ClientStats
import org.jetbrains.projector.client.web.misc.toScrollingMode
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.window.DragEventsInterceptor
import org.jetbrains.projector.client.web.window.WindowManager
import org.jetbrains.projector.common.protocol.toServer.*
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.*
import org.w3c.dom.get
import kotlin.math.roundToInt

class InputController(
  private val openingTimeStamp: Int,
  private val stateMachine: ClientStateMachine,
  private val windowManager: WindowManager,
) {

  val specialKeysState = SpecialKeysState()

  private val mouseButtonsDown = mutableSetOf<Short>()

  private var eventsInterceptor: DragEventsInterceptor? = null

  private var lastTouchStartTimeStamp = TimeStamp.current
  private var touchClickCount = 1
  private var lastTouchX = 1
  private var lastTouchY = 1

  private fun handleMouseMoveEvent(event: Event) {
    require(event is MouseEvent)

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    if (mouseButtonsDown.isEmpty()) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.MOVE, topWindow.id, event)
    }
    else {
      if (eventsInterceptor != null) {
        eventsInterceptor!!.onMouseMove(event.clientX, event.clientY)
      }
      else {
        fireMouseEvent(ClientMouseEvent.MouseEventType.DRAG, topWindow.id, event)
      }
    }
  }

  private fun handleTouchMoveEvent(event: Event) {
    require(event is TouchEvent)
    event.preventDefault()

    val touch = event.changedTouches[0] ?: return
    val topWindow = windowManager.getTopWindow(touch.clientX, touch.clientY) ?: return

    if (mouseButtonsDown.isEmpty()) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.MOVE, topWindow.id, event, x = touch.clientX, y = touch.clientY)
    }
    else {
      if (eventsInterceptor != null) {
        eventsInterceptor!!.onMouseMove(touch.clientX, touch.clientY)
      }
      else {
        fireMouseEvent(ClientMouseEvent.MouseEventType.TOUCH_DRAG, topWindow.id, event, x = touch.clientX, y = touch.clientY)
      }
    }
  }

  private fun handleMouseDownEvent(event: Event) {
    require(event is MouseEvent)

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    eventsInterceptor = topWindow.onMouseDown(event.clientX, event.clientY)
    if (eventsInterceptor == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.DOWN, topWindow.id, event)
    }
    else {
      windowManager.bringToFront(topWindow)
    }
    mouseButtonsDown.add(event.button)
  }

  private fun handleTouchStartEvent(event: Event) {
    require(event is TouchEvent)
    event.preventDefault()

    val touch = event.changedTouches[0] ?: return

    if (event.timeStamp.toDouble() - lastTouchStartTimeStamp < DOUBLE_CLICK_DELTA_MS) {
      ++touchClickCount
    }
    else {
      touchClickCount = 1
      lastTouchX = touch.clientX
      lastTouchY = touch.clientY
    }
    lastTouchStartTimeStamp = event.timeStamp.toDouble()

    val topWindow = windowManager.getTopWindow(touch.clientX, touch.clientY) ?: return
    eventsInterceptor = topWindow.onMouseDown(touch.clientX, touch.clientY)
    if (eventsInterceptor == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.DOWN, topWindow.id, event, x = lastTouchX, y = lastTouchY)
    }
    else {
      windowManager.bringToFront(topWindow)
    }
    mouseButtonsDown.add(LEFT_MOUSE_BUTTON_ID)
  }

  private fun handleMouseUpEvent(event: Event) {
    require(event is MouseEvent)
    if (eventsInterceptor != null) {
      eventsInterceptor!!.onMouseUp(event.clientX, event.clientY)
      eventsInterceptor = null
    }
    else {
      windowManager.getTopWindow(event.clientX, event.clientY)?.id?.let {
        fireMouseEvent(ClientMouseEvent.MouseEventType.UP, it, event)
      }
    }
    mouseButtonsDown.remove(event.button)
  }

  private fun handleTouchEndEvent(event: Event) {
    require(event is TouchEvent)
    event.preventDefault()

    val touch = event.changedTouches[0] ?: return
    val topWindow = windowManager.getTopWindow(touch.clientX, touch.clientY) ?: return

    val (x, y) = if (event.timeStamp.toDouble() - lastTouchStartTimeStamp < DOUBLE_CLICK_DELTA_MS) {
      lastTouchX to lastTouchY
    }
    else {
      touch.clientX to touch.clientY
    }

    if (eventsInterceptor != null) {
      eventsInterceptor!!.onMouseUp(touch.clientX, touch.clientY)
      eventsInterceptor = null
    }
    else {
      fireMouseEvent(ClientMouseEvent.MouseEventType.UP, topWindow.id, event, x = x, y = y)
    }
    mouseButtonsDown.remove(LEFT_MOUSE_BUTTON_ID)

    // Generate ClickEvent manually. It's needed but not generated automatically because we preventDefault to disable
    // generation of duplicating mouse events. If we allow generate mouse events but just skip some of them,
    // input via mouse will be impossible in mobile mode...
    val clickEventProperties = MouseEventInit(
      clientX = x,
      clientY = y,
      button = LEFT_MOUSE_BUTTON_ID,
      detail = touchClickCount,
      shiftKey = event.shiftKey,
      ctrlKey = event.ctrlKey,
      altKey = event.altKey,
      metaKey = event.metaKey
    )
    val clickEvent = MouseEvent("click", clickEventProperties)
    handleClickEvent(clickEvent)
  }

  private fun handleClickEvent(event: Event) {
    require(event is MouseEvent)
    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    if (topWindow.onMouseClick(event.clientX, event.clientY) == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.CLICK, topWindow.id, event)
    }
  }

  // This is extremely dangerous method, because it is called when mouse leave ANY canvas inside document!
  private fun handleMouseOutEvent(event: Event) {
    require(event is MouseEvent)
    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    fireMouseEvent(ClientMouseEvent.MouseEventType.OUT, topWindow.id, event)
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

  @OptIn(ExperimentalStdlibApi::class)
  private val documentActionListeners = buildMap<String, (Event) -> Unit> {
    putAll(mapOf(
      "paste" to ::handleClipboardChange,
      "touchstart" to ::handleTouchStartEvent,
      "touchend" to ::handleTouchEndEvent,
      "touchmove" to ::handleTouchMoveEvent,
      "mousemove" to ::handleMouseMoveEvent,
      "mousedown" to ::handleMouseDownEvent,
      "mouseup" to ::handleMouseUpEvent,
      "click" to ::handleClickEvent,
      "mouseout" to ::handleMouseOutEvent,
      "wheel" to ::fireWheelEvent
    ))

    if (ParamsProvider.MOBILE_SETTING != ParamsProvider.MobileSetting.ALL) {
      putAll(mapOf(
        "keydown" to ::handleKeyDownEvent,
        "keyup" to ::handleKeyUpEvent,
        "keypress" to ::fireKeyPressEvent
      ))
    }
  }

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

    val key = if (specialKeysState.isShiftEnabled) {
      event.key.toUpperCase()
    }
    else {
      event.key
    }

    val message = ClientKeyPressEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      key = key,
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

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO

    val message = ClientWheelEvent(
      timeStamp = event.timeStamp.toInt() - openingTimeStamp,
      windowId = topWindow.id,
      modifiers = event.modifiers,
      mode = event.deltaMode.toScrollingMode(),
      x = (event.clientX / userScalingRatio).roundToInt(),
      y = (event.clientY / userScalingRatio).roundToInt(),
      deltaX = event.deltaX,
      deltaY = event.deltaY
    )

    stateMachine.fire(ClientAction.AddEvent(message))
  }

  private fun fireMouseEvent(type: ClientMouseEvent.MouseEventType, windowId: Int, event: MouseEvent) = fireMouseEvent(
    type = type,
    windowId = windowId,
    eventTimeStamp = event.timeStamp,
    x = event.clientX,
    y = event.clientY,
    button = event.button,
    clickCount = event.detail,
    modifiers = event.modifiers
  )

  private fun fireMouseEvent(type: ClientMouseEvent.MouseEventType, windowId: Int, event: TouchEvent, x: Int, y: Int) = fireMouseEvent(
    type = type,
    windowId = windowId,
    eventTimeStamp = event.timeStamp,
    x = x,
    y = y,
    button = LEFT_MOUSE_BUTTON_ID,
    clickCount = touchClickCount,
    modifiers = event.modifiers
  )

  private fun fireMouseEvent(
    type: ClientMouseEvent.MouseEventType,
    windowId: Int,
    eventTimeStamp: Number,
    x: Int,
    y: Int,
    button: Short,
    clickCount: Int,
    modifiers: Set<MouseModifier>,
  ) {
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO

    val message = ClientMouseEvent(
      timeStamp = eventTimeStamp.toInt() - openingTimeStamp,
      windowId = windowId,
      x = (x / userScalingRatio).roundToInt(),
      y = (y / userScalingRatio).roundToInt(),
      button = button,
      clickCount = clickCount,
      modifiers = modifiers,
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

    if (type == ClientKeyEvent.KeyEventType.DOWN && event.key == "F10" && KeyModifier.CTRL_KEY in message.modifiers) {  // todo: move to client state
      ClientStats.printStats()
    }

    if (type == ClientKeyEvent.KeyEventType.DOWN && event.key == "F11" && KeyModifier.CTRL_KEY in message.modifiers) {  // todo: move to client state
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

      if (isBrowserSpecialKey && type == ClientKeyEvent.KeyEventType.DOWN) {
        // we've blocked special keys like Tab to stop the browser react
        // but we also disabled generation of PRESS events
        // so need to send them manually
        // (if somebody knows a way to stop browser reactions without blocking generation, please share!):

        when (message.key) {
          "Tab" -> stateMachine.fire(ClientAction.AddEvent(ClientKeyPressEvent(
            timeStamp = message.timeStamp,
            key = message.key,
            modifiers = message.modifiers,
          )))
        }
      }
    }
  }

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

      return modifiers.union(specialKeysState.mouseModifiers)
    }

  private val TouchEvent.modifiers: Set<MouseModifier>
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

      return modifiers.union(specialKeysState.mouseModifiers)
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

      return modifiers.union(specialKeysState.keyModifiers)
    }

  companion object {

    private fun Int.toCommonKeyLocation() = when (this) {
      KeyboardEvent.DOM_KEY_LOCATION_STANDARD -> ClientKeyEvent.KeyLocation.STANDARD
      KeyboardEvent.DOM_KEY_LOCATION_LEFT -> ClientKeyEvent.KeyLocation.LEFT
      KeyboardEvent.DOM_KEY_LOCATION_RIGHT -> ClientKeyEvent.KeyLocation.RIGHT
      KeyboardEvent.DOM_KEY_LOCATION_NUMPAD -> ClientKeyEvent.KeyLocation.NUMPAD
      else -> throw IllegalArgumentException("Bad key location: $this")
    }

    private const val LEFT_MOUSE_BUTTON_ID: Short = 0

    private const val DOUBLE_CLICK_DELTA_MS = 500
  }
}
