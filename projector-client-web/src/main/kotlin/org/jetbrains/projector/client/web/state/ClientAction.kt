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

import org.jetbrains.projector.client.web.WindowSizeController
import org.jetbrains.projector.common.protocol.toServer.ClientEvent

sealed class ClientAction {

  class Start(
    val stateMachine: ClientStateMachine,
    val url: String,
    val windowSizeController: WindowSizeController,
  ) : ClientAction()

  sealed class WebSocket : ClientAction() {

    class Open(val openingTimeStamp: Int) : WebSocket()
    class Message(val message: ByteArray) : WebSocket()
    class Close(val wasClean: Boolean, val code: Short, val reason: String) : WebSocket()
    class NoReplies(val elapsedTimeMs: Int) : WebSocket()
  }

  class AddEvent(val event: ClientEvent) : ClientAction()

  object Flush : ClientAction()

  object LoadAllFonts : ClientAction()

  object WindowResize : ClientAction()
}
