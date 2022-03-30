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
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.events.MouseEvent

class EmbeddedBrowserManager(
  zIndexByWindowIdGetter: (Int) -> Int?,
  private val openInExternalBrowser: (String) -> Unit,
) : ClientComponentManager<EmbeddedBrowserManager.EmbeddedBrowserPanel>(zIndexByWindowIdGetter) {

  class EmbeddedBrowserPanel(id: Int, private val openInExternalBrowser: (String) -> Unit) : ClientComponent(id) {

    var wasLoaded = false

    private var openLinksInExternalBrowser: Boolean = false

    private var previousClickHandler: ((MouseEvent) -> dynamic)? = null

    private val jsQueue = ArrayDeque<String>()

    init {
      iFrame.apply {
        onload = {
          setOpenLinksInExternalBrowser(openLinksInExternalBrowser, true)

          wasLoaded = true
          while (jsQueue.isNotEmpty()) {
            val code = jsQueue.removeFirst()
            executeJsImpl(code)
          }
        }
      }
    }

    fun setHtml(html: String) {
      iFrame.srcdoc = html
    }

    fun executeJs(code: String) {
      if (wasLoaded) {
        executeJsImpl(code)
      }
      else {
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

      setLinkProcessor(openInExternalBrowser)
    }

    fun setOpenLinksInExternalBrowser(openLinksInExternalBrowser: Boolean) = setOpenLinksInExternalBrowser(openLinksInExternalBrowser,
                                                                                                           false)
  }

  fun setOpenLinksInExternalBrowser(browserId: Int, openLinksInExternalBrowser: Boolean) {
    val panel = getOrCreate(browserId)
    panel.setOpenLinksInExternalBrowser(openLinksInExternalBrowser)
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

  override fun createComponent(componentId: Int) = EmbeddedBrowserPanel(componentId, openInExternalBrowser)

}
