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
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D

public fun <E> extractData(iterable: MutableIterable<E>): List<E> {
  val answer = mutableListOf<E>()

  iterable.removeAll(answer::add)

  return answer
}

private fun Rectangle2D.isVisible(clip: ServerSetClipEvent, tx: ServerSetTransformEvent): Boolean {
  val clipRect = clip.shape as? CommonRectangle ?: return true  // can't tell
  val identityTransformStrBounds = AffineTransform(tx.tx).createTransformedShape(this)

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

public fun List<List<ServerWindowEvent>>.convertToSimpleList(): List<ServerWindowEvent> {
  val answer = mutableListOf<ServerWindowEvent>()

  var lastCompositeEvent: ServerSetCompositeEvent? = null
  var lastClipEvent: ServerSetClipEvent? = null
  var lastTransformEvent: ServerSetTransformEvent? = null
  var lastFontEvent: ServerSetFontEvent? = null
  var lastSetPaintEvent: ServerSetPaintEvent? = null
  var lastStrokeEvent: ServerSetStrokeEvent? = null

  this.forEach { packedEvents ->
    packedEvents.forEach innerLoop@{ event ->
      Do exhaustive when (event) {
        is ServerWindowStateEvent -> Do exhaustive when (event) {
          is ServerSetCompositeEvent -> if (event == lastCompositeEvent) {
            Unit
          }
          else {
            lastCompositeEvent = event
            answer.add(event)
          }

          is ServerSetClipEvent -> if (event == lastClipEvent) {
            Unit
          }
          else {
            lastClipEvent = event
            answer.add(event)
          }

          is ServerSetTransformEvent -> if (event == lastTransformEvent) {
            Unit
          }
          else {
            lastTransformEvent = event
            answer.add(event)
          }

          is ServerSetPaintEvent -> if (event == lastSetPaintEvent) {
            Unit
          }
          else {
            lastSetPaintEvent = event
            answer.add(event)
          }

          is ServerSetFontEvent -> if (event == lastFontEvent) {
            Unit
          }
          else {
            lastFontEvent = event
            answer.add(event)
          }

          is ServerSetStrokeEvent -> if (event == lastStrokeEvent) {
            Unit
          }
          else {
            lastStrokeEvent = event
            answer.add(event)
          }

          is ServerWindowToDoStateEvent -> answer.add(event)
        }

        is ServerWindowPaintEvent -> {
          val visible = when (event) {
            is ServerDrawStringEvent -> event.isVisible(lastFontEvent, lastClipEvent, lastTransformEvent)
            is ServerPaintRectEvent -> event.isVisible(lastClipEvent, lastTransformEvent)
            else -> true
          }

          if (visible) {
            answer.add(event)
          }
          else {
            Unit
          }
        }
      }
    }
  }

  while (answer.isNotEmpty() && answer.last() is ServerWindowStateEvent) {
    answer.removeLast()
  }

  return answer
}
