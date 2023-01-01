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
package org.jetbrains.projector.client.web.window

import kotlinx.browser.document
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.misc.toJsResizeDirection
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.data.Point
import org.jetbrains.projector.common.protocol.toServer.ResizeDirection
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

class WindowBorder(private val resizeable: Boolean) : DragEventsInterceptor, LafListener {

  var onResize: (deltaX: Int, deltaY: Int, direction: ResizeDirection) -> Unit = { _, _, _ -> }

  private val shadow = document.createElement("canvas") as HTMLCanvasElement
  private val canvas = document.createElement("canvas") as HTMLCanvasElement
  private val style = canvas.style

  private var resizeDirection: ResizeDirection? = null
  private var lastPoint = Point(-1.0, -1.0)

  var visible: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        style.display = if (value) "block" else "none"
        shadow.style.display = if (value) "block" else "none"
      }
    }

  var zIndex: Int = 0
    set(value) {
      if (field != value) {
        field = value
        style.zIndex = "$zIndex"
        shadow.style.zIndex = "${zIndex - 1}"
      }
    }

  var bounds: CommonRectangle = CommonRectangle(0.0, 0.0, 0.0, 0.0)
    set(value) {
      if (field == value) {
        return
      }
      field = value

      style.left = "${value.x}px"
      style.top = "${value.y}px"
      style.width = "${value.width}px"
      style.height = "${value.height}px"

      val thickness = ProjectorUI.borderThickness * ParamsProvider.USER_SCALING_RATIO

      val shadowStyle = shadow.style
      shadowStyle.left = "${value.x + thickness}px"
      shadowStyle.top = "${value.y + thickness}px"
      shadowStyle.width = "${value.width - thickness * 2}px"
      shadowStyle.height = "${value.height - thickness * 2}px"
    }

  init {
    style.display = "block"
    style.position = "fixed"
    style.cursor = "default"

    shadow.style.display = "block"
    shadow.style.position = "fixed"
    shadow.style.boxShadow = "0 0 8px rgba(0, 0, 0, 0.3)"
    shadow.style.backgroundColor = "transparent"

    lookAndFeelChanged()

    document.body!!.appendChild(canvas)
    document.body!!.appendChild(shadow)

    if (resizeable) {
      canvas.onmousemove = ::onMouseMove
    }
  }

  override fun lookAndFeelChanged() {
    style.borderRadius = "${ProjectorUI.borderRadius}px"
    shadow.style.borderRadius = "${ProjectorUI.borderRadius}px"
  }

  fun dispose() {
    canvas.remove()
    shadow.remove()
  }

  private fun onMouseMove(event: Event) {
    require(event is MouseEvent)
    style.cursor = getResizeDirection(event.clientX, event.clientY)?.toJsResizeDirection() ?: "default"
  }

  override fun onMouseMove(x: Int, y: Int) {
    if (resizeDirection == null) {
      return
    }

    val deltaX = if (resizeDirection == ResizeDirection.N || resizeDirection == ResizeDirection.S) 0
    else ((x - lastPoint.x) / ParamsProvider.USER_SCALING_RATIO).toInt()
    val deltaY = if (resizeDirection == ResizeDirection.E || resizeDirection == ResizeDirection.W) 0
    else ((y - lastPoint.y) / ParamsProvider.USER_SCALING_RATIO).toInt()
    onResize(deltaX, deltaY, resizeDirection!!)
    lastPoint = Point(x.toDouble(), y.toDouble())
  }

  override fun onMouseDown(x: Int, y: Int): DragEventsInterceptor? {
    if (!resizeable || !bounds.contains(x, y)) {
      return null
    }

    resizeDirection = getResizeDirection(x, y)
    if (resizeDirection != null) {
      lastPoint = Point(x.toDouble(), y.toDouble())
      return this
    }

    return null
  }

  override fun onMouseUp(x: Int, y: Int) {
    resizeDirection = null
  }

  private fun getResizeDirection(x: Int, y: Int): ResizeDirection? {
    val left = bounds.x + ProjectorUI.borderThickness * 2
    val right = bounds.x + bounds.width - ProjectorUI.borderThickness * 2
    val top = bounds.y + ProjectorUI.borderThickness * 2
    val bottom = bounds.y + bounds.height - ProjectorUI.borderThickness * 2

    return when {
      left > x && top > y -> ResizeDirection.NW
      right < x && top > y -> ResizeDirection.NE
      right < x && bottom < y -> ResizeDirection.SE
      left > x && bottom < y -> ResizeDirection.SW

      left > x -> ResizeDirection.W
      right < x -> ResizeDirection.E
      bottom < y -> ResizeDirection.S
      top > y -> ResizeDirection.N

      else -> null
    }
  }
}
