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
import org.w3c.dom.HTMLIFrameElement

abstract class ClientComponent(
  protected val id: Int,
) {

  val iFrame: HTMLIFrameElement = createIFrame(id)

  var windowId: Int? = null

  fun dispose() {
    iFrame.remove()
  }

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
  }

  protected fun setLinkProcessor(linkProcessor: (String) -> Unit) {
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
          linkProcessor(href)
        }

        false
      }
      else {
        null
      }
    }
  }

  private fun getIFrameId(browserId: Int) = "${this::class.simpleName}$browserId"

}
