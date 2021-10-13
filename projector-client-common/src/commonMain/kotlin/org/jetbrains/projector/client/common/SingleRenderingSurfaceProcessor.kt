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
package org.jetbrains.projector.client.common

import org.jetbrains.projector.client.common.Renderer.Companion.RequestedRenderingState
import org.jetbrains.projector.client.common.canvas.Canvas
import org.jetbrains.projector.client.common.canvas.buffering.RenderingSurface
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.ImageEventInfo
import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.util.logging.Logger

class SingleRenderingSurfaceProcessor(private val renderingSurface: RenderingSurface, private val imageCacher: ImageCacher) {

  private val renderer = Renderer(renderingSurface)

  val stateSaver = StateSaver(renderer, renderingSurface)

  fun processNew(drawEvents: Iterable<DrawEvent>): Int? {
    var firstUnsuccessful: Int? = null

    drawEvents.forEachIndexed { index, drawEvent ->
      val drawIsSuccessful = handleDrawEvent(drawEvent)

      if (!drawIsSuccessful && firstUnsuccessful == null) {
        stateSaver.saveIfNeeded()
        firstUnsuccessful = index
      }
    }

    return firstUnsuccessful
  }

  fun flush() {
    renderingSurface.flush()
  }

  private fun handleDrawEvent(command: DrawEvent): Boolean {
    command.prerequisites.forEach {
      Do exhaustive when (it) {
        is ServerSetCompositeEvent -> renderer.setComposite(it.composite)

        is ServerSetPaintEvent -> it.paint.let { paintValue ->
          when (paintValue) {
            is PaintValue.Color -> renderer.setColor(paintValue.argb)

            is PaintValue.Gradient -> renderer.setGradientPaint(
              p1 = paintValue.p1,
              p2 = paintValue.p2,
              color1 = paintValue.argb1,
              color2 = paintValue.argb2
            )

            is PaintValue.Unknown -> logUnsupportedCommand(it)
          }
        }

        is ServerSetClipEvent -> renderer.setClip(it.shape)

        is ServerSetFontEvent -> renderer.setFont(it.fontId, it.fontSize, it.ligaturesOn)

        is ServerSetStrokeEvent -> renderer.setStroke(it.strokeData)

        is ServerSetTransformEvent -> renderer.setTransform(it.tx)

        is ServerWindowToDoStateEvent -> logUnsupportedCommand(it)
      }
    }

    var success = true

    with(command.paintEvent) {
      Do exhaustive when (this) {
        is ServerDrawLineEvent -> renderer.drawLine(
          x1 = x1.toDouble(),
          y1 = y1.toDouble(),
          x2 = x2.toDouble(),
          y2 = y2.toDouble()
        )

        is ServerDrawStringEvent -> renderer.drawString(
          string = str,
          x = x,
          y = y,
          desiredWidth = desiredWidth
        )

        is ServerPaintRectEvent -> renderer.paintRect(
          paintType = paintType,
          x = x,
          y = y,
          width = width,
          height = height
        )

        is ServerPaintRoundRectEvent -> renderer.paintRoundRect(
          paintType = paintType,
          x = x.toDouble(),
          y = y.toDouble(),
          w = width.toDouble(),
          h = height.toDouble(),
          r1 = arcWidth.toDouble(),
          r2 = arcHeight.toDouble()
        )

        is ServerDrawImageEvent -> {
          val image = imageCacher.getImageData(imageId)

          if (image == null) {
            success = false
          }
          else {
            val info = imageEventInfo

            Do exhaustive when (info) {
              is ImageEventInfo.Ds -> {
                // todo: manage bgcolor
                val dw = info.dx2 - info.dx1
                val dh = info.dy2 - info.dy1
                val sw = info.sx2 - info.sx1
                val sh = info.sy2 - info.sy1

                renderer.drawImage(
                  image = image,
                  dx = info.dx1.toDouble(),
                  dy = info.dy1.toDouble(),
                  dw = dw.toDouble(),
                  dh = dh.toDouble(),
                  sx = info.sx1.toDouble(),
                  sy = info.sy1.toDouble(),
                  sw = sw.toDouble(),
                  sh = sh.toDouble()
                )
              }

              is ImageEventInfo.Xy -> {
                // todo: manage bgcolor
                renderer.drawImage(image, info.x.toDouble(), info.y.toDouble())
              }

              is ImageEventInfo.XyWh -> {
                // todo: manage bgcolor
                renderer.drawImage(
                  image = image,
                  x = info.x.toDouble(),
                  y = info.y.toDouble(),
                  width = info.width.toDouble(),
                  height = info.height.toDouble()
                )
              }

              is ImageEventInfo.Transformed -> renderer.drawImage(image, info.tx)
            }
          }
        }

        is ServerPaintPathEvent -> renderer.paintPath(paintType, path)

        is ServerPaintOvalEvent -> renderer.paintOval(
          paintType = paintType,
          x = x.toDouble(),
          y = y.toDouble(),
          width = width.toDouble(),
          height = height.toDouble()
        )

        is ServerPaintPolygonEvent -> renderer.paintPolygon(paintType, points)

        is ServerDrawPolylineEvent -> renderer.drawPolyline(points)

        is ServerCopyAreaEvent -> renderer.copyArea(
          x = x.toDouble(),
          y = y.toDouble(),
          width = width.toDouble(),
          height = height.toDouble(),
          dx = dx.toDouble(),
          dy = dy.toDouble()
        )

        is ServerWindowToDoPaintEvent -> logUnsupportedCommand(this)
      }
    }

    return success
  }

  class StateSaver(private val renderer: Renderer, private val renderingSurface: RenderingSurface) {

    private var lastSuccessfulState: LastSuccessfulState? = null

    fun saveIfNeeded() {
      if (lastSuccessfulState != null) {
        return
      }

      lastSuccessfulState = LastSuccessfulState(
        renderingState = renderer.requestedState.copy(),
        image = renderingSurface.canvas.takeSnapshot()
      )
    }

    fun restoreIfNeeded() {
      lastSuccessfulState?.let {
        renderer.drawImageRaw(it.image)
        renderer.requestedState.setTo(it.renderingState)

        lastSuccessfulState = null
      }
    }

    private class LastSuccessfulState(val renderingState: RequestedRenderingState, val image: Canvas.Snapshot)
  }

  companion object {

    private val logger = Logger<SingleRenderingSurfaceProcessor>()

    private fun logUnsupportedCommand(command: ServerWindowEvent) {
      if (ParamsProvider.LOG_UNSUPPORTED_EVENTS) {
        logger.debug { "Unsupported: $command" }
      }
    }

    fun List<ServerWindowEvent>.shrinkByPaintEvents(): List<DrawEvent> {
      val result = mutableListOf<DrawEvent>()

      var prerequisites = mutableListOf<ServerWindowStateEvent>()

      this.forEach {
        Do exhaustive when (it) {
          is ServerWindowStateEvent -> prerequisites.add(it)

          is ServerWindowPaintEvent -> {
            result.add(DrawEvent(prerequisites, it))
            prerequisites = mutableListOf()
          }
        }
      }

      if (prerequisites.isNotEmpty()) {
        logger.error { "Bad commands received from server: ${prerequisites.size} state events are at the end" }
      }

      return result
    }
  }
}
