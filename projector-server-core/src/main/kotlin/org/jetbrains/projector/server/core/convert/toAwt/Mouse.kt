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
package org.jetbrains.projector.server.core.convert.toAwt

import org.jetbrains.projector.common.protocol.toServer.ClientMouseEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWheelEvent
import org.jetbrains.projector.common.protocol.toServer.MouseModifier
import org.jetbrains.projector.server.core.ReadyClientSettings.TouchState
import org.jetbrains.projector.server.core.convert.toClient.roundToInfinity
import org.jetbrains.projector.util.loading.getOption
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.absoluteValue

private const val DEFAULT_SCROLL_AMOUNT = 1

// todo: 100 is just a random but reasonable number;
//       need to calculate this number from the context,
//       maybe use the client's scaling ratio
private const val DEFAULT_PIXEL_PER_UNIT = "100"

public const val PIXEL_PER_UNIT_ENV_NAME: String = "ORG_JETBRAINS_PROJECTOR_SERVER_PIXEL_PER_UNIT"
private val PIXEL_PER_UNIT = getOption(PIXEL_PER_UNIT_ENV_NAME, DEFAULT_PIXEL_PER_UNIT).toInt()

// todo: 3 is a wild guess (scaling factor of mobile devices), need to get this number from the context
private val TOUCH_PIXEL_PER_UNIT = 3 * PIXEL_PER_UNIT
private const val PIXEL_DELTA_ENOUGH_FOR_SCROLLING = 10

public fun calculateNewTouchState(
  shiftedMessage: ClientMouseEvent,
  message: ClientMouseEvent,
  currentTouchState: TouchState,
): TouchState? {
  fun isEnoughDeltaForScrolling(previousTouchState: TouchState.Scrolling, newX: Int, newY: Int): Boolean {
    // reduce number of scroll events to make deltas bigger.
    // this helps when to generate proper MouseWheelEvents with correct transformation of pixels to scroll units

    return (newX - previousTouchState.lastX).absoluteValue > PIXEL_DELTA_ENOUGH_FOR_SCROLLING ||
           (newY - previousTouchState.lastY).absoluteValue > PIXEL_DELTA_ENOUGH_FOR_SCROLLING
  }

  return when (shiftedMessage.mouseEventType) {
    ClientMouseEvent.MouseEventType.UP -> TouchState.Released
    ClientMouseEvent.MouseEventType.DOWN -> TouchState.OnlyPressed(message.timeStamp, shiftedMessage.x, shiftedMessage.y)
    ClientMouseEvent.MouseEventType.TOUCH_DRAG -> when (currentTouchState) {
      is TouchState.Scrolling -> when (isEnoughDeltaForScrolling(currentTouchState, shiftedMessage.x, shiftedMessage.y)) {
        true -> TouchState.Scrolling(currentTouchState.initialX, currentTouchState.initialY, shiftedMessage.x, shiftedMessage.y)
        false -> null
      }
      is TouchState.Dragging -> TouchState.Dragging
      is TouchState.OnlyPressed -> when (currentTouchState.connectionMillis + 500 < shiftedMessage.timeStamp) {
        true -> TouchState.Dragging
        false -> TouchState.Scrolling(currentTouchState.lastX, currentTouchState.lastY, shiftedMessage.x, shiftedMessage.y)
      }
      is TouchState.Released -> TouchState.Released  // drag events shouldn't come when touch is not pressing so let's skip it
    }
    else -> currentTouchState
  }
}

public fun createMouseEvent(
  source: Component,
  event: ClientMouseEvent,
  previousTouchState: TouchState,
  newTouchState: TouchState,
  connectionMillis: Long,
): MouseEvent {
  val locationOnScreen = source.locationOnScreen

  val id = when (event.mouseEventType) {
    ClientMouseEvent.MouseEventType.MOVE -> MouseEvent.MOUSE_MOVED
    ClientMouseEvent.MouseEventType.DOWN -> MouseEvent.MOUSE_PRESSED
    ClientMouseEvent.MouseEventType.UP -> MouseEvent.MOUSE_RELEASED
    ClientMouseEvent.MouseEventType.CLICK -> MouseEvent.MOUSE_CLICKED
    ClientMouseEvent.MouseEventType.OUT -> MouseEvent.MOUSE_EXITED
    ClientMouseEvent.MouseEventType.DRAG -> MouseEvent.MOUSE_DRAGGED
    ClientMouseEvent.MouseEventType.TOUCH_DRAG -> {
      if (previousTouchState is TouchState.WithCoordinates && newTouchState is TouchState.Scrolling) {
        val deltaX = newTouchState.lastX - previousTouchState.lastX
        val deltaY = newTouchState.lastY - previousTouchState.lastY

        fun isHorizontal(): Boolean {
          return deltaX.absoluteValue > deltaY.absoluteValue
        }

        val (wheelDelta, modifiers) = if (isHorizontal()) {
          deltaX to (event.modifiers.toMouseInt() or InputEvent.SHIFT_DOWN_MASK)
        }
        else {
          deltaY to event.modifiers.toMouseInt()
        }

        val negatedWheelDelta = -wheelDelta  // touch scrolling is usually treated in reverse direction

        val normalizedWheelDelta = negatedWheelDelta.toDouble() / TOUCH_PIXEL_PER_UNIT
        val notNullNormalizedWheelDelta = roundToInfinity(normalizedWheelDelta).toInt()

        return MouseWheelEvent(
          source,
          MouseEvent.MOUSE_WHEEL, connectionMillis + event.timeStamp, modifiers,
          newTouchState.initialX - locationOnScreen.x, newTouchState.initialY - locationOnScreen.y,
          newTouchState.initialX - locationOnScreen.x, newTouchState.initialY - locationOnScreen.y, 0, false,
          MouseWheelEvent.WHEEL_UNIT_SCROLL, DEFAULT_SCROLL_AMOUNT, notNullNormalizedWheelDelta, normalizedWheelDelta
        )
      }

      MouseEvent.MOUSE_DRAGGED
    }
  }

  val awtEventButton = when (event.mouseEventType) {
    ClientMouseEvent.MouseEventType.MOVE,
    ClientMouseEvent.MouseEventType.OUT,
    -> MouseEvent.NOBUTTON

    else -> event.button + 1
  }

  val modifiers = event.modifiers.toMouseInt()
  val buttonModifier = if (awtEventButton == MouseEvent.NOBUTTON || event.mouseEventType == ClientMouseEvent.MouseEventType.UP) {
    0
  }
  else {
    InputEvent.getMaskForButton(awtEventButton)
  }

  val canTriggerPopup = awtEventButton == MouseEvent.BUTTON3 && id == MouseEvent.MOUSE_PRESSED

  return MouseEvent(
    source,
    id,
    connectionMillis + event.timeStamp,
    modifiers or buttonModifier,
    event.x - locationOnScreen.x, event.y - locationOnScreen.y,
    event.clickCount, canTriggerPopup, awtEventButton
  )
}

public fun createMouseWheelEvent(source: Component, event: ClientWheelEvent, connectionMillis: Long): MouseWheelEvent {
  fun isHorizontal(event: ClientWheelEvent): Boolean {
    return event.deltaX.absoluteValue > event.deltaY.absoluteValue
  }

  val (wheelDelta, modifiers) = if (isHorizontal(event)) {
    event.deltaX to (event.modifiers.toMouseInt() or InputEvent.SHIFT_DOWN_MASK)
  }
  else {
    event.deltaY to event.modifiers.toMouseInt()
  }

  val (mode, normalizedWheelDelta) = when (event.mode) {
    ClientWheelEvent.ScrollingMode.PIXEL -> MouseWheelEvent.WHEEL_UNIT_SCROLL to wheelDelta / PIXEL_PER_UNIT
    ClientWheelEvent.ScrollingMode.LINE -> MouseWheelEvent.WHEEL_UNIT_SCROLL to wheelDelta
    ClientWheelEvent.ScrollingMode.PAGE -> MouseWheelEvent.WHEEL_BLOCK_SCROLL to wheelDelta
  }
  val notNullNormalizedWheelDelta = roundToInfinity(normalizedWheelDelta).toInt()

  val locationOnScreen = source.locationOnScreen

  return MouseWheelEvent(
    source, MouseEvent.MOUSE_WHEEL, connectionMillis + event.timeStamp, modifiers,
    event.x - locationOnScreen.x, event.y - locationOnScreen.y,
    event.x - locationOnScreen.x, event.y - locationOnScreen.y, 0, false,
    mode, DEFAULT_SCROLL_AMOUNT, notNullNormalizedWheelDelta, normalizedWheelDelta
  )
}

private val mouseModifierMask = mapOf(
  MouseModifier.ALT_KEY to InputEvent.ALT_DOWN_MASK,
  MouseModifier.CTRL_KEY to InputEvent.CTRL_DOWN_MASK,
  MouseModifier.SHIFT_KEY to InputEvent.SHIFT_DOWN_MASK,
  MouseModifier.META_KEY to InputEvent.META_DOWN_MASK
)

private fun Set<MouseModifier>.toMouseInt(): Int {
  return map(mouseModifierMask::getValue).fold(0, Int::or)
}
