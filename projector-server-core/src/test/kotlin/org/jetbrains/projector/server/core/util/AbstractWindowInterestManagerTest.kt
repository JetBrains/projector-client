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
package org.jetbrains.projector.server.core.util

import org.jetbrains.projector.common.protocol.toClient.ServerClipboardEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ServerPingReplyEvent
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowInterestEvent
import kotlin.test.*

class AbstractWindowInterestManagerTest {
  private class DummyRedrawImpl : AbstractWindowDrawInterestManager() {
    var repaintCounter = 0
      private set
    override fun redrawWindow(id: Int) {
      repaintCounter++
    }
  }


  @Test
  fun `test interested by default`() {
    val interestManager = DummyRedrawImpl()

    assertTrue(interestManager.isInterestedInWindow(0))
  }

  @Test
  fun `test disinterest event handle`() {
    val interestManager = DummyRedrawImpl()

    assertTrue(interestManager.isInterestedInWindow(0))

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    assertFalse(interestManager.isInterestedInWindow(0))

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    assertTrue(interestManager.isInterestedInWindow(0))
  }

  @Test
  fun `test different windows not interfering`() {
    val interestManager = DummyRedrawImpl()

    interestManager.processClientEvent(ClientWindowInterestEvent(1, false))
    assertTrue(interestManager.isInterestedInWindow(0))

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    assertFalse(interestManager.isInterestedInWindow(0))

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    assertTrue(interestManager.isInterestedInWindow(0))
    assertFalse(interestManager.isInterestedInWindow(1))
  }

  @Test
  fun `test repeated events are idempotent`() {
    val interestManager = DummyRedrawImpl()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    assertFalse(interestManager.isInterestedInWindow(0))

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    assertTrue(interestManager.isInterestedInWindow(0))
  }

  @Test
  fun `test event filtering on clean interest manager keeps sequence instance`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
    )

    val sourceSequence = sampleEvents.asSequence()

    val filteredSequence = interestManager.filterEvents(sourceSequence)

    assertEquals(sourceSequence, filteredSequence, "Clean interest manager should return the same sequence")
  }

  @Test
  fun `test event filtering on fully interested interest manager keeps sequence instance`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
    )

    val sourceSequence = sampleEvents.asSequence()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))

    val filteredSequence = interestManager.filterEvents(sourceSequence)

    assertEquals(sourceSequence, filteredSequence, "Clean interest manager should return the same sequence")
  }

  @Test
  fun `test event filtering keeps non-draw events`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
      ServerClipboardEvent("a"),
      ServerPingReplyEvent(1, 2),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Offscreen(1, 2, 3))
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    assertContentEquals(sampleEvents, filteredEvents, "Non-draw events should not be filtered out")
  }

  @Test
  fun `test event filtering keeps interested events`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    assertTrue(filteredEvents.all { it is ServerDrawCommandsEvent && (it.target as ServerDrawCommandsEvent.Target.Onscreen).windowId == 1 }, "Interested events should not be filtered out")
  }

  @Test
  fun `test event filtering discards disinterested events 1`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(1, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    assertTrue(filteredEvents.isEmpty(), "Disinterested events should be filtered out")
  }

  @Test
  fun `test event filtering discards disinterested events 2`() {
    val interestManager = DummyRedrawImpl()
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    assertTrue(filteredEvents.all { it is ServerDrawCommandsEvent && (it.target as ServerDrawCommandsEvent.Target.Onscreen).windowId == 1 }, "Events should be filtered out precisely")
  }

  @Test
  fun `test gaining interest triggers abstract redraw`() {
    val interestManager = DummyRedrawImpl()

    val windowId = 0

    assertEquals(0, interestManager.repaintCounter)

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, false))
    assertEquals(0, interestManager.repaintCounter, "interest loss should not trigger repaint")

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    assertEquals(1, interestManager.repaintCounter, "interest gain should trigger repaint")

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    assertEquals(1, interestManager.repaintCounter, "repeated interest gain should not trigger repaint")

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    assertEquals(2, interestManager.repaintCounter, "interest gain should trigger repaint")

  }
}
