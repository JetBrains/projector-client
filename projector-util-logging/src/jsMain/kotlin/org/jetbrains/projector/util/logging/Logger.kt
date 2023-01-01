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
package org.jetbrains.projector.util.logging

import kotlin.js.Console

@Suppress("FunctionName")  // to make the function look like class, its name starts with a capital
public actual fun Logger(tag: String): Logger = JsLoggerImpl(tag)

private class JsLoggerImpl(private val tag: String) : Logger {

  override fun error(t: Throwable?, lazyMessage: () -> String) = log(wrapper::error, LogLevel.ERROR, tag, t, lazyMessage)
  override fun info(t: Throwable?, lazyMessage: () -> String) = log(wrapper::info, LogLevel.INFO, tag, t, lazyMessage)
  override fun debug(t: Throwable?, lazyMessage: () -> String) = log(wrapper::log, LogLevel.DEBUG, tag, t, lazyMessage)

  private enum class LogLevel(val prefix: String) {

    ERROR("ERROR"),
    INFO("INFO"),
    DEBUG("DEBUG"),
  }

  private companion object {

    // Remove console wrapper after https://youtrack.jetbrains.com/issue/KT-43497 is resolved
    private val wrapper = object : Console {

      override fun dir(o: Any) = console.dir(o)
      override fun error(vararg o: Any?) = console.error(*o)
      override fun info(vararg o: Any?) = console.info(*o)
      override fun log(vararg o: Any?) = console.log(*o)
      override fun warn(vararg o: Any?) = console.warn(*o)
    }

    private fun log(f: (Array<out Any?>) -> Unit, level: LogLevel, tag: String, t: Throwable?, lazyMessage: () -> String) {
      val fullMessage = "[${level.prefix}] :: $tag :: ${lazyMessage()}"
      val logArguments = t?.let { arrayOf(fullMessage, t) } ?: arrayOf(fullMessage)
      f(logArguments)
    }
  }
}
