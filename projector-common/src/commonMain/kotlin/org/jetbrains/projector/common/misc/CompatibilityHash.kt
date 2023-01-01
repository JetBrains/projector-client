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
package org.jetbrains.projector.common.misc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames

private val List<Int>.compatibilityHash: Int get() = reduce { acc, c -> 31 * acc + c }

@Suppress("DEPRECATION")  // https://youtrack.jetbrains.com/issue/KT-47644
private val String.compatibilityHash: Int get() = map(Char::toInt).compatibilityHash

private val Boolean.compatibilityHash: Int get() = if (this) 1 else 0

@OptIn(ExperimentalSerializationApi::class)
val SerialDescriptor.compatibilityHash: Int
  get() {
    val subDescriptorsHashes = when (kind) {
      is SerialKind.ENUM -> emptyList()

      else -> elementDescriptors.map(SerialDescriptor::compatibilityHash)
    }

    return listOf(
      serialName.compatibilityHash,
      kind.toString().compatibilityHash,
      isNullable.compatibilityHash,
      elementsCount,
      *elementNames.map(String::compatibilityHash).toTypedArray(),
      *subDescriptorsHashes.toTypedArray()
    )
      .compatibilityHash
  }
