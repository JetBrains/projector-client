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

import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.projector.client.web.misc.isDefined
import org.jetbrains.projector.client.web.misc.isElectron
import org.jetbrains.projector.client.web.misc.isGecko
import org.jetbrains.projector.common.protocol.toServer.ClientNotificationEvent
import org.jetbrains.projector.common.protocol.toServer.ClientNotificationType
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.HTMLTextAreaElement

/**
 * First try to copy via `window.navigator.clipboard`. If this method fails or is not available,
 * then try to copy via `document.execCommand` (deprecated, but still supported by firefox).
 * If this also fails, then fallback to asking user to copy manually.
 */
class ClipboardHandler(private val onCopyFailed: (ClientNotificationEvent) -> Unit) {

  private val logger = Logger<ClipboardHandler>()

  fun copyText(text: String) {
    if (isDefined(window.navigator.clipboard)) {
      window.navigator.clipboard.writeText(text)
        .catch {
          logger.error(it) { "Error writing clipboard: $it" }
          fallbackCopy(text, it.message ?: "Unknown error")
        }
    }
    else {
      fallbackCopy(text, "Clipboard API is not available")
    }
  }

  private fun fallbackCopy(textToCopy: String, reason: String) {
    val execCommandReason = tryCopyTextViaExecCommand(textToCopy)
    if (execCommandReason != null) {
      askUserToCopyClipboardManually(textToCopy, reason, execCommandReason)
    }
  }

  /**
   * Tries to automatically copy text to clipboard via Document.execCommand
   *
   * @return null if copy successful, string describing fail reason otherwise
   */
  private fun tryCopyTextViaExecCommand(textToCopy: String): String? {

    when {
      !isDefined(js("document.execCommand")) -> return "Document.execCommand is not available"

      // Chrome only allows Document.execCommand from event callbacks
      !isGecko() -> return "Copying using Document.execCommand works only in Firefox"
    }

    val textArea = document.createElement("textarea") as HTMLTextAreaElement
    textArea.id = "copyArea"
    textArea.value = textToCopy
    textArea.style.display = "hidden"

    document.body!!.appendChild(textArea)

    textArea.focus()
    textArea.select()

    var isCopied = false
    try {
      isCopied = document.execCommand("copy")
    }
    catch (t: Throwable) { // May throw SecurityError https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#the-copy-command
      logger.error(t) { "Error while running 'document.execCommand(\"copy\")'" }
    }
    finally {
      textArea.remove()
    }

    return if (isCopied) null else "Document.execCommand failed, check console for info from browser"
  }

  private fun askUserToCopyClipboardManually(textToCopy: String, vararg reasons: String) {
    val message = "A clipboard change on the server detected but can't synchronize your clipboard with it automatically " +
                  "(reasons: ${reasons.joinToString("; ")}). Please copy text on next line manually:"

    if (isElectron()) { // TODO window.prompt is not available in electron
      val title = "Copying failed"
      val notificationMessage = "Copying is not currently supported in insecure context in Projector launcher"
      val type = ClientNotificationType.ERROR
      onCopyFailed(ClientNotificationEvent(title, notificationMessage, type))
    }
    else {
      window.prompt(message, textToCopy)
    }
  }

}
