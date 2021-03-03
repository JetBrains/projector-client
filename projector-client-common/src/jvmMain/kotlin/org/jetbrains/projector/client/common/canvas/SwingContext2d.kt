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
package org.jetbrains.projector.client.common.canvas

import org.jetbrains.projector.client.common.SwingFontCache
import org.jetbrains.projector.client.common.change
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.PathSegment
import org.jetbrains.projector.common.protocol.data.Point
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.io.File

class SwingContext2d(graphics: Graphics2D) : Context2d {
  private var savedGraphics: Graphics2D = graphics.create() as Graphics2D

  var graphics: Graphics2D = graphics.create() as Graphics2D
    private set

  override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
    graphics.clearRect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
  }

  override fun drawImage(imageSource: Canvas.ImageSource, x: Double, y: Double) {
    graphics.drawImage((imageSource as SwingCanvas.SwingImageSource).image, x.toInt(), y.toInt(), null)
  }

  override fun drawImage(imageSource: Canvas.ImageSource, x: Double, y: Double, dw: Double, dh: Double) {
    graphics.drawImage((imageSource as SwingCanvas.SwingImageSource).image, x.toInt(), y.toInt(), dw.toInt(), dh.toInt(), null)
  }

  override fun drawImage(
    imageSource: Canvas.ImageSource,
    sx: Double,
    sy: Double,
    sw: Double,
    sh: Double,
    dx: Double,
    dy: Double,
    dw: Double,
    dh: Double,
  ) {
    graphics.drawImage((imageSource as SwingCanvas.SwingImageSource).image, dx.toInt(), dy.toInt(), (dx + dw).toInt(), (dy + dh).toInt(),
                       sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt(), null)
  }

  override fun beginPath() {
    myCurrentPath.reset()
  }

  override fun closePath() {
    myCurrentPath.closePath()
  }

  override fun stroke() {
    graphics.stroke = myCurrentStroke
    graphics.draw(myCurrentPath)
  }

  override fun fill(fillRule: Context2d.FillRule) {
    setFillRule(fillRule)

    graphics.stroke = myCurrentStroke
    graphics.fill(myCurrentPath)
  }

  override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
    graphics.fillRect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
  }

  private var myCurrentPath: Path2D.Double = Path2D.Double()
  private var myCurrentStroke = BasicStroke()

  override fun moveTo(x: Double, y: Double) {
    myCurrentPath.moveTo(x, y)
  }

  override fun moveBySegments(segments: List<PathSegment>) {
    segments.forEach {
      Do exhaustive when(it) {
        is PathSegment.MoveTo -> moveTo(it.point.x, it.point.y)
        is PathSegment.LineTo -> lineTo(it.point.x, it.point.y)
        is PathSegment.QuadTo -> quadraticCurveTo(it.point1.x, it.point1.y, it.point2.x, it.point2.y)
        is PathSegment.CubicTo -> bezierCurveTo(it.point1.x, it.point1.y, it.point2.x, it.point2.y, it.point3.x, it.point3.y)
        PathSegment.Close -> { }
      }
    }
  }

  override fun moveByPoints(points: List<Point>) {
    points.forEach {
      if (myCurrentPath.currentPoint == null)
        moveTo(it.x, it.y)
      else
        lineTo(it.x, it.y)
    }
  }

  override fun lineTo(x: Double, y: Double) {
    myCurrentPath.lineTo(x, y)
  }

  override fun roundedRect(x: Double, y: Double, w: Double, h: Double, r1: Double, r2: Double) {
    myCurrentPath.append(RoundRectangle2D.Double(x, y, w, h, r1, r2), false)
  }

  override fun rect(x: Double, y: Double, w: Double, h: Double) {
    myCurrentPath.append(Rectangle(x.toInt(), y.toInt(), w.toInt(), h.toInt()), false)
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
    // todo: this is likely incorrect
    graphics.fillArc(x.toInt(), y.toInt(), radiusX.toInt(), radiusY.toInt(), startAngle.toInt(), (endAngle - startAngle).toInt())
  }

  override fun save() {
    savedGraphics = graphics.create() as Graphics2D
  }

  override fun restore() {
    graphics = savedGraphics.create() as Graphics2D
  }

  override fun setFillStyle(color: PaintColor?) {
    Do exhaustive when(color) {
      is PaintColor.SolidColor -> graphics.color = Color(color.argb)
      is PaintColor.Gradient -> graphics.paint = (color as GradientImpl).getPaint()
      null -> {}
    }
  }

  override fun setStrokeStyle(color: PaintColor?) {
    Do exhaustive when(color) {
      is PaintColor.SolidColor -> graphics.color = Color(color.argb)
      is PaintColor.Gradient -> graphics.paint = (color as GradientImpl).getPaint()
      null -> {}
    }
  }

  override fun setGlobalAlpha(alpha: Double) {
    graphics.composite = (graphics.composite as AlphaComposite).derive(alpha.toFloat())
  }

  override fun setGlobalCompositeOperation(type: Context2d.CompositeOperationType) {
    graphics.composite = when(type) {
      Context2d.CompositeOperationType.SRC_OVER -> AlphaComposite.SrcOver
      Context2d.CompositeOperationType.DST_OVER -> AlphaComposite.DstOver
      Context2d.CompositeOperationType.SRC_IN -> AlphaComposite.SrcIn
      Context2d.CompositeOperationType.DST_IN -> AlphaComposite.DstIn
      Context2d.CompositeOperationType.SRC_OUT -> AlphaComposite.SrcOut
      Context2d.CompositeOperationType.DST_OUT -> AlphaComposite.DstOut
      Context2d.CompositeOperationType.SRC_ATOP -> AlphaComposite.SrcAtop
      Context2d.CompositeOperationType.DST_ATOP -> AlphaComposite.DstAtop
      Context2d.CompositeOperationType.XOR -> AlphaComposite.Xor
    }
  }

  override fun setFont(f: String) {
    graphics.font = SwingFontCache.getFont(f)
  }

  override fun setLineWidth(lineWidth: Double) {
    myCurrentStroke = myCurrentStroke.change(width = lineWidth.toFloat())
  }

  override fun strokeRect(x: Double, y: Double, w: Double, h: Double) {
    graphics.drawRect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
  }

  override fun strokeText(text: String, x: Double, y: Double) {
    // todo: outline somehow?
    graphics.drawString(text, x.toFloat(), y.toFloat())
  }

  override fun fillText(text: String, x: Double, y: Double) {
    graphics.drawString(text, x.toFloat(), y.toFloat())
  }

  override fun scale(x: Double, y: Double) {
    graphics.scale(x, y)
  }

  override fun rotate(angle: Double) {
    graphics.rotate(angle)
  }

  override fun translate(x: Double, y: Double) {
    graphics.translate(x, y)
  }

  override fun transform(m11: Double, m12: Double, m21: Double, m22: Double, dx: Double, dy: Double) {
    graphics.transform(AffineTransform(m11, m12, m21, m22, dx, dy))
  }

  override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {
    myCurrentPath.curveTo(cp1x, cp1y, cp2x, cp2y, x, y)
  }

  override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
    myCurrentPath.quadTo(cpx, cpy, x, y)
  }

  override fun setLineJoin(lineJoin: Context2d.LineJoin) {
    myCurrentStroke = myCurrentStroke.change(join = when(lineJoin) {
      Context2d.LineJoin.BEVEL -> BasicStroke.JOIN_BEVEL
      Context2d.LineJoin.MITER -> BasicStroke.JOIN_MITER
      Context2d.LineJoin.ROUND -> BasicStroke.JOIN_ROUND
    })
  }

  override fun setMiterLimit(limit: Double) {
    myCurrentStroke = myCurrentStroke.change(miter = limit.toFloat())
  }

  override fun setLineCap(lineCap: Context2d.LineCap) {
    myCurrentStroke = myCurrentStroke.change(cap = when(lineCap) {
      Context2d.LineCap.BUTT -> BasicStroke.CAP_BUTT
      Context2d.LineCap.ROUND -> BasicStroke.CAP_ROUND
      Context2d.LineCap.SQUARE -> BasicStroke.CAP_SQUARE
    })
  }

  override fun setTextBaseline(baseline: Context2d.TextBaseline) {
    TODO("Not yet implemented")
  }

  override fun setTextAlign(align: Context2d.TextAlign) {
    TODO("Not yet implemented")
  }

  override fun setTransform(m11: Double, m12: Double, m21: Double, m22: Double, dx: Double, dy: Double) {
    graphics.transform = AffineTransform(m11, m12, m21, m22, dx, dy)
  }

  override fun setLineDash(lineDash: DoubleArray) {
    myCurrentStroke = myCurrentStroke.change(dash = if(lineDash.isEmpty()) null else FloatArray(lineDash.size) { lineDash[it].toFloat() })
  }

  override fun setLineDashOffset(offset: Double) {
    myCurrentStroke = myCurrentStroke.change(dash_phase = offset.toFloat())
  }

  override fun measureText(str: String): Point {
    val result = graphics.fontMetrics.getStringBounds(str, graphics)
    return Point(result.width, result.height)
  }

  override fun clip(fillRule: Context2d.FillRule?) {
    setFillRule(fillRule)

    graphics.clip = myCurrentPath
  }

  private fun setFillRule(fillRule: Context2d.FillRule?) {
    myCurrentPath.windingRule = when (fillRule ?: Context2d.FillRule.EVENODD) {
      Context2d.FillRule.NONZERO -> Path2D.WIND_NON_ZERO
      Context2d.FillRule.EVENODD -> Path2D.WIND_EVEN_ODD
    }
  }

  override fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): PaintColor.Gradient {
    return GradientImpl(x0, y0, x1, y1)
  }

  override fun getTransform(): Context2d.Matrix {
    val t = graphics.transform
    return Context2d.Matrix(t.scaleX, t.shearY, t.shearX, t.scaleY, t.translateX, t.translateY)
  }

  private class GradientImpl(val x0: Double, val y0: Double, val x1: Double, val y1: Double) : PaintColor.Gradient() {
    private val points = ArrayList<Pair<Double, Int>>()

    override fun addColorStop(offset: Double, argb: Int) {
      points.add(offset to argb)
    }

    fun getPaint() : Paint {
      return when(points.size) {
        0 -> error("No points in gradient paint")
        1 -> Color(points[0].second)
        2 -> GradientPaint(x0.toFloat(), y0.toFloat(), Color(points[0].second), x1.toFloat(), y1.toFloat(), Color(points[1].second))
        else -> LinearGradientPaint(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), points.map { it.first.toFloat() }.toFloatArray(), points.map { Color(it.second) }.toTypedArray())
      }
    }
  }
}
