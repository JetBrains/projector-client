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
package org.jetbrains.projector.server.core.convert.toClient

import org.jetbrains.projector.common.protocol.data.*
import org.jetbrains.projector.common.protocol.data.Point
import org.jetbrains.projector.common.protocol.toClient.ServerSetClipEvent
import org.jetbrains.projector.common.protocol.toClient.ServerSetStrokeEvent
import org.jetbrains.projector.common.protocol.toClient.ServerSetUnknownStrokeEvent
import org.jetbrains.projector.common.protocol.toClient.ServerWindowStateEvent
import org.jetbrains.projector.util.logging.Logger
import java.awt.*
import java.awt.Cursor.*
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.ceil
import kotlin.math.floor

public fun Point2D.toPoint(): Point = Point(x, y)

public fun Dimension.toCommonIntSize(): CommonIntSize = CommonIntSize(width, height)

public fun Rectangle.toCommonRectangle(): CommonRectangle = CommonRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

public fun Shape.toCommonPath(): CommonPath {
  val segments = mutableListOf<PathSegment>()

  val pi = this.getPathIterator(null)

  while (!pi.isDone) {
    val coordinates = DoubleArray(6)
    val pathSegmentType = pi.currentSegment(coordinates)

    val points = coordinates
      .asList()
      .chunked(2)
      .map { (x, y) -> Point(x, y) }

    val segment = when (pathSegmentType) {
      PathIterator.SEG_MOVETO -> PathSegment.MoveTo(points[0])
      PathIterator.SEG_LINETO -> PathSegment.LineTo(points[0])
      PathIterator.SEG_QUADTO -> PathSegment.QuadTo(points[0], points[1])
      PathIterator.SEG_CUBICTO -> PathSegment.CubicTo(points[0], points[1], points[2])
      PathIterator.SEG_CLOSE -> PathSegment.Close

      else -> throw IllegalArgumentException("Unsupported path segment type: $pathSegmentType")
    }

    segments.add(segment)
    pi.next()
  }

  val windingType = when (val windingRule = pi.windingRule) {
    PathIterator.WIND_EVEN_ODD -> CommonPath.WindingType.EVEN_ODD
    PathIterator.WIND_NON_ZERO -> CommonPath.WindingType.NON_ZERO

    else -> throw IllegalArgumentException("Unsupported winding rule: $windingRule")
  }

  return CommonPath(segments, windingType)
}

/* Converts an ARGB number to a color. */
public fun Number.toColor(): Color = Color(this.toInt(), true)

public fun StrokeData.toStroke(): Stroke {
  when (this) {
    is StrokeData.Basic -> {
      val cap = when (endCap) {
        StrokeData.Basic.CapType.BUTT -> BasicStroke.CAP_BUTT
        StrokeData.Basic.CapType.SQUARE -> BasicStroke.CAP_SQUARE
        StrokeData.Basic.CapType.ROUND -> BasicStroke.CAP_ROUND
      }

      val join = when (lineJoin) {
        StrokeData.Basic.JoinType.MITER -> BasicStroke.JOIN_MITER
        StrokeData.Basic.JoinType.BEVEL -> BasicStroke.JOIN_BEVEL
        StrokeData.Basic.JoinType.ROUND -> BasicStroke.JOIN_ROUND
      }

      return BasicStroke(
        lineWidth,
        cap,
        join,
        miterLimit,
        dashArray?.toFloatArray(),
        dashPhase
      )
    }
  }
}

public fun BasicStroke.toBasicStrokeData(): StrokeData.Basic {
  val cap = when (val cap = endCap) {
    BasicStroke.CAP_BUTT -> StrokeData.Basic.CapType.BUTT
    BasicStroke.CAP_SQUARE -> StrokeData.Basic.CapType.SQUARE
    BasicStroke.CAP_ROUND -> StrokeData.Basic.CapType.ROUND

    else -> throw IllegalArgumentException("Bad s.endCap: $cap")
  }

  val join = when (val join = lineJoin) {
    BasicStroke.JOIN_MITER -> StrokeData.Basic.JoinType.MITER
    BasicStroke.JOIN_BEVEL -> StrokeData.Basic.JoinType.BEVEL
    BasicStroke.JOIN_ROUND -> StrokeData.Basic.JoinType.ROUND

    else -> throw IllegalArgumentException("Bad s.lineJoin: $join")
  }

  return StrokeData.Basic(
    lineWidth = lineWidth,
    lineJoin = join,
    endCap = cap,
    miterLimit = miterLimit,
    dashPhase = dashPhase,
    dashArray = dashArray?.toList()
  )
}

public fun Int.toCursorType(): CursorType = when (this) {
  DEFAULT_CURSOR -> CursorType.DEFAULT
  CROSSHAIR_CURSOR -> CursorType.CROSSHAIR
  TEXT_CURSOR -> CursorType.TEXT
  WAIT_CURSOR -> CursorType.WAIT
  SW_RESIZE_CURSOR -> CursorType.SW_RESIZE
  SE_RESIZE_CURSOR -> CursorType.SE_RESIZE
  NW_RESIZE_CURSOR -> CursorType.NW_RESIZE
  NE_RESIZE_CURSOR -> CursorType.NE_RESIZE
  N_RESIZE_CURSOR -> CursorType.N_RESIZE
  S_RESIZE_CURSOR -> CursorType.S_RESIZE
  W_RESIZE_CURSOR -> CursorType.W_RESIZE
  E_RESIZE_CURSOR -> CursorType.E_RESIZE
  HAND_CURSOR -> CursorType.HAND
  MOVE_CURSOR -> CursorType.MOVE
  CUSTOM_CURSOR -> CursorType.CUSTOM

  else -> {
    logger.error { "Int.toCursorType(): Bad cursor id: $this. Returning default." }

    CursorType.DEFAULT
  }
}

public fun Paint.toPaintValue(): PaintValue = when (this) {
  is Color -> PaintValue.Color(rgb)

  is GradientPaint -> PaintValue.Gradient(
    p1 = point1.toPoint(),
    p2 = point2.toPoint(),
    fractions = listOf(0.0, 1.0),
    argbs = listOf(color1.rgb, color2.rgb),
  )

  is LinearGradientPaint -> PaintValue.Gradient(
    p1 = startPoint.toPoint(),
    p2 = endPoint.toPoint(),
    fractions = fractions.map(Float::toDouble),
    argbs = colors.map { it.rgb },
  )

  is MultipleGradientPaint -> PaintValue.Unknown("MultipleGradientPaint, maybe split to Linear and Radial")

  is TexturePaint -> PaintValue.Unknown("TexturePaint")

  else -> PaintValue.Unknown(this::class.qualifiedName.toString())
}

public fun createSetClipEvent(identitySpaceClip: Shape?): ServerWindowStateEvent = ServerSetClipEvent(
  with(identitySpaceClip) {
    when (this) {
      null -> null

      is Rectangle2D -> CommonRectangle(x, y, width, height)

      else -> this.toCommonPath()
    }
  }
)

public fun Stroke.toSetStrokeEvent(): ServerWindowStateEvent = when (this) {
  is BasicStroke -> ServerSetStrokeEvent(this.toBasicStrokeData())

  else -> ServerSetUnknownStrokeEvent(this::class.qualifiedName.toString())
}

private fun AlphaComposite.toCommonAlphaComposite(): CommonAlphaComposite {
  val acRule = when (rule) {
    AlphaComposite.SRC_OVER -> AlphaCompositeRule.SRC_OVER
    AlphaComposite.DST_OVER -> AlphaCompositeRule.DST_OVER
    AlphaComposite.SRC_IN -> AlphaCompositeRule.SRC_IN
    AlphaComposite.CLEAR -> AlphaCompositeRule.CLEAR
    AlphaComposite.SRC -> AlphaCompositeRule.SRC
    AlphaComposite.DST -> AlphaCompositeRule.DST
    AlphaComposite.DST_IN -> AlphaCompositeRule.DST_IN
    AlphaComposite.SRC_OUT -> AlphaCompositeRule.SRC_OUT
    AlphaComposite.DST_OUT -> AlphaCompositeRule.DST_OUT
    AlphaComposite.SRC_ATOP -> AlphaCompositeRule.SRC_ATOP
    AlphaComposite.DST_ATOP -> AlphaCompositeRule.DST_ATOP
    AlphaComposite.XOR -> AlphaCompositeRule.XOR

    else -> {
      logger.error { "AlphaComposite.toCommonAlphaComposite: Bad alpha composite rule: $rule. Returning SRC_OVER." }

      AlphaCompositeRule.SRC_OVER
    }
  }

  return CommonAlphaComposite(acRule, alpha)
}

public fun Composite.toCommonComposite(): CommonComposite = when (this) {
  is AlphaComposite -> this.toCommonAlphaComposite()

  else -> UnknownComposite("Unknown composite class: ${this::class.java.canonicalName}")
}

public fun roundToInfinity(x: Double): Double = when {
  x.isNaN() || x.isInfinite() -> x
  x > 0 -> ceil(x)
  else -> floor(x)
}

private val logger = Logger("ConvertKt")
