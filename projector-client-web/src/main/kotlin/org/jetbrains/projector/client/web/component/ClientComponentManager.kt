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

import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.CommonIntSize
import org.jetbrains.projector.common.protocol.data.Point

abstract class ClientComponentManager<Component : ClientComponent>(
  protected val zIndexByWindowIdGetter: (Int) -> Int?,
) {

  private val idToComponent = mutableMapOf<Int, Component>()

  abstract fun createComponent(componentId: Int): Component

  protected fun getOrCreate(componentId: Int): Component {
    return idToComponent.getOrPut(componentId) { createComponent(componentId) }
  }

  fun placeToWindow(componentId: Int, windowId: Int) {
    val panel = getOrCreate(componentId)

    panel.windowId = windowId

    val zIndex = zIndexByWindowIdGetter(windowId) ?: return

    panel.iFrame.style.zIndex = (zIndex + 1).toString()
  }

  fun updatePlacements() {
    idToComponent.forEach { (componentId, panel) ->
      panel.windowId?.let { placeToWindow(componentId, it) }
    }
  }

  fun show(componentId: Int, show: Boolean, windowId: Int? = null) {
    val component = getOrCreate(componentId)

    Do exhaustive when (show) {
      true -> {
        windowId?.also { placeToWindow(componentId, it) }
        component.iFrame.style.display = "block"
      }

      false -> component.dispose()
    }
  }

  fun move(componentId: Int, point: Point) {
    val component = getOrCreate(componentId)

    component.iFrame.style.apply {
      left = "${point.x}px"
      top = "${point.y}px"
    }
  }

  fun resize(componentId: Int, size: CommonIntSize) {
    val component = getOrCreate(componentId)

    component.iFrame.style.apply {
      width = "${size.width}px"
      height = "${size.height}px"
    }
  }

  fun dispose(componentId: Int) {
    val component = getOrCreate(componentId)

    component.dispose()

    idToComponent.remove(componentId)
  }

  fun disposeAll() {
    idToComponent.values.forEach { it.dispose() }

    idToComponent.clear()
  }

}
