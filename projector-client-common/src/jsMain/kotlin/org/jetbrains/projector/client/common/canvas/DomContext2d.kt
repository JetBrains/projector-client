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

import org.jetbrains.projector.client.common.canvas.Canvas.ImageSource
import org.jetbrains.projector.client.common.canvas.Context2d.*
import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.client.common.canvas.PaintColor.Gradient
import org.jetbrains.projector.client.common.canvas.PaintColor.SolidColor
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.PathSegment
import org.jetbrains.projector.common.protocol.data.Point
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.*

internal class DomContext2d(private val myContext2d: CanvasRenderingContext2D) : Context2d {

  private fun convertLineJoin(lineJoin: LineJoin): CanvasLineJoin {
    return when (lineJoin) {
      LineJoin.BEVEL -> CanvasLineJoin.BEVEL
      LineJoin.MITER -> CanvasLineJoin.MITER
      LineJoin.ROUND -> CanvasLineJoin.ROUND
    }
  }

  private fun convertLineCap(lineCap: LineCap): CanvasLineCap {
    return when (lineCap) {
      LineCap.BUTT -> CanvasLineCap.BUTT
      LineCap.ROUND -> CanvasLineCap.ROUND
      LineCap.SQUARE -> CanvasLineCap.SQUARE
    }
  }

  private fun convertTextBaseline(baseline: TextBaseline): CanvasTextBaseline {
    return when (baseline) {
      TextBaseline.ALPHABETIC -> CanvasTextBaseline.ALPHABETIC
      TextBaseline.BOTTOM -> CanvasTextBaseline.BOTTOM
      TextBaseline.HANGING -> CanvasTextBaseline.HANGING
      TextBaseline.IDEOGRAPHIC -> CanvasTextBaseline.IDEOGRAPHIC
      TextBaseline.MIDDLE -> CanvasTextBaseline.MIDDLE
      TextBaseline.TOP -> CanvasTextBaseline.TOP
    }
  }

  private fun convertTextAlign(align: TextAlign): CanvasTextAlign {
    return when (align) {
      TextAlign.CENTER -> CanvasTextAlign.CENTER
      TextAlign.END -> CanvasTextAlign.END
      TextAlign.LEFT -> CanvasTextAlign.LEFT
      TextAlign.RIGHT -> CanvasTextAlign.RIGHT
      TextAlign.START -> CanvasTextAlign.START
    }
  }

  private fun convertFillRule(fillRule: FillRule): CanvasFillRule {
    return when (fillRule) {
      FillRule.NONZERO -> CanvasFillRule.NONZERO
      FillRule.EVENODD -> CanvasFillRule.EVENODD
    }
  }

  private fun convertCompositeOperationType(type: CompositeOperationType): String {
    return when (type) {
      CompositeOperationType.SRC_OVER -> "source-over"
      CompositeOperationType.DST_OVER -> "destination-over"
      CompositeOperationType.SRC_IN -> "source-in"
      CompositeOperationType.DST_IN -> "destination-in"
      CompositeOperationType.SRC_OUT -> "source-out"
      CompositeOperationType.DST_OUT -> "destination-out"
      CompositeOperationType.SRC_ATOP -> "source-atop"
      CompositeOperationType.DST_ATOP -> "destination-atop"
      CompositeOperationType.XOR -> "xor"
      CompositeOperationType.SRC,
      CompositeOperationType.CLEAR,
      CompositeOperationType.DST
      -> "source-over".also {
        logger.info { "Missing implementation for $this, applying source-over" }
      }
    }
  }

  override fun drawImage(imageSource: ImageSource, x: Double, y: Double) {
    myContext2d.drawImage(imageSource.asPlatformImageSource().canvasElement, x, y)
  }

  override fun drawImage(imageSource: ImageSource, x: Double, y: Double, dw: Double, dh: Double) {
    myContext2d.drawImage(imageSource.asPlatformImageSource().canvasElement, x, y, dw, dh)
  }

  override fun drawImage(
    imageSource: ImageSource,
    sx: Double,
    sy: Double,
    sw: Double,
    sh: Double,
    dx: Double,
    dy: Double,
    dw: Double,
    dh: Double,
  ) {
    myContext2d.drawImage(imageSource.asPlatformImageSource().canvasElement, sx, sy, sw, sh, dx, dy, dw, dh)
  }

  override fun beginPath() {
    myContext2d.beginPath()
  }

  override fun closePath() {
    myContext2d.closePath()
  }

  override fun stroke() {
    myContext2d.stroke()
  }

  override fun fill(fillRule: FillRule) {
    myContext2d.fill(convertFillRule(fillRule))
  }

  override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
    myContext2d.fillRect(x, y, w, h)
  }

  override fun moveTo(x: Double, y: Double) {
    myContext2d.moveTo(x, y)
  }

  override fun moveBySegments(segments: List<PathSegment>) {
    myContext2d.moveBy(segments)
  }

  override fun moveByPoints(points: List<Point>) {
    myContext2d.moveBy(points)
  }

  override fun lineTo(x: Double, y: Double) {
    myContext2d.lineTo(x, y)
  }

  override fun roundedRect(x: Double, y: Double, w: Double, h: Double, r1: Double, r2: Double) {
    moveTo(x + r1, y)
    arcTo(x + w, y, x + w, y + h, r1)
    arcTo(x + w, y + h, x, y + h, r2)
    arcTo(x, y + h, x, y, r1)
    arcTo(x, y, x + w, y, r2)
  }

  fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double) {
    myContext2d.arcTo(x1, y1, x2, y2, radius)
  }

  fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double) {
    myContext2d.arc(x, y, radius, startAngle, endAngle)
  }

  override fun rect(x: Double, y: Double, w: Double, h: Double) {
    myContext2d.rect(x, y, w, h)
  }

  override fun ellipse(
    x: Double,
    y: Double,
    radiusX: Double,
    radiusY: Double,
    rotation: Double,
    startAngle: Double,
    endAngle: Double,
    anticlockwise: Boolean,
  ) {
    myContext2d.ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, anticlockwise)
  }

  override fun save() {
    myContext2d.save()
  }

  override fun restore() {
    myContext2d.restore()
  }

  override fun setFillStyle(color: PaintColor?) {
    myContext2d.fillStyle = color?.extract()
  }

  override fun setStrokeStyle(color: PaintColor?) {
    myContext2d.strokeStyle = color?.extract()
  }

  override fun setGlobalAlpha(alpha: Double) {
    myContext2d.globalAlpha = alpha
  }

  override fun setGlobalCompositeOperation(type: CompositeOperationType) {
    myContext2d.globalCompositeOperation = convertCompositeOperationType(type)
  }

  override fun setFont(f: String) {
    myContext2d.font = f
  }

  override fun setLineWidth(lineWidth: Double) {
    myContext2d.lineWidth = lineWidth
  }

  override fun strokeRect(x: Double, y: Double, w: Double, h: Double) {
    myContext2d.strokeRect(x, y, w, h)
  }

  override fun strokeText(text: String, x: Double, y: Double) {
    myContext2d.strokeText(text, x, y)
  }

  override fun fillText(text: String, x: Double, y: Double) {
    myContext2d.fillText(text, x, y)
  }

  override fun scale(x: Double, y: Double) {
    myContext2d.scale(x, y)
  }

  override fun rotate(angle: Double) {
    myContext2d.rotate(angle)
  }

  override fun translate(x: Double, y: Double) {
    myContext2d.translate(x, y)
  }

  override fun transform(m11: Double, m12: Double, m21: Double, m22: Double, dx: Double, dy: Double) {
    myContext2d.transform(m11, m12, m21, m22, dx, dy)
  }

  override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {
    myContext2d.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)
  }

  override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
    myContext2d.quadraticCurveTo(cpx, cpy, x, y)
  }

  override fun setLineJoin(lineJoin: LineJoin) {
    myContext2d.lineJoin = convertLineJoin(lineJoin)
  }

  override fun setMiterLimit(limit: Double) {
    myContext2d.miterLimit = limit
  }

  override fun setLineCap(lineCap: LineCap) {
    myContext2d.lineCap = convertLineCap(lineCap)
  }

  override fun setTextBaseline(baseline: TextBaseline) {
    myContext2d.textBaseline = convertTextBaseline(baseline)
  }

  override fun setTextAlign(align: TextAlign) {
    myContext2d.textAlign = convertTextAlign(align)
  }

  override fun setTransform(m11: Double, m12: Double, m21: Double, m22: Double, dx: Double, dy: Double) {
    myContext2d.setTransform(m11, m12, m21, m22, dx, dy)
  }

  override fun setLineDash(lineDash: DoubleArray) {
    myContext2d.setLineDash(lineDash.toTypedArray())
  }

  override fun setLineDashOffset(offset: Double) {
    myContext2d.lineDashOffset = offset
  }

  override fun measureText(str: String): Point {
    val measure = myContext2d.measureText(str)
    return Point(measure.width, measure.actualBoundingBoxAscent)
  }

  override fun clip(fillRule: FillRule?) {
    fillRule
      ?.let { myContext2d.clip(convertFillRule(it)) }
    ?: myContext2d.clip()
  }

  override fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): Gradient {
    return DOMGradient(myContext2d.createLinearGradient(x0, y0, x1, y1))
  }

  override fun getTransform(): Matrix {
    return with(myContext2d.getTransform()) {
      Matrix(a, b, c, d, e, f)
    }
  }

  override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
    myContext2d.clearRect(x, y, w, h)
  }

  private fun ImageSource.asPlatformImageSource(): DomCanvas.DomImageSource {
    return this as DomCanvas.DomImageSource
  }

  @Suppress("IMPLICIT_CAST_TO_ANY")
  fun PaintColor.extract() = when (this) {
    is SolidColor -> argb.argbIntToRgbaString()
    is Gradient -> (this as DOMGradient).canvasGradient
  }

  class DOMGradient(val canvasGradient: CanvasGradient) : Gradient() {
    override fun addColorStop(offset: Double, argb: Int) {
      canvasGradient.addColorStop(offset, argb.argbIntToRgbaString())
    }
  }

  companion object {
    private val logger = Logger<DomContext2d>()

    fun CanvasPath.moveBy(segments: List<PathSegment>) {
      segments.forEach {
        Do exhaustive when (it) {
          is PathSegment.MoveTo -> this.moveTo(it.point.x, it.point.y)
          is PathSegment.LineTo -> this.lineTo(it.point.x, it.point.y)
          is PathSegment.QuadTo -> this.quadraticCurveTo(  // todo: check parameters ordering
            it.point1.x, it.point1.y,
            it.point2.x, it.point2.y
          )
          is PathSegment.CubicTo -> this.bezierCurveTo(  // todo: check parameters ordering
            it.point1.x, it.point1.y,
            it.point2.x, it.point2.y,
            it.point3.x, it.point3.y
          )
          is PathSegment.Close -> this.closePath()
        }
      }
    }

    fun CanvasPath.moveBy(points: List<Point>) {
      points.forEachIndexed { i, point ->
        if (i == 0) {
          moveTo(point.x, point.y)
        }
        else {
          lineTo(point.x, point.y)
        }
      }
    }
  }
}
