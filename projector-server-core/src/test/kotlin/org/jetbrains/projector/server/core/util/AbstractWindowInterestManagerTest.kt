/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.jetbrains.projector.common.protocol.toClient.ServerClipboardEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ServerPingReplyEvent
import org.jetbrains.projector.common.protocol.toClient.ServerWindowSetChangedEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowInterestEvent

class AbstractWindowInterestManagerTest : AnnotationSpec() {
  class DummyRedrawImpl : AbstractWindowDrawInterestManager() {
    var repaintCounter = 0
      private set

    override fun redrawWindow(id: Int) {
      repaintCounter++
    }
  }

  private lateinit var interestManager: DummyRedrawImpl

  @BeforeEach
  fun beforeTest() {
    interestManager = DummyRedrawImpl()
  }

  @Test
  fun `should interested by default`() {
    interestManager.isInterestedInWindow(0).shouldBeTrue()
  }

  @Test
  fun `disinterest event should handle`() {
    interestManager.isInterestedInWindow(0).shouldBeTrue()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.isInterestedInWindow(0).shouldBeFalse()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    interestManager.isInterestedInWindow(0).shouldBeTrue()
  }

  @Test
  fun `different windows should not interfering`() {
    interestManager.processClientEvent(ClientWindowInterestEvent(1, false))
    interestManager.isInterestedInWindow(0).shouldBeTrue()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.isInterestedInWindow(0).shouldBeFalse()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    interestManager.isInterestedInWindow(0).shouldBeTrue()
    interestManager.isInterestedInWindow(1).shouldBeFalse()
  }

  @Test
  fun `repeated events should be idempotent`() {
    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.isInterestedInWindow(0).shouldBeFalse()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))
    interestManager.isInterestedInWindow(0).shouldBeTrue()
  }

  @Test
  fun `event filtering on clean interest manager should keep sequence instance`() {
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
    )

    val sourceSequence = sampleEvents.asSequence()

    val filteredSequence = interestManager.filterEvents(sourceSequence)

    withClue("Clean interest manager should return the same sequence") {
      filteredSequence shouldBe sourceSequence
    }
  }

  @Test
  fun `event filtering on fully interested interest manager should keep sequence instance`() {
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
    )

    val sourceSequence = sampleEvents.asSequence()

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(0, true))

    val filteredSequence = interestManager.filterEvents(sourceSequence)

    withClue("Clean interest manager should return the same sequence") {
      filteredSequence shouldBe sourceSequence
    }
  }

  @Test
  fun `event filtering should keep non-draw events`() {
    val sampleEvents = listOf(
      ServerWindowSetChangedEvent(),
      ServerClipboardEvent("a"),
      ServerPingReplyEvent(1, 2),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Offscreen(1, 2, 3))
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    withClue("Non-draw events should not be filtered out") {
      filteredEvents shouldBe sampleEvents
    }
  }

  @Test
  fun `event filtering should keep interested events`() {
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    withClue("Interested events should not be filtered out") {
      filteredEvents.all {
        it is ServerDrawCommandsEvent && (it.target as ServerDrawCommandsEvent.Target.Onscreen).windowId == 1
      }.shouldBeTrue()
    }
  }

  @Test
  fun `event filtering should discard disinterested events 1`() {
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(1, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()
    withClue("Disinterested events should be filtered out") {
      filteredEvents.isEmpty().shouldBeTrue()
    }
  }

  @Test
  fun `event filtering should discard disinterested events 2`() {
    val sampleEvents = listOf(
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(0)),
      ServerDrawCommandsEvent(ServerDrawCommandsEvent.Target.Onscreen(1)),
    )

    interestManager.processClientEvent(ClientWindowInterestEvent(0, false))

    val filteredEvents = interestManager.filterEvents(sampleEvents.asSequence()).toList()

    withClue("Events should be filtered out precisely") {
      filteredEvents.all {
        it is ServerDrawCommandsEvent && (it.target as ServerDrawCommandsEvent.Target.Onscreen).windowId == 1
      }.shouldBeTrue()
    }
  }

  @Test
  fun `gaining interest should trigger abstract redraw`() {
    val windowId = 0

    interestManager.repaintCounter shouldBe 0

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, false))
    withClue("interest loss should not trigger repaint") {
      interestManager.repaintCounter shouldBe 0
    }

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    withClue("interest gain should trigger repaint") {
      interestManager.repaintCounter shouldBe 1
    }

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    withClue("repeated interest gain should not trigger repaint") {
      interestManager.repaintCounter shouldBe 1
    }

    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, false))
    interestManager.processClientEvent(ClientWindowInterestEvent(windowId, true))
    withClue("interest gain should trigger repaint") {
      interestManager.repaintCounter shouldBe 2
    }
  }
}
