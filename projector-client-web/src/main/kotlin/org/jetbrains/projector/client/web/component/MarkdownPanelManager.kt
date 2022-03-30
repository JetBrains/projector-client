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

import kotlinx.dom.clear
import org.jetbrains.projector.util.logging.Logger
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.Node
import org.w3c.dom.get
import org.w3c.dom.parsing.DOMParser
import kotlin.math.absoluteValue

class MarkdownPanelManager(
  zIndexByWindowIdGetter: (Int) -> Int?,
  private val openInExternalBrowser: (String) -> Unit,
) : ClientComponentManager<MarkdownPanelManager.MarkdownPanel>(zIndexByWindowIdGetter) {

  class MarkdownPanel(id: Int, openInExternalBrowser: (String) -> Unit) : ClientComponent(id) {

    init {
      setLinkProcessor(openInExternalBrowser)
    }
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

  override fun createComponent(componentId: Int) = MarkdownPanel(componentId, openInExternalBrowser)

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
