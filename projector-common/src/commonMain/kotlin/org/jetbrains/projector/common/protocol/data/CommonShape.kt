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
package org.jetbrains.projector.common.protocol.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CommonShape

@Serializable
@SerialName("a")
data class CommonRectangle(
  @SerialName("a")
  val x: Double,
  @SerialName("b")
  val y: Double,
  @SerialName("c")
  val width: Double,
  @SerialName("d")
  val height: Double,
) : CommonShape() {

  fun contains(x: Int, y: Int) = this.x <= x && x < this.x + this.width && this.y <= y && y < this.y + this.height

  fun createExtended(extend: Double) = CommonRectangle(x - extend, y - extend, width + extend * 2, height + extend * 2)
}

@Serializable
@SerialName("b")
data class CommonPath(
  @SerialName("a")
  val segments: List<PathSegment> = emptyList(),  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806
  @SerialName("b")
  val winding: WindingType,
) : CommonShape() {

  @Serializable
  enum class WindingType {

    @SerialName("a")
    EVEN_ODD,

    @SerialName("b")
    NON_ZERO,
  }
}
