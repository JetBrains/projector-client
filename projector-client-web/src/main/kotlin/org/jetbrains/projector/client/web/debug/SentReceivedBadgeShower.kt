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
package org.jetbrains.projector.client.web.debug

import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.projector.client.web.externalDeclarartion.pointerEvents
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement

interface SentReceivedBadgeShower {

  fun onHandshakeFinished()

  fun onToClientMessage()

  fun onToServerMessage()
}

object NoSentReceivedBadgeShower : SentReceivedBadgeShower {

  override fun onHandshakeFinished() {}

  override fun onToClientMessage() {}

  override fun onToServerMessage() {}
}

class DivSentReceivedBadgeShower : SentReceivedBadgeShower {

  override fun onHandshakeFinished() {
    (document.createElement("span") as HTMLSpanElement).apply {
      style.position = "fixed"
      style.bottom = "30px"
      style.right = "5%"
      style.zIndex = "1"
      style.pointerEvents = "none"

      style.padding = "5px"
      style.background = DEFAULT_SENT_BACKGROUND
      style.display = "none"
      id = SENT_ID

      innerText = "sent"

      document.body!!.appendChild(this)
    }

    (document.createElement("span") as HTMLSpanElement).apply {
      style.position = "fixed"
      style.bottom = "10px"
      style.right = "5%"
      style.zIndex = "1"
      style.pointerEvents = "none"

      style.padding = "5px"
      style.background = DEFAULT_RECEIVED_BACKGROUND
      style.display = "none"
      id = RECEIVED_ID

      innerText = "received"

      document.body!!.appendChild(this)
    }
  }

  override fun onToClientMessage() {
    val element = (document.getElementById(RECEIVED_ID) as HTMLElement?) ?: return
    element.style.display = "block"

    window.setTimeout({ element.style.display = "none" }, DEFAULT_SHOWING_TIMEOUT)
  }

  override fun onToServerMessage() {
    val element = (document.getElementById(SENT_ID) as HTMLElement?) ?: return
    element.style.display = "block"

    window.setTimeout({ element.style.display = "none" }, DEFAULT_SHOWING_TIMEOUT)
  }

  companion object {

    private const val DEFAULT_SENT_BACKGROUND = "rgba(225, 25, 25, 0.5)"
    private const val DEFAULT_RECEIVED_BACKGROUND = "rgba(25, 25, 225, 0.5)"
    private const val DEFAULT_SHOWING_TIMEOUT = 20

    private const val SENT_ID = "sent"
    private const val RECEIVED_ID = "received"
  }
}
