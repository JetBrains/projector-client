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
package org.jetbrains.projector.server.core

import org.jetbrains.projector.common.protocol.compress.MessageCompressor
import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.toClient.ServerEvent
import org.jetbrains.projector.common.protocol.toClient.ToClientMessageEncoder
import org.jetbrains.projector.common.protocol.toClient.ToClientTransferableType
import org.jetbrains.projector.common.protocol.toServer.ToServerMessageDecoder
import org.jetbrains.projector.common.protocol.toServer.ToServerTransferableType
import org.jetbrains.projector.server.core.util.AbstractWindowDrawInterestManager
import org.jetbrains.projector.server.core.util.SizeAware
import org.jetbrains.projector.util.logging.Logger
import java.util.concurrent.ConcurrentLinkedQueue

public sealed class ClientSettings {

  public abstract val connectionMillis: Long
  public abstract val address: String?
}

public class ConnectedClientSettings(
  override val connectionMillis: Long,
  override val address: String?,
) : ClientSettings()

public class SupportedHandshakeClientSettings(
  override val connectionMillis: Long,
  override val address: String?,
) : ClientSettings()

public data class SetUpClientData(
  val hasWriteAccess: Boolean,
  val toClientMessageEncoder: ToClientMessageEncoder,
  val toClientMessageCompressor: MessageCompressor<ToClientTransferableType>,
  val toServerMessageDecoder: ToServerMessageDecoder,
  val toServerMessageDecompressor: MessageDecompressor<ToServerTransferableType>,
)

public class SetUpClientSettings(
  override val connectionMillis: Long,
  override val address: String?,
  public val setUpClientData: SetUpClientData,
) : ClientSettings()

public class ReadyClientSettings(
  override val connectionMillis: Long,
  override val address: String?,
  public val setUpClientData: SetUpClientData,
  @Suppress("unused")  // used by server
  public val interestManager: AbstractWindowDrawInterestManager,
  bigCollectionSize: Int?,
) : ClientSettings() {

  public var touchState: TouchState = TouchState.Released

  public val requestedData: ConcurrentLinkedQueue<ServerEvent> by SizeAware(ConcurrentLinkedQueue<ServerEvent>(), bigCollectionSize, logger)

  private companion object {

    private val logger = Logger<ReadyClientSettings>()
  }

  public sealed class TouchState {

    public object Released : TouchState()

    public data class OnlyPressed(
      val connectionMillis: Int,
      override val lastX: Int,
      override val lastY: Int,
    ) : TouchState(), WithCoordinates

    public object Dragging : TouchState()

    public data class Scrolling(
      val initialX: Int, val initialY: Int,
      override val lastX: Int, override val lastY: Int,
    ) : TouchState(), WithCoordinates

    public interface WithCoordinates {

      public val lastX: Int
      public val lastY: Int
    }
  }
}

public class ClosedClientSettings(
  override val connectionMillis: Long,
  override val address: String?,
  public val reason: String,
) : ClientSettings()
