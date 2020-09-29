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
package org.jetbrains.projector.client.web.misc

import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.common.misc.Do
import org.w3c.dom.HTMLSpanElement
import kotlin.math.roundToInt

class ConnectionWatcher {

  private var intervalHandle: Int? = null
  private var lastTimeStamp = TimeStamp.current
  private val message = (document.createElement("span") as HTMLSpanElement).apply {
    style.position = "absolute"
    style.bottom = "80px"
    style.right = "5%"
    style.zIndex = "1"
    style.asDynamic().pointerEvents = "none"

    style.padding = "5px"
    style.background = "red"
    style.display = "none"

    className = "connection-watcher-warning"
  }

  fun setWatcher() {
    check(intervalHandle == null) { "Can't set the watcher more than once" }
    intervalHandle = window.setInterval(
      handler = {
        Do exhaustive when (lastTimeStamp + ParamsProvider.PING_INTERVAL * 3 < TimeStamp.current) {
          true -> {
            message.innerText = "No messages from server for ${(TimeStamp.current - lastTimeStamp).roundToInt()} ms..."
            message.style.display = "block"
          }

          false -> message.style.display = "none"
        }
      },
      timeout = ParamsProvider.PING_INTERVAL * 3,
    )
    document.body!!.appendChild(message)
  }

  fun removeWatcher() {
    val handle = checkNotNull(intervalHandle) { "Can't remove initialized watcher" }
    window.clearInterval(handle)
    message.style.display = "none"
  }

  fun resetTime() {
    lastTimeStamp = TimeStamp.current
  }
}
