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
sealed class StrokeData {

  @Serializable
  @SerialName("a")
  data class Basic(
    @SerialName("a")
    val lineWidth: Float,
    @SerialName("b")
    val lineJoin: JoinType,
    @SerialName("c")
    val endCap: CapType,
    @SerialName("d")
    val miterLimit: Float,
    @SerialName("e")
    val dashPhase: Float,
    @SerialName("f")
    val dashArray: List<Float>? = null,
  ) : StrokeData() {

    @Serializable
    enum class JoinType {

      @SerialName("a")
      MITER,

      @SerialName("b")
      ROUND,

      @SerialName("c")
      BEVEL,
    }

    @Serializable
    enum class CapType {

      @SerialName("a")
      BUTT,

      @SerialName("b")
      ROUND,

      @SerialName("c")
      SQUARE,
    }
  }
}
