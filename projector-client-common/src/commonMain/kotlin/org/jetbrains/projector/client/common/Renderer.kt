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

import org.jetbrains.projector.client.common.canvas.*
import org.jetbrains.projector.client.common.canvas.Canvas.ImageSource
import org.jetbrains.projector.client.common.canvas.Context2d.FillRule
import org.jetbrains.projector.client.common.canvas.Context2d.Matrix.Companion.IDENTITY_LIST
import org.jetbrains.projector.client.common.canvas.Extensions.applyStrokeData
import org.jetbrains.projector.client.common.canvas.Extensions.toContext2dRule
import org.jetbrains.projector.client.common.canvas.Extensions.toFillRule
import org.jetbrains.projector.client.common.canvas.PaintColor.SolidColor
import org.jetbrains.projector.client.common.canvas.buffering.RenderingSurface
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.ParamsProvider.REPAINT_AREA
import org.jetbrains.projector.common.misc.Defaults
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.*
import org.jetbrains.projector.util.logging.Logger
import kotlin.math.PI
import kotlin.random.Random

class Renderer(private val renderingSurface: RenderingSurface) {
  private val ctx: Context2d
    get() = renderingSurface.canvas.context2d()

  private val canvasState = CanvasRenderingState()
  val requestedState = RequestedRenderingState()

  private val textScaleCache = HashMap<Int, Double>()

  private fun applyFillStyle(newFillStyle: PaintColor?) {
    ctx.setFillStyle(newFillStyle)
    canvasState.fillStyle = newFillStyle
  }

  private fun ensureFillStyle() {
    if (REPAINT_AREA.not()) {
      requestedState.paint?.let { requestedPaint ->
        if( canvasState.fillStyle == null ){
          applyFillStyle(requestedPaint)
        }else{
          val fillStyle = canvasState.fillStyle?: SolidColor(0xFFFFFF)
          if( fillStyle.tpe.ordinal == requestedPaint.tpe.ordinal ){
            when(fillStyle.tpe.ordinal){
              PaintColorType.SolidColor.ordinal -> if(fillStyle.argb  != requestedPaint.argb){
                applyFillStyle(requestedPaint)
              }
              PaintColorType.Gradient.ordinal -> applyFillStyle(requestedPaint)
            }
          }
        }
      }
    }
    else {
      applyFillStyle(createNextRandomColor())
    }
  }

  private fun applyStrokeStyle(newStrokeStyle: PaintColor?) {
    ctx.setStrokeStyle(newStrokeStyle)
    canvasState.strokeStyle = newStrokeStyle
  }

  private fun ensureStrokeStyle() {

    if (REPAINT_AREA) {
      applyStrokeStyle(createNextRandomColor())
    }
    else {
      requestedState.paint?.let { requestedPaint ->
        if( canvasState.strokeStyle == null){
          applyStrokeStyle(requestedPaint)
        }else{
          val strokeStyle = canvasState.strokeStyle ?: SolidColor(0xFFFFFF)
          if( strokeStyle.tpe.ordinal == requestedPaint.tpe.ordinal ){
            when(strokeStyle.tpe.ordinal){
              PaintColorType.SolidColor.ordinal -> if(strokeStyle.argb  != requestedPaint.argb){
                applyStrokeStyle(requestedPaint)
              }
              PaintColorType.Gradient.ordinal -> applyStrokeStyle(requestedPaint)
            }
          }
        }
      }
    }
  }

  private fun applyTransform(newTransform: DoubleArray) {
    renderingSurface.scalingRatio.let {
      with(newTransform) {
        ctx.setTransform(it,.0,.0,it,.0,.0)
        ctx.transform(get(0),get(1), get(2), get(3), get(4), get(5))
      }
    }
    canvasState.transform = newTransform
  }

  private fun ensureTransform() {
    if (requestedState.transform != null && requestedState.transform.size >= 6) {
      if (canvasState.transform == null || !(
          requestedState.transform[0] == canvasState.transform[0] &&
          requestedState.transform[1] == canvasState.transform[1] &&
          requestedState.transform[2] == canvasState.transform[2] &&
          requestedState.transform[3] == canvasState.transform[3] &&
          requestedState.transform[4] == canvasState.transform[4] &&
          requestedState.transform[5] == canvasState.transform[5]
                                            )
      ) {
        applyTransform(requestedState.transform)
      }
    }
  }

  private fun doClip(){
    canvasState.identitySpaceClip?.apply {
      when(this.tpe.ordinal){
        CommonShapeType.CommonRectangle.ordinal -> ctx.clip()
        CommonShapeType.CommonPath.ordinal -> ctx.clip(this.asUnsafe<CommonPath>().winding.toFillRule())
      }
    }
  }

  private fun applyClip(newIdentitySpaceClip: CommonShape?) {
    newIdentitySpaceClip?.apply {
      ctx.beginPath()

      renderingSurface.scalingRatio.let {
        ctx.setTransform(it, 0.0, 0.0, it, 0.0, 0.0)
      }

      when(this.tpe.ordinal){
        CommonShapeType.CommonRectangle.ordinal -> with(this.asUnsafe<CommonRectangle>()){
            ctx.rect(x,y,width,height)
          }

        CommonShapeType.CommonPath.ordinal -> with(this.asUnsafe<CommonPath>()){
          ctx.moveBySegments(segments)
        }
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

    }
    canvasState.transform = IDENTITY_LIST
    canvasState.identitySpaceClip = newIdentitySpaceClip
  }

  private fun drawClipRegion() {
    if(requestedState.identitySpaceClip == null){
      applyClip(canvasState.identitySpaceClip)
    }else{
      applyClip(requestedState.identitySpaceClip)
    }
  }

  private fun applyStroke(newStrokeData: StrokeData) {
    ctx.applyStrokeData(newStrokeData)
    canvasState.strokeData = newStrokeData
  }

  private fun ensureStroke() {
    requestedState.strokeData?.asUnsafe<StrokeData.Basic>().let {
      val curr = canvasState.strokeData.asUnsafe<StrokeData.Basic>()
      if(!(
          it.dashPhase == curr.dashPhase &&
          it.lineWidth == curr.lineWidth &&
          it.miterLimit == curr.miterLimit &&
          it.endCap.ordinal == curr.endCap.ordinal &&
          it.lineJoin.ordinal == curr.lineJoin.ordinal &&
          it.dashArray?.size == curr.dashArray?.size )
      ){
        applyStroke(it)
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

  private fun applyFont(fontSize: Int, fontName: String) {
    ctx.setFont(Extensions.fontSizeStrCache[fontSize] + fontName)
    canvasState.fontSize = fontSize
    canvasState.fontName = fontName
  }

  private fun ensureFont() {
    if (requestedState.fontSize != canvasState.fontSize || requestedState.fontName.hashCode() != canvasState.fontName.hashCode()) {
      applyFont(requestedState.fontSize, requestedState.fontName)
    }
  }

  private fun applyRule(newRule: AlphaCompositeRule) {
    ctx.setGlobalCompositeOperation(newRule.toContext2dRule())
    canvasState.rule = newRule
  }

  private fun ensureRule() {
    if (canvasState.rule != requestedState.rule) {
      applyRule(requestedState.rule)
    }
  }

  private fun applyAlpha(newAlpha: Double) {
    ctx.setGlobalAlpha(newAlpha)
    canvasState.alpha = newAlpha
  }

  private fun ensureAlpha() {
    if (canvasState.alpha != requestedState.alpha) {
      applyAlpha(requestedState.alpha)
    }
  }

  private fun ensureComposite() {
    ensureRule()
    ensureAlpha()
  }

  fun setColor(color: Int) {
    requestedState.paint = color.toColor()
  }

  fun setGradientPaint(p1: Point, p2: Point, color1: Int, color2: Int) {
    val linearGradient = ctx.createLinearGradient(p1.x, p1.y, p2.x, p2.y).apply {
      addColorStop(0.0, color1)
      addColorStop(1.0, color2)
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
    drawClipRegion()
    ensureTransform()
    ensureFillStyle()
    ensureFont()
    ensureComposite()
    if (ParamsProvider.SHOW_TEXT_WIDTH) {
      ensureStrokeStyle()
    }

    ctx.apply {
      save()
      doClip()

      val textRescale = if (!textScaleCache.containsKey(canvasState.fontSize)) {
        val dimension = ctx.measureText(string)
        val scale = desiredWidth / dimension.x
        textScaleCache.put(canvasState.fontSize, scale)
        scale
      }
      else {
        textScaleCache.get(canvasState.fontSize) ?: 1.0
      }


      //
      //


      if (textRescale < 1 - 1e-4 || textRescale > 1 + 1e-4) {
        translate(x, y)
        scale(textRescale, 1.0)
        fillText(string, 0.0, 0.0)
      } else {
        fillText(string, x, y)
      }


      if (ParamsProvider.SHOW_TEXT_WIDTH) {
        beginPath()
        moveTo(x, y)
        lineTo(x + desiredWidth, y)
        stroke()
      }
      restore()
    }
  }

  fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
    drawClipRegion()
    ensureTransform()
    ensureStrokeStyle()
    ensureStroke()
    ensureComposite()
    with(ctx) {
      save()
      doClip()
      beginPath()
      moveTo(x1, y1)
      lineTo(x2, y2)
      stroke()
      restore()
    }
  }

  fun paintRoundRect(paintType: PaintType, x: Double, y: Double, w: Double, h: Double, r1: Double, r2: Double) {
    drawClipRegion()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    @Suppress("NAME_SHADOWING") val r1 = minOf(r1, w / 2)
    @Suppress("NAME_SHADOWING") val r2 = minOf(r2, h / 2)
    with(ctx) {
      save()
      doClip()
      beginPath()
      roundedRect(x, y, w, h, r1, r2)
      when (paintType.ordinal) {
        PaintType.FILL.ordinal -> fill()

        PaintType.DRAW.ordinal -> stroke()
      }
      restore()
    }
  }

  fun paintPath(paintType: PaintType, path: CommonPath) {
    drawClipRegion()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    with(ctx) {
      save()
      doClip()
      beginPath()
      moveBySegments(path.segments)

      Do exhaustive when (paintType) {
        PaintType.FILL -> fill(path.winding.toFillRule())

        PaintType.DRAW -> stroke()
      }
      restore()
    }
  }

  fun paintRect(paintType: PaintType, x: Double, y: Double, width: Double, height: Double) {
    drawClipRegion()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    with(ctx) {
      save()
      doClip()
      //TODO: hacks, kotlin's `when` pattern matching with type casting is expensive.
      /*
        using a type enum signature to avoid type checking in JS runtime which is expensive.
       */
      when (paintType.ordinal) {
        PaintType.FILL.ordinal -> fillRect(x, y, width, height)

        PaintType.DRAW.ordinal -> strokeRect(x, y, width, height)
      }
      restore()
    }
  }

  fun paintPolygon(paintType: PaintType, points: List<Point>) {
    drawClipRegion()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    ctx.apply {
      save()
      doClip()
      beginPath()
      moveByPoints(points)

      Do exhaustive when (paintType) {
        PaintType.FILL -> fill(FillRule.EVENODD)  // even-odd is from fillPolygon java doc

        PaintType.DRAW -> stroke()
      }
      restore()
    }
  }

  fun drawPolyline(points: List<Point>) {
    points
      .windowed(2)
      .forEach { (p1, p2) ->
        drawLine(p1.x, p1.y, p2.x, p2.y)
      }
  }

  fun setClip(identitySpaceClip: CommonShape?) {
    requestedState.identitySpaceClip = identitySpaceClip
  }

  fun setTransform(tx: DoubleArray) {
    requestedState.transform = tx
  }

  fun setFont(fontId: Int?, fontSize: Int, ligaturesOn: Boolean) {
    requestedState.fontSize = fontSize
    requestedState.fontName = fontId?.let { Extensions.serverFontNameCache[fontId] } ?: "Arial"
    renderingSurface.canvas.fontVariantLigatures = ligaturesOn.toLigatureVariant()
    renderingSurface.canvas
  }

  private fun Boolean.toLigatureVariant(): String {
    return if (this) "normal" else "none"
  }

  fun drawImage(image: ImageSource, x: Double, y: Double) {
    if (image.isEmpty()) return

    drawClipRegion()
    ensureTransform()
    ensureComposite()
    ctx.save()
    doClip()
    ctx.drawImage(image, x, y)
    ctx.restore()
  }

  fun drawImage(image: ImageSource, x: Double, y: Double, width: Double, height: Double) {
    if (image.isEmpty()) return

    drawClipRegion()
    ensureTransform()
    ensureComposite()

    ctx.save()
    doClip()
    ctx.drawImage(image, x, y, width, height)
    ctx.restore()
  }

  fun drawImage(image: ImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double) {
    if (image.isEmpty()) return

    drawClipRegion()
    ensureTransform()
    ensureComposite()

    ctx.save()
    doClip()
    ctx.drawImage(
      image,
      sx = sx, sy = sy, sw = sw, sh = sh,
      dx = dx, dy = dy, dw = dw, dh = dh
    )
    ctx.restore()
  }

  fun drawImage(image: ImageSource, tx: DoubleArray) {
    if (image.isEmpty()) return

    drawClipRegion()
    ensureTransform()
    ensureComposite()

    ctx.apply {
      save()
      doClip()
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
    drawClipRegion()
    ensureTransform()
    ensurePaint(paintType)
    ensureComposite()

    ctx.apply {
      save()
      doClip()
      beginPath()
      ellipse(x + width / 2, y + height / 2, width / 2, height / 2, 0.0, 0.0, 2 * PI)
      when (paintType) {
        PaintType.DRAW -> stroke()

        PaintType.FILL -> fill()
      }
      restore()
    }
  }

  fun copyArea(x: Double, y: Double, width: Double, height: Double, dx: Double, dy: Double) {
    drawClipRegion()
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
      doClip()

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
      var transform: DoubleArray = DEFAULT_TRANSFORM,
      var strokeData: StrokeData = DEFAULT_STROKE_DATA,
      var fontSize: Int = Defaults.FONT_SIZE,
      var fontName: String = Defaults.FONT_NAME,
      var rule: AlphaCompositeRule = DEFAULT_RULE,
      var alpha: Double = DEFAULT_ALPHA,
      var fillStyle: PaintColor? = DEFAULT_FILL_STYLE,
      var strokeStyle: PaintColor? = DEFAULT_STROKE_STYLE,
    ) {

      fun reset() {
        identitySpaceClip = DEFAULT_IDENTITY_SPACE_CLIP
        transform = DEFAULT_TRANSFORM
        strokeData = DEFAULT_STROKE_DATA
        fontSize = Defaults.FONT_SIZE
        fontName = Defaults.FONT_NAME
        alpha = DEFAULT_ALPHA
        fillStyle = DEFAULT_FILL_STYLE
        strokeStyle = DEFAULT_STROKE_STYLE
      }

      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CanvasRenderingState

        if (identitySpaceClip != other.identitySpaceClip) return false
        if (!transform.contentEquals(other.transform)) return false
        if (strokeData != other.strokeData) return false
        if (fontSize != other.fontSize) return false
        if (fontName != other.fontName) return false
        if (rule != other.rule) return false
        if (alpha != other.alpha) return false
        if (fillStyle != other.fillStyle) return false
        if (strokeStyle != other.strokeStyle) return false

        return true
      }

      override fun hashCode(): Int {
        var result = identitySpaceClip?.hashCode() ?: 0
        result = 31 * result + transform.contentHashCode()
        result = 31 * result + strokeData.hashCode()
        result = 31 * result + fontSize
        result = 31 * result + fontName.hashCode()
        result = 31 * result + rule.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + (fillStyle?.hashCode() ?: 0)
        result = 31 * result + (strokeStyle?.hashCode() ?: 0)
        return result
      }

      companion object {

        private val DEFAULT_IDENTITY_SPACE_CLIP: CommonShape? = CommonRectangle(0.0,0.0,0.0,0.0)
        private val DEFAULT_TRANSFORM: DoubleArray = IDENTITY_LIST
        private val DEFAULT_STROKE_DATA: StrokeData = Defaults.STROKE
        private val DEFAULT_RULE: AlphaCompositeRule = AlphaCompositeRule.SRC_OVER
        private const val DEFAULT_ALPHA: Double = 1.0
        private val DEFAULT_FILL_STYLE: PaintColor? = null
        private val DEFAULT_STROKE_STYLE: PaintColor? = null
      }
    }

    data class RequestedRenderingState(
      var identitySpaceClip: CommonShape? = null,
      var transform: DoubleArray = IDENTITY_LIST,
      var strokeData: StrokeData = Defaults.STROKE,
      var font: String = "${Defaults.FONT_SIZE}px Arial",
      var rule: AlphaCompositeRule = AlphaCompositeRule.SRC_OVER,
      var alpha: Double = 1.0,
      var paint: PaintColor? = SolidColor(Defaults.FOREGROUND_COLOR_ARGB),
      var fontSize: Int = Defaults.FONT_SIZE,
      var fontName: String = "Arial",
    )
  }
}
