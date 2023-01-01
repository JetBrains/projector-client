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
sealed class CommonComposite

@Serializable
@SerialName("a")
data class CommonAlphaComposite(
  @SerialName("a")
  val rule: AlphaCompositeRule,
  @SerialName("b")
  val alpha: Float,
) : CommonComposite()

@Serializable
enum class AlphaCompositeRule {
  @SerialName("a")
  SRC_OVER,

  @SerialName("b")
  DST_OVER,

  @SerialName("c")
  SRC_IN,

  @SerialName("d")
  CLEAR,

  @SerialName("e")
  SRC,

  @SerialName("f")
  DST,

  @SerialName("g")
  DST_IN,

  @SerialName("h")
  SRC_OUT,

  @SerialName("i")
  DST_OUT,

  @SerialName("j")
  SRC_ATOP,

  @SerialName("k")
  DST_ATOP,

  @SerialName("l")
  XOR,
}

@Serializable
@SerialName("b")
data class UnknownComposite(
  @SerialName("a")
  val message: String,
) : CommonComposite()
