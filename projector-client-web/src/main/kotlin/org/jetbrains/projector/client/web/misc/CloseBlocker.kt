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

import org.w3c.dom.BeforeUnloadEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

interface CloseBlocker {

  fun setListener()
  fun removeListener()
}

object NopCloseBlocker : CloseBlocker {

  override fun setListener() {}

  override fun removeListener() {}
}

class CloseBlockerImpl(private val window: Window) : CloseBlocker {

  // Can't use simple function here because its reference isn't equal to itself: KT-15101
  private val onBeforeUnload = fun(e: Event) {
    require(e is BeforeUnloadEvent)

    e.preventDefault()
    e.returnValue = ""
  }

  override fun setListener() {
    window.addEventListener(beforeUnloadType, onBeforeUnload)
  }

  override fun removeListener() {
    window.removeEventListener(beforeUnloadType, onBeforeUnload)
  }

  companion object {

    private const val beforeUnloadType = "beforeunload"
  }
}
