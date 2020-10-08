/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import org.jetbrains.projector.server.core.util.GetRequestResult
import org.jetbrains.projector.server.core.util.HttpWsServer

public abstract class ProjectorHttpWsServer(port: Int) : HttpWsServer(port) {

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
}
