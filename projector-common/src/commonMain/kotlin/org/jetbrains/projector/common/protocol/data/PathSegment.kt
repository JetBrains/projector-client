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
sealed class PathSegment {

  @Serializable
  @SerialName("a")
  data class MoveTo(
    @SerialName("a")
    val point: Point,
  ) : PathSegment()

  @Serializable
  @SerialName("b")
  data class LineTo(
    @SerialName("a")
    val point: Point,
  ) : PathSegment()

  @Serializable
  @SerialName("c")
  data class QuadTo(
    @SerialName("a")
    val point1: Point,
    @SerialName("b")
    val point2: Point,
  ) : PathSegment()

  @Serializable
  @SerialName("d")
  data class CubicTo(
    @SerialName("a")
    val point1: Point,
    @SerialName("b")
    val point2: Point,
    @SerialName("c")
    val point3: Point,
  ) : PathSegment()

  @Serializable
  @SerialName("e")
  object Close : PathSegment()
}
