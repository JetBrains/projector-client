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
package org.jetbrains.projector.server.core.util

import org.java_websocket.WebSocketServerFactory
import org.java_websocket.server.DefaultSSLWebSocketServerFactory
import org.jetbrains.projector.util.logging.Logger
import java.io.FileInputStream
import java.security.KeyStore
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private val logger = Logger("KeyKt")

public const val SSL_ENV_NAME: String = "ORG_JETBRAINS_PROJECTOR_SERVER_SSL_PROPERTIES_PATH"
public const val SSL_STORE_TYPE: String = "STORE_TYPE"
public const val SSL_FILE_PATH: String = "FILE_PATH"
public const val SSL_STORE_PASSWORD: String = "STORE_PASSWORD"
public const val SSL_KEY_PASSWORD: String = "KEY_PASSWORD"

public fun setSsl(setWebSocketFactory: (WebSocketServerFactory) -> Unit): String? {
  val sslPropertiesFilePath = getOption(SSL_ENV_NAME) ?: return null

  try {
    val properties = Properties().apply {
      load(FileInputStream(sslPropertiesFilePath))
    }

    fun Properties.getOrThrow(key: String) = requireNotNull(this.getProperty(key)) { "Can't find $key in properties file" }

    val storetype = properties.getOrThrow(SSL_STORE_TYPE)
    val filePath = properties.getOrThrow(SSL_FILE_PATH)
    val storePassword = properties.getOrThrow(SSL_STORE_PASSWORD)
    val keyPassword = properties.getOrThrow(SSL_KEY_PASSWORD)

    val keyStore = KeyStore.getInstance(storetype).apply {
      load(FileInputStream(filePath), storePassword.toCharArray())
    }

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509").apply {
      init(keyStore, keyPassword.toCharArray())
    }

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509").apply {
      init(keyStore)
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
      init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
    }

    setWebSocketFactory(DefaultSSLWebSocketServerFactory(sslContext))

    return sslPropertiesFilePath
  }
  catch (t: Throwable) {
    logger.info(t) { "Can't enable SSL from '$sslPropertiesFilePath' file" }

    return null
  }
}
