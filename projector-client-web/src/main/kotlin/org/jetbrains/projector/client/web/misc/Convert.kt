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
package org.jetbrains.projector.client.web.misc

import org.jetbrains.projector.common.protocol.data.CursorType
import org.jetbrains.projector.common.protocol.toServer.ClientWheelEvent
import org.jetbrains.projector.common.protocol.toServer.ResizeDirection
import org.w3c.dom.events.WheelEvent

fun ResizeDirection.toJsResizeDirection(): String = when (this) {
  ResizeDirection.NW -> "nw-resize"
  ResizeDirection.SW -> "sw-resize"
  ResizeDirection.NE -> "ne-resize"
  ResizeDirection.SE -> "se-resize"
  ResizeDirection.N -> "n-resize"
  ResizeDirection.W -> "w-resize"
  ResizeDirection.S -> "s-resize"
  ResizeDirection.E -> "e-resize"
}

fun CursorType.toJsCursorType(): String = when (this) {
  CursorType.DEFAULT -> "default"
  CursorType.CROSSHAIR -> "crosshair"
  CursorType.TEXT -> "text"
  CursorType.WAIT -> "wait"
  CursorType.SW_RESIZE -> "sw-resize"
  CursorType.SE_RESIZE -> "se-resize"
  CursorType.NW_RESIZE -> "nw-resize"
  CursorType.NE_RESIZE -> "ne-resize"
  CursorType.N_RESIZE -> "n-resize"
  CursorType.S_RESIZE -> "s-resize"
  CursorType.W_RESIZE -> "w-resize"
  CursorType.E_RESIZE -> "e-resize"
  CursorType.HAND -> "pointer"
  CursorType.MOVE -> "move"
  CursorType.CUSTOM -> "none"  // todo: the major use-case of "custom" is to hide the cursor in IDEA,
  //       so it's correct to use "none" for this, but need to support arbitrary cursors
}

fun Int.toScrollingMode(): ClientWheelEvent.ScrollingMode = when (this) {
  WheelEvent.DOM_DELTA_PIXEL -> ClientWheelEvent.ScrollingMode.PIXEL
  WheelEvent.DOM_DELTA_LINE -> ClientWheelEvent.ScrollingMode.LINE
  WheelEvent.DOM_DELTA_PAGE -> ClientWheelEvent.ScrollingMode.PAGE

  else -> throw IllegalArgumentException("Unknown scrolling mode")
}
