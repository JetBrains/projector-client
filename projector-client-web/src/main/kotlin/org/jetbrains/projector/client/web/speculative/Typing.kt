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
package org.jetbrains.projector.client.web.speculative

import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.client.common.canvas.Extensions.toFontFaceName
import org.jetbrains.projector.client.common.misc.ParamsProvider.SCALING_RATIO
import org.jetbrains.projector.common.protocol.toClient.ServerCaretInfoChangedEvent
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

sealed class Typing {

  abstract fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange)

  abstract fun removeSpeculativeImage()

  abstract fun addEventChar(event: ClientKeyPressEvent): Boolean

  abstract fun dispose()

  object NotSpeculativeTyping : Typing() {

    override fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
      // do nothing
    }

    override fun removeSpeculativeImage() {
      // do nothing
    }

    override fun addEventChar(event: ClientKeyPressEvent): Boolean {
      return false
    }

    override fun dispose() {
      // do nothing
    }
  }

  class SpeculativeTyping(private val canvasByIdGetter: (Int) -> HTMLCanvasElement?) : Typing() {

    private val speculativeCanvasImage = (document.createElement("canvas") as HTMLCanvasElement).apply {
      style.apply {
        position = "fixed"
        display = "none"
      }

      innerHTML = ""

      document.body!!.appendChild(this)
    }

    private val speculativeCanvasContext = (speculativeCanvasImage.getContext("2d") as CanvasRenderingContext2D).apply {
      save()
    }

    private var carets: ServerCaretInfoChangedEvent.CaretInfoChange = ServerCaretInfoChangedEvent.CaretInfoChange.NoCarets

    private fun updateCanvas() {
      val caret = carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets

      val canvas = caret?.editorWindowId?.let { canvasByIdGetter(it) }

      if (canvas != null) {
        canvas.style.display = "block"

        ensureSpeculativeCanvasSize(canvas)

        speculativeCanvasContext.apply {
          restore()
          save()

          drawImage(canvas, 0.0, 0.0)
        }
      }

      speculativeCanvasImage.style.display = "none"
    }

    override fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
      carets = caretInfoChange
    }

    override fun removeSpeculativeImage() {
      updateCanvas()
    }

    /**
     * Skip drawing speculative symbol when we cannot be sure char will be actually drawn on the server.
     * Potentially invisible inputted chars:
     *
     * - Chars inputted with any of Ctrl/Alt/Meta modifiers (Example: Ctrl+C)
     * - Chars with Unicode category from "Other" group (Example: Esc or Meta)
     */
    internal fun shouldSkipEvent(event: ClientKeyPressEvent): Boolean {
      return KeyModifier.CTRL_KEY in event.modifiers
             || KeyModifier.ALT_KEY in event.modifiers
             || KeyModifier.META_KEY in event.modifiers
             || event.char.category.fromOtherUnicodeGroup
    }

    override fun addEventChar(event: ClientKeyPressEvent): Boolean {
      if (shouldSkipEvent(event)) return false

      val currentCarets = carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets ?: return false

      val canvas = canvasByIdGetter(currentCarets.editorWindowId) ?: return false

      val firstCaretLocation = currentCarets.caretInfoList.firstOrNull()?.locationInWindow ?: return false  // todo: support multiple carets

      ensureSpeculativeCanvasSize(canvas)

      speculativeCanvasContext.apply {
        restore()
        save()

        SCALING_RATIO.let { scale(it, it) }

        val editorMetrics = currentCarets.editorMetrics
        beginPath()
        rect(
          x = editorMetrics.x,
          y = editorMetrics.y,
          w = editorMetrics.width,
          h = editorMetrics.height
        )
        clip()

        val fontFace = currentCarets.fontId?.toFontFaceName() ?: "Arial"
        val fontSize = "${currentCarets.fontSize}px"

        font = "$fontSize $fontFace"

        val speculativeCharWidth = measureText(event.char.toString()).width

        val widthToMove = editorMetrics.width - (firstCaretLocation.x - editorMetrics.x) - currentCarets.verticalScrollBarWidth - speculativeCharWidth

        if (widthToMove > 0) {
          val imageData = getImageData(
            sx = firstCaretLocation.x,
            sy = firstCaretLocation.y,
            sw = widthToMove,
            sh = currentCarets.lineHeight.toDouble()
          )

          putImageData(imageData, firstCaretLocation.x + speculativeCharWidth, firstCaretLocation.y)
        }

        val stringYPos = firstCaretLocation.y + currentCarets.lineAscent

        fillStyle = currentCarets.backgroundColor.argbIntToRgbaString()
        fillRect(firstCaretLocation.x, firstCaretLocation.y, speculativeCharWidth, currentCarets.lineHeight.toDouble())

        fillStyle = currentCarets.textColor.argbIntToRgbaString()
        fillText(event.char.toString(), firstCaretLocation.x, stringYPos)
      }

      speculativeCanvasImage.style.display = "block"
      canvas.style.display = "none"

      return true
    }

    private fun ensureSpeculativeCanvasSize(canvas: HTMLCanvasElement) {
      canvas.style.let { canvasStyle ->
        speculativeCanvasImage.style.let { speculativeStyle ->
          speculativeStyle.left = canvasStyle.left
          speculativeStyle.top = canvasStyle.top
          speculativeStyle.width = canvasStyle.width
          speculativeStyle.height = canvasStyle.height
          speculativeStyle.zIndex = canvasStyle.zIndex
        }
      }

      if (speculativeCanvasImage.width != canvas.width || speculativeCanvasImage.height != canvas.height) {
        speculativeCanvasImage.width = canvas.width
        speculativeCanvasImage.height = canvas.height
      }
    }

    override fun dispose() {
      speculativeCanvasImage.remove()
    }
  }
}
