/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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
package org.jetbrains.projector.server.core

import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.common.protocol.toClient.toJson
import org.jetbrains.projector.server.core.websocket.GetRequestResult
import org.jetbrains.projector.server.core.websocket.HttpWsServer
import org.jetbrains.projector.server.core.util.getWildcardHostAddress
import java.net.InetAddress

public abstract class ProjectorHttpWsServer(host: InetAddress, port: Int) : HttpWsServer(host, port) {

  public constructor(port: Int) : this(getWildcardHostAddress(), port)

  private companion object {

    private fun String.calculateContentType() = when (this.substringAfterLast('.').toLowerCase()) {
      "css" -> "text/css"
      "html" -> "text/html"
      "js" -> "text/javascript"
      "png" -> "image/png"
      "svg" -> "image/svg+xml"
      "webmanifest" -> "application/manifest+json"
      else -> "application/octet-stream"
    }

    fun getResourceName(pathWithoutParams: String) = pathWithoutParams
      .let {
        // support paths from old docker instructions
        when (it.startsWith("/projector/")) {
          true -> it.removePrefix("/projector")
          false -> it
        }
      }
      .let {
        // support paths for PWA (start_url from webmanifest) – it's the name of repo so PWA could work for GH Pages too
        when (it.startsWith("/projector-client/")) {
          true -> it.removePrefix("/projector-client")
          false -> it
        }
      }
      .let {
        // support root
        when (it == "/") {
          true -> "/index.html"
          false -> it
        }
      }

    fun getResource(resourceFileName: String): ByteArray? {
      // todo: restrict going to upper dirs
      return this::class.java.getResource("/projector-client-web-distribution$resourceFileName")?.readBytes()
    }
  }

  final override fun onGetRequest(path: String): GetRequestResult {
    val pathWithoutParams = path.substringBefore('?').substringBefore('#')

    if (pathWithoutParams == "/mainWindows") {
      val mainWindows = getMainWindows()
      val json = mainWindows.toJson().toByteArray()
      return GetRequestResult(
        statusCode = 200,
        statusText = "OK",
        contentType = "text/json",
        content = json,
      )
    }

    val resourceFileName = getResourceName(pathWithoutParams)
    val resourceBytes = getResource(resourceFileName)

    if (resourceBytes != null) {
      return GetRequestResult(
        statusCode = 200,
        statusText = "OK",
        contentType = resourceFileName.calculateContentType(),
        content = resourceBytes,
      )
    }

    return GetRequestResult(
      statusCode = 404,
      statusText = "Not found",
      contentType = "text/html",
      content = "<h1>404 Not found requested path: $path</h1>".toByteArray(),
    )
  }

  public abstract fun getMainWindows(): List<MainWindow>
}
