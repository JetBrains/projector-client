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
package org.jetbrains.projector.client.web.component

import kotlinx.browser.document
import org.jetbrains.projector.client.web.UriHandler
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.CommonIntSize
import org.jetbrains.projector.common.protocol.data.Point
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent

class EmbeddedBrowserManager(private val zIndexByWindowIdGetter: (Int) -> Int?, private val openInExternalBrowser: (String) -> Unit) {

  private class EmbeddedBrowserPanel(private val id: Int, private val openInExternalBrowser: (String) -> Unit) {

    init {
      console.log("createPanel id: $id")
    }

    var openLinksInExternalBrowser: Boolean = false
      private set

    val iFrame: HTMLIFrameElement = createIFrame(id)

    var windowId: Int? = null

    private var previousClickHandler: ((MouseEvent) -> dynamic)? = null

    fun dispose() {
      iFrame.remove()
    }

    var wasLoaded = false

    private val jsQueue = ArrayDeque<String>()

    fun setHtml(html: String) {
      iFrame.srcdoc = html
    }

    fun executeJs(code: String) {
      if (wasLoaded) {
        executeJsImpl(code)
      } else {
        jsQueue.addLast(code)
      }
    }

    private fun executeJsImpl(code: String) {
      val elem = document.createElement("script") as HTMLScriptElement

      elem.type = "text/javascript"
      elem.text = code

      iFrame.contentDocument!!.head!!.append(elem)
    }

    private fun setOpenLinksInExternalBrowser(openLinksInExternalBrowser: Boolean, forceIfTrue: Boolean) {
      val changed = openLinksInExternalBrowser != this.openLinksInExternalBrowser
      if (!(changed || forceIfTrue)) {
        return
      }
      this.openLinksInExternalBrowser = openLinksInExternalBrowser
      if (!openLinksInExternalBrowser) {
        if (changed) {
          iFrame.contentDocument?.onclick = previousClickHandler
        }
        return
      }

      if (changed) {
        previousClickHandler = iFrame.contentDocument?.onclick
      }

      iFrame.contentDocument?.onclick = { e ->
        var target = e.target.asDynamic()
        while (target != null && target.tagName != "A") {
          target = target.parentNode
        }

        if (target == null) {
          true
        }
        else if (target.tagName == "A" && target.hasAttribute("href").unsafeCast<Boolean>()) {
          e.stopPropagation()

          val href = target.getAttribute("href").unsafeCast<String>()
          if (href[0] == '#') {
            val elementId = href.substring(1)
            iFrame.contentDocument?.getElementById(elementId)?.scrollIntoView()
          }
          else {
            openInExternalBrowser(href)
          }

          false
        }
        else {
          null
        }
      }
    }

    fun setOpenLinksInExternalBrowser(openLinksInExternalBrowser: Boolean) = setOpenLinksInExternalBrowser(openLinksInExternalBrowser, false)

    private fun createIFrame(browserId: Int) = (document.createElement("iframe") as HTMLIFrameElement).apply {
      id = getIFrameId(browserId)
      style.apply {
        position = "fixed"
        backgroundColor = "#FFF"
        overflowX = "scroll"
        overflowY = "scroll"
        display = "none"
      }

      frameBorder = "0"

      document.body!!.appendChild(this)

      // cancel auto-started load of about:blank in Firefox
      // https://stackoverflow.com/questions/7828502/cannot-set-document-body-innerhtml-of-iframe-in-firefox
      contentDocument!!.apply {
        open()
        close()
      }

      contentDocument!!.oncontextmenu = { false }

      onload = {
        setOpenLinksInExternalBrowser(openLinksInExternalBrowser, true)

        wasLoaded = true
        while (jsQueue.isNotEmpty()) {
          val code = jsQueue.removeFirst()
          executeJsImpl(code)
        }
      }
    }

    companion object {

      internal fun getIFrameId(browserId: Int) = "${EmbeddedBrowserPanel::class.simpleName}$browserId"
    }
  }

  private val idToPanel = mutableMapOf<Int, EmbeddedBrowserPanel>()

  private fun getOrCreate(browserId: Int): EmbeddedBrowserPanel {
    return idToPanel.getOrPut(browserId) { EmbeddedBrowserPanel(browserId, openInExternalBrowser) }
  }

  private fun placeToWindow(browserId: Int, windowId: Int) {
    val panel = getOrCreate(browserId)

    panel.windowId = windowId

    val zIndex = zIndexByWindowIdGetter(windowId) ?: return

    panel.iFrame.style.zIndex = (zIndex + 1).toString()
  }

  fun updatePlacements() {
    idToPanel.forEach { (browserId, panel) ->
      panel.windowId?.let { placeToWindow(browserId, it) }
    }
  }

  fun show(browserId: Int, show: Boolean, windowId: Int?) {
    val panel = getOrCreate(browserId)

    Do exhaustive when (show) {
      true -> {
        windowId?.also { placeToWindow(browserId, it) }
        panel.iFrame.style.display = "block"
      }

      false -> panel.iFrame.style.display = "none"
    }
  }

  fun move(browserId: Int, point: Point) {
    val panel = getOrCreate(browserId)

    panel.iFrame.style.apply {
      left = "${point.x}px"
      top = "${point.y}px"
    }
  }

  fun resize(browserId: Int, size: CommonIntSize) {
    val panel = getOrCreate(browserId)

    panel.iFrame.style.apply {
      width = "${size.width}px"
      height = "${size.height}px"
    }
  }

  fun setOpenLinksInExternalBrowser(browserId: Int, openLinksInExternalBrowser: Boolean) {
    val panel = getOrCreate(browserId)
    panel.setOpenLinksInExternalBrowser(openLinksInExternalBrowser)
  }

  fun dispose(browserId: Int) {
    val panel = getOrCreate(browserId)

    panel.dispose()

    idToPanel.remove(browserId)
  }

  fun disposeAll() {
    idToPanel.values.forEach { it.dispose() }

    idToPanel.clear()
  }

  fun setHtml(browserId: Int, html: String) {
    val panel = getOrCreate(browserId)

    panel.wasLoaded = false
    panel.setHtml(html)
  }

  fun loadUrl(browserId: Int, url: String, show: Boolean) {
    val panel = getOrCreate(browserId)

    panel.setOpenLinksInExternalBrowser(true)

    // TODO add styles corresponding to IDE ones
    panel.setHtml(
      """
        <html>
          <head>
          </head>
          <body>
            <p>Following non-localhost link was opened in new window:</p>
            <a href="$url">$url</a>
          </body>
        </html>
      """.trimIndent()
    )

    if (show) {
      UriHandler.browse(url)
    }
  }

  fun executeJs(browserId: Int, code: String) {
    val panel = getOrCreate(browserId)

    panel.executeJs(code)
  }

}
