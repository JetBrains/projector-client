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
package org.jetbrains.projector.client.common.misc

import kotlinx.browser.window
import org.w3c.dom.url.URL

actual object ParamsProvider {

  private fun getCurrentHostname(): String = when (val hostname = window.location.hostname) {
    "" -> "localhost"  // when you open a file in a browser (like "file://..."), the hostname is empty, so assume the localhost is preferred

    else -> hostname
  }

  private fun getCurrentPort(): String {
    val port = window.location.port

    if (port == "")
      if (window.location.protocol == "https:") return "443" else return "80"
    else
      return port
  }

  private val DEFAULT_HOST = getCurrentHostname()
  private val DEFAULT_PORT = getCurrentPort()
  private val DEFAULT_TO_CLIENT_FORMAT: ToClientFormat = ToClientFormat.KOTLINX_JSON_MANUAL
  private const val DEFAULT_IMAGE_TTL = 60_000.0  // in ms
  private const val DEFAULT_FLUSH_DELAY = 1
  private const val DEFAULT_BACKGROUND_COLOR = "#282"
  private val DEFAULT_PING_AVERAGE_COUNT: Int? = null
  private const val DEFAULT_USER_SCALING_RATIO = 1.0
  private const val DEFAULT_PING_INTERVAL = 1000
  private const val DEFAULT_SHOW_NOT_SECURE_WARNING = true.toString()
  private const val DEFAULT_REPAINT_INTERVAL_MS = 333
  private const val DEFAULT_IMAGE_CACHE_SIZE_CHARS = 5_000_000
  private const val DEFAULT_BLOCK_CLOSING = true

  val SYSTEM_SCALING_RATIO
    get() = window.devicePixelRatio  // get every time because it can be changed
  val USER_SCALING_RATIO: Double

  actual val CLIPPING_BORDERS: Boolean
  val HOST: String
  val PORT: String
  actual val LOG_UNSUPPORTED_EVENTS: Boolean
  val DOUBLE_BUFFERING: Boolean
  val ENABLE_COMPRESSION: Boolean
  val TO_CLIENT_FORMAT: ToClientFormat
  actual val IMAGE_TTL: Double
  val FLUSH_DELAY: Int?
  actual val SHOW_TEXT_WIDTH: Boolean
  val SHOW_SENT_RECEIVED: Boolean
  val SHOW_PING: Boolean
  val BACKGROUND_COLOR: String
  val PING_AVERAGE_COUNT: Int?
  val PING_INTERVAL: Int
  val SHOW_PROCESSING_TIME: Boolean
  actual val REPAINT_AREA: RepaintAreaSetting
  val SPECULATIVE_TYPING: Boolean
  val ENABLE_WSS: Boolean
  val HANDSHAKE_TOKEN: String?
  val INPUT_METHOD_TYPE: InputMethodType
  val IDE_WINDOW_ID: Int?
  val SHOW_NOT_SECURE_WARNING: Boolean
  val REPAINT_INTERVAL_MS: Int
  actual val IMAGE_CACHE_SIZE_CHARS: Int
  val BLOCK_CLOSING: Boolean
  val LAYOUT_TYPE: LayoutType
  val SCALING_RATIO: Double
    get() = SYSTEM_SCALING_RATIO * USER_SCALING_RATIO

  init {
    with(URL(window.location.href)) {
      CLIPPING_BORDERS = searchParams.has("clipping")
      HOST = searchParams.get("host") ?: DEFAULT_HOST
      PORT = searchParams.get("port") ?: DEFAULT_PORT
      LOG_UNSUPPORTED_EVENTS = searchParams.has("logUnsupportedEvents")
      DOUBLE_BUFFERING = searchParams.has("doubleBuffering")
      ENABLE_COMPRESSION = searchParams.has("enableCompression")
      TO_CLIENT_FORMAT = when (searchParams.get("toClientFormat")) {
        "json" -> ToClientFormat.KOTLINX_JSON
        "jsonManual" -> ToClientFormat.KOTLINX_JSON_MANUAL
        "protoBuf" -> ToClientFormat.KOTLINX_PROTOBUF
        else -> DEFAULT_TO_CLIENT_FORMAT
      }
      IMAGE_TTL = searchParams.get("imageTtl")?.toDoubleOrNull() ?: DEFAULT_IMAGE_TTL
      FLUSH_DELAY = when (val flushDelay = searchParams.get("flushDelay")) {
        null -> DEFAULT_FLUSH_DELAY
        else -> flushDelay.toIntOrNull()
      }
      SHOW_TEXT_WIDTH = searchParams.has("showTextWidth")
      SHOW_SENT_RECEIVED = searchParams.has("showSentReceived")
      SHOW_PING = searchParams.has("showPing")
      BACKGROUND_COLOR = searchParams.get("backgroundColor") ?: DEFAULT_BACKGROUND_COLOR
      PING_AVERAGE_COUNT = searchParams.get("pingAverageCount")?.toIntOrNull() ?: DEFAULT_PING_AVERAGE_COUNT
      USER_SCALING_RATIO = searchParams.get("userScalingRatio")?.toDoubleOrNull() ?: DEFAULT_USER_SCALING_RATIO
      PING_INTERVAL = searchParams.get("pingInterval")?.toIntOrNull() ?: DEFAULT_PING_INTERVAL
      SHOW_PROCESSING_TIME = searchParams.has("showProcessingTime")
      REPAINT_AREA = when (searchParams.has("repaintArea")) {
        false -> RepaintAreaSetting.Disabled
        true -> RepaintAreaSetting.Enabled(show = false)
      }
      SPECULATIVE_TYPING = searchParams.has("speculativeTyping")
      ENABLE_WSS = searchParams.has("wss") || window.location.protocol == "https:"
      HANDSHAKE_TOKEN = searchParams.get("token")
      INPUT_METHOD_TYPE = when (searchParams.get("inputMethod")) {
        "default" -> InputMethodType.DEFAULT
        "mobileOnlyButtons" -> InputMethodType.OVERLAY_BUTTONS
        "mobile" -> InputMethodType.OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD
        "ime" -> InputMethodType.IME
        else -> when (searchParams.has("mobile")) {  // save support for legacy params for now
          true -> when (searchParams.get("mobile")) {
            "onlyButtons" -> InputMethodType.OVERLAY_BUTTONS
            else -> InputMethodType.OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD
          }
          false -> InputMethodType.DEFAULT
        }
      }
      IDE_WINDOW_ID = searchParams.get("ideWindow")?.toIntOrNull()
      SHOW_NOT_SECURE_WARNING = (searchParams.get("notSecureWarning") ?: DEFAULT_SHOW_NOT_SECURE_WARNING).toBoolean()
      REPAINT_INTERVAL_MS = searchParams.get("repaintInterval")?.toIntOrNull() ?: DEFAULT_REPAINT_INTERVAL_MS
      IMAGE_CACHE_SIZE_CHARS = searchParams.get("cacheSize")?.toIntOrNull() ?: DEFAULT_IMAGE_CACHE_SIZE_CHARS
      BLOCK_CLOSING = searchParams.get("blockClosing")?.toBoolean() ?: DEFAULT_BLOCK_CLOSING
      LAYOUT_TYPE = when (searchParams.get("layout")) {
        "frAzerty" -> LayoutType.FR_AZERTY
        else -> LayoutType.JS_DEFAULT
      }
    }
  }

  enum class ToClientFormat {
    KOTLINX_JSON,
    KOTLINX_JSON_MANUAL,
    KOTLINX_PROTOBUF,
  }

  enum class InputMethodType {
    DEFAULT,  // todo: if IME is stable enough, we can try dropping default and switching to IME
    OVERLAY_BUTTONS,
    OVERLAY_BUTTONS_N_VIRTUAL_KEYBOARD,
    IME,
  }

  enum class LayoutType {
    JS_DEFAULT,
    FR_AZERTY,
  }
}
