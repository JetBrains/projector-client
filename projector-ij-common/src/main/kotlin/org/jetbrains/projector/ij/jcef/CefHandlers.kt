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
package org.jetbrains.projector.ij.jcef

import org.cef.CefClient
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefClientHandler
import org.cef.handler.CefLifeSpanHandler
import org.cef.handler.CefMessageRouterHandler
import org.jetbrains.projector.util.loading.UseProjectorLoader

@UseProjectorLoader
public object CefHandlers {

  @JvmStatic
  private val handlers = mutableMapOf<CefMessageRouter, MutableList<CefMessageRouterHandler>>()

  @JvmStatic
  public fun addMessageRouterHandler(router: CefMessageRouter, handler: CefMessageRouterHandler, first: Boolean): Boolean {
    handlers.addCreateIfNeeded(router, handler, first)
    return true
  }

  @JvmStatic
  public fun removeMessageRouterHandler(router: CefMessageRouter, handler: CefMessageRouterHandler): Boolean {
    return handlers[router]?.remove(handler) ?: false
  }

  @JvmStatic
  public fun clearMessageRouterHandlers(router: CefMessageRouter) {
    handlers.remove(router)
  }

  internal fun getRouterHandlers(router: CefMessageRouter): List<CefMessageRouterHandler> = handlers[router] ?: emptyList()

  //===================================================================================

  @JvmStatic
  private val routers = mutableMapOf<CefClientHandler, MutableList<CefMessageRouter>>()

  @JvmStatic
  public fun onMessageRouterAdded(handler: CefClientHandler, router: CefMessageRouter) {
    routers.addCreateIfNeeded(handler, router)
  }

  @JvmStatic
  public fun onMessageRouterRemoved(handler: CefClientHandler, router: CefMessageRouter) {
    routers[handler]?.remove(router)
  }

  internal fun getMessageRouters(handler: CefClientHandler): List<CefMessageRouter> = routers[handler] ?: emptyList()

  //===================================================================================

  @JvmStatic
  public fun onLifeSpanHandlerAdded(client: CefClient, handler: CefLifeSpanHandler) {
    ProjectorCefBrowser.getClientInstances(client).forEach { it.onLifeSpanHandlerAdded(handler) }
  }

  @JvmStatic
  public fun onLifeSpanHandlerRemoved(client: CefClient) {
    ProjectorCefBrowser.getClientInstances(client).forEach { it.onLifeSpanHandlerRemoved() }
  }

  //===================================================================================

  private fun <K, T> MutableMap<K, MutableList<T>>.addCreateIfNeeded(key: K, item: T, first: Boolean = false) {
    val list = getOrPut(key) { mutableListOf() }
    if (first) list.add(0, item) else list.add(item)
  }
}
