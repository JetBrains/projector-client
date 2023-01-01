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
package org.jetbrains.projector.client.common

import org.jetbrains.projector.client.common.canvas.Canvas.ImageSource
import org.jetbrains.projector.client.common.canvas.Context2d
import org.jetbrains.projector.client.common.canvas.Context2d.FillRule
import org.jetbrains.projector.client.common.canvas.Context2d.Matrix
import org.jetbrains.projector.client.common.canvas.Context2d.Matrix.Companion.IDENTITY_LIST
import org.jetbrains.projector.client.common.canvas.Extensions.applyStrokeData
import org.jetbrains.projector.client.common.canvas.Extensions.toContext2dRule
import org.jetbrains.projector.client.common.canvas.Extensions.toFillRule
import org.jetbrains.projector.client.common.canvas.Extensions.toFontFaceName
import org.jetbrains.projector.client.common.canvas.PaintColor
import org.jetbrains.projector.client.common.canvas.PaintColor.SolidColor
import org.jetbrains.projector.client.common.canvas.buffering.RenderingSurface
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.ParamsProvider.REPAINT_AREA
import org.jetbrains.projector.client.common.misc.RepaintAreaSetting
import org.jetbrains.projector.common.misc.Defaults
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.*
import org.jetbrains.projector.util.logging.Logger
import kotlin.math.PI
import kotlin.random.Random

class Renderer(private val renderingSurface: RenderingSurface) {
  private val ctx: Context2d
    get() = renderingSurface.canvas.context2d

  private val canvasState = CanvasRenderingState()
  val requestedState = RequestedRenderingState()

  private fun applyFillStyle(newFillStyle: PaintColor?) {
    ctx.setFillStyle(newFillStyle)
    canvasState.fillStyle = newFillStyle
  }

  private fun ensureFillStyle() {
    fun realEnsureFillStyle() {
      requestedState.paint?.let { requestedPaint ->
        canvasState.fillStyle.let { currentPaint ->
          if (currentPaint != requestedPaint) {
            applyFillStyle(requestedPaint)
          }
        }
      }
    }

    Do exhaustive when (val repaintArea = REPAINT_AREA) {
      is RepaintAreaSetting.Disabled -> realEnsureFillStyle()
      is RepaintAreaSetting.Enabled -> Do exhaustive when (repaintArea.show) {
        false -> realEnsureFillStyle()
        true -> applyFillStyle(createNextRandomColor())
      }
    }
  }

  private fun applyStrokeStyle(newStrokeStyle: PaintColor?) {
    ctx.setStrokeStyle(newStrokeStyle)
    canvasState.strokeStyle = newStrokeStyle
  }

  private fun ensureStrokeStyle() {
    fun realEnsureStrokeStyle() {
      requestedState.paint?.let { requestedPaint ->
        canvasState.strokeStyle.let { currentPaint ->
          if (currentPaint != requestedPaint) {
            applyStrokeStyle(requestedPaint)
          }
        }
      }
    }

    Do exhaustive when (val repaintArea = REPAINT_AREA) {
      is RepaintAreaSetting.Disabled -> realEnsureStrokeStyle()
      is RepaintAreaSetting.Enabled -> Do exhaustive when (repaintArea.show) {
        false -> realEnsureStrokeStyle()
        true -> applyStrokeStyle(createNextRandomColor())
      }
    }
  }

  private fun applyTransform(newTransform: List<Double>) {
    renderingSurface.scalingRatio.let {
      ctx.setTransform(it, 0.0, 0.0, it, 0.0, 0.0)
    }

    with(Matrix(newTransform)) { ctx.transform(a, b, c, d, e, f) }

    canvasState.transform = newTransform
  }

  private fun ensureTransform() {
    requestedState.transform.let { requestedTransform ->
      canvasState.transform.let { currentTransform ->
        if (currentTransform != requestedTransform) {
          applyTransform(requestedTransform)
        }
      }
    }
  }

  private fun applyClip(newIdentitySpaceClip: CommonShape?) {
    ctx.restore()
    ctx.save()

    renderingSurface.scalingRatio.let {
      ctx.setTransform(it, 0.0, 0.0, it, 0.0, 0.0)
    }

    newIdentitySpaceClip?.apply {
      ctx.beginPath()

      Do exhaustive when (this) {
        is CommonRectangle -> ctx.rect(x, y, width, height)
        is CommonPath -> ctx.moveBySegments(segments)
      }

      if (ParamsProvider.CLIPPING_BORDERS) {
        ctx.save()

        Do exhaustive when (this) {
          is CommonRectangle -> SolidColor(0xFFCC0000)
          is CommonPath -> SolidColor(0xFF0000CC)
        }.run(ctx::setStrokeStyle)

        ctx.stroke()

        ctx.restore()
      }

      Do exhaustive when (this) {
        is CommonRectangle -> ctx.clip()
        is CommonPath -> ctx.clip(winding.toFillRule())
      }
    }

    canvasState.identitySpaceClip = newIdentitySpaceClip

    with(canvasState) {
      applyTransform(transform)
      applyStrokeStyle(strokeStyle)
      applyFillStyle(fillStyle)
      applyStroke(strokeData)
      applyFont(font)
      applyRule(rule)
      applyAlpha(alpha)
    }
  }

  private fun ensureClip() {
    requestedState.identitySpaceClip.let { requestedClip ->
      canvasState.identitySpaceClip.let { currentClip ->
        if (currentClip != requestedClip) {
          applyClip(requestedClip)
        }
      }
    }
  }

  private fun applyStroke(newStrokeData: StrokeData) {
    ctx.applyStrokeData(newStrokeData)
    canvasState.strokeData = newStrokeData
  }

  private fun ensureStroke() {
    requestedState.strokeData.let { requestedStroke ->
      canvasState.strokeData.let { currentStroke ->
        if (currentStroke != requestedStroke) {
          applyStroke(requestedStroke)
        }
      }
    }
  }

  private fun ensurePaint(paintType: PaintType) {
    Do exhaustive when (paintType) {
      PaintType.DRAW -> {
        ensureStrokeStyle()
        ensureStroke()
      }

      PaintType.FILL -> ensureFillStyle()
    }
  }

  private fun applyFont(newFont: String) {
    ctx.setFont(newFont)
    canvasState.font = newFont
  }

  private fun ensureFont() {
    requestedState.font.let { requestedFont ->
      canvasState.font.let { currentFont ->
        if (currentFont != requestedFont) {
          applyFont(requestedFont)
        }
      }
    }
  }

  private fun applyRule(newRule: AlphaCompositeRule) {
    ctx.setGlobalCompositeOperation(newRule.toContext2dRule())
    canvasState.rule = newRule
  }

  private fun ensureRule() {
    requestedState.rule.let { requestedRule ->
      canvasState.rule.let { currentRule ->
        if (currentRule != requestedRule) {
          applyRule(requestedRule)
        }
      }
    }
  }

  private fun applyAlpha(newAlpha: Double) {
    ctx.setGlobalAlpha(newAlpha)
    canvasState.alpha = newAlpha
  }

  private fun ensureAlpha() {
    requestedState.alpha.let { requestedAlpha ->
      canvasState.alpha.let { currentAlpha ->
        if (currentAlpha != requestedAlpha) {
          applyAlpha(requestedAlpha)
        }
      }
    }
  }

  private fun ensureComposite() {
    ensureRule()
    ensureAlpha()
  }

  fun setColor(color: Int) {
    requestedState.paint = color.toColor()
  }

  fun setGradientPaint(p1: Point, p2: Point, fractions: List<Double>, argbs: List<Int>) {
    val linearGradient = ctx.createLinearGradient(p1.x, p1.y, p2.x, p2.y)
    fractions.zip(argbs).forEach { (fraction, argb) ->
      linearGradient.addColorStop(fraction, argb)
    }

    requestedState.paint = linearGradient
  }

  fun setComposite(composite: CommonComposite) {
    Do exhaustive when (composite) {
      is CommonAlphaComposite -> requestedState.apply {
        alpha = composite.alpha.toDouble()
        rule = composite.rule
      }

      is UnknownComposite -> requestedState.apply {
        alpha = 1.0
        rule = AlphaCompositeRule.SRC_OVER

        logger.debug { "setComposite: $composite" }
      }
    }
  }

  fun drawString(string: String, x: Double, y: Double, desiredWidth: Double) {
    ensureClip()
    ensureTransform()
    ensureFillStyle()
    ensureFont()
    ensureComposite()
    if (ParamsProvider.SHOW_TEXT_WIDTH) {
      ensureStrokeStyle()
    }

    ctx.apply {
      val textDimensions = measureText(string)

      save()

      val width = textDimensions.x
      translate(x, y)
      scale(desiredWidth / width, 1.0)
      fillText(string, 0.0, 0.0)

      restore()

      if (ParamsProvider.SHOW_TEXT_WIDTH) {
        beginPath()
        moveTo(x, y)
        lineTo(x + desiredWidth, y)
        stroke()

        val height = textDimensions.y
        beginPath()
        moveTo(x, y - height)
        lineTo(x + width, y - height)
        stroke()
      }
    }
  }

  fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
    ensureClip()
    ensureTransform()
    ensureStrokeStyle()
    ensureStroke()
    ensureComposite()

    ctx.apply {
      beginPath()
      moveTo(x1, y1)
      lineTo(x2, y2)
      stroke()
    }
  }

  fun paintRoundRect(paintType: PaintType, x: Double, y: Double, w: Double, h: Double, r1: Double, r2: Double) {
    ensureClip()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    @Suppress("NAME_SHADOWING") val r1 = minOf(r1, w / 2)
    @Suppress("NAME_SHADOWING") val r2 = minOf(r2, h / 2)
    ctx.apply {
      beginPath()
      roundedRect(x, y, w, h, r1, r2)

      Do exhaustive when (paintType) {
        PaintType.FILL -> fill()

        PaintType.DRAW -> stroke()
      }
    }
  }

  fun paintPath(paintType: PaintType, path: CommonPath) {
    ensureClip()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    ctx.apply {
      beginPath()
      moveBySegments(path.segments)

      Do exhaustive when (paintType) {
        PaintType.FILL -> fill(path.winding.toFillRule())

        PaintType.DRAW -> stroke()
      }
    }
  }

  fun paintRect(paintType: PaintType, x: Double, y: Double, width: Double, height: Double) {
    ensureClip()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    Do exhaustive when (paintType) {
      PaintType.FILL -> ctx.fillRect(x, y, width, height)

      PaintType.DRAW -> ctx.strokeRect(x, y, width, height)
    }
  }

  fun paintPolygon(paintType: PaintType, points: List<Point>) {
    ensureClip()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    ctx.apply {
      beginPath()
      moveByPoints(points)

      Do exhaustive when (paintType) {
        PaintType.FILL -> fill(FillRule.EVENODD)  // even-odd is from fillPolygon java doc

        PaintType.DRAW -> stroke()
      }
    }
  }

  fun drawPolyline(points: List<Point>) {
    ensureClip()
    ensureTransform()
    ensureStrokeStyle()
    ensureStroke()
    ensureComposite()

    points
      .windowed(2)
      .forEach { (p1, p2) ->
        drawLine(p1.x, p1.y, p2.x, p2.y)
      }
  }

  fun setClip(identitySpaceClip: CommonShape?) {
    requestedState.identitySpaceClip = identitySpaceClip
  }

  fun setTransform(tx: List<Double>) {
    requestedState.transform = tx
  }

  fun setFont(fontId: Short?, fontSize: Int, ligaturesOn: Boolean) {
    val font = if (fontId == null) {
      logger.debug { "null is used as a font ID. Using Arial..." }

      "${fontSize}px Arial"
    }
    else {
      "${fontSize}px ${fontId.toFontFaceName()}"
    }

    requestedState.font = font
    renderingSurface.canvas.fontVariantLigatures = ligaturesOn.toLigatureVariant()
    renderingSurface.canvas
  }

  private fun Boolean.toLigatureVariant(): String {
    return if (this) "normal" else "none"
  }

  fun drawImage(image: ImageSource, x: Double, y: Double) {
    if (image.isEmpty()) return

    ensureClip()
    ensureTransform()
    ensureComposite()
    ctx.drawImage(image, x, y)
  }

  fun drawImage(image: ImageSource, x: Double, y: Double, width: Double, height: Double) {
    if (image.isEmpty()) return

    ensureClip()
    ensureTransform()
    ensureComposite()

    ctx.drawImage(image, x, y, width, height)
  }

  fun drawImage(image: ImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double) {
    if (image.isEmpty()) return

    ensureClip()
    ensureTransform()
    ensureComposite()

    ctx.drawImage(
      image,
      sx = sx, sy = sy, sw = sw, sh = sh,
      dx = dx, dy = dy, dw = dw, dh = dh
    )
  }

  fun drawImage(image: ImageSource, tx: List<Double>) {
    if (image.isEmpty()) return

    ensureClip()
    ensureTransform()
    ensureComposite()

    ctx.apply {
      save()

      transform(tx[0], tx[1], tx[2], tx[3], tx[4], tx[5])
      ctx.drawImage(image, 0.0, 0.0)

      restore()
    }
  }

  fun drawImageRaw(image: ImageSource) {
    if (image.isEmpty()) return

    ctx.restore()

    ctx.drawImage(image, 0.0, 0.0)

    canvasState.reset()
  }

  fun setStroke(strokeData: StrokeData) {
    requestedState.strokeData = strokeData
  }

  fun paintOval(paintType: PaintType, x: Double, y: Double, width: Double, height: Double) {
    ensureClip()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    ctx.apply {
      beginPath()
      ellipse(x + width / 2, y + height / 2, width / 2, height / 2, 0.0, 0.0, 2 * PI)
      when (paintType) {
        PaintType.DRAW -> stroke()

        PaintType.FILL -> fill()
      }
    }
  }

  fun copyArea(x: Double, y: Double, width: Double, height: Double, dx: Double, dy: Double) {
    ensureClip()
    ensureTransform()
    ensureComposite()

    ctx.apply {
      // Algorithm (NOTE: it supports scaling ratio):
      // (1) Firstly, we clip to destination triangle (in requested coordinate space).
      //
      // After it, we can safely copy area:
      //
      // We are doing it by drawing the same canvas as an image on itself. How?
      // Actually, the only task here is to align the drawn image correctly.
      // The solution is to set the upper-left corner of the image to (dx, dy) but in requested coordinate space.
      // So we reset the translation (2) and then translate to (dx, dy) with scaling and shearing (3).
      // After it, we reset scaling and shearing (4) because we need to paint our image in 1-to-1 scale and without rotation.
      // Finally, we paint the image.

      save()

      // 1:
      beginPath()
      rect(x + dx, y + dy, width, height)
      clip()

      // 2:
      with(getTransform()) { this@apply.setTransform(a, b, c, d, 0.0, 0.0) }

      // 3:
      translate(dx, dy)

      // 4:
      with(getTransform()) { this@apply.setTransform(1.0, 0.0, 0.0, 1.0, e, f) }

      drawImage(renderingSurface.canvas.imageSource, 0.0, 0.0)

      restore()
    }
  }

  companion object {

    fun Int.toColor(): PaintColor {
      return SolidColor(this)
    }

    private val logger = Logger<Renderer>()

    private fun createNextRandomColor(): PaintColor {
      // Random argb color with 0.5 alpha
      return SolidColor(0x7F_00_00_00 + Random.nextInt(0x01_00_00_00))
    }

    private data class CanvasRenderingState(
      var identitySpaceClip: CommonShape? = DEFAULT_IDENTITY_SPACE_CLIP,
      var transform: List<Double> = DEFAULT_TRANSFORM,
      var strokeData: StrokeData = DEFAULT_STROKE_DATA,
      var font: String = DEFAULT_FONT,
      var rule: AlphaCompositeRule = DEFAULT_RULE,
      var alpha: Double = DEFAULT_ALPHA,
      var fillStyle: PaintColor? = DEFAULT_FILL_STYLE,
      var strokeStyle: PaintColor? = DEFAULT_STROKE_STYLE,
    ) {

      fun reset() {
        identitySpaceClip = DEFAULT_IDENTITY_SPACE_CLIP
        transform = DEFAULT_TRANSFORM
        strokeData = DEFAULT_STROKE_DATA
        font = DEFAULT_FONT
        rule = DEFAULT_RULE
        alpha = DEFAULT_ALPHA
        fillStyle = DEFAULT_FILL_STYLE
        strokeStyle = DEFAULT_STROKE_STYLE
      }

      companion object {

        private val DEFAULT_IDENTITY_SPACE_CLIP: CommonShape? = null
        private val DEFAULT_TRANSFORM: List<Double> = IDENTITY_LIST
        private val DEFAULT_STROKE_DATA: StrokeData = Defaults.STROKE
        private var DEFAULT_FONT: String = "${Defaults.FONT_SIZE}px Arial"
        private val DEFAULT_RULE: AlphaCompositeRule = AlphaCompositeRule.SRC_OVER
        private const val DEFAULT_ALPHA: Double = 1.0
        private val DEFAULT_FILL_STYLE: PaintColor? = null
        private val DEFAULT_STROKE_STYLE: PaintColor? = null
      }
    }

    data class RequestedRenderingState(
      var identitySpaceClip: CommonShape? = null,
      var transform: List<Double> = IDENTITY_LIST,
      var strokeData: StrokeData = Defaults.STROKE,
      var font: String = "${Defaults.FONT_SIZE}px Arial",
      var rule: AlphaCompositeRule = AlphaCompositeRule.SRC_OVER,
      var alpha: Double = 1.0,
      var paint: PaintColor? = SolidColor(Defaults.FOREGROUND_COLOR_ARGB),
    ) {

      fun setTo(other: RequestedRenderingState) {
        identitySpaceClip = other.identitySpaceClip
        transform = other.transform
        strokeData = other.strokeData
        font = other.font
        rule = other.rule
        alpha = other.alpha
        paint = other.paint
      }
    }
  }
}
