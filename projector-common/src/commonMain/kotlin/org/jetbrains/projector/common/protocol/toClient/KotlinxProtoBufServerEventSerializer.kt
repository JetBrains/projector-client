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
package org.jetbrains.projector.common.protocol.toClient

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

object KotlinxProtoBufServerEventSerializer {

  // todo: remove after https://github.com/Kotlin/kotlinx.serialization/issues/93 is resolved
  @Serializable
  private class ListWrapper(val value: List<ServerEvent> = emptyList())  // todo: remove default after https://github.com/Kotlin/kotlinx.serialization/issues/806

  private val protoBuf = ProtoBuf(encodeDefaults = false)

  fun serializeList(msg: List<ServerEvent>): ByteArray = protoBuf.dump(ListWrapper.serializer(), ListWrapper(msg))

  fun deserializeList(data: ByteArray): List<ServerEvent> = protoBuf.load(ListWrapper.serializer(), data).value
}
