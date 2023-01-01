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
package org.jetbrains.projector.client.web.state

import kotlinx.browser.window

sealed class MessagingPolicy(
  protected val isFlushNeeded: () -> Boolean,
  protected val flush: () -> Unit,
) {

  abstract fun onHandshakeFinished()

  abstract fun onToClientMessage()

  abstract fun onAddEvent()

  protected fun flushIfNeeded() {
    if (isFlushNeeded()) {
      flush()
    }
  }

  class Unbuffered(isFlushNeeded: () -> Boolean, flush: () -> Unit) : MessagingPolicy(isFlushNeeded, flush) {

    override fun onHandshakeFinished() {
      flush()
    }

    override fun onToClientMessage() {
      flushIfNeeded()
    }

    override fun onAddEvent() {
      flush()
    }
  }

  class Buffered(private val timeout: Int, isFlushNeeded: () -> Boolean, flush: () -> Unit) : MessagingPolicy(isFlushNeeded, flush) {

    override fun onHandshakeFinished() {
      flush()
    }

    override fun onToClientMessage() {
      window.setTimeout(::flushIfNeeded, timeout)
    }

    override fun onAddEvent() {
      window.setTimeout(::flushIfNeeded, timeout)
    }
  }
}
