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
package org.jetbrains.projector.client.swing

import kotlinx.coroutines.channels.Channel
import org.jetbrains.projector.client.common.protocol.SerializationToServerMessageEncoder
import org.jetbrains.projector.common.protocol.toServer.ClientEvent
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

interface ProjectorTransport {
  val onOpen: CompletionStage<Unit>
  val onClosed: CompletionStage<Unit>

  val messages: Channel<ByteArray>
  val connectionTime: Long

  fun connect()

  fun send(bytes: ByteArray)
  fun send(string: String)
}

fun ProjectorTransport.send(event: ClientEvent) {
  val serialized = SerializationToServerMessageEncoder.encode(listOf(event))
  send(serialized)
}
