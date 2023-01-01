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
package org.jetbrains.projector.client.web

import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.common.protocol.data.CommonIntSize
import org.jetbrains.projector.common.protocol.toServer.ClientResizeEvent
import org.w3c.dom.events.Event
import kotlin.math.roundToInt

class WindowSizeController(private val stateMachine: ClientStateMachine) {

  val currentSize: CommonIntSize
    get() {
      val userScalingRatio = ParamsProvider.USER_SCALING_RATIO

      return CommonIntSize(
        width = (window.innerWidth / userScalingRatio).roundToInt(),
        height = (window.innerHeight / userScalingRatio).roundToInt()
      )
    }

  // todo: remove SUPPRESS after KT-35120 is solved
  private fun handleResizeEvent(@Suppress("UNUSED_PARAMETER") event: Event) {
    stateMachine.fire(ClientAction.AddEvent(ClientResizeEvent(size = currentSize)))
    stateMachine.fire(ClientAction.WindowResize)
  }

  init {
    window.addEventListener(RESIZE_EVENT_TYPE, ::handleResizeEvent)
  }

  companion object {

    private const val RESIZE_EVENT_TYPE = "resize"
  }
}
