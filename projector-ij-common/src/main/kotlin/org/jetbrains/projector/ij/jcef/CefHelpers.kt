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
@file:UseProjectorLoader

package org.jetbrains.projector.ij.jcef

import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefClientHandler
import org.cef.handler.CefMessageRouterHandler
import org.jetbrains.projector.util.loading.UseProjectorLoader

public fun CefClientHandler.getMessageRouters(): List<CefMessageRouter> {
  return CefHandlers.getMessageRouters(this)
}

@Suppress("unused") // used in server
public fun CefMessageRouter.getHandlers(): List<CefMessageRouterHandler> {
  return CefHandlers.getRouterHandlers(this)
}

@Suppress("unused") // used in server
public fun CefMessageRouterHandler.onProjectorQuery(projectorCefBrowser: ProjectorCefBrowser, query: String) {
  onQuery(projectorCefBrowser, DEFAULT_FRAME, 0, query, false, DEFAULT_CALLBACK)
}

internal val DEFAULT_FRAME = ProjectorCefFrame()

private val DEFAULT_CALLBACK = object : CefQueryCallback {
  override fun success(response: String?) {
    // TODO
  }

  override fun failure(error_code: Int, error_message: String?) {
    // TODO
  }
}
