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

import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.window.WindowManager
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.state.LafListener
import org.jetbrains.projector.common.protocol.toClient.WindowData
import org.jetbrains.projector.common.protocol.toServer.ClientWindowsActivationEvent
import org.jetbrains.projector.common.protocol.toServer.ClientWindowsDeactivationEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.FocusEvent

class WebWindowManager(private val stateMachine: ClientStateMachine, override val imageCacher: ImageCacher) : WindowManager<WebWindow>, LafListener {

  companion object {
    const val zIndexStride = 10
  }

  init {
    window.onblur = ::onDeactivated
    window.onfocus = ::onActivated
  }

  private val visibleWindows get() = windows.values.filter { it.isShowing }

  // todo: remove SUPPRESS after KT-8112 is implemented or KTIJ-15401 is solved in some other way
  private fun onActivated(@Suppress("UNUSED_PARAMETER") event: FocusEvent) {
    val windowIds = visibleWindows.map { it.id }
    stateMachine.fire(ClientAction.AddEvent(ClientWindowsActivationEvent(windowIds)))
  }

  // todo: remove SUPPRESS after KT-8112 is implemented or KTIJ-15401 is solved in some other way
  private fun onDeactivated(@Suppress("UNUSED_PARAMETER") event: FocusEvent) {
    val windowIds = visibleWindows.map { it.id }
    stateMachine.fire(ClientAction.AddEvent(ClientWindowsDeactivationEvent(windowIds)))
  }

  private val windows = mutableMapOf<Int, WebWindow>()

  fun getWindowCanvas(windowId: Int): HTMLCanvasElement? = windows[windowId]?.canvas

  fun getWindowZIndex(windowId: Int): Int? = windows[windowId]?.zIndex

  /** Returns topmost visible window, containing point. Contain check includes window header and borders.  */
  fun getTopWindow(x: Int, y: Int): WebWindow? = windows.values.filter { it.isShowing && it.contains(x, y) }.maxByOrNull { it.zIndex }

  fun getOrCreate(windowData: WindowData): WebWindow {
    return windows.getOrPut(windowData.id) { WebWindow(windowData, stateMachine, imageCacher) }
  }

  fun cleanup(presentedWindowIds: Set<Int>) {
    windows.keys.retainAll { id ->
      if (id in presentedWindowIds) {
        true
      }
      else {
        windows.getValue(id).dispose()
        false
      }
    }
  }

  override fun lookAndFeelChanged() {
    windows.forEach { it.value.lookAndFeelChanged() }
  }

  override operator fun get(windowId: Int): WebWindow? = windows[windowId]

  override fun iterator(): Iterator<WebWindow> = windows.values.iterator()

  fun bringToFront(window: WebWindow) {
    val topWindow = windows.maxByOrNull { it.value.zIndex }?.value ?: return
    if (topWindow == window) {
      return
    }

    val currentZIndex = window.zIndex
    val topZIndex = topWindow.zIndex

    windows.filter { it.value.zIndex in currentZIndex..topZIndex }.forEach { it.value.zIndex -= zIndexStride }
    window.zIndex = topZIndex
  }
}
