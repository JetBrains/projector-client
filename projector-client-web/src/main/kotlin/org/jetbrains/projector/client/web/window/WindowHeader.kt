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
import org.jetbrains.projector.client.common.canvas.Context2d
import org.jetbrains.projector.client.common.canvas.DomCanvas
import org.jetbrains.projector.client.common.canvas.PaintColor
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.data.Point
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.roundToInt

class WindowHeader(var title: String? = null) : DragEventsInterceptor, LafListener {

  companion object {
    val closeIconNormal = Image().apply { src = "close.svg" }
    val closeIconHover = Image().apply { src = "closeHover.svg" }
  }

  private var closeIcon = closeIconNormal

  private val canvas: HTMLCanvasElement = document.createElement("canvas") as HTMLCanvasElement
  private val style = canvas.style
  private val headerRenderingSurface = WebWindow.createRenderingSurface(canvas)

  private var dragStarted = false
  private var lastPoint = Point(-1.0, -1.0)

  var onMove: (deltaX: Int, deltaY: Int) -> Unit = { _, _ -> }
  var onClose: () -> Unit = {}

  /** Undecorated windows have no any buttons on header. */
  var undecorated: Boolean = false

  private var clientCloseBounds: CommonRectangle = CommonRectangle(0.0, 0.0, 0.0, 0.0)
  var bounds: CommonRectangle = CommonRectangle(0.0, 0.0, 0.0, 0.0)
    set(value) {

      val scalingRatio = ParamsProvider.SCALING_RATIO / ParamsProvider.USER_SCALING_RATIO
      headerRenderingSurface.scalingRatio = ParamsProvider.SCALING_RATIO

      headerRenderingSurface.setBounds(
        width = (value.width * scalingRatio).roundToInt(),
        height = (value.height * scalingRatio).roundToInt()
      )

      if (field == value) {
        return
      }

      field = value

      clientCloseBounds = CommonRectangle(value.x + value.width - value.height, value.y, value.height, value.height)

      style.left = "${value.x}px"
      style.top = "${value.y}px"
      style.width = "${value.width}px"
      style.height = "${value.height}px"
    }

  var zIndex: Int = 0
    set(value) {
      if (field != value) {
        field = value
        style.zIndex = "$zIndex"
      }
    }

  var visible: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        style.display = if (value) "block" else "none"
      }
    }

  init {
    style.display = "block"
    style.position = "fixed"
    style.cursor = "default"

    lookAndFeelChanged()

    document.body!!.appendChild(canvas)

    canvas.onmousemove = ::onMouseMove
    canvas.onmouseleave = ::onMouseLeave
  }

  fun dispose() {
    canvas.remove()
  }

  override fun lookAndFeelChanged() {
    style.borderRadius = "${ProjectorUI.borderRadius}px ${ProjectorUI.borderRadius}px 0 0"

    style.borderTop = ProjectorUI.borderStyle
    style.borderLeft = ProjectorUI.borderStyle
    style.borderRight = ProjectorUI.borderStyle
  }

  @Suppress("UNUSED_PARAMETER")  // actually, parameter is used in function refs
  private fun onMouseLeave(event: Event) {
    if (closeIcon != closeIconNormal) {
      closeIcon = closeIconNormal
      draw()
    }
  }

  private fun onMouseMove(event: Event) {
    require(event is MouseEvent)
    val newCloseIcon = if (clientCloseBounds.contains(event.clientX, event.clientY)) closeIconHover else closeIconNormal
    if (newCloseIcon != closeIcon) {
      closeIcon = newCloseIcon
      draw()
    }
  }

  override fun onMouseMove(x: Int, y: Int) {
    if (dragStarted) {
      onMove(((x - lastPoint.x) / ParamsProvider.USER_SCALING_RATIO).toInt(),
             ((y - lastPoint.y) / ParamsProvider.USER_SCALING_RATIO).toInt())
      lastPoint = Point(x.toDouble(), y.toDouble())
    }
  }

  override fun onMouseDown(x: Int, y: Int): DragEventsInterceptor? {
    if (!bounds.contains(x, y)) {
      return null
    }

    dragStarted = true
    lastPoint = Point(x.toDouble(), y.toDouble())
    return this
  }

  override fun onMouseUp(x: Int, y: Int) {
    dragStarted = false
  }

  override fun onMouseClick(x: Int, y: Int): DragEventsInterceptor? {
    if (!undecorated && clientCloseBounds.contains(x, y)) {
      onClose()
      return this
    }
    return null
  }

  fun draw() {

    val context = headerRenderingSurface.canvas.context2d
    val offset = ProjectorUI.crossOffset * headerRenderingSurface.scalingRatio

    // Fill header background.
    context.setFillStyle(PaintColor.SolidColor(ProjectorUI.windowHeaderActiveBackgroundArgb))
    context.fillRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

    if (!undecorated) {
      // Cross on close button.
      context.drawImage(DomCanvas.DomImageSource(closeIcon),
                        canvas.width.toDouble() - canvas.height + offset,
                        offset,
                        canvas.height.toDouble() - offset * 2,
                        canvas.height.toDouble() - offset * 2
      )
    }

    if (title != null) {
      context.setFillStyle(PaintColor.SolidColor(ProjectorUI.windowHeaderActiveTextArgb))
      context.setFont("bold ${0.5 * ProjectorUI.headerHeight * headerRenderingSurface.scalingRatio}px Arial")
      context.setTextAlign(Context2d.TextAlign.CENTER)
      context.setTextBaseline(Context2d.TextBaseline.MIDDLE)
      context.fillText(title!!, canvas.width / 2.0, canvas.height / 2.0)
    }

    headerRenderingSurface.flush()
  }
}
