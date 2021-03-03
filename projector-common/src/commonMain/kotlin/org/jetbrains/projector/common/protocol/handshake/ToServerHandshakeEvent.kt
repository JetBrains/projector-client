/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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
package org.jetbrains.projector.common.protocol.handshake

import kotlinx.serialization.Serializable

@Serializable
data class ToServerHandshakeEvent(
  val commonVersion: Int,
  val commonVersionId: Int,
  val token: String? = null,
  val clientDoesWindowManagement: Boolean,
  val displays: List<DisplayDescription>,
  val supportedToClientCompressions: List<CompressionType>,
  val supportedToClientProtocols: List<ProtocolType>,
  val supportedToServerCompressions: List<CompressionType>,
  val supportedToServerProtocols: List<ProtocolType>,
)

@Serializable
data class DisplayDescription(val x: Int, val y: Int, val width: Int, val height: Int, val scaleFactor: Double)
