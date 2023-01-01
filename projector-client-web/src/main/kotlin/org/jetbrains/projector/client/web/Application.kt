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
package org.jetbrains.projector.client.web

import kotlinx.browser.window
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.externalDeclarartion.permissions
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.util.logging.Logger
import kotlin.js.json
import kotlin.random.Random

class Application {

  private val stateMachine = ClientStateMachine()
  private val windowSizeController = WindowSizeController(stateMachine)

  fun start() {
    val scheme = when (ParamsProvider.ENABLE_WSS) {
      false -> "ws://"
      true -> "wss://"
    }
    val host = ParamsProvider.HOST
    val port = ParamsProvider.PORT
    val path = ParamsProvider.PATH
    val relayPath = when (ParamsProvider.ENABLE_RELAY) {
      true -> "/connect/${ParamsProvider.RELAY_SERVER_ID}/${generateKey()}"
      false -> ""
    }

    val url = "$scheme$host:$port$path$relayPath"

    try {
      setClipboardPermissions()
    }
    catch (t: Throwable) {
      logger.error(t) { "Can't set clipboard permissions, maybe they are unsupported on your browser..." }
    }

    stateMachine.fire(
      ClientAction.Start(
        stateMachine = stateMachine,
        url = url,
        windowSizeController = windowSizeController
      )
    )

    stateMachine.runMainLoop()
  }

  private fun setClipboardPermissions() {
    val permissions = window.navigator.permissions

    listOf(
      "clipboard-read",
      "clipboard-write"
    )
      .forEach { permissionName ->
        permissions
          .query(json("name" to permissionName))
          .then { logger.debug { "got permission $permissionName" } }
          .catch { logger.debug { "can't get permission $permissionName" } }
      }
  }

  companion object {

    private val logger = Logger<Application>()

    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun generateKey(): String =
      (1 .. 20)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
  }
}
