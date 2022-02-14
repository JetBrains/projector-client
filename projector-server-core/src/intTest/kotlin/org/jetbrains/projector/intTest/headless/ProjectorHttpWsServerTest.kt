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
package org.jetbrains.projector.intTest.headless

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
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

class ProjectorHttpWsServerTest : AnnotationSpec() {

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

  lateinit var server: HttpWsServer
  lateinit var client: HttpClient

  @BeforeEach
  fun beforeTest() {
    server = createServer().also { it.start() }
  }

  @AfterEach
  fun afterTest() {
    client.close()
    server.stop()
  }

  @Test
  fun `webSocket should return success message`() {
    client = HttpClient {
      install(WebSockets)
    }
    runBlocking {
      client.ws(host = "localhost", port = PORT) {
        val actual = (incoming.receive() as Frame.Text).readText()

        actual shouldBe WS_SUCCESS
      }
    }
  }

  @Test
  fun `incorrect request should return 404`() {
    client = HttpClient {
      expectSuccess = false
    }
    val response = runBlocking { client.get<HttpResponse>(prj("/abc")) }
    response.status.value shouldBe 404
  }

  @Test
  fun `correct request should return correct HTML`() {
    client = HttpClient()

    val response = runBlocking { client.get<HttpResponse>(prj("/")) }
    val responseBytes = runBlocking { response.readBytes() }.toList()
    val expected = File("$clientRoot/index.html").readBytes().toList()

    responseBytes shouldBe expected
    response.contentType() shouldBe ContentType.Text.Html
  }

  @Test
  fun `main windows should have correct title and icon`() {
    client = HttpClient()

    val response = runBlocking { client.get<String>(prj("/mainWindows")) }.toMainWindowList()

    response.size shouldBe 1
    response[0].title shouldBe "abc"
    response[0].pngBase64Icon shouldBe "png base 64"
  }

  @Test
  fun `files should be returned by the response`() {
    client = HttpClient()

    File(clientRoot).listFiles()!!.forEach {
      val filename = it.name
      if (filename.endsWith(".js.map")) {
        return@forEach
      }

      val expected = File("$clientRoot/$filename").readBytes().toList()

      for (prefix in listOf("/", "/projector/")) {
        val path = "$prefix$filename"
        val response = runBlocking { client.get<ByteArray>(prj(path)) }.toList()

        withClue("mismatching response for path: $path") {
          response shouldBe expected
        }
        println("$path OK")
      }
    }
  }
}
