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
package org.jetbrains.projector.client.web.protocol

import org.jetbrains.projector.common.protocol.data.*
import org.jetbrains.projector.common.protocol.handshake.ProtocolType
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.common.protocol.toClient.data.idea.CaretInfo
import kotlin.js.Json
import kotlin.math.roundToLong

object ManualJsonToClientMessageDecoder : ToClientMessageDecoder {

  override val protocolType = ProtocolType.KOTLINX_JSON

  @OptIn(ExperimentalStdlibApi::class)
  override fun decode(message: ByteArray): ToClientMessageType {
    val string = message.decodeToString()

    val jsonArray = JSON.parse<Array<Array<Any>>>(string)
    return jsonArray.map { it.toEvent() }
  }

  private fun Array<Any>.toEvent(): ServerEvent {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ServerImageDataReplyEvent(content["a"].unsafeCast<Array<Any>>().toImageId(),
                                       content["b"].unsafeCast<Array<Any>>().toImageData())
      "b" -> ServerPingReplyEvent(content["a"] as Int, content["b"] as Int)
      "c" -> ServerClipboardEvent(content["a"] as String)
      "d" -> ServerWindowSetChangedEvent(content["a"].unsafeCast<Array<Json>>().map { it.toWindowData() })
      "e" -> ServerDrawCommandsEvent(
        content["a"].unsafeCast<Array<Any>>().toTarget(),
        content["b"].unsafeCast<Array<Array<Any>>>().map { it.toWindowEvent() }
      )
      "f" -> ServerCaretInfoChangedEvent(content["a"].unsafeCast<Array<Any>>().toCaretInfoChange())
      "g" -> ServerMarkdownEvent.ServerMarkdownShowEvent(content["a"] as Int, content["b"] as Boolean)
      "h" -> ServerMarkdownEvent.ServerMarkdownResizeEvent(content["a"] as Int, content["b"].unsafeCast<Json>().toCommonIntSize())
      "i" -> ServerMarkdownEvent.ServerMarkdownMoveEvent(content["a"] as Int, content["b"].unsafeCast<Json>().toPoint())
      "j" -> ServerMarkdownEvent.ServerMarkdownDisposeEvent(content["a"] as Int)
      "k" -> ServerMarkdownEvent.ServerMarkdownPlaceToWindowEvent(content["a"] as Int, content["b"] as Int)
      "l" -> ServerMarkdownEvent.ServerMarkdownSetHtmlEvent(content["a"] as Int, content["b"] as String)
      "m" -> ServerMarkdownEvent.ServerMarkdownSetCssEvent(content["a"] as Int, content["b"] as String)
      "n" -> ServerMarkdownEvent.ServerMarkdownScrollEvent(content["a"] as Int, content["b"] as Int)
      "o" -> ServerBrowseUriEvent(content["a"] as String)
      "p" -> ServerWindowColorsEvent(content["a"].unsafeCast<Json>().toColorsStorage())
      else -> throw IllegalArgumentException("Unsupported event type: ${JSON.stringify(this)}")
    }
  }

  private fun Json.toColorsStorage(): ServerWindowColorsEvent.ColorsStorage {
    return ServerWindowColorsEvent.ColorsStorage(
      this["a"].unsafeCast<Json>().toColor(),
      this["b"].unsafeCast<Json>().toColor(),
      this["c"].unsafeCast<Json>().toColor(),
      this["d"].unsafeCast<Json>().toColor(),
      this["e"].unsafeCast<Json>().toColor(),
      this["f"].unsafeCast<Json>().toColor()
    )
  }

  private fun Array<Any>.toCaretInfoChange(): ServerCaretInfoChangedEvent.CaretInfoChange {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ServerCaretInfoChangedEvent.CaretInfoChange.NoCarets
      "b" -> ServerCaretInfoChangedEvent.CaretInfoChange.Carets(
        content["a"].unsafeCast<Array<Json>>().map { it.toCaretInfo() },
        content["b"] as Short?,
        content["c"] as Int,
        content["d"] as Int,
        content["e"].unsafeCast<Json>().toCommonRectangle(),
        content["f"] as Int,
        content["g"] as Int,
        content["h"] as Int,
        content["i"] as Int,
        content["j"] as Int,
      )
      else -> throw IllegalArgumentException("Unsupported caret info type: ${JSON.stringify(this)}")
    }
  }

  private fun Json.toCaretInfo(): CaretInfo {
    return CaretInfo(this["a"].unsafeCast<Json>().toPoint())
  }

  private fun Array<Any>.toTarget(): ServerDrawCommandsEvent.Target {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ServerDrawCommandsEvent.Target.Onscreen(content["a"] as Int)
      "b" -> ServerDrawCommandsEvent.Target.Offscreen((content["a"] as Double).roundToLong(), content["b"] as Int,
                                                      content["c"] as Int)  // todo: is it a correct way of decoding Long?
      else -> throw IllegalArgumentException("Unsupported target type: ${JSON.stringify(this)}")
    }
  }

  private fun Json.toCommonIntSize(): CommonIntSize {
    return CommonIntSize(this["a"] as Int, this["b"] as Int)
  }

  private fun Json.toWindowData(): WindowData {
    return WindowData(
      this["a"] as Int,
      this["b"] as String?,
      this["c"].unsafeCast<Array<Array<Any>>?>()?.map { it.toImageId() },
      this["d"] as Boolean,
      this["e"] as Int,
      this["f"].unsafeCast<Json>().toCommonRectangle(),
      (this["g"] as String?)?.toCursorType(),
      this["h"] as Boolean,
      this["i"] as Boolean,
      this["j"] as Boolean,
      (this["k"] as String).toWindowType(),
      this["l"] as Int?,
      (this["m"] as String).toWindowClass(),
      this["n"] as Boolean,
      this["o"] as Boolean,
      this["p"] as Int?
    )
  }

  private fun String.toWindowType(): WindowType {
    return when (this) {
      "a" -> WindowType.WINDOW
      "b" -> WindowType.POPUP
      "c" -> WindowType.IDEA_WINDOW
      else -> throw IllegalArgumentException("Unsupported window type: $this")
    }
  }

  private fun String.toWindowClass(): WindowClass {
    return when (this) {
      "a" -> WindowClass.OTHER
      "b" -> WindowClass.FRAME
      "c" -> WindowClass.DIALOG
      "d" -> WindowClass.JWINDOW
      else -> throw IllegalArgumentException("Unsupported window class: $this")
    }
  }

  private val cursorTypes = mapOf(
    "a" to CursorType.DEFAULT,
    "b" to CursorType.CROSSHAIR,
    "c" to CursorType.TEXT,
    "d" to CursorType.WAIT,
    "e" to CursorType.SW_RESIZE,
    "f" to CursorType.SE_RESIZE,
    "g" to CursorType.NW_RESIZE,
    "h" to CursorType.NE_RESIZE,
    "i" to CursorType.N_RESIZE,
    "j" to CursorType.S_RESIZE,
    "k" to CursorType.W_RESIZE,
    "l" to CursorType.E_RESIZE,
    "m" to CursorType.HAND,
    "n" to CursorType.MOVE,
    "o" to CursorType.CUSTOM,
  )

  private fun String.toCursorType(): CursorType {
    return cursorTypes[this] ?: throw IllegalArgumentException("Unsupported cursor type: $this")
  }

  private fun Json.toCommonRectangle(): CommonRectangle {
    return CommonRectangle(this["a"] as Double, this["b"] as Double, this["c"] as Double, this["d"] as Double)
  }

  private fun Array<Any>.toWindowEvent(): ServerWindowEvent {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ServerPaintArcEvent(
        (content["a"] as String).toPaintType(),
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int,
        content["f"] as Int,
        content["g"] as Int
      )

      "b" -> ServerPaintOvalEvent(
        (content["a"] as String).toPaintType(),
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int
      )

      "c" -> ServerPaintRoundRectEvent(
        (content["a"] as String).toPaintType(),
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int,
        content["f"] as Int,
        content["g"] as Int
      )

      "d" -> ServerPaintRectEvent(
        (content["a"] as String).toPaintType(),
        content["b"] as Double,
        content["c"] as Double,
        content["d"] as Double,
        content["e"] as Double
      )

      "e" -> ServerDrawLineEvent(
        content["a"] as Int,
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int
      )

      "f" -> ServerCopyAreaEvent(
        content["a"] as Int,
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int,
        content["f"] as Int
      )

      "g" -> ServerSetFontEvent(content["a"] as Short, content["b"] as Int, content["c"] as Boolean)

      "h" -> ServerSetClipEvent(content["a"].unsafeCast<Array<Any>?>()?.toCommonShape())

      "i" -> ServerSetStrokeEvent(content["a"].unsafeCast<Array<Any>>().toStrokeData())

      "j" -> ServerDrawRenderedImageEvent

      "k" -> ServerDrawRenderableImageEvent

      "l" -> ServerDrawImageEvent(content["a"].unsafeCast<Array<Any>>().toImageId(),
                                  content["b"].unsafeCast<Array<Any>>().toImageEventInfo())

      "m" -> ServerDrawStringEvent(content["a"] as String, content["b"] as Double, content["c"] as Double, content["d"] as Double)

      "n" -> ServerPaintPolygonEvent((content["a"] as String).toPaintType(), content["b"].unsafeCast<Array<Json>>().map { it.toPoint() })

      "o" -> ServerDrawPolylineEvent(content["a"].unsafeCast<Array<Json>>().map { it.toPoint() })

      "p" -> ServerSetTransformEvent(content["a"].unsafeCast<Array<Double>>().toList())

      "q" -> ServerPaintPathEvent((content["a"] as String).toPaintType(), content["b"].unsafeCast<Json>().toCommonPath())

      "r" -> ServerSetCompositeEvent(content["a"].unsafeCast<Array<Any>>().toCommonComposite())

      "s" -> ServerSetPaintEvent(content["a"].unsafeCast<Array<Any>>().toPaintValue())

      "t" -> ServerSetUnknownStrokeEvent(content["a"] as String)

      else -> throw IllegalArgumentException("Unsupported event type: ${JSON.stringify(this)}")
    }
  }

  private val alphaCompositeRuleMap = mapOf(
    "a" to AlphaCompositeRule.SRC_OVER,
    "b" to AlphaCompositeRule.DST_OVER,
    "c" to AlphaCompositeRule.SRC_IN,
    "d" to AlphaCompositeRule.CLEAR,
    "e" to AlphaCompositeRule.SRC,
    "f" to AlphaCompositeRule.DST,
    "g" to AlphaCompositeRule.DST_IN,
    "h" to AlphaCompositeRule.SRC_OUT,
    "i" to AlphaCompositeRule.DST_OUT,
    "j" to AlphaCompositeRule.SRC_ATOP,
    "k" to AlphaCompositeRule.DST_ATOP,
    "l" to AlphaCompositeRule.XOR
  )

  private fun Array<Any>.toCommonComposite(): CommonComposite {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> CommonAlphaComposite(
        alphaCompositeRuleMap[content["a"] as String] ?: throw IllegalArgumentException("Unsupported rule: ${content["a"]}"),
        content["b"] as Float
      )
      "b" -> UnknownComposite(content["a"] as String)
      else -> throw IllegalArgumentException("Unsupported common composite type: ${JSON.stringify(this)}")
    }
  }

  private fun String.toPaintType(): PaintType {
    return when (this) {
      "a" -> PaintType.DRAW
      "b" -> PaintType.FILL
      else -> throw IllegalArgumentException("Unsupported paint type: $this")
    }
  }

  private fun Json.toColor(): PaintValue.Color {
    return PaintValue.Color(this["a"] as Int)
  }

  private fun Array<Any>.toPaintValue(): PaintValue {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> content.toColor()
      "b" -> PaintValue.Gradient(
        content["a"].unsafeCast<Json>().toPoint(), content["b"].unsafeCast<Json>().toPoint(),
        content["c"] as Int, content["d"] as Int
      )
      "c" -> PaintValue.Unknown(content["a"] as String)
      else -> throw IllegalArgumentException("Unsupported paint value type: ${JSON.stringify(this)}")
    }
  }

  private fun Array<Any>.toImageEventInfo(): ImageEventInfo {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ImageEventInfo.Xy(content["a"] as Int, content["b"] as Int, content["c"] as Int?)
      "b" -> ImageEventInfo.XyWh(
        content["a"] as Int,
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int?
      )
      "c" -> ImageEventInfo.Ds(
        content["a"] as Int,
        content["b"] as Int,
        content["c"] as Int,
        content["d"] as Int,
        content["e"] as Int,
        content["f"] as Int,
        content["g"] as Int,
        content["h"] as Int,
        content["i"] as Int?
      )
      "d" -> ImageEventInfo.Transformed(content["a"].unsafeCast<Array<Double>>().toList())
      else -> throw IllegalArgumentException("Unsupported image info type: ${JSON.stringify(this)}")
    }
  }

  private fun Array<Any>.toStrokeData(): StrokeData {
    val thisType = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (thisType) {
      "a" -> StrokeData.Basic(
        content["a"] as Float,
        when (val type = content["b"] as String) {
          "a" -> StrokeData.Basic.JoinType.MITER
          "b" -> StrokeData.Basic.JoinType.ROUND
          "c" -> StrokeData.Basic.JoinType.BEVEL
          else -> throw IllegalArgumentException("Unsupported join type: $type")
        },
        when (val type = content["c"] as String) {
          "a" -> StrokeData.Basic.CapType.BUTT
          "b" -> StrokeData.Basic.CapType.ROUND
          "c" -> StrokeData.Basic.CapType.SQUARE
          else -> throw IllegalArgumentException("Unsupported cap type: $type")
        },
        content["d"] as Float,
        content["e"] as Float,
        content["f"].unsafeCast<Array<Float>?>()?.toList()
      )

      else -> throw IllegalArgumentException("Unsupported stroke type: ${JSON.stringify(this)}")
    }
  }

  private fun Array<Any>.toImageId(): ImageId {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ImageId.BufferedImageId(content["a"] as Int, content["b"] as Int)
      "b" -> ImageId.PVolatileImageId((content["a"] as Double).roundToLong())  // todo: is it a correct way?
      "c" -> ImageId.Unknown(content["a"] as String)
      else -> throw IllegalArgumentException("Invalid image id type: ${JSON.stringify(this)}")
    }
  }

  private fun Array<Any>.toImageData(): ImageData {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> ImageData.PngBase64(content["a"] as String)
      "b" -> ImageData.Empty
      else -> throw IllegalArgumentException("Invalid image data type: $${JSON.stringify(this)}")
    }
  }

  private fun Array<Any>.toCommonShape(): CommonShape {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> content.toCommonRectangle()
      "b" -> content.toCommonPath()
      else -> throw IllegalArgumentException("Unsupported common shape: ${JSON.stringify(this)}")
    }
  }

  private fun Json.toCommonPath(): CommonPath {
    val segments = this["a"].unsafeCast<Array<Array<Any>>>().map { it.toPathSegment() }
    val winding = when (val type = this["b"] as String) {
      "a" -> CommonPath.WindingType.EVEN_ODD
      "b" -> CommonPath.WindingType.NON_ZERO
      else -> throw IllegalArgumentException("Invalid winding type: $type")
    }

    return CommonPath(segments, winding)
  }

  private fun Array<Any>.toPathSegment(): PathSegment {
    val type = this[0] as String
    val content = this[1].unsafeCast<Json>()

    return when (type) {
      "a" -> PathSegment.MoveTo(content["a"].unsafeCast<Json>().toPoint())
      "b" -> PathSegment.LineTo(content["a"].unsafeCast<Json>().toPoint())
      "c" -> PathSegment.QuadTo(content["a"].unsafeCast<Json>().toPoint(), content["b"].unsafeCast<Json>().toPoint())
      "d" -> PathSegment.CubicTo(
        content["a"].unsafeCast<Json>().toPoint(),
        content["b"].unsafeCast<Json>().toPoint(),
        content["c"].unsafeCast<Json>().toPoint()
      )
      "e" -> PathSegment.Close
      else -> throw IllegalArgumentException("Unsupported path segment: ${JSON.stringify(this)}")
    }
  }

  private fun Json.toPoint(): Point {
    return Point(this["a"] as Double, this["b"] as Double)
  }
}
