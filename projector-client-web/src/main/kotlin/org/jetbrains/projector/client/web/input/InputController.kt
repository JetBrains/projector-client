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
package org.jetbrains.projector.client.web.input

import kotlinx.browser.document
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.web.input.key.ImeInputMethod
import org.jetbrains.projector.client.web.input.key.LegacyInputMethod
import org.jetbrains.projector.client.web.input.key.MobileInputMethod
import org.jetbrains.projector.client.web.misc.toScrollingMode
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.window.DragEventsInterceptor
import org.jetbrains.projector.client.web.window.Positionable
import org.jetbrains.projector.client.web.window.WebWindowManager
import org.jetbrains.projector.common.protocol.toClient.ServerCaretInfoChangedEvent
import org.jetbrains.projector.common.protocol.toServer.*
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.get
import kotlin.math.roundToInt

class InputController(
  private val openingTimeStamp: Int,
  private val stateMachine: ClientStateMachine,
  private val windowManager: WebWindowManager,
  windowPositionByIdGetter: (windowId: Int) -> Positionable?,
) {

  private val specialKeysState = SpecialKeysState()

  private val inputMethod = { event: ClientEvent -> stateMachine.fire(ClientAction.AddEvent(event)) }.let {
    when (ParamsProvider.INPUT_METHOD_TYPE) {
      ParamsProvider.InputMethodType.LEGACY -> LegacyInputMethod(openingTimeStamp, specialKeysState, it)
      ParamsProvider.InputMethodType.IME -> ImeInputMethod(openingTimeStamp, it, windowPositionByIdGetter)
      ParamsProvider.InputMethodType.OVERLAY_BUTTONS,
      -> MobileInputMethod(openingTimeStamp, specialKeysState, false, it)
      ParamsProvider.InputMethodType.OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD,
      -> MobileInputMethod(openingTimeStamp, specialKeysState, true, it)
    }
  }

  private val mouseButtonsDown = mutableSetOf<Short>()

  private var eventsInterceptor: DragEventsInterceptor? = null

  private var lastTouchStartTimeStamp = TimeStamp.current
  private var touchClickCount = 1
  private var lastTouchX = 1
  private var lastTouchY = 1

  private fun handleMouseMoveEvent(event: Event) {
    require(event is MouseEvent)

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY)
    if (mouseButtonsDown.isEmpty()) {
      topWindow?.let { fireMouseEvent(ClientMouseEvent.MouseEventType.MOVE, it.id, event) }
    }
    else {
      if (eventsInterceptor != null) {
        eventsInterceptor!!.onMouseMove(event.clientX, event.clientY)
      }
      else {
        topWindow?.let { fireMouseEvent(ClientMouseEvent.MouseEventType.DRAG, it.id, event) }
      }
    }
  }

  private fun handleTouchMoveEvent(event: Event) {
    require(event is TouchEvent)
    event.preventDefault()

    val touch = event.changedTouches[0] ?: return
    val topWindow = windowManager.getTopWindow(touch.clientX, touch.clientY)

    if (mouseButtonsDown.isEmpty()) {
      topWindow?.let { fireMouseEvent(ClientMouseEvent.MouseEventType.MOVE, it.id, event, x = touch.clientX, y = touch.clientY) }
    }
    else {
      if (eventsInterceptor != null) {
        eventsInterceptor!!.onMouseMove(touch.clientX, touch.clientY)
      }
      else {
        topWindow?.let { fireMouseEvent(ClientMouseEvent.MouseEventType.TOUCH_DRAG, it.id, event, x = touch.clientX, y = touch.clientY) }
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

    if (eventsInterceptor != null) {
      eventsInterceptor!!.onMouseUp(touch.clientX, touch.clientY)
      eventsInterceptor = null
    }
    else {
      val topWindow = windowManager.getTopWindow(touch.clientX, touch.clientY)

      val (x, y) = if (event.timeStamp.toDouble() - lastTouchStartTimeStamp < DOUBLE_CLICK_DELTA_MS) {
        lastTouchX to lastTouchY
      }
      else {
        touch.clientX to touch.clientY
      }

      topWindow?.let { fireMouseEvent(ClientMouseEvent.MouseEventType.UP, it.id, event, x = x, y = y) }

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
    mouseButtonsDown.remove(LEFT_MOUSE_BUTTON_ID)
  }

  private fun handleClickEvent(event: Event) {
    require(event is MouseEvent)
    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    if (topWindow.onMouseClick(event.clientX, event.clientY) == null) {
      fireMouseEvent(ClientMouseEvent.MouseEventType.CLICK, topWindow.id, event)
    }
  }

  // Translate a "oncontextmenu" event into a click with the right mouse button.
  // "onclick" is only called for clicks with the left mouse button.
  private fun handleContextMenuEvent(event: Event) {
    require(event is MouseEvent)
    event.preventDefault()

    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    if (topWindow.onMouseClick(event.clientX, event.clientY) == null) {
      fireMouseEvent(
        type = ClientMouseEvent.MouseEventType.CLICK,
        windowId = topWindow.id,
        eventTimeStamp = event.timeStamp,
        x = event.clientX,
        y = event.clientY,
        button = 2, // 2 is the right mouse button
        clickCount = event.detail,
        modifiers = event.modifiers
      )
    }
  }

  // This is extremely dangerous method, because it is called when mouse leave ANY canvas inside document!
  private fun handleMouseOutEvent(event: Event) {
    require(event is MouseEvent)
    val topWindow = windowManager.getTopWindow(event.clientX, event.clientY) ?: return
    fireMouseEvent(ClientMouseEvent.MouseEventType.OUT, topWindow.id, event)
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
      "contextmenu" to ::handleContextMenuEvent,
      "mouseout" to ::handleMouseOutEvent,
      "wheel" to ::fireWheelEvent
    ))
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
    inputMethod.dispose()
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

  fun handleCaretInfoChange(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
    inputMethod.handleCaretInfoChange(caretInfoChange)
  }

  private companion object {

    private const val LEFT_MOUSE_BUTTON_ID: Short = 0

    private const val DOUBLE_CLICK_DELTA_MS = 500
  }
}
