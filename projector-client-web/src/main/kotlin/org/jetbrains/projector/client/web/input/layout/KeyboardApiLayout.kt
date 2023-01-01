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
package org.jetbrains.projector.client.web.input.layout

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.projector.client.web.externalDeclarartion.keyboard
import org.jetbrains.projector.common.protocol.data.VK

object KeyboardApiLayout {

  suspend fun getVirtualKey(code: String): VK? {
    if (window != window.parent) return null  // getLayoutMap() must be called from a top-level browser context, so return immediately if it's not
    val keyboard = window.navigator.keyboard ?: return null
    val currentLayout = keyboard.getLayoutMap().await()
    val currentSymbolOnKeyboard = currentLayout.get(code)?.single()

    return VK.codeMap[currentSymbolOnKeyboard]
  }
}
