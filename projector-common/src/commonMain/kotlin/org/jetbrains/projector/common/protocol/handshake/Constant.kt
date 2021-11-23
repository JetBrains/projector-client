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

import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.projector.common.misc.compatibilityHash
import org.jetbrains.projector.common.protocol.toClient.ServerEvent
import org.jetbrains.projector.common.protocol.toServer.ClientEvent

val HANDSHAKE_VERSION = listOf(ToClientHandshakeEvent.serializer(), ToServerHandshakeEvent.serializer())
  .map { ListSerializer(it).descriptor.compatibilityHash }
  .reduce(Int::xor)

// Don't change order here: it's used to obtain readable "human id"
val handshakeVersionList = listOf(
  456250626,
)

val COMMON_VERSION = listOf(ServerEvent.serializer(), ClientEvent.serializer())
  .map { ListSerializer(it).descriptor.compatibilityHash }
  .reduce(Int::xor)

// Don't change order here: it's used to obtain readable "human id"
val commonVersionList = listOf(
  -1663032476,
  615706807,
  891030124,
  -1205505588,
  581264379,
  -625612891,
  -560999684,
  471600343,
  -215338327,
  1580903519,
  36358479,
  1670488062,
  -733798733,
  -1255984693,
  -1671626343,
  1980644253,
  1328208692,
)
