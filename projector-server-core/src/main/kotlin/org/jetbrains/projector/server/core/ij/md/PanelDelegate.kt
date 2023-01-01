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
package org.jetbrains.projector.server.core.ij.md

import org.jetbrains.projector.util.logging.Logger
import java.awt.Component
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class PanelDelegate(agentComponent: JComponent?) {

  private val logger = Logger<PanelDelegate>()

  val id = NEXT_ID.incrementAndGet()

  init {
    PanelUpdater.put(this)  // todo: handle the case when it's constructed but not used
  }

  var shown = false
    private set

  var width = 0
    private set

  var height = 0
    private set

  var x = 0
    private set

  var y = 0
    private set

  var lastChangedHtml = ""
    private set

  var rootComponent: Component? = null
    private set

  var lastCssString = ""
    private set

  var lastScrollOffset = 0
    private set

  private val headlessBackingComponent = PanelComponent()
  private val backingComponent = agentComponent ?: headlessBackingComponent

  private var lastInlineCss: String? = null
  private var lastCssFileUrls: List<String?> = emptyList()
  private var lastHtml: String = ""

  private var disposed = false

  // todo: to determine where our component is located, a thread is used. it's better to rework the logic
  private var helperThreadOnceLaunched = false
  private val helperThreadRunnableGetter: Runnable get() = helperThreadRunnable
  private val helperThreadRunnable = Runnable {
    Thread.sleep(1000)

    if (disposed) {
      return@Runnable
    }

    SwingUtilities.invokeLater {
      checkComponentMoved()
      checkComponentResized()
      checkComponentShown()
      checkRootChanged()

      Thread(helperThreadRunnableGetter).start()
    }
  }

  fun render() {
    if (!helperThreadOnceLaunched) {
      Thread(helperThreadRunnable).start()
    }

    val componentText = buildString {
      if (disposed) {
        appendLine("DISPOSED")
        appendLine()
      }

      appendLine("inlineCss:")
      appendLine(lastInlineCss)
      appendLine()

      appendLine("cssFileUrls:")
      appendLine(lastCssFileUrls.joinToString(separator = "\n"))
      appendLine()

      appendLine("html:")
      appendLine(lastHtml.replace(">", ">\n"))
    }

    headlessBackingComponent.setText(componentText)
  }

  fun setCSS(inlineCss: String?, vararg fileUris: String?) {
    lastInlineCss = inlineCss
    lastCssFileUrls = fileUris.toList()

    lastCssString = CssProcessor.makeCss(lastInlineCss, lastCssFileUrls)

    PanelUpdater.setCss(id)
  }

  fun setHtml(html: String) {
    try {
      var changedHtml = html

      if (html.isNotEmpty()) {
        changedHtml = LocalImagesInliner.inlineLocalImages(changedHtml)
      }

      lastChangedHtml = changedHtml
      lastHtml = html

      PanelUpdater.setHtml(id)
    }
    catch (t: Throwable) {
      logger.error { "Can't set HTML in Panel #$id..." }
    }
  }

  // added in JetBrains IDEs, version 2020.3+
  fun setHtml(html: String, initialScrollOffset: Int) {
    setHtml(html)
    scrollToMarkdownSrcOffset(initialScrollOffset)
  }

  fun getComponent(): JComponent {
    return backingComponent
  }

  fun scrollToMarkdownSrcOffset(offset: Int) {
    lastScrollOffset = offset

    PanelUpdater.scroll(id)
  }

  fun dispose() {
    disposed = true

    PanelUpdater.dispose(id)
  }

  private fun checkComponentMoved() {
    // todo: don't use location on screen but location in window and set location on client relative to window

    if (!backingComponent.isShowing) {
      return
    }

    backingComponent.locationOnScreen.let {
      if (x != it.x || y != it.y) {
        x = it.x
        y = it.y

        PanelUpdater.move(id)
      }
    }
  }

  private fun checkComponentResized() {
    backingComponent.size.let {
      if (width != it.width || height != it.height) {
        width = it.width
        height = it.height

        PanelUpdater.resize(id)
      }
    }
  }

  private fun checkComponentShown() {
    backingComponent.isShowing.let {
      if (it != shown) {
        shown = it

        PanelUpdater.show(id)
      }
    }
  }

  private fun checkRootChanged() {
    if (!backingComponent.isShowing) {
      return
    }

    SwingUtilities.getRoot(backingComponent).let {
      if (it != rootComponent) {
        rootComponent = it

        PanelUpdater.placeToWindow(id)
      }
    }
  }

  companion object {

    private val NEXT_ID = AtomicInteger()
  }
}
