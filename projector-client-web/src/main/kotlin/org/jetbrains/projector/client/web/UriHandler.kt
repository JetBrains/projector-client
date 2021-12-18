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
package org.jetbrains.projector.client.web

import kotlinx.browser.window
import org.jetbrains.projector.client.web.misc.isElectron
import org.w3c.dom.url.URL

object UriHandler {

  fun browse(link: String) {
    val url = URL(link)

    if(url.hostname == "localhost" || url.hostname == "127.0.0.1" || url.host == "::1") {
      url.hostname = window.location.hostname
      url.protocol = window.location.protocol
    }

    if (isElectron()) { // electron doesn't block opening url
      window.open(url.href, "_blank")
      return
    }

    // local file cannot be opened in browser
    if (url.protocol == "file:") return // TODO: handle opening of a file. Easier to do this on the server side

    val popUpWindow = window.open(url.href, "_blank")

    if (popUpWindow != null) {
      popUpWindow.focus()  // browser has allowed it to be opened
    } else {
      window.alert("To open $link, please allow popups for this website")  // browser has blocked it
    }
  }

}
