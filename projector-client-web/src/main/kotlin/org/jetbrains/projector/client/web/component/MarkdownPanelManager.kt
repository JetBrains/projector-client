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
package org.jetbrains.projector.client.web.component

import kotlinx.browser.document
import kotlinx.dom.clear
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.CommonIntSize
import org.jetbrains.projector.common.protocol.data.Point
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener
import org.w3c.dom.get
import org.w3c.dom.parsing.DOMParser
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.absoluteValue

class MarkdownPanelManager(private val zIndexByWindowIdGetter: (Int) -> Int?, private val openInExternalBrowser: (String) -> Unit) {

  private class MarkdownPanel(openInExternalBrowser: (String) -> Unit) {

    private val documentListeners = mutableMapOf<String, EventListener>()

    val iFrame: HTMLIFrameElement = createIFrame(openInExternalBrowser)

    var windowId: Int? = null

    fun dispose() {
      documentListeners.forEach { document.removeEventListener(it.key, it.value) }
      iFrame.remove()
    }

    private fun createIFrame(openInExternalBrowser: (String) -> Unit) = (document.createElement("iframe") as HTMLIFrameElement).apply {
      sandbox.value = "allow-same-origin"

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

      documentListeners["mousedown"] = EventListener {
        style.asDynamic().pointerEvents = "none"
      }
      documentListeners["mouseup"] = EventListener {
        style.asDynamic().pointerEvents = "auto"
      }

      documentListeners.forEach { document.addEventListener(it.key, it.value) }

      // adopted from processLinks.js
      contentDocument!!.onclick = { e ->
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
            contentDocument!!.getElementById(elementId)?.scrollIntoView()
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
  }

  private val idToPanel = mutableMapOf<Int, MarkdownPanel>()

  private fun getOrCreate(markdownPanelId: Int): MarkdownPanel {
    return idToPanel.getOrPut(markdownPanelId) { MarkdownPanel(openInExternalBrowser) }
  }

  fun placeToWindow(markdownPanelId: Int, windowId: Int) {
    val panel = getOrCreate(markdownPanelId)

    panel.windowId = windowId

    val zIndex = zIndexByWindowIdGetter(windowId) ?: return

    panel.iFrame.style.zIndex = (zIndex + 1).toString()
  }

  fun updatePlacements() {
    idToPanel.forEach { (markdownPanelId, panel) ->
      panel.windowId?.let { placeToWindow(markdownPanelId, it) }
    }
  }

  fun show(markdownPanelId: Int, show: Boolean) {
    val panel = getOrCreate(markdownPanelId)

    Do exhaustive when (show) {
      true -> panel.iFrame.style.display = "block"

      false -> panel.iFrame.style.display = "none"
    }
  }

  fun move(markdownPanelId: Int, point: Point) {
    val panel = getOrCreate(markdownPanelId)

    panel.iFrame.style.apply {
      left = "${point.x}px"
      top = "${point.y}px"
    }
  }

  fun resize(markdownPanelId: Int, size: CommonIntSize) {
    val panel = getOrCreate(markdownPanelId)

    panel.iFrame.style.apply {
      width = "${size.width}px"
      height = "${size.height}px"
    }
  }

  fun dispose(markdownPanelId: Int) {
    val panel = getOrCreate(markdownPanelId)

    panel.dispose()

    idToPanel.remove(markdownPanelId)
  }

  fun disposeAll() {
    idToPanel.values.forEach { it.dispose() }

    idToPanel.clear()
  }

  fun setHtml(markdownPanelId: Int, html: String) {
    val panel = getOrCreate(markdownPanelId)

    panel.iFrame.contentDocument!!.apply {
      body!!.clear()

      if (html.isBlank()) {
        return
      }

      val parsedContent = domParser.parseFromString(html, "text/html")
      body = parsedContent.body
    }
  }

  fun setCss(markdownPanelId: Int, css: String) {
    val panel = getOrCreate(markdownPanelId)

    val style = panel.iFrame.contentDocument!!.head!!.getElementsByTagName("style").let {
      when (it.length) {
        0 -> panel.iFrame.contentDocument!!.createElement("style").apply {
          asDynamic().type = "text/css"

          panel.iFrame.contentDocument!!.head!!.appendChild(this)
        }

        else -> it[0]!!
      }
    }

    style.apply {
      clear()
      appendChild(panel.iFrame.contentDocument!!.createTextNode(css))
    }
  }

  fun scroll(markdownPanelId: Int, scrollOffset: Int) {
    val panel = getOrCreate(markdownPanelId)

    try {
      Scroller.scrollToSrcOffset(panel.iFrame, scrollOffset, SRC_ATTRIBUTE_NAME)
    }
    catch (t: Throwable) {
      logger.error(t) { "Can't scroll" }
    }
  }

  companion object {

    private val logger = Logger<MarkdownPanelManager>()

    private val domParser = DOMParser()

    private const val SRC_ATTRIBUTE_NAME = "md-src-pos"  // todo: send this constant from server: HtmlGenerator.SRC_ATTRIBUTE_NAME
  }

  // adopted from scrollToElement.js
  private object Scroller {

    private fun getSrcFromTo(attributeName: String, node: Node?): Array<Int>? {
      if (node == null || !node.asDynamic().getAttribute) {
        return null
      }

      val attrValue = node.asDynamic().getAttribute(attributeName) ?: return null

      return attrValue.split("..").unsafeCast<Array<Int>?>()
    }

    private fun dfs(attributeName: String, offsetToScroll: Int, node: Node): Pair<Node, Node?> {
      var closestParent = node
      var closestLeftSibling: Node? = null
      var child = node.firstChild
      while (child != null) {
        val fromTo = getSrcFromTo(attributeName, child)

        if (fromTo == null) {
          child = child.nextSibling
          continue
        }
        if (fromTo[1] <= offsetToScroll) {
          closestLeftSibling = child
          child = child.nextSibling
          continue
        }

        if (fromTo[0] <= offsetToScroll) {
          val (node1, node2) = dfs(attributeName, offsetToScroll, child)

          closestParent = node1
          closestLeftSibling = node2
        }
        break
      }

      return closestParent to closestLeftSibling
    }

    fun scrollToSrcOffset(iFrame: HTMLIFrameElement, offsetToScroll: Int, attributeName: String) {
      val body = iFrame.contentDocument?.body
      if (body?.firstChild == null) {
        return
      }

      val (closestParent, closestLeftSibling) = dfs(attributeName, offsetToScroll, body)

      var rightSibling = closestLeftSibling
      while (closestLeftSibling != null && rightSibling != null) {
        rightSibling = rightSibling.nextSibling
        if (getSrcFromTo(attributeName, rightSibling) != null) {
          break
        }
      }

      val leftSrcPos = getSrcFromTo(attributeName, closestLeftSibling)
      val parentSrcPos = getSrcFromTo(attributeName, closestParent)
      val rightSrcPos = getSrcFromTo(attributeName, rightSibling)

      val leftBound = if (leftSrcPos != null) {
        listOf(
          leftSrcPos[1],
          closestLeftSibling.unsafeCast<HTMLElement>().offsetTop + closestLeftSibling.unsafeCast<HTMLElement>().offsetHeight
        )
      }
      else {
        listOf(parentSrcPos!![0], closestParent.unsafeCast<HTMLElement>().offsetTop)
      }

      val rightBound = if (rightSrcPos != null) {
        listOf(rightSrcPos[0], rightSibling.unsafeCast<HTMLElement>().offsetTop)
      }
      else {
        listOf(parentSrcPos!![1], closestParent.unsafeCast<HTMLElement>().offsetTop + closestParent.unsafeCast<HTMLElement>().offsetHeight)
      }

      val resultY = if (leftBound[0] == rightBound[0]) {
        (leftBound[1] + rightBound[1]) / 2
      }
      else {
        val srcRatio = (offsetToScroll - leftBound[0]) / (rightBound[0] - leftBound[0])
        leftBound[1] + (rightBound[1] - leftBound[1]) * srcRatio
      }

      val height = iFrame.contentWindow!!.innerHeight
      val newValue = resultY - height / 2.0
      val oldValue = iFrame.contentDocument!!.documentElement?.scrollTop ?: iFrame.contentDocument!!.body!!.scrollTop

      if ((newValue - oldValue).absoluteValue > 50) {
        iFrame.contentDocument!!.documentElement!!.scrollTop = newValue
        iFrame.contentDocument!!.body!!.scrollTop = newValue
      }
    }
  }
}
