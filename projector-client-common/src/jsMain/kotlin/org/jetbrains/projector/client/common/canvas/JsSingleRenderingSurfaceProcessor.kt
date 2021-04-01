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
package org.jetbrains.projector.client.common.canvas

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.projector.client.common.DrawEvent
import org.jetbrains.projector.client.common.Renderer
import org.jetbrains.projector.client.common.Renderer.Companion.RequestedRenderingState
import org.jetbrains.projector.client.common.RenderingSurfaceProcessor
import org.jetbrains.projector.client.common.canvas.Canvas
import org.jetbrains.projector.client.common.canvas.buffering.RenderingSurface
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.ImageEventInfo
import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.util.logging.Logger

class JsSingleRenderingSurfaceProcessor(renderingSurface: RenderingSurface): RenderingSurfaceProcessor {

  private val renderer = Renderer(renderingSurface)

  private val stateSaver = StateSaver(renderer, renderingSurface)

  @OptIn(ExperimentalStdlibApi::class)
  override fun process(drawEvents: List<ServerWindowEvent>): List<ServerWindowEvent>? {
    stateSaver.restoreIfNeeded()
    var start = 0
    var curr = 0
    var pendingEvents: MutableList<ServerWindowEvent>? = null
    for (drawEvent in drawEvents) {
      curr ++
      if(drawEvent.tpe.isDrawEvent()){
        if(!handleDrawEvents(drawEvents.subList(start,curr))){
          if(start == 0 && curr == drawEvents.size){
            return drawEvents
          }else {
            if (pendingEvents == null) {
              pendingEvents = mutableListOf()
            }
            pendingEvents?.addAll(drawEvents.subList(start, curr))
          }
        }
        start = curr
      }
    }

    return pendingEvents
  }

  private fun handleDrawEvents(events: List<ServerWindowEvent>): Boolean {
    var succeed = true
    events.forEach {
      when (it.tpe.ordinal) {
        EventType.ServerSetCompositeEvent.ordinal -> renderer.setComposite(it.unsafeCast<ServerSetCompositeEvent>().composite)

        EventType.ServerSetPaintEvent.ordinal -> (it.unsafeCast<ServerSetPaintEvent>()).paint.let { paintValue ->
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

        EventType.ServerSetClipEvent.ordinal -> renderer.setClip((it.unsafeCast<ServerSetClipEvent>()).shape)

        EventType.ServerSetFontEvent.ordinal -> (it.unsafeCast<ServerSetFontEvent>()).apply{ renderer.setFont(fontId,fontSize, ligaturesOn) }

        EventType.ServerSetStrokeEvent.ordinal -> renderer.setStroke((it.unsafeCast<ServerSetStrokeEvent>()).strokeData)

        EventType.ServerSetTransformEvent.ordinal -> renderer.setTransform((it.unsafeCast<ServerSetTransformEvent>()).tx)



        EventType.ServerDrawLineEvent.ordinal -> (it.unsafeCast<ServerDrawLineEvent>()).apply {
          renderer.drawLine(
            x1 = x1.unsafeCast<Double>(),
            y1 = y1.unsafeCast<Double>(),
            x2 = x2.unsafeCast<Double>(),
            y2 = y2.unsafeCast<Double>()
          )
        }

        EventType.ServerDrawStringEvent.ordinal -> (it.unsafeCast<ServerDrawStringEvent>()).apply {
          renderer.drawString(
            string = str,
            x = x,
            y = y,
            desiredWidth = desiredWidth
          )
        }

        EventType.ServerPaintRectEvent.ordinal -> (it.unsafeCast<ServerPaintRectEvent>()).apply {
          renderer.paintRect(
            paintType = paintType,
            x = x,
            y = y,
            width = width,
            height = height
          )
        }

        EventType.ServerPaintRoundRectEvent.ordinal -> (it.unsafeCast<ServerPaintRoundRectEvent>() ).apply {
          renderer.paintRoundRect(
            paintType = paintType,
            x = x.unsafeCast<Double>(),
            y = y.unsafeCast<Double>(),
            w = width.unsafeCast<Double>(),
            h = height.unsafeCast<Double>(),
            r1 = arcWidth.unsafeCast<Double>(),
            r2 = arcHeight.unsafeCast<Double>()
          )
        }

        EventType.ServerDrawImageEvent.ordinal -> (it.unsafeCast<ServerDrawImageEvent>()).apply{
          val image = ImageCacher.getImageData(imageId)

          if (image != null){
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
                  dx = info.dx1.unsafeCast<Double>(),
                  dy = info.dy1.unsafeCast<Double>(),
                  dw = dw.unsafeCast<Double>(),
                  dh = dh.unsafeCast<Double>(),
                  sx = info.sx1.unsafeCast<Double>(),
                  sy = info.sy1.unsafeCast<Double>(),
                  sw = sw.unsafeCast<Double>(),
                  sh = sh.unsafeCast<Double>()
                )
              }

              is ImageEventInfo.Xy -> {
                // todo: manage bgcolor
                renderer.drawImage(image, info.x.unsafeCast<Double>(), info.y.unsafeCast<Double>())
              }

              is ImageEventInfo.XyWh -> {
                // todo: manage bgcolor
                renderer.drawImage(
                  image = image,
                  x = info.x.unsafeCast<Double>(),
                  y = info.y.unsafeCast<Double>(),
                  width = info.width.unsafeCast<Double>(),
                  height = info.height.unsafeCast<Double>()
                )
              }

              is ImageEventInfo.Transformed -> renderer.drawImage(image, info.tx)
            }
          }else{
            succeed = false
          }
        }

        EventType.ServerPaintPathEvent.ordinal -> (it.unsafeCast<ServerPaintPathEvent>()).apply{ renderer.paintPath(paintType, path)}

        EventType.ServerPaintOvalEvent.ordinal -> (it.unsafeCast<ServerPaintOvalEvent>()).apply{ renderer.paintOval(
          paintType = paintType,
          x = x.unsafeCast<Double>(),
          y = y.unsafeCast<Double>(),
          width = width.unsafeCast<Double>(),
          height = height.unsafeCast<Double>()
        )}

        EventType.ServerPaintPolygonEvent.ordinal -> (it.unsafeCast<ServerPaintPolygonEvent>()).apply{ renderer.paintPolygon(paintType, points)}

        EventType.ServerDrawPolylineEvent.ordinal -> renderer.drawPolyline((it.unsafeCast<ServerDrawPolylineEvent>()).points)

        EventType.ServerCopyAreaEvent.ordinal -> (it.unsafeCast<ServerCopyAreaEvent>()).apply {
          renderer.copyArea(
            x = x.unsafeCast<Double>(),
            y = y.unsafeCast<Double>(),
            width = width.unsafeCast<Double>(),
            height = height.unsafeCast<Double>(),
            dx = dx.unsafeCast<Double>(),
            dy = dy.unsafeCast<Double>()
          )
        }

        else -> logUnsupportedCommand(it)
      }
    }
    return succeed
  }

  private class StateSaver(private val renderer: Renderer, private val renderingSurface: RenderingSurface) {

    private var lastSuccessfulState: LastSuccessfulState? = null

    suspend fun save() {
      lastSuccessfulState = LastSuccessfulState(
        renderingState = renderer.requestedState.copy(),
        image = renderingSurface.canvas.takeSnapshot()
      )
    }

    fun restoreIfNeeded() {
      lastSuccessfulState?.let {
        renderer.drawImageRaw(it.image)

        it.renderingState.let {
          renderer.requestedState.apply {
            identitySpaceClip = it.identitySpaceClip
            transform = it.transform
            strokeData = it.strokeData
            font = it.font
            paint = it.paint
          }
        }

        lastSuccessfulState = null
      }
    }

    private class LastSuccessfulState(val renderingState: RequestedRenderingState, val image: Canvas.ImageSource)
  }

  companion object {

    private val logger = Logger<JsSingleRenderingSurfaceProcessor>()

    private fun logUnsupportedCommand(command: ServerWindowEvent) {
      if (ParamsProvider.LOG_UNSUPPORTED_EVENTS) {
        logger.debug { "Unsupported: $command" }
      }
    }
  }
}
