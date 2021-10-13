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
package org.jetbrains.projector.client.web

import org.jetbrains.projector.client.common.DrawEvent
import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor
import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor.Companion.shrinkByPaintEvents
import org.jetbrains.projector.client.web.window.WindowManager
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.util.logging.Logger

class RenderingQueue(private val windowManager: WindowManager) {

  private val pendingDrawEvents = ArrayDeque<ShrunkDrawEvents>()
  private val newDrawEvents = ArrayDeque<ShrunkDrawEvents>()

  fun add(drawCommands: Iterable<ServerDrawCommandsEvent>) {
    newDrawEvents.addAll(drawCommands.map { ShrunkDrawEvents(it.target, it.drawEvents.shrinkByPaintEvents()) })
  }

  private fun ServerDrawCommandsEvent.Target.getCommandProcessor(): SingleRenderingSurfaceProcessor? = when (this) {
    is ServerDrawCommandsEvent.Target.Onscreen -> when (val window = windowManager[windowId]) {
      null -> {
        logger.error { "Skipping nonexistent window: $windowId" }
        null
      }
      else -> window.commandProcessor
    }

    is ServerDrawCommandsEvent.Target.Offscreen -> windowManager.imageCacher.getOffscreenProcessor(this)
  }

  private fun forEachRenderingSurface(op: (SingleRenderingSurfaceProcessor) -> Unit) {
    windowManager.forEach { op(it.commandProcessor) }
    // todo: and the same for offscreen surfaces
  }

  fun drawNewEvents() {
    newDrawEvents.forEach { shrunk ->
      val commandProcessor = shrunk.target.getCommandProcessor() ?: return@forEach

      val firstUnsuccessful = commandProcessor.processNew(shrunk.events)
      commandProcessor.flush()

      if (pendingDrawEvents.isNotEmpty()) {
        pendingDrawEvents.add(shrunk)
      }
      else if (firstUnsuccessful != null) {
        if (pendingDrawEvents.isNotEmpty()) {
          logger.error { "Non empty pendingDrawEvents are handled by another branch, aren't they? This branch works only for empty." }
        }
        pendingDrawEvents.add(shrunk.copy(events = shrunk.events.subList(firstUnsuccessful, shrunk.events.size)))

        forEachRenderingSurface { singleRenderingSurfaceProcessor ->
          singleRenderingSurfaceProcessor.stateSaver.saveIfNeeded()
        }
      }
    }
    newDrawEvents.clear()
  }

  fun drawPendingEvents() {
    var firstUnsuccessful: Pair<Int, Int>? = null

    forEachRenderingSurface { singleRenderingSurfaceProcessor ->
      singleRenderingSurfaceProcessor.stateSaver.restoreIfNeeded()
    }

    pendingDrawEvents.forEachIndexed { i, shrunk ->
      val commandProcessor = shrunk.target.getCommandProcessor() ?: return@forEachIndexed

      val firstUnsuccessfulItem = commandProcessor.processNew(shrunk.events)
      commandProcessor.flush()

      if (firstUnsuccessful == null && firstUnsuccessfulItem != null) {
        firstUnsuccessful = i to firstUnsuccessfulItem
      }
    }

    firstUnsuccessful?.let { (i0, i1) ->
      repeat(i0) {
        pendingDrawEvents.removeFirst()
      }
      val first = pendingDrawEvents.removeFirst()
      pendingDrawEvents.addFirst(first.copy(events = first.events.subList(i1, first.events.size)))
    }
    ?: pendingDrawEvents.clear()
  }

  companion object {

    private data class ShrunkDrawEvents(val target: ServerDrawCommandsEvent.Target, val events: List<DrawEvent>)

    private val logger = Logger<RenderingQueue>()
  }
}
