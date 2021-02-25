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
package org.jetbrains.projector.client.swing

import org.jetbrains.projector.client.common.canvas.SwingCanvas
import org.jetbrains.projector.common.protocol.toServer.*
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.*
import javax.swing.JPanel

class ProjectorViewPanel(val canvas: SwingCanvas, val connectionTime: Long) : JPanel() {
  override fun paintComponent(g: Graphics) {
    g.drawImage(canvas.image, 0, 0, this)
  }

  override fun preferredSize(): Dimension {
    return Dimension(canvas.width, canvas.height)
  }

  fun addListeners(windowId: Int, frame: Component, eventSink: (ClientEvent) -> Unit) {
    val mouseListener = object: MouseAdapter() {
      fun convertMouseEvent(event: MouseEvent, eventType: ClientMouseEvent.MouseEventType): ClientMouseEvent {
        return ClientMouseEvent((System.currentTimeMillis() - connectionTime).toInt(), windowId, event.xOnScreen, event.yOnScreen, (maxOf(0, event.button - 1)).toShort(), event.clickCount,
                                convertModifiers(event.modifiersEx), eventType)
      }

      fun convertModifiers(modifiers: Int): Set<MouseModifier> {
        val set = mutableSetOf<MouseModifier>()
        if (modifiers and InputEvent.SHIFT_DOWN_MASK != 0) {
          set.add(MouseModifier.SHIFT_KEY)
        }
        if (modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
          set.add(MouseModifier.CTRL_KEY)
        }
        if (modifiers and InputEvent.ALT_DOWN_MASK != 0) {
          set.add(MouseModifier.ALT_KEY)
        }
        if (modifiers and InputEvent.META_DOWN_MASK != 0) {
          set.add(MouseModifier.META_KEY)
        }
        return set
      }

      override fun mouseClicked(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.CLICK))
      }

      override fun mouseWheelMoved(e: MouseWheelEvent) {
        val scrollType = when (e.scrollType) {
          MouseWheelEvent.WHEEL_BLOCK_SCROLL -> ClientWheelEvent.ScrollingMode.PAGE
          MouseWheelEvent.WHEEL_UNIT_SCROLL -> ClientWheelEvent.ScrollingMode.LINE
          else -> error("Unknown scroll type ${e.scrollType}")
        }
        val isHorizontal = e.isShiftDown
        val delta = when(e.scrollType) {
          MouseWheelEvent.WHEEL_UNIT_SCROLL -> e.wheelRotation.toDouble()
          MouseWheelEvent.WHEEL_BLOCK_SCROLL -> e.wheelRotation.toDouble()
          else -> error("Unknown scroll type ${e.scrollType}")
        }
        val deltaX = if(isHorizontal) delta else 0.0
        val deltaY = if(isHorizontal) 0.0 else delta
        eventSink(ClientWheelEvent((System.currentTimeMillis() - connectionTime).toInt(), windowId, convertModifiers(e.modifiersEx), scrollType, e.xOnScreen, e.yOnScreen, deltaX, deltaY))
      }

      override fun mouseMoved(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.MOVE))
      }

      override fun mousePressed(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.DOWN))
      }

      override fun mouseReleased(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.UP))
      }

      override fun mouseExited(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.OUT))
      }

      override fun mouseDragged(e: MouseEvent) {
        eventSink(convertMouseEvent(e, ClientMouseEvent.MouseEventType.DRAG))
      }
    }

    val keyboardListener = object : KeyAdapter() {
      private fun convertKeyEvent(e: KeyEvent, eventType: ClientRawKeyEvent.RawKeyEventType): ClientRawKeyEvent {
        return ClientRawKeyEvent((System.currentTimeMillis() - connectionTime).toInt(), e.keyCode, e.keyChar, e.modifiersEx, e.keyLocation, eventType)
      }

      override fun keyTyped(e: KeyEvent) {
        eventSink(convertKeyEvent(e, ClientRawKeyEvent.RawKeyEventType.TYPED))
      }

      override fun keyPressed(e: KeyEvent) {
        eventSink(convertKeyEvent(e, ClientRawKeyEvent.RawKeyEventType.DOWN))
      }

      override fun keyReleased(e: KeyEvent) {
        eventSink(convertKeyEvent(e, ClientRawKeyEvent.RawKeyEventType.UP))
      }
    }

    addMouseListener(mouseListener)
    addMouseMotionListener(mouseListener)
    addMouseWheelListener(mouseListener)

    frame.addKeyListener(keyboardListener)
  }
}
