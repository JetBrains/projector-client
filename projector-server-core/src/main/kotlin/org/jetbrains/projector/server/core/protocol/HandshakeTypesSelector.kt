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
package org.jetbrains.projector.server.core.protocol

import org.jetbrains.projector.common.protocol.compress.MessageCompressor
import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.compress.NotCompressor
import org.jetbrains.projector.common.protocol.compress.NotDecompressor
import org.jetbrains.projector.common.protocol.handshake.CompressionType
import org.jetbrains.projector.common.protocol.handshake.ProtocolType
import org.jetbrains.projector.common.protocol.toClient.ToClientMessageEncoder
import org.jetbrains.projector.common.protocol.toClient.ToClientTransferableType
import org.jetbrains.projector.common.protocol.toServer.ToServerMessageDecoder
import org.jetbrains.projector.common.protocol.toServer.ToServerTransferableType

public object HandshakeTypesSelector {

  public fun selectToClientCompressor(supportedToClientCompressions: List<CompressionType>): MessageCompressor<ToClientTransferableType>? {
    fun CompressionType.toToClientCompressor(): MessageCompressor<ToClientTransferableType>? = when (this) {
      CompressionType.GZIP -> GZipMessageCompressor
      CompressionType.NONE -> NotCompressor()
    }

    return supportedToClientCompressions.firstNotNullOfOrNull(CompressionType::toToClientCompressor)
  }

  public fun selectToClientEncoder(supportedToClientProtocols: List<ProtocolType>): ToClientMessageEncoder? {
    fun ProtocolType.toToClientEncoder(): ToClientMessageEncoder? = when (this) {
      ProtocolType.KOTLINX_JSON -> KotlinxJsonToClientMessageEncoder
      ProtocolType.KOTLINX_PROTOBUF -> KotlinxProtoBufToClientMessageEncoder
    }

    return supportedToClientProtocols.firstNotNullOfOrNull(ProtocolType::toToClientEncoder)
  }

  public fun selectToServerDecompressor(supportedToServerCompressions: List<CompressionType>): MessageDecompressor<ToServerTransferableType>? {
    fun CompressionType.toToServerDecompressor(): MessageDecompressor<ToServerTransferableType>? = when (this) {
      CompressionType.NONE -> NotDecompressor()
      else -> null
    }

    return supportedToServerCompressions.firstNotNullOfOrNull(CompressionType::toToServerDecompressor)
  }

  public fun selectToServerDecoder(supportedToServerProtocols: List<ProtocolType>): ToServerMessageDecoder? {
    fun ProtocolType.toToServerDecoder(): ToServerMessageDecoder? = when (this) {
      ProtocolType.KOTLINX_JSON -> KotlinxJsonToServerMessageDecoder
      ProtocolType.KOTLINX_PROTOBUF -> KotlinxJsonToServerMessageDecoder
    }

    return supportedToServerProtocols.firstNotNullOfOrNull(ProtocolType::toToServerDecoder)
  }
}
