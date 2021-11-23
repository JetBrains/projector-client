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
package org.jetbrains.projector.common.protocol.toClient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.data.CursorType
import org.jetbrains.projector.common.protocol.data.ImageId

@Serializable
enum class WindowType {
  @SerialName("a")
  WINDOW,

  @SerialName("b")
  POPUP,

  @SerialName("c")
  IDEA_WINDOW
}

// unlike WindowType, this is intended to always be the lowest inheritor of Window that's useful for clientside purposes
@Serializable
enum class WindowClass {
  @SerialName("a")
  OTHER,

  @SerialName("b")
  FRAME,

  @SerialName("c")
  DIALOG,

  @SerialName("d")
  JWINDOW
}

@Serializable
data class WindowData(
  @SerialName("a")
  val id: Int,
  @SerialName("b")
  val title: String? = null,
  @SerialName("c")
  val icons: List<ImageId>? = null,
  @SerialName("d")
  val isShowing: Boolean,
  /** Big value means front. */
  @SerialName("e")
  val zOrder: Int,
  @SerialName("f")
  val bounds: CommonRectangle,
  /** Null means no change. */
  @SerialName("g")
  val cursorType: CursorType? = null,
  @SerialName("h")
  val resizable: Boolean,
  @SerialName("i")
  val modal: Boolean,
  @SerialName("j")
  val undecorated: Boolean,
  @SerialName("k")
  val windowType: WindowType,
  /**
   * If the window has a header on the host, its sizes are included in the window bounds.
   * The client header is drawn above the window, outside its bounds. At the same time,
   * the coordinates of the contents of the window come taking into account the size
   * of the header. As a result, on client an empty space is obtained between header
   * and the contents of the window. To get rid of this, we transfer the height of the system
   * window header and if it > 0, we draw the heading not over the window but inside
   * the window's bounds, filling in the empty space.
   * If the window has not a header on the host, headerHeight == null.
   */
  @SerialName("l")
  val headerHeight: Int? = null,
  @SerialName("m")
  val windowClass: WindowClass = WindowClass.OTHER,
  @SerialName("n")
  val autoRequestFocus: Boolean = false,
  @SerialName("o")
  val isAlwaysOnTop: Boolean = false,
  @SerialName("p")
  val parentId: Int? = null,
)
