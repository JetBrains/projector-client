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
package org.jetbrains.projector.server.core.convert.toClient

import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.common.protocol.toClient.*
import org.jetbrains.projector.util.logging.Logger
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import kotlin.reflect.KMutableProperty1

public fun <E> extractData(iterable: MutableIterable<E>): List<E> {
  val answer = mutableListOf<E>()

  iterable.removeAll(answer::add)

  return answer
}

private fun Rectangle2D.isVisible(clip: ServerSetClipEvent, tx: ServerSetTransformEvent): Boolean {
  val clipRect = clip.shape as? CommonRectangle ?: return true  // can't tell
  val identityTransformStrBounds = AffineTransform(tx.tx.toDoubleArray()).createTransformedShape(this)

  return identityTransformStrBounds.intersects(clipRect.x, clipRect.y, clipRect.width, clipRect.height)
}

private fun ServerDrawStringEvent.isVisible(font: ServerSetFontEvent?, clip: ServerSetClipEvent?, tx: ServerSetTransformEvent?): Boolean {
  if (font == null || clip == null || tx == null) {
    return true  // can't tell
  }

  val height = font.fontSize * 3  // todo: it's rough rounding up

  val strBounds = Rectangle2D.Double(
    this.x,
    this.y - height,
    this.desiredWidth,
    2.0 * height,
  )

  return strBounds.isVisible(clip, tx)
}

private fun ServerPaintRectEvent.isVisible(clip: ServerSetClipEvent?, tx: ServerSetTransformEvent?): Boolean {
  if (clip == null || tx == null) {
    return true  // can't tell
  }

  val strBounds = Rectangle2D.Double(
    this.x,
    this.y,
    this.width,
    this.height,
  )

  return strBounds.isVisible(clip, tx)
}

private class TargetState {
  var lastCompositeEvent: ServerSetCompositeEvent? = null
  var lastClipEvent: ServerSetClipEvent? = null
  var lastTransformEvent: ServerSetTransformEvent? = null
  var lastFontEvent: ServerSetFontEvent? = null
  var lastSetPaintEvent: ServerSetPaintEvent? = null
  var lastStrokeEvent: ServerSetStrokeEvent? = null
}

private fun <T : ServerWindowStateEvent?> MutableCollection<ServerWindowEvent>.addIfNeeded(
  property: KMutableProperty1<TargetState, T>,
  actualState: TargetState,
  requestedState: TargetState,
) {
  val actualValue = property.get(actualState)
  val requestedValue = property.get(requestedState)

  if (actualValue != requestedValue) {
    property.set(actualState, requestedValue)
    requestedValue?.let { add(it) }
  }
}

public fun Iterable<Pair<ServerDrawCommandsEvent.Target, List<ServerWindowEvent>>>.shrinkEvents(): List<ServerDrawCommandsEvent> {
  val actualStates = mutableMapOf<ServerDrawCommandsEvent.Target, TargetState>()
  val requestedStates = mutableMapOf<ServerDrawCommandsEvent.Target, TargetState>()

  val withoutUnneededStates = mutableListOf<ServerDrawCommandsEvent>()

  this.forEach { (target, events) ->
    val actualState = actualStates.getOrPut(target) { TargetState() }
    val requestedState = requestedStates.getOrPut(target) { TargetState() }

    events.forEach { event ->
      Do exhaustive when (event) {
        is ServerWindowStateEvent -> Do exhaustive when (event) {
          is ServerSetCompositeEvent -> requestedState.lastCompositeEvent = event
          is ServerSetClipEvent -> requestedState.lastClipEvent = event
          is ServerSetTransformEvent -> requestedState.lastTransformEvent = event
          is ServerSetPaintEvent -> requestedState.lastSetPaintEvent = event
          is ServerSetFontEvent -> requestedState.lastFontEvent = event
          is ServerSetStrokeEvent -> requestedState.lastStrokeEvent = event
          is ServerWindowToDoStateEvent -> logger.info { "Skipping unsupported state event: $event" }
        }

        is ServerWindowPaintEvent -> {
          val visible = when (event) {
            is ServerDrawStringEvent -> with(requestedState) { event.isVisible(lastFontEvent, lastClipEvent, lastTransformEvent) }
            is ServerPaintRectEvent -> with(requestedState) { event.isVisible(lastClipEvent, lastTransformEvent) }
            else -> true  // such visibility check isn't supported yet
          }

          if (visible) {
            val eventsToAdd = mutableListOf<ServerWindowEvent>()

            eventsToAdd.addIfNeeded(TargetState::lastCompositeEvent, actualState, requestedState)
            eventsToAdd.addIfNeeded(TargetState::lastClipEvent, actualState, requestedState)
            eventsToAdd.addIfNeeded(TargetState::lastTransformEvent, actualState, requestedState)
            eventsToAdd.addIfNeeded(TargetState::lastFontEvent, actualState, requestedState)
            eventsToAdd.addIfNeeded(TargetState::lastSetPaintEvent, actualState, requestedState)
            eventsToAdd.addIfNeeded(TargetState::lastStrokeEvent, actualState, requestedState)
            eventsToAdd.add(event)

            withoutUnneededStates.add(ServerDrawCommandsEvent(target, eventsToAdd))
          }

          @Suppress("RedundantUnitExpression")  // needed for exhaustive when
          Unit
        }
      }
    }
  }

  val shrunk = mutableListOf<ServerDrawCommandsEvent>()
  var lastTarget: ServerDrawCommandsEvent.Target? = null
  val lastEvents = mutableListOf<ServerWindowEvent>()

  fun flush(target: ServerDrawCommandsEvent.Target) {
    shrunk.add(ServerDrawCommandsEvent(target, lastEvents.toList()))
    lastEvents.clear()
  }

  withoutUnneededStates.forEach { (target, events) ->
    if (target != lastTarget) {
      lastTarget?.let { flush(it) }
      lastTarget = target
    }

    lastEvents.addAll(events)
  }

  lastTarget?.let { flush(it) }

  return shrunk
}

private val logger = Logger("TransformKt")
