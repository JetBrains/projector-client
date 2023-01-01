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
package org.jetbrains.projector.client.common.misc

actual object ParamsProvider {
  actual val CLIPPING_BORDERS: Boolean
    get() = JvmParamsProviderImpl.CLIPPING_BORDERS
  actual val SHOW_TEXT_WIDTH: Boolean
    get() = JvmParamsProviderImpl.SHOW_TEXT_WIDTH
  actual val REPAINT_AREA: RepaintAreaSetting
    get() = JvmParamsProviderImpl.REPAINT_AREA
  actual val LOG_UNSUPPORTED_EVENTS: Boolean
    get() = JvmParamsProviderImpl.LOG_UNSUPPORTED_EVENTS
  actual val IMAGE_TTL: Double
    get() = JvmParamsProviderImpl.IMAGE_TTL
  actual val IMAGE_CACHE_SIZE_CHARS: Int
    get() = JvmParamsProviderImpl.IMAGE_CACHE_SIZE_CHARS
}

object JvmParamsProviderImpl {
  var CLIPPING_BORDERS: Boolean = false
  var SHOW_TEXT_WIDTH: Boolean = false
  var REPAINT_AREA: RepaintAreaSetting = RepaintAreaSetting.Disabled
  var LOG_UNSUPPORTED_EVENTS: Boolean = true
  var IMAGE_TTL: Double = 60_000.0
  var IMAGE_CACHE_SIZE_CHARS: Int = 5_000_000
}
