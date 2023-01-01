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
package org.jetbrains.projector.server.core.util

import org.jetbrains.projector.util.logging.Logger
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

// todo: Add test with mocking of logger
public class SizeAware<T : Collection<*>>(
  initialValue: T,
  private val bigCollectionSize: Int?,
  private val logger: Logger,
) : ObservableProperty<T>(initialValue) {

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    val value = super.getValue(thisRef, property)

    if (bigCollectionSize != null) {
      val size = value.size
      if (size >= bigCollectionSize) {
        logger.error { "${property.name} is too big: $size" }
      }
    }

    return value
  }
}
