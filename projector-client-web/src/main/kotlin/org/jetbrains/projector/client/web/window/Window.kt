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
package org.jetbrains.projector.client.web.window

import kotlinx.browser.document
import kotlinx.dom.addClass
import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor
import org.jetbrains.projector.client.common.canvas.DomCanvas
import org.jetbrains.projector.client.common.canvas.buffering.DoubleBufferedRenderingSurface
import org.jetbrains.projector.client.common.canvas.buffering.UnbufferedRenderingSurface
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.jetbrains.projector.client.web.misc.toJsCursorType
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.data.CursorType
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toClient.WindowType
import org.jetbrains.projector.common.protocol.toServer.ClientWindowCloseEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowMoveEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowResizeEvent
import org.jetbrains.projector.common.protocol.toServer.ResizeDirection
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.roundToInt

interface Positionable {

  val bounds: CommonRectangle
  val zIndex: Int
}

class Window(windowData: WindowData, private val stateMachine: ClientStateMachine, imageCacher: ImageCacher) : LafListener, Positionable {

  val id = windowData.id

  var title: String? = null
    set(value) {
      field = value
      header?.title = value
    }

  var modal: Boolean = windowData.modal

  var isShowing: Boolean = false
    set(value) {
      header?.visible = value
      border.visible = value

      if (field == value) {
        return
      }
      field = value
      canvas.style.display = value.toDisplayType()
    }

  //public only for speculative typing.
  val canvas = createCanvas()
  private val renderingSurface = createRenderingSurface(canvas)

  private var header: WindowHeader? = null
  private var headerVerticalPosition: Double = 0.0
  private var headerHeight: Double = 0.0
  private val border = WindowBorder(windowData.resizable)

  val commandProcessor = SingleRenderingSurfaceProcessor(renderingSurface, imageCacher)

  override var bounds: CommonRectangle = CommonRectangle(0.0, 0.0, 0.0, 0.0)
    set(value) {
      if (field == value) {
        return
      }
      field = value
      applyBounds()
    }

  override var zIndex: Int = 0
    set(value) {
      if (field != value) {
        field = value
        canvas.style.zIndex = "$zIndex"
        header?.zIndex = zIndex
        border.zIndex = zIndex - 1
      }
    }

  var cursorType: CursorType = CursorType.DEFAULT
    set(value) {
      if (field != value) {
        field = value
        canvas.style.cursor = value.toJsCursorType()
      }
    }

  init {
    applyBounds()

    if (windowData.windowType == WindowType.IDEA_WINDOW || windowData.windowType == WindowType.POPUP) {
      canvas.style.border = "none"
    }
    else if (windowData.windowType == WindowType.WINDOW) {
      if (windowData.undecorated) {
        canvas.style.border = ProjectorUI.borderStyle
      }
      else {
        // If the window has a header on the host, its sizes are included in the window bounds.
        // The client header is drawn above the window, outside its bounds. At the same time,
        // the coordinates of the contents of the window come taking into account the size
        // of the header. As a result, on client an empty space is obtained between header
        // and the contents of the window. To get rid of this, we transfer the height of the system
        // window header and if it > 0, we draw the heading not over the window but inside
        // the window's bounds, filling in the empty space.

        header = WindowHeader(windowData.title)
        header!!.undecorated = windowData.undecorated
        header!!.onMove = ::onMove
        header!!.onClose = ::onClose

        headerVerticalPosition = when (windowData.headerHeight) {
          0, null -> ProjectorUI.headerHeight
          else -> 0.0
        }

        headerHeight = when (windowData.headerHeight) {
          0, null -> ProjectorUI.headerHeight
          else -> windowData.headerHeight!!.toDouble()
        }

        canvas.style.borderBottom = ProjectorUI.borderStyle
        canvas.style.borderLeft = ProjectorUI.borderStyle
        canvas.style.borderRight = ProjectorUI.borderStyle
        canvas.style.borderRadius = "0 0 ${ProjectorUI.borderRadius}px ${ProjectorUI.borderRadius}px"
      }
    }

    if (windowData.resizable) {
      border.onResize = ::onResize
    }
  }

  override fun lookAndFeelChanged() {
    if (header != null) {
      canvas.style.borderBottom = ProjectorUI.borderStyle
      canvas.style.borderLeft = ProjectorUI.borderStyle
      canvas.style.borderRight = ProjectorUI.borderStyle
      canvas.style.borderRadius = "0 0 ${ProjectorUI.borderRadius}px ${ProjectorUI.borderRadius}px"
    }
    else if (canvas.style.border != "none") {
      canvas.style.border = ProjectorUI.borderStyle
    }

    header?.lookAndFeelChanged()
    border.lookAndFeelChanged()
  }

  fun contains(x: Int, y: Int): Boolean {
    return border.bounds.contains(x, y)
  }

  fun onMouseDown(x: Int, y: Int): DragEventsInterceptor? {
    return border.onMouseDown(x, y) ?: header?.onMouseDown(x, y)
  }

  fun onMouseClick(x: Int, y: Int): DragEventsInterceptor? {
    return border.onMouseClick(x, y) ?: header?.onMouseClick(x, y)
  }

  private fun onResize(deltaX: Int, deltaY: Int, direction: ResizeDirection) {
    stateMachine.fire(ClientAction.AddEvent(ClientWindowResizeEvent(id, deltaX, deltaY, direction)))
  }

  private fun onMove(deltaX: Int, deltaY: Int) {
    stateMachine.fire(ClientAction.AddEvent(ClientWindowMoveEvent(id, deltaX, deltaY)))
  }

  private fun onClose() {
    stateMachine.fire(ClientAction.AddEvent(ClientWindowCloseEvent(id)))
  }

  private fun createCanvas() = (document.createElement("canvas") as HTMLCanvasElement).apply {
    style.position = "fixed"
    style.display = isShowing.toDisplayType()

    addClass("window")  // to easily locate windows in integration tests

    document.body!!.appendChild(this)
  }

  fun applyBounds() {
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO
    val scalingRatio = ParamsProvider.SCALING_RATIO

    val clientBounds = CommonRectangle(
      bounds.x * userScalingRatio,
      bounds.y * userScalingRatio,
      bounds.width * userScalingRatio,
      bounds.height * userScalingRatio
    )

    if (header != null) {
      header!!.bounds = CommonRectangle(
        clientBounds.x,
        (bounds.y - headerVerticalPosition) * userScalingRatio,
        clientBounds.width,
        headerHeight * userScalingRatio
      )
      header!!.draw()
    }

    border.bounds = CommonRectangle(
      clientBounds.x,
      (bounds.y - headerVerticalPosition) * userScalingRatio,
      clientBounds.width,
      clientBounds.height + headerVerticalPosition * userScalingRatio
    ).createExtended(ProjectorUI.borderThickness * userScalingRatio)

    canvas.style.apply {
      left = "${clientBounds.x}px"
      top = "${clientBounds.y}px"
      width = "${clientBounds.width}px"
      height = "${clientBounds.height}px"
    }

    renderingSurface.scalingRatio = scalingRatio
    renderingSurface.setBounds(
      width = (bounds.width * scalingRatio).roundToInt(),
      height = (bounds.height * scalingRatio).roundToInt()
    )
  }

  fun dispose() {
    canvas.remove()
    border.dispose()
    header?.dispose()
  }

  companion object {
    fun createRenderingSurface(canvas: HTMLCanvasElement) = when (ParamsProvider.DOUBLE_BUFFERING) {
      true -> DoubleBufferedRenderingSurface(DomCanvas(canvas))
      false -> UnbufferedRenderingSurface(DomCanvas(canvas))
    }
  }
}
