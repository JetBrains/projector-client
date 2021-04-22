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

import org.jetbrains.projector.client.common.DrawEvent
import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor
import org.jetbrains.projector.client.common.canvas.SwingCanvas
import org.jetbrains.projector.client.common.canvas.buffering.DoubleBufferedRenderingSurface
import org.jetbrains.projector.common.protocol.toClient.ServerWindowEvent
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.util.logging.Logger

abstract class AbstractWindowManager<FrameType> {
  private val logger = Logger<AbstractWindowManager<FrameType>>()
  private val currentWindows = HashMap<Int, FrameData>()

  abstract fun newFrame(windowId: Int, canvas: SwingCanvas): FrameType
  abstract fun updateFrameProperties(frameData: FrameData)
  abstract fun deleteFrame(frame: FrameType)
  abstract fun updateWindow(frame: FrameData)

  fun windowSetUpdated(event: ServerWindowSetChangedEvent) {
    logger.info { "Updating window set with ${event.windowDataList.size} windows" }
    val windowsToDelete = currentWindows.keys.toHashSet()
    event.windowDataList.forEach {
      windowsToDelete.remove(it.id)

      val existing = currentWindows.getOrPut(it.id) {
        val canvas = SwingCanvas()
        val surface = DoubleBufferedRenderingSurface(canvas)
        FrameData(newFrame(it.id, canvas), it, ArrayDeque(), surface, SingleRenderingSurfaceProcessor(surface))
      }
      existing.windowData = it
      existing.surface.setBounds(it.bounds.width.toInt(), it.bounds.height.toInt())
      updateFrameProperties(existing)
    }

    windowsToDelete.forEach {
      val oldFrame = currentWindows.remove(it) ?: return@forEach
      deleteFrame(oldFrame.frame)
    }
  }

  fun doWindowDraw(windowId: Int, drawEvents: List<ServerWindowEvent>) {
    val window = currentWindows[windowId]
    if (window == null) {
      logger.error { "Received draw command for unknown window $windowId" }
      return
    }


    window.processor.process(drawEvents)

    window.surface.flush()

    updateWindow(window)
  }

  inner class FrameData(
    val frame: FrameType,
    var windowData: WindowData,
    var drawEvents: List<DrawEvent>,
    val surface: DoubleBufferedRenderingSurface,
    val processor: SingleRenderingSurfaceProcessor,
  )
}
