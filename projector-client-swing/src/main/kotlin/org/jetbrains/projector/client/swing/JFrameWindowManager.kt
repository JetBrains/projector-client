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
package org.jetbrains.projector.client.swing

import org.jetbrains.projector.client.common.canvas.SwingCanvas
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toServer.ClientWindowCloseEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowMoveEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowResizeEvent
import org.jetbrains.projector.common.protocol.toServer.ResizeDirection
import org.jetbrains.projector.util.logging.Logger
import org.jetbrains.projector.util.logging.loggerFactory
import java.awt.Dimension
import java.awt.Point
import java.awt.event.*
import javax.swing.JFrame

open class JFrameWindowManager(val transport: ProjectorTransport) : AbstractWindowManager<JFrame>() {
  companion object {
    private val logger = Logger<JFrameWindowManager>()
  }

  private var windowOldSizesMap = HashMap<Int, Dimension>()
  private var windowOldPositionsMap = HashMap<Int, Point>()

  override fun newFrame(windowId: Int, canvas: SwingCanvas, windowData: WindowData): JFrame {
    return JFrame().apply {
      rootPane.contentPane.add(ProjectorViewPanel(canvas, transport.connectionTime).apply a2@{
        addListeners(windowId, this@apply) {
          // todo: grouping?
          transport.send(it)
        }
      })
      addListeners(this, windowId)
      isUndecorated = windowData.undecorated
    }
  }

  open fun shouldWindowBeShown(frameData: FrameData): Boolean = true

  private fun addListeners(jFrame: JFrame, windowId: Int) {
    windowOldSizesMap[windowId] = Dimension(jFrame.width, jFrame.height)

    jFrame.addWindowListener(object: WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        transport.send(ClientWindowCloseEvent(windowId))
      }
    })

    jFrame.rootPane.addComponentListener(object: ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        // todo: proper resize direction
        val oldDim = windowOldSizesMap[windowId]!!
        val deltaX = jFrame.width - oldDim.width
        val deltaY = jFrame.height - oldDim.height

        if (deltaX != 0 || deltaY != 0)
          transport.send(ClientWindowResizeEvent(windowId, deltaX, deltaY, ResizeDirection.SE))
        else
          logger.info { "Zero delta resize" }
        windowOldSizesMap[windowId] = Dimension(jFrame.width, jFrame.height)
      }
    })

    jFrame.addComponentListener(object: ComponentAdapter() {
      override fun componentMoved(e: ComponentEvent) {
        val oldPoint = windowOldPositionsMap[windowId]!!

        val deltaX = jFrame.x - oldPoint.x
        val deltaY = jFrame.y - oldPoint.y
        if (deltaX != 0 || deltaY != 0)
          transport.send(ClientWindowMoveEvent(windowId, deltaX, deltaY))
        else
          logger.info { "Zero delta move" }
        windowOldPositionsMap[windowId] = Point(jFrame.x, jFrame.y)
      }
    })
  }

  override fun updateFrameProperties(frameData: FrameData) {
    val newFrame = frameData.frame
    val it = frameData.windowData

    newFrame.title = it.title
    newFrame.isResizable = it.resizable
    val newDimension = Dimension(it.bounds.width.toInt(), it.bounds.height.toInt())
    windowOldSizesMap[it.id] = newDimension
    newFrame.size = newDimension
    newFrame.isVisible = it.isShowing && shouldWindowBeShown(frameData)

    windowOldPositionsMap[it.id] = Point(it.bounds.x.toInt(), it.bounds.y.toInt())
    newFrame.setLocation(it.bounds.x.toInt(), it.bounds.y.toInt())
  }

  override fun redrawWindow(frame: FrameData) {
    val component = frame.frame.rootPane.contentPane.getComponent(0) as ProjectorViewPanel
    component.revalidate()
    component.repaint()
  }

  override fun deleteFrame(frame: JFrame) {
    frame.isVisible = false
    frame.dispose()
  }
}
