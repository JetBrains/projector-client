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
package org.jetbrains.projector.server.core.util

import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ServerEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowInterestEvent
import java.util.concurrent.ConcurrentHashMap

public abstract class AbstractWindowDrawInterestManager {
  private val disinterestedWindows = ConcurrentHashMap<Int, Unit>().keySet(Unit)

  public fun isInterestedInWindow(windowId: Int): Boolean {
    return windowId !in disinterestedWindows
  }

  public fun filterEvents(events: Sequence<ServerEvent>): Sequence<ServerEvent> {
    if (disinterestedWindows.isEmpty())
      return events

    return events.filter {
      it !is ServerDrawCommandsEvent || it.target.let { target ->
        target !is ServerDrawCommandsEvent.Target.Onscreen || isInterestedInWindow(target.windowId)
      }
    }
  }

  protected abstract fun redrawWindow(id: Int)

  public fun processClientEvent(event: ClientWindowInterestEvent) {
    if (event.isInterested) {
      if (disinterestedWindows.remove(event.windowId))
        // trigger redraw to force resend of up-to-date window state
        redrawWindow(event.windowId)
    } else
      disinterestedWindows.add(event.windowId)
  }
}
