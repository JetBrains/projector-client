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
package org.jetbrains.projector.client.web.state

import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.common.misc.Logger
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.InputController
import org.jetbrains.projector.client.web.ServerEventsProcessor
import org.jetbrains.projector.client.web.WindowSizeController
import org.jetbrains.projector.client.web.component.MarkdownPanelManager
import org.jetbrains.projector.client.web.debug.DivSentReceivedBadgeShower
import org.jetbrains.projector.client.web.debug.NoSentReceivedBadgeShower
import org.jetbrains.projector.client.web.debug.SentReceivedBadgeShower
import org.jetbrains.projector.client.web.misc.*
import org.jetbrains.projector.client.web.protocol.KotlinxJsonToClientHandshakeDecoder
import org.jetbrains.projector.client.web.protocol.KotlinxJsonToServerHandshakeEncoder
import org.jetbrains.projector.client.web.protocol.SupportedTypesProvider
import org.jetbrains.projector.client.web.speculative.Typing
import org.jetbrains.projector.client.web.window.OnScreenMessenger
import org.jetbrains.projector.client.web.window.WindowDataEventsProcessor
import org.jetbrains.projector.client.web.window.WindowManager
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.MessageDecoder
import org.jetbrains.projector.common.protocol.MessageEncoder
import org.jetbrains.projector.common.protocol.compress.MessageCompressor
import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.handshake.COMMON_VERSION
import org.jetbrains.projector.common.protocol.handshake.ToClientHandshakeFailureEvent
import org.jetbrains.projector.common.protocol.handshake.ToClientHandshakeSuccessEvent
import org.jetbrains.projector.common.protocol.handshake.ToServerHandshakeEvent
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ToClientMessageDecoder
import org.jetbrains.projector.common.protocol.toServer.*
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.math.roundToInt

sealed class ClientState {

  open fun consume(action: ClientAction): ClientState {
    logger.error { "${this::class.simpleName}: can't consume ${action::class.simpleName}" }
    return this
  }

  object Uninitialized : ClientState() {

    private fun configureWebPage(url: String) {
      document.body!!.apply {
        style.apply {
          backgroundColor = "#2A2"
          asDynamic().overscrollBehaviorY = "none"
        }

        oncontextmenu = { false }
      }

      OnScreenMessenger.showText("Starting connection", "Waiting for response from $url...", canReload = false)
    }

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.Start -> {
        configureWebPage(action.url)

        val webSocket = WebSocket(action.url).apply {
          binaryType = BinaryType.ARRAYBUFFER

          onopen = fun(_: Event) {
            action.stateMachine.fire(ClientAction.WebSocket.Open(openingTimeStamp = TimeStamp.current.roundToInt()))
          }

          onclose = fun(event: Event) {
            require(event is CloseEvent)

            action.stateMachine.fire(ClientAction.WebSocket.Close(this@apply.url, event.code))
          }

          onmessage = fun(messageEvent: MessageEvent) {
            action.stateMachine.fire(ClientAction.WebSocket.Message(message = (messageEvent.data as ArrayBuffer).asByteArray()))
          }
        }

        WaitingOpening(
          stateMachine = action.stateMachine,
          webSocket = webSocket,
          windowSizeController = action.windowSizeController
        )
      }

      else -> super.consume(action)
    }
  }

  class WaitingOpening(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Open -> {
        OnScreenMessenger.showText("Connection is opened", "Sending handshake...", canReload = false)

        ClientStats.resetStats(action.openingTimeStamp.toDouble())

        val handshakeEvent = with(SupportedTypesProvider) {
          ToServerHandshakeEvent(
            commonVersion = COMMON_VERSION,
            token = ParamsProvider.HANDSHAKE_TOKEN,
            initialSize = windowSizeController.currentSize,
            supportedToClientCompressions = supportedToClientDecompressors.map(MessageDecompressor<*>::compressionType),
            supportedToClientProtocols = supportedToClientDecoders.map(MessageDecoder<*, *>::protocolType),
            supportedToServerCompressions = supportedToServerCompressors.map(MessageCompressor<*>::compressionType),
            supportedToServerProtocols = supportedToServerEncoders.map(MessageEncoder<*, *>::protocolType)
          )
        }

        webSocket.send(KotlinxJsonToServerHandshakeEncoder.encode(handshakeEvent))

        OnScreenMessenger.showText("Connection is opened", "Handshake is sent...", canReload = false)

        WaitingHandshakeReply(
          stateMachine = stateMachine,
          webSocket = webSocket,
          windowSizeController = windowSizeController,
          openingTimeStamp = action.openingTimeStamp
        )
      }

      is ClientAction.WebSocket.Close -> {
        showDisconnectedMessage(action.url, action.closeCode)

        Disconnected
      }

      else -> super.consume(action)
    }
  }

  class WaitingHandshakeReply(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    private val openingTimeStamp: Int
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Message -> {
        val command = KotlinxJsonToClientHandshakeDecoder.decode(action.message)

        Do exhaustive when (command) {
          is ToClientHandshakeFailureEvent -> {
            OnScreenMessenger.showText("Handshake failure", "Reason: ${command.reason}", canReload = true)
            webSocket.close()

            this  // todo: handle this case
          }

          is ToClientHandshakeSuccessEvent -> {

            ProjectorUI.setColors(command.colors)

            OnScreenMessenger.showText(
              "Loading fonts...",
              "0 of ${command.fontDataHolders.size} font(s) loaded",
              canReload = false
            )

            command.fontDataHolders.forEach { fontDataHolder ->
              FontFaceAppender.appendFontFaceToPage(fontDataHolder.fontId, fontDataHolder.fontData) { loadedFontCount ->
                if (loadedFontCount == command.fontDataHolders.size) {
                  logger.info { "${command.fontDataHolders.size} font(s) loaded" }
                  OnScreenMessenger.hide()

                  stateMachine.fire(ClientAction.LoadAllFonts)
                }
                else {
                  OnScreenMessenger.showText(
                    "Loading fonts",
                    "$loadedFontCount of ${command.fontDataHolders.size} font(s) loaded.",
                    canReload = false
                  )
                }
              }
            }

            LoadingFonts(
              stateMachine = stateMachine,
              webSocket = webSocket,
              windowSizeController = windowSizeController,
              openingTimeStamp = openingTimeStamp,
              encoder = SupportedTypesProvider.supportedToServerEncoders.first { it.protocolType == command.toServerProtocol },
              decoder = SupportedTypesProvider.supportedToClientDecoders.first { it.protocolType == command.toClientProtocol },
              decompressor = SupportedTypesProvider.supportedToClientDecompressors.first { it.compressionType == command.toClientCompression },
              compressor = SupportedTypesProvider.supportedToServerCompressors.first { it.compressionType == command.toServerCompression }
            )
          }
        }
      }

      else -> super.consume(action)
    }
  }

  class LoadingFonts(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    private val openingTimeStamp: Int,
    private val encoder: ToServerMessageEncoder,
    private val decoder: ToClientMessageDecoder,
    private val decompressor: MessageDecompressor<ByteArray>,
    private val compressor: MessageCompressor<String>
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.LoadAllFonts -> {
        logger.debug { "All fonts are loaded. Ready to draw!" }

        webSocket.send("Unused string meaning fonts loading is done")  // todo: change this string

        ReadyToDraw(
          stateMachine = stateMachine,
          webSocket = webSocket,
          windowSizeController = windowSizeController,
          openingTimeStamp = openingTimeStamp,
          encoder = encoder,
          decoder = decoder,
          decompressor = decompressor,
          compressor = compressor
        )
      }

      else -> super.consume(action)
    }
  }

  class ReadyToDraw(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    openingTimeStamp: Int,
    private val encoder: ToServerMessageEncoder,
    private val decoder: ToClientMessageDecoder,
    private val decompressor: MessageDecompressor<ByteArray>,
    private val compressor: MessageCompressor<String>
  ) : ClientState() {

    private val eventsToSend = mutableListOf<ClientEvent>(ClientSetKeymapEvent(nativeKeymap))

    private val windowManager = WindowManager(stateMachine)

    private val windowDataEventsProcessor = WindowDataEventsProcessor(windowManager)

    private val serverEventsProcessor = ServerEventsProcessor(windowDataEventsProcessor)

    private val messagingPolicy = (
      ParamsProvider.FLUSH_DELAY
        ?.let {
          MessagingPolicy.Buffered(
            timeout = it,
            isFlushNeeded = { eventsToSend.isNotEmpty() },
            flush = { stateMachine.fire(ClientAction.Flush) }
          )
        }
      ?: MessagingPolicy.Unbuffered(
        isFlushNeeded = { eventsToSend.isNotEmpty() },
        flush = { stateMachine.fire(ClientAction.Flush) }
      )
                                  ).apply {
        onHandshakeFinished()
      }

    private val sentReceivedBadgeShower: SentReceivedBadgeShower = if (ParamsProvider.SHOW_SENT_RECEIVED) {
      DivSentReceivedBadgeShower()
    }
    else {
      NoSentReceivedBadgeShower
    }.apply {
      onHandshakeFinished()
    }

    private val pingStatistics = PingStatistics(
      openingTimeStamp = openingTimeStamp,
      requestPing = {
        stateMachine.fire(ClientAction.AddEvent(ClientRequestPingEvent(clientTimeStamp = TimeStamp.current.toInt() - openingTimeStamp)))
      }
    ).apply {
      onHandshakeFinished()
    }

    private val inputController = InputController(
      openingTimeStamp = openingTimeStamp,
      stateMachine = stateMachine,
      windowManager = windowManager
    ).apply {
      addListeners()
    }

    private val typing = when (ParamsProvider.SPECULATIVE_TYPING) {
      false -> Typing.NotSpeculativeTyping
      true -> Typing.SpeculativeTyping(windowManager::getWindowCanvas)
    }

    private val markdownPanelManager = MarkdownPanelManager(windowManager::getWindowZIndex) { link ->
      stateMachine.fire(ClientAction.AddEvent(ClientOpenLinkEvent(link)))
    }

    init {
      windowSizeController.addListener()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Message -> {
        val receiveTimeStamp = TimeStamp.current
        val decompressed = decompressor.decompress(action.message)
        val decompressTimeStamp = TimeStamp.current
        val commands = decoder.decode(decompressed)
        val decodeTimestamp = TimeStamp.current
        serverEventsProcessor.process(commands, pingStatistics, typing, markdownPanelManager)
        val drawTimestamp = TimeStamp.current

        ImageCacher.collectGarbage()

        eventsToSend.addAll(ImageCacher.extractImagesToRequest())

        messagingPolicy.onToClientMessage()
        sentReceivedBadgeShower.onToClientMessage()

        val drawEventCount = commands
          .filterIsInstance<ServerDrawCommandsEvent>()
          .map { it.drawEvents.size }
          .sum()

        val decompressingTimeMs = decompressTimeStamp - receiveTimeStamp
        val decodingTimeMs = decodeTimestamp - decompressTimeStamp
        val drawingTimeMs = drawTimestamp - decodeTimestamp

        ClientStats.toClientMessageSizeAverage.add(action.message.size.toLong())
        ClientStats.toClientMessageSizeRate.add(action.message.size.toLong())

        ClientStats.toClientCompressionRatioAverage.add(action.message.size.toDouble() / decompressed.size)

        ClientStats.drawEventCountAverage.add(drawEventCount.toLong())
        ClientStats.drawEventCountRate.add(drawEventCount.toLong())

        ClientStats.decompressingTimeMsAverage.add(decompressingTimeMs)
        ClientStats.decompressingTimeMsRate.add(decompressingTimeMs)

        ClientStats.decodingTimeMsAverage.add(decodingTimeMs)
        ClientStats.decodingTimeMsRate.add(decodingTimeMs)

        ClientStats.drawingTimeMsAverage.add(drawingTimeMs)
        ClientStats.drawingTimeMsRate.add(drawingTimeMs)

        val processTimestamp = TimeStamp.current

        val otherProcessingTimeMs = processTimestamp - drawTimestamp
        val totalTimeMs = processTimestamp - receiveTimeStamp

        ClientStats.otherProcessingTimeMsAverage.add(otherProcessingTimeMs)
        ClientStats.otherProcessingTimeMsRate.add(otherProcessingTimeMs)

        ClientStats.totalTimeMsAverage.add(totalTimeMs)
        ClientStats.totalTimeMsRate.add(totalTimeMs)

        if (ParamsProvider.SHOW_PROCESSING_TIME) {
          fun roundToTwoDecimals(number: Double) = number.asDynamic().toFixed(2)

          if (drawEventCount > 0) {
            logger.debug {
              "Timestamp is ${roundToTwoDecimals(processTimestamp)}:\t" +
              "draw events count: $drawEventCount,\t" +
              "decompressing: ${roundToTwoDecimals(decompressingTimeMs)} ms,\t\t" +
              "decoding: ${roundToTwoDecimals(decodingTimeMs)} ms,\t\t" +
              "drawing: ${roundToTwoDecimals(drawingTimeMs)} ms,\t\t" +
              "other processing: ${roundToTwoDecimals(otherProcessingTimeMs)} ms,\t\t" +
              "total: ${roundToTwoDecimals(totalTimeMs)} ms"
            }
          }
        }

        this
      }

      is ClientAction.AddEvent -> {
        val event = action.event

        if (event is ClientKeyPressEvent) {
          event.key.singleOrNull()?.let(typing::addChar)
        }

        eventsToSend.add(event)
        messagingPolicy.onAddEvent()

        this
      }

      is ClientAction.Flush -> {
        val message = encoder.encode(eventsToSend).also { eventsToSend.clear() }
        webSocket.send(compressor.compress(message))

        sentReceivedBadgeShower.onToServerMessage()

        this
      }

      is ClientAction.WindowResize -> {
        serverEventsProcessor.onResized()

        this
      }

      is ClientAction.WebSocket.Close -> {
        logger.info { "Connection is closed..." }

        pingStatistics.onClose()

        windowDataEventsProcessor.onClose()

        inputController.removeListeners()

        windowSizeController.removeListener()

        typing.dispose()

        markdownPanelManager.disposeAll()

        showDisconnectedMessage(action.url, action.closeCode)

        Disconnected
      }

      else -> super.consume(action)
    }
  }

  object Disconnected : ClientState()

  companion object {

    private val logger = Logger(ClientState::class.simpleName!!)

    private const val NORMAL_CLOSURE_STATUS_CODE: Short = 1000
    private const val GOING_AWAY_STATUS_CODE: Short = 1001

    private fun showDisconnectedMessage(url: String, closeCode: Short) {
      Do exhaustive when (closeCode in setOf(NORMAL_CLOSURE_STATUS_CODE, GOING_AWAY_STATUS_CODE)) {
        true -> OnScreenMessenger.showText(
          "Disconnected",
          "It seems that your connection is ended normally.",
          canReload = true
        )

        false -> OnScreenMessenger.showText(
          "Connection problem",
          buildString {
            append("There is no connection to <strong>$url</strong>. ")
            append("The browser console can contain the error and a more detailed description. ")
            append("Everything we know is that <code>CloseEvent.code=$closeCode</code> ")
            append(
              "(<a href='https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#Status_codes'>https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#Status_codes</a>).")
          },
          canReload = true
        )
      }
    }
  }
}
