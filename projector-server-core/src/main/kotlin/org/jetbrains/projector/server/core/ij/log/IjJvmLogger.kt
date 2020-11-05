/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
package org.jetbrains.projector.server.core.ij.log

import org.jetbrains.projector.util.logging.Logger

// todo: check for IDEA Logger settings and suppress disabled log levels
internal class IjJvmLogger(ideaClassLoader: ClassLoader, tag: String) : Logger {

  private val loggerClass = Class.forName("com.intellij.openapi.diagnostic.Logger", false, ideaClassLoader)

  private val logger = loggerClass
    .getDeclaredMethod("getInstance", String::class.java)
    .invoke(null, tag)

  private val errorMethod = loggerClass
    .getDeclaredMethod("error", String::class.java, Throwable::class.java)

  private val infoMethod = loggerClass
    .getDeclaredMethod("info", String::class.java, Throwable::class.java)

  private val debugMethod = loggerClass
    .getDeclaredMethod("debug", String::class.java, Throwable::class.java)

  override fun error(t: Throwable?, lazyMessage: () -> String) {
    errorMethod.invoke(logger, lazyMessage(), t)
  }

  override fun info(t: Throwable?, lazyMessage: () -> String) {
    infoMethod.invoke(logger, lazyMessage(), t)
  }

  override fun debug(t: Throwable?, lazyMessage: () -> String) {
    debugMethod.invoke(logger, lazyMessage(), t)
  }
}
