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
package org.jetbrains.projector.intTest

import com.codeborne.selenide.Condition.*
import com.codeborne.selenide.Configuration
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.projector.common.protocol.toClient.ServerPingReplyEvent
import org.jetbrains.projector.common.protocol.toServer.ClientRequestPingEvent
import org.jetbrains.projector.intTest.ConnectionUtil.clientUrl
import org.jetbrains.projector.intTest.ConnectionUtil.startServerAndDoHandshake
import kotlin.properties.Delegates
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class NoMessagesFromServerTest {

  private companion object {

    private const val PING_INTERVAL = 1000L

    private fun openClient() {
      open("$clientUrl?pingInterval=$PING_INTERVAL")
    }
  }

  private var oldTimeout by Delegates.notNull<Long>()
  private lateinit var clientLoadNotifier: Channel<Unit>
  private lateinit var server: ApplicationEngine

  @BeforeTest
  fun before() {
    oldTimeout = Configuration.timeout
    Configuration.timeout = 10 * PING_INTERVAL
    clientLoadNotifier = Channel()
  }

  @AfterTest
  fun after() {
    server.stop(500, 1000)
    Configuration.timeout = oldTimeout
  }

  @Test
  fun shouldShowWarningWhenNoMessagesFromServer() {
    server = startServerAndDoHandshake(handlePing = false) { (_, receiver) ->
      clientLoadNotifier.send(Unit)

      while (true) {
        receiver()
      }
    }
    server.start()

    openClient()

    runBlocking {
      clientLoadNotifier.receive()
    }
    element(".connection-watcher-warning").should(appear)
  }

  @Test
  fun shouldNotShowWarningWhenMessagesFromServer() {
    server = startServerAndDoHandshake { (_, receiver) ->
      clientLoadNotifier.send(Unit)

      while (true) {
        receiver()
      }
    }
    server.start()

    openClient()

    runBlocking {
      clientLoadNotifier.receive()
    }
    repeat(5) {
      element(".connection-watcher-warning").should(be(hidden))
      runBlocking {
        delay(2 * PING_INTERVAL)
      }
    }
  }

  @Test
  fun shouldHideWarningWhenReceivedMessagesFromServer() {
    var canAnswerPing = false

    server = startServerAndDoHandshake(handlePing = false) { (sender, receiver) ->
      clientLoadNotifier.send(Unit)

      while (true) {
        val events = receiver()
        if (canAnswerPing) {
          events.forEach {
            if (it is ClientRequestPingEvent) {
              sender(listOf(ServerPingReplyEvent(
                clientTimeStamp = it.clientTimeStamp,
                serverReadEventTimeStamp = it.clientTimeStamp,  // todo: maybe need to send actual info here
              )))
            }
          }
        }
      }
    }
    server.start()

    openClient()

    runBlocking {
      clientLoadNotifier.receive()
    }
    element(".connection-watcher-warning").should(appear)

    canAnswerPing = true
    element(".connection-watcher-warning").should(disappear)

    canAnswerPing = false
    element(".connection-watcher-warning").should(appear)
  }
}
