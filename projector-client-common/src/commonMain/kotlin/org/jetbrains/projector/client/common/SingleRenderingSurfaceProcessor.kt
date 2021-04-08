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
import org.jetbrains.projector.client.common.canvas.asUnsafe
import org.jetbrains.projector.client.common.canvas.buffering.RenderingSurface
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.ImageEventInfo
import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.data.PaintValueType
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.util.logging.Logger

class SingleRenderingSurfaceProcessor(renderingSurface: RenderingSurface) : RenderingSurfaceProcessor {

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
            pendingEvents.addAll(drawEvents.subList(start, curr))
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
        EventType.ServerSetCompositeEvent.ordinal -> renderer.setComposite(it.asUnsafe<ServerSetCompositeEvent>().composite)

        EventType.ServerSetPaintEvent.ordinal -> (it.asUnsafe<ServerSetPaintEvent>()).paint.let { paintValue ->
          when (paintValue.tpe.ordinal) {
            PaintValueType.Color.ordinal -> renderer.setColor(paintValue.asUnsafe<PaintValue.Color>().argb)

            PaintValueType.Gradient.ordinal -> with(paintValue.asUnsafe<PaintValue.Gradient>()) {
              renderer.setGradientPaint(
                p1 = p1,
                p2 = p2,
                color1 = argb1,
                color2 = argb2
              )
            }
            PaintValueType.Unknown.ordinal -> logUnsupportedCommand(it)
          }
        }

        EventType.ServerSetClipEvent.ordinal -> renderer.setClip((it.asUnsafe<ServerSetClipEvent>()).shape)

        EventType.ServerSetFontEvent.ordinal -> (it.asUnsafe<ServerSetFontEvent>()).apply{ renderer.setFont(fontId.asUnsafe<Int>(),fontSize, ligaturesOn) }

        EventType.ServerSetStrokeEvent.ordinal -> renderer.setStroke((it.asUnsafe<ServerSetStrokeEvent>()).strokeData)

        EventType.ServerSetTransformEvent.ordinal -> renderer.setTransform((it.asUnsafe<ServerSetTransformEvent>()).tx)



        EventType.ServerDrawLineEvent.ordinal -> (it.asUnsafe<ServerDrawLineEvent>()).apply {
          renderer.drawLine(
            x1 = x1.asUnsafe<Double>(),
            y1 = y1.asUnsafe<Double>(),
            x2 = x2.asUnsafe<Double>(),
            y2 = y2.asUnsafe<Double>()
          )
        }

        EventType.ServerDrawStringEvent.ordinal -> (it.asUnsafe<ServerDrawStringEvent>()).apply {
          renderer.drawString(
            string = str,
            x = x,
            y = y,
            desiredWidth = desiredWidth
          )
        }

        EventType.ServerPaintRectEvent.ordinal -> (it.asUnsafe<ServerPaintRectEvent>()).apply {
          renderer.paintRect(
            paintType = paintType,
            x = x,
            y = y,
            width = width,
            height = height
          )
        }

        EventType.ServerPaintRoundRectEvent.ordinal -> (it.asUnsafe<ServerPaintRoundRectEvent>() ).apply {
          renderer.paintRoundRect(
            paintType = paintType,
            x = x.asUnsafe<Double>(),
            y = y.asUnsafe<Double>(),
            w = width.asUnsafe<Double>(),
            h = height.asUnsafe<Double>(),
            r1 = arcWidth.asUnsafe<Double>(),
            r2 = arcHeight.asUnsafe<Double>()
          )
        }

        EventType.ServerDrawImageEvent.ordinal -> (it.asUnsafe<ServerDrawImageEvent>()).apply{
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
                  dx = info.dx1.asUnsafe<Double>(),
                  dy = info.dy1.asUnsafe<Double>(),
                  dw = dw.asUnsafe<Double>(),
                  dh = dh.asUnsafe<Double>(),
                  sx = info.sx1.asUnsafe<Double>(),
                  sy = info.sy1.asUnsafe<Double>(),
                  sw = sw.asUnsafe<Double>(),
                  sh = sh.asUnsafe<Double>()
                )
              }

              is ImageEventInfo.Xy -> {
                // todo: manage bgcolor
                renderer.drawImage(image, info.x.asUnsafe<Double>(), info.y.asUnsafe<Double>())
              }

              is ImageEventInfo.XyWh -> {
                // todo: manage bgcolor
                renderer.drawImage(
                  image = image,
                  x = info.x.asUnsafe<Double>(),
                  y = info.y.asUnsafe<Double>(),
                  width = info.width.asUnsafe<Double>(),
                  height = info.height.asUnsafe<Double>()
                )
              }

              is ImageEventInfo.Transformed -> renderer.drawImage(image, info.tx)
            }
          }else{
            succeed = false
          }
        }

        EventType.ServerPaintPathEvent.ordinal -> (it.asUnsafe<ServerPaintPathEvent>()).apply{ renderer.paintPath(paintType, path)}

        EventType.ServerPaintOvalEvent.ordinal -> (it.asUnsafe<ServerPaintOvalEvent>()).apply{ renderer.paintOval(
          paintType = paintType,
          x = x.asUnsafe<Double>(),
          y = y.asUnsafe<Double>(),
          width = width.asUnsafe<Double>(),
          height = height.asUnsafe<Double>()
        )}

        EventType.ServerPaintPolygonEvent.ordinal -> (it.asUnsafe<ServerPaintPolygonEvent>()).apply{ renderer.paintPolygon(paintType, points)}

        EventType.ServerDrawPolylineEvent.ordinal -> renderer.drawPolyline((it.asUnsafe<ServerDrawPolylineEvent>()).points)

        EventType.ServerCopyAreaEvent.ordinal -> (it.asUnsafe<ServerCopyAreaEvent>()).apply {
          renderer.copyArea(
            x = x.asUnsafe<Double>(),
            y = y.asUnsafe<Double>(),
            width = width.asUnsafe<Double>(),
            height = height.asUnsafe<Double>(),
            dx = dx.asUnsafe<Double>(),
            dy = dy.asUnsafe<Double>()
          )
        }

        else -> logUnsupportedCommand(it)
      }
    }
    return succeed
  }
  
  private class StateSaver(private val renderer: Renderer, private val renderingSurface: RenderingSurface) {

    private var lastSuccessfulState: LastSuccessfulState? = null

    fun save() {
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

    private val logger = Logger<SingleRenderingSurfaceProcessor>()

    private fun logUnsupportedCommand(command: ServerWindowEvent) {
      if (ParamsProvider.LOG_UNSUPPORTED_EVENTS) {
        logger.debug { "Unsupported: $command" }
      }
    }
  }
}
