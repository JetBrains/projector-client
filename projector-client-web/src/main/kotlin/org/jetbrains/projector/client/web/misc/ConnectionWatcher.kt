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
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import kotlin.math.roundToInt

class ConnectionWatcher(private val reloader: (elapsedTimeMs: Int) -> Unit) {

  private var intervalHandle: Int? = null
  private var lastTimeStamp = TimeStamp.current

  fun setWatcher() {
    check(intervalHandle == null) { "Can't set the watcher more than once" }
    intervalHandle = window.setInterval(
      handler = {
        if (lastTimeStamp + ParamsProvider.PING_INTERVAL * 3 < TimeStamp.current) {
          reloader((TimeStamp.current - lastTimeStamp).roundToInt())
        }
      },
      timeout = ParamsProvider.PING_INTERVAL * 3,
    )
  }

  fun removeWatcher() {
    val handle = checkNotNull(intervalHandle) { "Can't remove initialized watcher" }
    window.clearInterval(handle)
  }

  fun resetTime() {
    lastTimeStamp = TimeStamp.current
  }
}
