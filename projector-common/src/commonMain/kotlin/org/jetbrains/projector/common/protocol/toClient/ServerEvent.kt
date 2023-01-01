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
package org.jetbrains.projector.common.protocol.toClient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.projector.common.protocol.data.*
import org.jetbrains.projector.common.protocol.toClient.data.idea.CaretInfo

@Serializable
sealed class ServerEvent

@Serializable
@SerialName("a")
data class ServerImageDataReplyEvent(
  @SerialName("a")
  val imageId: ImageId,
  @SerialName("b")
  val imageData: ImageData,
) : ServerEvent()

@Serializable
@SerialName("b")
data class ServerPingReplyEvent(
  /** From connection opening. */
  @SerialName("a")
  val clientTimeStamp: Int,
  /** From connection opening. */
  @SerialName("b")
  val serverReadEventTimeStamp: Int,
) : ServerEvent()

@Serializable
@SerialName("c")
data class ServerClipboardEvent(
  @SerialName("a")
  val stringContent: String,  // TODO: support more types
) : ServerEvent()

@Serializable
@SerialName("d")
data class ServerWindowSetChangedEvent(
  @SerialName("a")
  val windowDataList: List<WindowData> = emptyList(),  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806
) : ServerEvent()

@Serializable
@SerialName("e")
data class ServerDrawCommandsEvent(
  @SerialName("a")
  val target: Target,
  @SerialName("b")
  val drawEvents: List<ServerWindowEvent> = emptyList(),  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806
) : ServerEvent() {

  @Serializable
  sealed class Target {

    @Serializable
    @SerialName("a")
    data class Onscreen(
      @SerialName("a")
      val windowId: Int,
    ) : Target()

    @Serializable
    @SerialName("b")
    data class Offscreen(
      @SerialName("a")
      val pVolatileImageId: Long,
      @SerialName("b")
      val width: Int,
      @SerialName("c")
      val height: Int,
    ) : Target()
  }
}

@Serializable
@SerialName("f")
data class ServerCaretInfoChangedEvent(
  @SerialName("a")
  val data: CaretInfoChange,
) : ServerEvent() {

  @Serializable
  sealed class CaretInfoChange {

    @Serializable
    @SerialName("a")
    object NoCarets : CaretInfoChange()

    @Serializable
    @SerialName("b")
    data class Carets(
      @SerialName("a")
      val caretInfoList: List<CaretInfo> = emptyList(),  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806
      @SerialName("b")
      val fontId: Short? = null,
      @SerialName("c")
      val fontSize: Int,
      @SerialName("d")
      val editorWindowId: Int,
      @SerialName("e")
      val editorMetrics: CommonRectangle,
      @SerialName("f")
      val lineHeight: Int,
      @SerialName("g")
      val lineAscent: Int,
      @SerialName("h")
      val verticalScrollBarWidth: Int,
      @SerialName("i")
      val textColor: Int,
      @SerialName("j")
      val backgroundColor: Int,
    ) : CaretInfoChange()
  }
}

@Serializable
sealed class ServerMarkdownEvent : ServerEvent() {

  @Serializable
  @SerialName("g")
  data class ServerMarkdownShowEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val show: Boolean,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("h")
  data class ServerMarkdownResizeEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val size: CommonIntSize,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("i")
  data class ServerMarkdownMoveEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val point: Point,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("j")
  data class ServerMarkdownDisposeEvent(
    @SerialName("a")
    val panelId: Int,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("k")
  data class ServerMarkdownPlaceToWindowEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val windowId: Int,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("l")
  data class ServerMarkdownSetHtmlEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val html: String,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("m")
  data class ServerMarkdownSetCssEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val css: String,
  ) : ServerMarkdownEvent()

  @Serializable
  @SerialName("n")
  data class ServerMarkdownScrollEvent(
    @SerialName("a")
    val panelId: Int,
    @SerialName("b")
    val scrollOffset: Int,
  ) : ServerMarkdownEvent()
}

@Serializable
@SerialName("o")
data class ServerBrowseUriEvent(
  @SerialName("a")
  val link: String,
) : ServerEvent()

@Serializable
@SerialName("p")
data class ServerWindowColorsEvent(
  @SerialName("a")
  val colors: ColorsStorage,
) : ServerEvent() {

  @Serializable
  data class ColorsStorage(
    @SerialName("a")
    val windowHeaderActiveBackground: PaintValue.Color,
    @SerialName("b")
    val windowHeaderInactiveBackground: PaintValue.Color,
    @SerialName("c")
    val windowActiveBorder: PaintValue.Color,
    @SerialName("d")
    val windowInactiveBorder: PaintValue.Color,
    @SerialName("e")
    val windowHeaderActiveText: PaintValue.Color,
    @SerialName("f")
    val windowHeaderInactiveText: PaintValue.Color,
  )
}
