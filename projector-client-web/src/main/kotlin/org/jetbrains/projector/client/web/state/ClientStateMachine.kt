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

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.projector.util.logging.Logger

class ClientStateMachine {

  private val scope = MainScope()

  private val eventQueue = Channel<ClientAction>(capacity = Channel.UNLIMITED)

  private var currentState: ClientState = ClientState.UninitializedPage

  fun fire(action: ClientAction) {
    scope.launch {
      eventQueue.send(action)
    }
  }

  fun runMainLoop() {
    scope.launch {
      while (true) {
        val action = eventQueue.receive()
        try {
          currentState = currentState.consume(action)
        }
        catch (t: Throwable) {
          logger.error(t) { "Error consuming action, skipping the action" }
        }
      }
    }
  }

  private companion object {

    private val logger = Logger<ClientStateMachine>()
  }
}
