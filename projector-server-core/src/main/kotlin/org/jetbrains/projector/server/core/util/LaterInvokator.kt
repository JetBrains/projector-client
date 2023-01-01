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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.jetbrains.projector.server.core.util

import org.jetbrains.projector.util.loading.unprotect
import sun.awt.AppContext
import java.awt.AWTEvent
import java.awt.EventQueue

public interface LaterInvokator {

  public operator fun invoke(awtEvent: AWTEvent)

  public companion object {

    public val defaultLaterInvokator: LaterInvokator = object : LaterInvokator {

      private val dispatchEventMethod = EventQueue::class.java.getDeclaredMethod("dispatchEvent", AWTEvent::class.java).apply {
        unprotect()
      }

      override fun invoke(awtEvent: AWTEvent) {
        dispatchEventMethod.invoke(eventQueue, awtEvent)
      }
    }

    private val eventQueue get() = AppContext.getAppContext().get(AppContext.EVENT_QUEUE_KEY) as EventQueue
  }
}
