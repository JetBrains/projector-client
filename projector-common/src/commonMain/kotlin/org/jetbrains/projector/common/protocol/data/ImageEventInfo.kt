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
sealed class ImageEventInfo {

  @Serializable
  @SerialName("a")
  data class Xy(
    @SerialName("a")
    val x: Int,
    @SerialName("b")
    val y: Int,
    @SerialName("c")
    val argbBackgroundColor: Int? = null,
  ) : ImageEventInfo()

  @Serializable
  @SerialName("b")
  data class XyWh(
    @SerialName("a")
    val x: Int,
    @SerialName("b")
    val y: Int,
    @SerialName("c")
    val width: Int,
    @SerialName("d")
    val height: Int,
    @SerialName("e")
    val argbBackgroundColor: Int? = null,
  ) : ImageEventInfo()

  @Serializable
  @SerialName("c")
  data class Ds(
    @SerialName("a")
    val dx1: Int,
    @SerialName("b")
    val dy1: Int,
    @SerialName("c")
    val dx2: Int,
    @SerialName("d")
    val dy2: Int,
    @SerialName("e")
    val sx1: Int,
    @SerialName("f")
    val sy1: Int,
    @SerialName("g")
    val sx2: Int,
    @SerialName("h")
    val sy2: Int,
    @SerialName("i")
    val argbBackgroundColor: Int? = null,
  ) : ImageEventInfo()

  @Serializable
  @SerialName("d")
  data class Transformed(
    @SerialName("a")
    val tx: List<Double> = emptyList(),  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806
  ) : ImageEventInfo()
}
