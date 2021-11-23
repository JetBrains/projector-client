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

import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.AlphaCompositeRule
import org.jetbrains.projector.common.protocol.data.CommonPath
import org.jetbrains.projector.common.protocol.data.StrokeData
import kotlin.math.absoluteValue

object Extensions {

  fun Canvas.resizeSavingImage(width: Int, height: Int) {
    if (this.width == width && this.height == height) {
      return  // prevents clearing canvas
    }

    if (this.width == 0 || this.height == 0) {
      this.width = width
      this.height = height

      return
    }

    val snapshot = this.takeSnapshot()
    val scalingChanged = (this.width.toDouble() / width - this.height.toDouble() / height).absoluteValue < 0.1

    this.width = width
    this.height = height

    if (scalingChanged) {
      this.context2d.drawImage(
        snapshot,
        0.0,
        0.0,
        this.width.toDouble(),
        this.height.toDouble()
      )  // save previous image with scaling
    }
    else {
      this.context2d.drawImage(snapshot, 0.0, 0.0)  // save previous image with the same size to avoid stretching
    }
  }

  fun CommonPath.WindingType.toFillRule() = when (this) {
    CommonPath.WindingType.EVEN_ODD -> Context2d.FillRule.EVENODD
    CommonPath.WindingType.NON_ZERO -> Context2d.FillRule.NONZERO
  }

  fun Context2d.applyStrokeData(strokeData: StrokeData) {
    Do exhaustive when (strokeData) {
      is StrokeData.Basic -> {
        setLineWidth(strokeData.lineWidth.toDouble())
        setLineCap(strokeData.endCap.toCanvasLineCap())
        setLineJoin(strokeData.lineJoin.toCanvasLineJoin())
        setMiterLimit(strokeData.miterLimit.toDouble())
        setLineDash(strokeData.dashArray?.map(Float::toDouble)?.toDoubleArray() ?: DoubleArray(0))
        setLineDashOffset(strokeData.dashPhase.toDouble())
      }
    }
  }

  fun StrokeData.Basic.CapType.toCanvasLineCap(): Context2d.LineCap {
    return when (this) {
      StrokeData.Basic.CapType.ROUND -> Context2d.LineCap.ROUND
      StrokeData.Basic.CapType.SQUARE -> Context2d.LineCap.SQUARE
      StrokeData.Basic.CapType.BUTT -> Context2d.LineCap.BUTT
    }
  }

  fun StrokeData.Basic.JoinType.toCanvasLineJoin(): Context2d.LineJoin {
    return when (this) {
      StrokeData.Basic.JoinType.ROUND -> Context2d.LineJoin.ROUND
      StrokeData.Basic.JoinType.BEVEL -> Context2d.LineJoin.BEVEL
      StrokeData.Basic.JoinType.MITER -> Context2d.LineJoin.MITER
    }
  }

  fun AlphaCompositeRule.toContext2dRule(): Context2d.CompositeOperationType = when (this) {
    AlphaCompositeRule.SRC_OVER -> Context2d.CompositeOperationType.SRC_OVER
    AlphaCompositeRule.DST_OVER -> Context2d.CompositeOperationType.DST_OVER
    AlphaCompositeRule.SRC_IN -> Context2d.CompositeOperationType.SRC_IN
    AlphaCompositeRule.DST_IN -> Context2d.CompositeOperationType.DST_IN
    AlphaCompositeRule.SRC_OUT -> Context2d.CompositeOperationType.SRC_OUT
    AlphaCompositeRule.DST_OUT -> Context2d.CompositeOperationType.DST_OUT
    AlphaCompositeRule.SRC_ATOP -> Context2d.CompositeOperationType.SRC_ATOP
    AlphaCompositeRule.DST_ATOP -> Context2d.CompositeOperationType.DST_ATOP
    AlphaCompositeRule.XOR -> Context2d.CompositeOperationType.XOR
    AlphaCompositeRule.SRC -> Context2d.CompositeOperationType.SRC
    AlphaCompositeRule.CLEAR -> Context2d.CompositeOperationType.CLEAR
    AlphaCompositeRule.DST -> Context2d.CompositeOperationType.DST
  }

  fun Short.toFontFaceName(): String = "serverFont$this"

  /* Creates an rgba(...) string (JS-like) by an ARGB number (Java-like). */
  fun Int.argbIntToRgbaString(): String {
    val colorValue = this

    val b = colorValue and 0xFF
    val g = (colorValue ushr 8) and 0xFF
    val r = (colorValue ushr 16) and 0xFF
    val a = ((colorValue ushr 24) and 0xFF) / 255.0

    return "rgba($r,$g,$b,$a)"
  }
}

