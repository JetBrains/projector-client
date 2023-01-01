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
package org.jetbrains.projector.client.web.misc

import kotlinx.browser.window
import org.jetbrains.projector.common.protocol.data.UserKeymap
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

val currentTimeStamp: Double get() = window.performance.now()

fun ArrayBuffer.asByteArray(): ByteArray = Int8Array(this).unsafeCast<ByteArray>()  // todo: remove after KT-30098 is resolved

val nativeKeymap: UserKeymap
  get() {
    val lowerCasedPlatformName = window.navigator.platform.lowercase()

    return when {
      "win" in lowerCasedPlatformName -> UserKeymap.WINDOWS
      "mac" in lowerCasedPlatformName -> UserKeymap.MAC
      else -> UserKeymap.LINUX
    }
  }

fun Boolean.toDisplayType(): String = when (this) {
  true -> "block"
  false -> "none"
}

@Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")
inline fun jsBoolean(expression: Any?): Boolean = js("Boolean(expression)") as Boolean

@Suppress("NOTHING_TO_INLINE")
inline fun isDefined(obj: Any?): Boolean = jsTypeOf(obj) != "undefined"
