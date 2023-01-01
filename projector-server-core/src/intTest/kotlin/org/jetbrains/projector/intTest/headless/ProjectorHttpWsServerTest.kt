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
package org.jetbrains.projector.intTest.headless

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.java_websocket.WebSocket
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.common.protocol.toClient.toMainWindowList
import org.jetbrains.projector.server.core.websocket.HttpWsServer
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectorHttpWsServerTest {

  private companion object {

    private const val clientRoot = "../projector-client-web/build/distributions"

    private const val PORT = 8080

    private const val WS_SUCCESS = "ws -- success"

    private fun prj(path: String) = "http://localhost:$PORT$path"

    private fun createServer() = object : HttpWsServer(PORT) {

      override fun onError(connection: WebSocket?, e: Exception) {}

      override fun onWsOpen(connection: WebSocket) {
        connection.send(WS_SUCCESS)
      }

      override fun onWsClose(connection: WebSocket) {}

      override fun onWsMessage(connection: WebSocket, message: String) {}

      override fun onWsMessage(connection: WebSocket, message: ByteBuffer) {}

      override fun getMainWindows(): List<MainWindow> = listOf(
        MainWindow(
          title = "abc",
          pngBase64Icon = "png base 64",
        )
      )
    }
  }

  @Test
  fun testWebSocket() {
    val server = createServer().also { it.start() }
    val client = HttpClient {
      install(WebSockets)
    }

    runBlocking {
      client.ws(host = "localhost", port = PORT) {
        val actual = (incoming.receive() as Frame.Text).readText()

        assertEquals(WS_SUCCESS, actual)
      }
    }

    client.close()
    server.stop()
  }

  @Test
  fun test404() {
    val server = createServer().also { it.start() }
    val client = HttpClient {
      expectSuccess = false
    }

    val response = runBlocking { client.get<HttpResponse>(prj("/abc")) }

    assertEquals(404, response.status.value)

    client.close()
    server.stop()
  }

  @Test
  fun testRoot() {
    val server = createServer().also { it.start() }
    val client = HttpClient()

    val response = runBlocking { client.get<HttpResponse>(prj("/")) }
    val responseBytes = runBlocking { response.readBytes() }.toList()
    val expected = File("$clientRoot/index.html").readBytes().toList()

    assertEquals(expected, responseBytes)
    assertEquals(ContentType.Text.Html, response.contentType())

    client.close()
    server.stop()
  }

  @Test
  fun testMainWindows() {
    val server = createServer().also { it.start() }
    val client = HttpClient()

    val response = runBlocking { client.get<String>(prj("/mainWindows")) }.toMainWindowList()

    assertEquals(1, response.size)
    assertEquals("abc", response[0].title)
    assertEquals("png base 64", response[0].pngBase64Icon)

    client.close()
    server.stop()
  }

  @Test
  fun testFiles() {
    val server = createServer().also { it.start() }
    val client = HttpClient()

    File(clientRoot).listFiles()!!.forEach {
      val filename = it.name
      if (filename.endsWith(".js.map")) {
        return@forEach
      }

      val expected = File("$clientRoot/$filename").readBytes().toList()

      for (prefix in listOf("/", "/projector/")) {
        val path = "$prefix$filename"
        val response = runBlocking { client.get<ByteArray>(prj(path)) }.toList()

        assertEquals(expected, response, "mismatching responce for path: $path")
        println("$path OK")
      }
    }

    client.close()
    server.stop()
  }
}
