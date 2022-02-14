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
package org.jetbrains.projector.client.web.electron

import kotlinx.browser.window
import org.jetbrains.projector.client.web.externalDeclarartion.process

// adopted from https://github.com/cheton/is-electron
internal fun isElectron(): Boolean {
  // Renderer process
  if (isDefined(window) && jsTypeOf(window.process) == "object" && window.process.type == "renderer") {
    return true
  }

  try {
    // Main process
    if (isDefined(process) && jsTypeOf(process.versions) == "object" && jsBoolean(process.versions.electron)) {
      return true
    }
  }
  catch (_: Throwable) {
    // Began to throw 'ReferenceError: process is not defined' after we switched to JS IR
  }

  // Detect the user agent when the `nodeIntegration` option is set to true
  if (jsTypeOf(window.navigator) == "object" && jsTypeOf(window.navigator.userAgent) == "string" && window.navigator.userAgent.contains("Electron")) {
    return true
  }

  return false
}

@Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")
private inline fun jsBoolean(expression: Any?): Boolean = js("Boolean(expression)") as Boolean

@Suppress("NOTHING_TO_INLINE")
private inline fun isDefined(obj: Any?): Boolean = jsTypeOf(obj) != "undefined"
