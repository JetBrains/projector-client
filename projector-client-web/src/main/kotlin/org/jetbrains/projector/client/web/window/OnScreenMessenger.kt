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
package org.jetbrains.projector.client.web.window

import kotlinx.browser.document
import org.jetbrains.projector.client.common.canvas.Extensions.argbIntToRgbaString
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.misc.toDisplayType
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.common.protocol.data.CommonRectangle
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLParagraphElement

object OnScreenMessenger : LafListener {

  private val logger = Logger<OnScreenMessenger>()

  private val header = WindowHeader().apply {
    undecorated = true
    visible = false
  }

  private val div = (document.createElement("div") as HTMLDivElement).apply {
    style.apply {
      position = "fixed"
      zIndex = "567"

      // put to center:
      width = "400px"
      top = "50%"
      left = "50%"
      transform = "translate(-50%, -50%)"

      padding = "5px"
    }
  }

  private val text = (document.createElement("p") as HTMLParagraphElement).apply {
    div.appendChild(this)
  }

  private val reload = (document.createElement("div") as HTMLDivElement).apply {
    innerHTML = "<p>If you wish, you can try to <a onclick='location.reload();' href=''>reconnect</a>.</p>"

    div.appendChild(this)
  }

  init {
    lookAndFeelChanged()
  }

  fun showText(title: String, content: String, canReload: Boolean) {
    logger.info { "$title - $content" }

    header.title = title
    text.innerText = content

    reload.style.display = canReload.toDisplayType()

    if (div.parentElement == null) {
      document.body!!.appendChild(div)
    }

    updatePosition()
    header.visible = true
    header.zIndex = div.style.zIndex.toInt()
    header.draw()
  }

  fun updatePosition() {
    val userScalingRatio = ParamsProvider.USER_SCALING_RATIO
    val mainDivBounds = div.getBoundingClientRect()
    header.bounds = CommonRectangle(
      mainDivBounds.x * userScalingRatio,
      (mainDivBounds.y - ProjectorUI.headerHeight) * userScalingRatio,
      div.clientWidth * userScalingRatio,
      ProjectorUI.headerHeight * userScalingRatio
    )
  }

  fun hide() {
    if (div.parentElement != null) {
      div.remove()
    }
    header.visible = false
  }

  override fun lookAndFeelChanged() {
    header.lookAndFeelChanged()

    div.style.apply {
      backgroundColor = ProjectorUI.windowHeaderInactiveBackgroundArgb.argbIntToRgbaString()
      borderLeft = ProjectorUI.borderStyle
      borderRight = ProjectorUI.borderStyle
      borderBottom = ProjectorUI.borderStyle
      borderRadius = "0 0 ${ProjectorUI.borderRadius}px ${ProjectorUI.borderRadius}px"
      color = ProjectorUI.windowHeaderActiveTextArgb.argbIntToRgbaString()
    }
  }
}
