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
package org.jetbrains.projector.client.web.state

import kotlinext.js.jsObject
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.projector.client.common.misc.ImageCacher
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.common.misc.TimeStamp
import org.jetbrains.projector.client.common.protocol.KotlinxJsonToClientHandshakeDecoder
import org.jetbrains.projector.client.common.protocol.KotlinxJsonToServerHandshakeEncoder
import org.jetbrains.projector.client.web.RenderingQueue
import org.jetbrains.projector.client.web.ServerEventsProcessor
import org.jetbrains.projector.client.web.WindowSizeController
import org.jetbrains.projector.client.web.component.MarkdownPanelManager
import org.jetbrains.projector.client.web.debug.DivSentReceivedBadgeShower
import org.jetbrains.projector.client.web.debug.NoSentReceivedBadgeShower
import org.jetbrains.projector.client.web.debug.SentReceivedBadgeShower
import org.jetbrains.projector.client.web.input.InputController
import org.jetbrains.projector.client.web.misc.*
import org.jetbrains.projector.client.web.protocol.SupportedTypesProvider
import org.jetbrains.projector.client.web.speculative.Typing
import org.jetbrains.projector.client.web.ui.ReconnectionMessage
import org.jetbrains.projector.client.web.ui.ReconnectionMessageProps
import org.jetbrains.projector.client.web.window.OnScreenMessenger
import org.jetbrains.projector.client.web.window.WindowDataEventsProcessor
import org.jetbrains.projector.client.web.window.WindowManager
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.MessageDecoder
import org.jetbrains.projector.common.protocol.MessageEncoder
import org.jetbrains.projector.common.protocol.compress.MessageCompressor
import org.jetbrains.projector.common.protocol.compress.MessageDecompressor
import org.jetbrains.projector.common.protocol.handshake.*
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toClient.ToClientMessageDecoder
import org.jetbrains.projector.common.protocol.toServer.*
import org.jetbrains.projector.util.logging.Logger
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.*
import org.w3c.dom.events.Event
import react.createElement
import react.dom.render
import kotlin.math.roundToInt

sealed class ClientState {

  open fun consume(action: ClientAction): ClientState {
    logger.error { "${this::class.simpleName}: can't consume ${action::class.simpleName}" }
    return this
  }

  private class AppLayers(
    val reconnectionMessageUpdater: (String?) -> Unit,
  )

  object UninitializedPage : ClientState() {

    private fun configureWebPage(url: String): AppLayers {
      document.body!!.apply {
        style.apply {
          backgroundColor = ParamsProvider.BACKGROUND_COLOR
          asDynamic().overscrollBehaviorX = "none"
          asDynamic().overscrollBehaviorY = "none"
          asDynamic().touchAction = "none"
        }

        oncontextmenu = { false }
      }

      val reloadingMessageLayer = (document.createElement("div") as HTMLDivElement).apply {
        id = "reloading-message-layer"
        style.apply {
          position = "absolute"
          zIndex = "1"
          margin = "0px"
          width = "100%"
          height = "100%"
          top = "0px"
          left = "0px"
          asDynamic().pointerEvents = "none"
        }

        document.body!!.appendChild(this)
      }

      val reconnectionMessageUpdater = { newMessage: String? ->
        val reconnectionMessage = createElement(ReconnectionMessage::class.js, jsObject<ReconnectionMessageProps> {
          this.message = newMessage
        })
        render(reconnectionMessage, reloadingMessageLayer)
      }

      reconnectionMessageUpdater(null)

      OnScreenMessenger.showText("Starting connection", "Waiting for response from $url...", canReload = false)

      if (!(window.asDynamic().isSecureContext as Boolean) && ParamsProvider.SHOW_NOT_SECURE_WARNING) {
        window.alert(buildString {
          append("Warning: You have opened this page in a not secure context. ")
          append("This means that the browser will restrict usage of some features such as the clipboard access. ")
          append("You can find the full list here: ")
          append("https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts/features_restricted_to_secure_contexts. ")
          appendLine("To make the context secure, please use HTTPS for the web page or host it locally.")
          append("To disable this warning, please add `notSecureWarning=false` query parameter.")
        })
      }

      return AppLayers(
        reconnectionMessageUpdater = reconnectionMessageUpdater,
      )
    }

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.Start -> {
        val layers = configureWebPage(action.url)

        val webSocket = createWebSocketConnection(action.url, action.stateMachine)

        WaitingOpening(
          stateMachine = action.stateMachine,
          webSocket = webSocket,
          windowSizeController = action.windowSizeController,
          layers = layers,
        )
      }

      else -> super.consume(action)
    }
  }

  private class WaitingOpening(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    private val layers: AppLayers,
    private val onHandshakeFinish: () -> Unit = {},
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Open -> {
        OnScreenMessenger.showText("Connection is opened", "Sending handshake...", canReload = false)

        ClientStats.resetStats(action.openingTimeStamp.toDouble())

        val handshakeEvent = with(SupportedTypesProvider) {
          ToServerHandshakeEvent(
            commonVersion = COMMON_VERSION,
            commonVersionId = commonVersionList.indexOf(COMMON_VERSION),
            token = ParamsProvider.HANDSHAKE_TOKEN,
            displays = listOf(DisplayDescription(0, 0, windowSizeController.currentSize.width, windowSizeController.currentSize.height, 1.0)),
            clientDoesWindowManagement = false,
            supportedToClientCompressions = supportedToClientDecompressors.map(MessageDecompressor<*>::compressionType),
            supportedToClientProtocols = supportedToClientDecoders.map(MessageDecoder<*, *>::protocolType),
            supportedToServerCompressions = supportedToServerCompressors.map(MessageCompressor<*>::compressionType),
            supportedToServerProtocols = supportedToServerEncoders.map(MessageEncoder<*, *>::protocolType)
          )
        }

        webSocket.send("$HANDSHAKE_VERSION;${handshakeVersionList.indexOf(HANDSHAKE_VERSION)}")
        webSocket.send(KotlinxJsonToServerHandshakeEncoder.encode(handshakeEvent))

        OnScreenMessenger.showText("Connection is opened", "Handshake is sent...", canReload = false)

        WaitingHandshakeReply(
          stateMachine = stateMachine,
          webSocket = webSocket,
          windowSizeController = windowSizeController,
          openingTimeStamp = action.openingTimeStamp,
          onHandshakeFinish = onHandshakeFinish,
          layers = layers,
        )
      }

      is ClientAction.WebSocket.Close -> {
        showDisconnectedMessage(webSocket.url, action)
        onHandshakeFinish()

        Disconnected
      }

      else -> super.consume(action)
    }
  }

  private class WaitingHandshakeReply(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    private val openingTimeStamp: Int,
    private val onHandshakeFinish: () -> Unit,
    private val layers: AppLayers,
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Message -> {
        val command = KotlinxJsonToClientHandshakeDecoder.decode(action.message)

        Do exhaustive when (command) {
          is ToClientHandshakeFailureEvent -> {
            OnScreenMessenger.showText("Handshake failure", "Reason: ${command.reason}", canReload = true)
            webSocket.close()
            onHandshakeFinish()

            Disconnected
          }

          is ToClientHandshakeSuccessEvent -> {
            command.colors?.let { ProjectorUI.setColors(it) }

            FontFaceAppender.removeAppendedFonts()

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
              compressor = SupportedTypesProvider.supportedToServerCompressors.first { it.compressionType == command.toServerCompression },
              onHandshakeFinish = onHandshakeFinish,
              layers = layers,
            )
          }
        }
      }

      is ClientAction.WebSocket.Close -> {
        showDisconnectedMessage(webSocket.url, action)
        onHandshakeFinish()

        Disconnected
      }

      else -> super.consume(action)
    }
  }

  private class LoadingFonts(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    private val openingTimeStamp: Int,
    private val encoder: ToServerMessageEncoder,
    private val decoder: ToClientMessageDecoder,
    private val decompressor: MessageDecompressor<ByteArray>,
    private val compressor: MessageCompressor<String>,
    private val onHandshakeFinish: () -> Unit,
    private val layers: AppLayers,
  ) : ClientState() {

    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.LoadAllFonts -> {
        logger.debug { "All fonts are loaded. Ready to draw!" }

        webSocket.send("Unused string meaning fonts loading is done")  // todo: change this string
        onHandshakeFinish()

        ReadyToDraw(
          stateMachine = stateMachine,
          webSocket = webSocket,
          windowSizeController = windowSizeController,
          openingTimeStamp = openingTimeStamp,
          encoder = encoder,
          decoder = decoder,
          decompressor = decompressor,
          compressor = compressor,
          layers = layers,
          ImageCacher()
        )
      }

      else -> super.consume(action)
    }
  }

  private class ReadyToDraw(
    private val stateMachine: ClientStateMachine,
    private val webSocket: WebSocket,
    private val windowSizeController: WindowSizeController,
    openingTimeStamp: Int,
    private val encoder: ToServerMessageEncoder,
    private val decoder: ToClientMessageDecoder,
    private val decompressor: MessageDecompressor<ByteArray>,
    private val compressor: MessageCompressor<String>,
    private val layers: AppLayers,
    private val imageCacher: ImageCacher
  ) : ClientState() {

    private val eventsToSend = mutableListOf<ClientEvent>(ClientSetKeymapEvent(nativeKeymap))

    private val windowManager = WindowManager(stateMachine, imageCacher)

    private val windowDataEventsProcessor = WindowDataEventsProcessor(windowManager)

    private val renderingQueue = RenderingQueue(windowManager)

    private val drawPendingEvents = GlobalScope.launch {
      // redraw windows in case any missing images are loaded now
      while (true) {
        renderingQueue.drawPendingEvents()
        delay(ParamsProvider.REPAINT_INTERVAL_MS.toLong())
      }
    }

    private val serverEventsProcessor = ServerEventsProcessor(windowManager, windowDataEventsProcessor, renderingQueue)

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
      windowManager = windowManager,
      windowPositionByIdGetter = windowManager::get,
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

    private val closeBlocker = when (ParamsProvider.BLOCK_CLOSING) {
      true -> CloseBlockerImpl(window)
      false -> NopCloseBlocker
    }.apply {
      setListener()
    }

    private val selectionBlocker = SelectionBlocker(window).apply {
      blockSelection()
    }

    private val connectionWatcher = ConnectionWatcher { stateMachine.fire(ClientAction.WebSocket.NoReplies(it)) }.apply {
      setWatcher()
    }

    init {
      windowSizeController.addListener()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun consume(action: ClientAction) = when (action) {
      is ClientAction.WebSocket.Message -> {
        connectionWatcher.resetTime()

        val receiveTimeStamp = TimeStamp.current
        val decompressed = decompressor.decompress(action.message)
        val decompressTimeStamp = TimeStamp.current
        val commands = decoder.decode(decompressed)
        val decodeTimestamp = TimeStamp.current
        serverEventsProcessor.process(commands, pingStatistics, typing, markdownPanelManager, inputController)
        val drawTimestamp = TimeStamp.current

        imageCacher.collectGarbage()

        eventsToSend.addAll(imageCacher.extractImagesToRequest())

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
          typing.addEventChar(event)
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
        Do exhaustive when (action.endedNormally) {
          true -> {
            logger.info { "Connection is closed..." }

            drawPendingEvents.cancel()
            pingStatistics.onClose()
            windowDataEventsProcessor.onClose()
            inputController.removeListeners()
            windowSizeController.removeListener()
            typing.dispose()
            markdownPanelManager.disposeAll()
            closeBlocker.removeListener()
            selectionBlocker.unblockSelection()
            connectionWatcher.removeWatcher()

            showDisconnectedMessage(webSocket.url, action)
            Disconnected
          }

          false -> reloadConnection("Connection is closed unexpectedly, retrying the connection...")
        }
      }

      is ClientAction.WebSocket.NoReplies ->
        reloadConnection("No messages from server for ${action.elapsedTimeMs} ms, retrying the connection...")

      else -> super.consume(action)
    }

    private fun reloadConnection(messageText: String): ClientState {
      logger.info { messageText }

      drawPendingEvents.cancel()
      pingStatistics.onClose()
      inputController.removeListeners()
      windowSizeController.removeListener()
      typing.dispose()
      connectionWatcher.removeWatcher()

      layers.reconnectionMessageUpdater(messageText)

      val newConnection = createWebSocketConnection(webSocket.url, stateMachine)
      return WaitingOpening(stateMachine, newConnection, windowSizeController, layers) {
        windowDataEventsProcessor.onClose()
        markdownPanelManager.disposeAll()
        closeBlocker.removeListener()
        selectionBlocker.unblockSelection()
        layers.reconnectionMessageUpdater(null)
      }
    }
  }

  object Disconnected : ClientState()

  companion object {

    private val logger = Logger<ClientState>()

    private const val NORMAL_CLOSURE_STATUS_CODE: Short = 1000
    private const val GOING_AWAY_STATUS_CODE: Short = 1001

    private val ClientAction.WebSocket.Close.endedNormally: Boolean
      get() = this.wasClean && this.code in setOf(NORMAL_CLOSURE_STATUS_CODE, GOING_AWAY_STATUS_CODE)

    private fun showDisconnectedMessage(url: String, action: ClientAction.WebSocket.Close) {
      val reason = action.reason.ifBlank { null }?.let { "Reason: $it" } ?: "The server hasn't reported a reason of the disconnection."

      Do exhaustive when (action.endedNormally) {
        true -> OnScreenMessenger.showText(
          "Disconnected",
          "It seems that your connection is ended normally. $reason",
          canReload = true
        )

        false -> OnScreenMessenger.showText(
          "Connection problem",
          "There is no connection to <strong>$url</strong>. " +
          "The browser console can contain the error and a more detailed description. " +
          "Everything we know is that <code>CloseEvent.code=${action.code}</code>, " +
          "<code>CloseEvent.wasClean=${action.wasClean}</code>. $reason",
          canReload = true
        )
      }
    }

    private fun createWebSocketConnection(url: String, stateMachine: ClientStateMachine): WebSocket {
      return WebSocket(url).apply {
        binaryType = BinaryType.ARRAYBUFFER

        onopen = fun(_: Event) {
          stateMachine.fire(ClientAction.WebSocket.Open(openingTimeStamp = TimeStamp.current.roundToInt()))
        }

        onclose = fun(event: Event) {
          require(event is CloseEvent)

          stateMachine.fire(ClientAction.WebSocket.Close(wasClean = event.wasClean, code = event.code, reason = event.reason))
        }

        onmessage = fun(messageEvent: MessageEvent) {
          stateMachine.fire(ClientAction.WebSocket.Message(message = (messageEvent.data as ArrayBuffer).asByteArray()))
        }
      }
    }
  }
}
