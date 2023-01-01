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
package org.jetbrains.projector.client.web.protocol

import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.protocol.KotlinxProtoBufToClientMessageDecoder
import org.jetbrains.projector.client.common.protocol.SerializationToServerMessageEncoder
import org.jetbrains.projector.common.protocol.compress.MessageCompressor
import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.compress.NotCompressor
import org.jetbrains.projector.common.protocol.compress.NotDecompressor
import org.jetbrains.projector.common.protocol.toClient.ToClientMessageDecoder
import org.jetbrains.projector.common.protocol.toServer.ToServerMessageEncoder

object SupportedTypesProvider {

  val supportedToClientDecompressors: List<MessageDecompressor<ByteArray>> = when (ParamsProvider.ENABLE_COMPRESSION) {
    true -> listOf(GZipToClientMessageDecompressor)

    false -> listOf(NotDecompressor())
  }

  val supportedToClientDecoders: List<ToClientMessageDecoder> = when (ParamsProvider.TO_CLIENT_FORMAT) {
    ParamsProvider.ToClientFormat.KOTLINX_JSON -> listOf(KotlinxJsonToClientMessageDecoder)

    ParamsProvider.ToClientFormat.KOTLINX_JSON_MANUAL -> listOf(ManualJsonToClientMessageDecoder)

    ParamsProvider.ToClientFormat.KOTLINX_PROTOBUF -> listOf(KotlinxProtoBufToClientMessageDecoder)
  }

  val supportedToServerCompressors: List<MessageCompressor<String>> = listOf(
    NotCompressor()
  )

  val supportedToServerEncoders: List<ToServerMessageEncoder> = listOf(
    SerializationToServerMessageEncoder
  )
}
