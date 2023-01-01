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
package org.jetbrains.projector.client.web

import kotlinx.browser.window
import org.jetbrains.projector.client.web.misc.isElectron
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.url.URL

object UriHandler {

  private val logger = Logger<UriHandler>()

  fun browse(link: String) {
    val url = URL(link)

    // ensure only save URLs are opened, for example, avoid opening executable files
    if (url.protocol !in setOf("http:", "https:")) {
      // TODO: handle opening of "file:" links on the server-side, to ensure feature-parity with IDEA
      logger.error { "Opening $link is avoided (opening non-http(s) links is unsupported)" }
      return
    }

    if (url.hostname == "localhost" || url.hostname == "127.0.0.1" || url.host == "::1") {
      url.hostname = window.location.hostname
      url.protocol = window.location.protocol
    }

    if (isElectron()) { // electron doesn't block opening url
      window.open(url.href, "_blank")
      return
    }

    val popUpWindow = window.open(url.href, "_blank")

    if (popUpWindow != null) {
      popUpWindow.focus()  // browser has allowed it to be opened
    } else {
      window.alert("To open $link, please allow popups for this website")  // browser has blocked it
    }
  }

}
