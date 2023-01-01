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
package org.jetbrains.projector.common.protocol

import org.jetbrains.projector.common.protocol.handshake.COMMON_VERSION
import org.jetbrains.projector.common.protocol.handshake.HANDSHAKE_VERSION
import org.jetbrains.projector.common.protocol.handshake.commonVersionList
import org.jetbrains.projector.common.protocol.handshake.handshakeVersionList
import kotlin.test.Test
import kotlin.test.assertTrue

class CommonVersionPresenceTest {

  @Test
  fun handshakeVersionListShouldContainCurrentHandshakeVersion() {
    assertTrue(
      HANDSHAKE_VERSION in handshakeVersionList,
      """
        |Current handshake version of protocol should be in `handshakeVersionList`.
        |It seems that protocol has been updated.
        |Please append the current HANDSHAKE_VERSION to the `handshakeVersionList` as the last element.
        |Current values:
        |HANDSHAKE_VERSION = $HANDSHAKE_VERSION
        |handshakeVersionList = $handshakeVersionList
      """.trimMargin()
    )
  }

  @Test
  fun commonVersionListShouldContainCurrentCommonVersion() {
    assertTrue(
      COMMON_VERSION in commonVersionList,
      """
        |Current common version of protocol should be in `commonVersionList`.
        |It seems that protocol has been updated.
        |Please append the current COMMON_VERSION to the `commonVersionList` as the last element.
        |Current values:
        |COMMON_VERSION = $COMMON_VERSION
        |commonVersionList = $commonVersionList
      """.trimMargin()
    )
  }
}
