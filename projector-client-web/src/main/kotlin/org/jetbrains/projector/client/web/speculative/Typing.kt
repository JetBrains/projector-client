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
import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.client.common.canvas.Extensions.toFontFaceName
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.ParamsProvider.SCALING_RATIO
import org.jetbrains.projector.common.protocol.data.Point
import org.jetbrains.projector.common.protocol.toClient.ServerCaretInfoChangedEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawStringEvent
import org.jetbrains.projector.common.protocol.toClient.data.idea.CaretInfo
import org.jetbrains.projector.common.protocol.toClient.data.idea.SelectionInfo
import org.jetbrains.projector.common.protocol.toServer.ClientKeyPressEvent
import org.jetbrains.projector.common.protocol.toServer.KeyModifier
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

sealed class Typing {

  abstract fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange)

  abstract fun removeSpeculativeImage()

  abstract fun onDrawString(stringEvent: ServerDrawStringEvent, offset: Point)

  abstract fun onSymbolValidated(requestId: Int)

  abstract fun addEventChar(event: ClientKeyPressEvent) : DrawingResult

  abstract fun dispose()

  sealed class DrawingResult {

    object Skip : DrawingResult()

    data class Drawn(val operationId: Int, val editorId: Int, val virtualOffset: Int, val selection: SelectionInfo?): DrawingResult()

  }

  object NotSpeculativeTyping : Typing() {

    override fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
      // do nothing
    }

    override fun removeSpeculativeImage() {
      // do nothing
    }

    override fun onDrawString(stringEvent: ServerDrawStringEvent, offset: Point) {
      // do nothing
    }

    override fun onSymbolValidated(requestId: Int) {
      // do nothing
    }

    override fun addEventChar(event: ClientKeyPressEvent) = DrawingResult.Skip

    override fun dispose() {
      // do nothing
    }
  }

  class SpeculativeTyping(private val canvasByIdGetter: (Int) -> HTMLCanvasElement?) : Typing() {

    private val speculativeCanvasImage = (document.createElement("canvas") as HTMLCanvasElement).apply {
      id = "speculativeCanvas"
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

    private val CaretInfo.isVisibleInEditor: Boolean
      get() = locationInWindow.x >= 0 && locationInWindow.y >= 0

    private var carets: ServerCaretInfoChangedEvent.CaretInfoChange = ServerCaretInfoChangedEvent.CaretInfoChange.NoCarets

    private var drawnSpeculativeSymbols = mutableMapOf<Int, DrawnSymbol>()

    private var lastId = 0

    private fun updateCanvas() {
      val caret = carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets

      val canvas = caret?.editorWindowId?.let { canvasByIdGetter(it) }

      if (canvas != null && ParamsProvider.HIDE_MAIN_CANVAS_ON_SPECULATIVE_TYPING) {
        canvas.style.display = "block"
      }

      speculativeCanvasImage.style.display = "none"
    }

    override fun changeCaretInfo(caretInfoChange: ServerCaretInfoChangedEvent.CaretInfoChange) {
      val oldCarets = carets
      if (caretInfoChange is ServerCaretInfoChangedEvent.CaretInfoChange.Carets
          && oldCarets is ServerCaretInfoChangedEvent.CaretInfoChange.Carets
          && caretInfoChange.editorScrolled != oldCarets.editorScrolled
      ) {
        removeSpeculativeImage()
      }
      carets = caretInfoChange
    }

    override fun removeSpeculativeImage() {
      if (drawnSpeculativeSymbols.isNotEmpty()) {
        drawnSpeculativeSymbols.clear()
        updateCanvas()
      }
    }

    /**
     * Checks that symbol is located at event right bound
     */
    private fun isSymbolAtEventEnd(symbol: DrawnSymbol, stringEvent: ServerDrawStringEvent, eventOffset: Point): Boolean {
      val currentCarets = (carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets) ?: return false

      val shiftToLineTop = currentCarets.lineAscent
      val totalVerticalPaintOffset = stringEvent.y + eventOffset.y - shiftToLineTop

      val offsetInEditor = totalVerticalPaintOffset - currentCarets.editorMetrics.y

      val symbolShiftX = symbol.carets.run { editorMetrics.x - editorScrolled.x }
      val symbolShiftY = symbol.carets.run { editorMetrics.y - editorScrolled.y }

      // event rectangle
      val topLeftX1 = stringEvent.x
      val topLeftY1 = offsetInEditor + currentCarets.editorScrolled.y
      val bottomRightX1 = topLeftX1 + stringEvent.desiredWidth
      val bottomRightY1 = topLeftY1 + currentCarets.lineHeight

      // speculative symbol rectangle
      val topLeftX2 = symbol.x - symbolShiftX
      val topLeftY2 = symbol.y - symbolShiftY
      val bottomRightX2 = topLeftX2 + symbol.width
      val bottomRightY2 = topLeftY2 + symbol.height

      return bottomRightX1 == bottomRightX2 && topLeftY1 == topLeftY2 && bottomRightY1 == bottomRightY2
    }

    private fun removeSymbolIfNeeded(symbol: DrawnSymbol, stringEvent: ServerDrawStringEvent, eventOffset: Point): Boolean {
      if (!isSymbolAtEventEnd(symbol, stringEvent, eventOffset)) return false
      symbol.removeFromCanvas()
      return true
    }

    private fun clearSpeculativeCanvas() {
      speculativeCanvasContext.clearRect(0.0, 0.0, speculativeCanvasImage.width.toDouble(), speculativeCanvasImage.height.toDouble())
      updateCanvas()
    }

    private fun clearSpeculativeCanvasIfNeeded() {
      if (drawnSpeculativeSymbols.isEmpty()) {
        clearSpeculativeCanvas()
      }
    }

    override fun onDrawString(stringEvent: ServerDrawStringEvent, offset: Point) {
      if (drawnSpeculativeSymbols.isNotEmpty()) {
        val notRemoved = mutableSetOf<Int>() // skipped ids
        val toRemove = mutableSetOf<Int>() // ids' of chars that should be removed

        drawnSpeculativeSymbols.entries.forEach { (id, symbol) ->
          val removed = removeSymbolIfNeeded(symbol, stringEvent, offset)
          if (removed) {
            // remove all previously skipped chars
            notRemoved.removeAll { notRemovedId ->
              drawnSpeculativeSymbols[notRemovedId]?.removeFromCanvas()
              toRemove.add(notRemovedId)
            }
            toRemove += id
          } else {
            notRemoved += id
          }
        }

        toRemove.forEach { drawnSpeculativeSymbols.remove(it) }

        clearSpeculativeCanvasIfNeeded()
      }
    }

    override fun onSymbolValidated(requestId: Int) {
      val symbol = drawnSpeculativeSymbols.remove(requestId) ?: return
      symbol.removeFromCanvas()
      clearSpeculativeCanvasIfNeeded()
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

    override fun addEventChar(event: ClientKeyPressEvent): DrawingResult {
      if (shouldSkipEvent(event)) return DrawingResult.Skip

      val currentCarets = carets as? ServerCaretInfoChangedEvent.CaretInfoChange.Carets ?: return DrawingResult.Skip

      val canvas = canvasByIdGetter(currentCarets.editorWindowId) ?: return DrawingResult.Skip

      val serverFirstCaretInfo = currentCarets.caretInfoList.firstOrNull() ?: return DrawingResult.Skip  // todo: support multiple carets

      val paintOffset = drawnSpeculativeSymbols.values.sumOf { it.width }
      val firstCaretInfo = serverFirstCaretInfo.copy(
        locationInWindow = serverFirstCaretInfo.locationInWindow.copy(
          x = serverFirstCaretInfo.locationInWindow.x + paintOffset
        )
      )

      if (!firstCaretInfo.isVisibleInEditor) return DrawingResult.Skip

      val firstCaretLocation = firstCaretInfo.locationInWindow

      ensureSpeculativeCanvasSize(canvas)

      val newId = lastId + 1
      val virtualOffset = firstCaretInfo.offset + drawnSpeculativeSymbols.size
      val virtualSelection = if (drawnSpeculativeSymbols.isEmpty()) firstCaretInfo.selection else null

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

        drawnSpeculativeSymbols[newId] = DrawnSymbol(firstCaretLocation.x, firstCaretLocation.y,
                                                     speculativeCharWidth, currentCarets.lineHeight.toDouble(),
                                                     currentCarets)
      }

      speculativeCanvasImage.style.display = "block"
      if (ParamsProvider.HIDE_MAIN_CANVAS_ON_SPECULATIVE_TYPING) {
        canvas.style.display = "none"
      }

      lastId = newId

      return DrawingResult.Drawn(newId, currentCarets.editorId, virtualOffset, virtualSelection)
    }

    private fun ensureSpeculativeCanvasSize(canvas: HTMLCanvasElement) {
      canvas.style.let { canvasStyle ->
        speculativeCanvasImage.style.let { speculativeStyle ->
          speculativeStyle.left = canvasStyle.left
          speculativeStyle.top = canvasStyle.top
          speculativeStyle.width = canvasStyle.width
          speculativeStyle.height = canvasStyle.height
          speculativeStyle.zIndex = canvasStyle.zIndex.let zTransform@ {
            if (ParamsProvider.HIDE_MAIN_CANVAS_ON_SPECULATIVE_TYPING) return@zTransform it
            (it.toInt() + 1).toString()
          }
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

    private data class DrawnSymbol(
      val x: Double, val y: Double,
      val width: Double, val height: Double,
      val carets: ServerCaretInfoChangedEvent.CaretInfoChange.Carets,
    )

    private fun DrawnSymbol.removeFromCanvas() {
      speculativeCanvasContext.clearRect(x, y, width, height)
    }
  }
}
