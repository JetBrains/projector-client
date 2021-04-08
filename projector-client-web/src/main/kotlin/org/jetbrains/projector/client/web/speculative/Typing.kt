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
package org.jetbrains.projector.client.web.speculative

import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Extensions
import org.jetbrains.projector.client.common.misc.ParamsProvider.SCALING_RATIO
import org.jetbrains.projector.common.protocol.toClient.ServerCaretInfoChangedEvent
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

sealed class Typing {

  abstract fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange)

  abstract fun removeSpeculativeImage()

  abstract fun addChar(char: Char)

  abstract fun dispose()

  object NotSpeculativeTyping : Typing() {

    override fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
      // do nothing
    }

    override fun removeSpeculativeImage() {
      // do nothing
    }

    override fun addChar(char: Char) {
      // do nothing
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

    override fun addChar(char: Char) {
      val currentCarets = carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets ?: return

      val canvas = canvasByIdGetter(currentCarets.editorWindowId) ?: return

      val firstCaretLocation = currentCarets.caretInfoList.firstOrNull()?.locationInWindow ?: return  // todo: support multiple carets

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
          w = editorMetrics.width - 10,  // todo: -10 to prevent scrollbar be repainted
          h = editorMetrics.height
        )
        clip()

        val imageData = getImageData(
          sx = firstCaretLocation.x,
          sy = firstCaretLocation.y,
          sw = editorMetrics.width - (firstCaretLocation.x - editorMetrics.x),
          sh = currentCarets.nominalLineHeight.toDouble() + 2 + 2
        )

        putImageData(imageData, firstCaretLocation.x + currentCarets.plainSpaceWidth, firstCaretLocation.y)

        val fontFace = Extensions.serverFontNameCache[currentCarets.fontId?.unsafeCast<Int>()?: 0]
        val fontSize = "${currentCarets.fontSize}px"

        font = "$fontSize $fontFace"  // todo: use a proper font style
        fillStyle = "#888"  // todo: use a proper color

        val stringYPos = firstCaretLocation.y + currentCarets.fontSize  // todo: offsetting by fontSize seems to be not exact
        fillText(char.toString(), firstCaretLocation.x, stringYPos)
      }

      speculativeCanvasImage.style.display = "block"
      canvas.style.display = "none"
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
