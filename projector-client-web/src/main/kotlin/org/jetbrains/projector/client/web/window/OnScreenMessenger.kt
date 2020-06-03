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
package org.jetbrains.projector.client.web.window

import org.jetbrains.projector.client.common.misc.Logger
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document

object OnScreenMessenger {

  private val logger = Logger(OnScreenMessenger::class.simpleName!!)

  private val div = (document.createElement("div") as HTMLDivElement).apply {
    style.apply {
      position = "fixed"
      zIndex = "567"

      // put to center:
      width = "400px"
      top = "50%"
      left = "50%"
      transform = "translate(-50%, -50%)"

      backgroundColor = "#ddd"
      borderWidth = "1px"
      borderStyle = "solid"
      borderColor = "#222"
      borderRadius = "5px"

      padding = "5px"
    }
  }

  private val text = (document.createElement("div") as HTMLDivElement).apply {
    div.appendChild(this)
  }

  private val reload = (document.createElement("div") as HTMLDivElement).apply {
    innerHTML = "<p>If you wish, you can try to <a onclick='location.reload();' href=''>reconnect</a>.</p>"

    div.appendChild(this)
  }

  fun showText(header: String, content: String, canReload: Boolean) {
    logger.info { "$header - $content" }

    text.innerHTML = buildString {
      append("<h2>$header</h2>")
      append("<p>$content</p>")
    }

    reload.style.display = canReload.toDisplayType()

    if (div.parentElement == null) {
      document.body!!.appendChild(div)
    }
  }

  fun hide() {
    if (div.parentElement != null) {
      div.remove()
    }
  }
}
