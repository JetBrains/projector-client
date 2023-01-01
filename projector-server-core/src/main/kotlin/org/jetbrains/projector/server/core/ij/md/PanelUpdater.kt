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
package org.jetbrains.projector.server.core.ij.md

import org.jetbrains.projector.util.logging.Logger
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Point
import java.net.URI
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

public object PanelUpdater {

  private val logger = Logger<PanelUpdater>()

  public var showCallback: ((id: Int, shown: Boolean) -> Unit)? = null
  public var resizeCallback: ((id: Int, size: Dimension) -> Unit)? = null
  public var moveCallback: ((id: Int, pos: Point) -> Unit)? = null
  public var disposeCallback: ((id: Int) -> Unit)? = null
  public var placeToWindowCallback: ((id: Int, rootComponent: Component?) -> Unit)? = null
  public var setHtmlCallback: ((id: Int, lastChangedHtml: String) -> Unit)? = null
  public var setCssCallback: ((id: Int, lastCssString: String) -> Unit)? = null
  public var scrollCallback: ((id: Int, lastScrollOffset: Int) -> Unit)? = null

  public fun openInExternalBrowser(link: String) {
    Desktop.getDesktop().browse(URI(link))
  }

  private val idToPanelLock = ReentrantReadWriteLock()

  private val idToPanel = mutableMapOf<Int, PanelDelegate>()

  internal fun put(panel: PanelDelegate) {
    idToPanelLock.write {
      idToPanel[panel.id] = panel
    }
  }

  public fun updateAll() {
    idToPanelLock.read {
      idToPanel.keys.forEach {
        show(it)
        resize(it)
        move(it)
        placeToWindow(it)
        setHtml(it)
        setCss(it)
        scroll(it)
      }
    }
  }

  internal fun show(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    showCallback?.invoke(id, panel.shown)
  }

  internal fun resize(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    resizeCallback?.invoke(id, Dimension(panel.width, panel.height))
  }

  internal fun move(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    moveCallback?.invoke(id, Point(panel.x, panel.y))
  }

  internal fun dispose(id: Int) {
    idToPanelLock.write { idToPanel.remove(id) }
    disposeCallback?.invoke(id)
  }

  internal fun placeToWindow(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    placeToWindowCallback?.invoke(id, panel.rootComponent)
  }

  internal fun setHtml(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    setHtmlCallback?.invoke(id, panel.lastChangedHtml)
  }

  internal fun setCss(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    setCssCallback?.invoke(id, panel.lastCssString)
  }

  internal fun scroll(id: Int) {
    val panel = idToPanelLock.read { idToPanel[id] ?: return }
    scrollCallback?.invoke(id, panel.lastScrollOffset)
  }
}
