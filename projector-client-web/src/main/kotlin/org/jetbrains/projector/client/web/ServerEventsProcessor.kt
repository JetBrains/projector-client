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

import org.jetbrains.projector.client.common.RenderingQueue
import org.jetbrains.projector.client.web.component.MarkdownPanelManager
import org.jetbrains.projector.client.web.input.InputController
import org.jetbrains.projector.client.web.misc.*
import org.jetbrains.projector.client.web.speculative.Typing
import org.jetbrains.projector.client.web.state.ClientAction
import org.jetbrains.projector.client.web.state.ClientStateMachine
import org.jetbrains.projector.client.web.state.ProjectorUI
import org.jetbrains.projector.client.web.window.OnScreenMessenger
import org.jetbrains.projector.client.web.window.WebWindowManager
import org.jetbrains.projector.client.web.window.WindowDataEventsProcessor
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.toClient.*

class ServerEventsProcessor(
  private val windowManager: WebWindowManager,
  private val windowDataEventsProcessor: WindowDataEventsProcessor,
  private val renderingQueue: RenderingQueue,
  private val stateMachine: ClientStateMachine,
) {

  private val clipboardHandler = ClipboardHandler { stateMachine.fire(ClientAction.AddEvent(it)) }

  fun process(
    commands: ToClientMessageType, pingStatistics: PingStatistics, typing: Typing, markdownPanelManager: MarkdownPanelManager,
    inputController: InputController,
  ) {
    val drawCommandsEvents = mutableListOf<ServerDrawCommandsEvent>()

    commands.forEach { command ->
      Do exhaustive when (command) {
        is ServerWindowSetChangedEvent -> {
          windowDataEventsProcessor.process(command)
          markdownPanelManager.updatePlacements()
        }

        is ServerDrawCommandsEvent -> drawCommandsEvents.add(command)

        is ServerImageDataReplyEvent -> windowManager.imageCacher.putImageData(
          command.imageId,
          command.imageData,
        )

        is ServerCaretInfoChangedEvent -> {
          typing.changeCaretInfo(command.data)
          inputController.handleCaretInfoChange(command.data)
        }

        is ServerClipboardEvent -> clipboardHandler.copyText(command.stringContent)

        is ServerPingReplyEvent -> pingStatistics.onPingReply(command)

        is ServerMarkdownEvent -> when (command) {
          is ServerMarkdownEvent.ServerMarkdownShowEvent -> markdownPanelManager.show(command.panelId, command.show)
          is ServerMarkdownEvent.ServerMarkdownResizeEvent -> markdownPanelManager.resize(command.panelId, command.size)
          is ServerMarkdownEvent.ServerMarkdownMoveEvent -> markdownPanelManager.move(command.panelId, command.point)
          is ServerMarkdownEvent.ServerMarkdownDisposeEvent -> markdownPanelManager.dispose(command.panelId)
          is ServerMarkdownEvent.ServerMarkdownPlaceToWindowEvent -> markdownPanelManager.placeToWindow(command.panelId, command.windowId)
          is ServerMarkdownEvent.ServerMarkdownSetHtmlEvent -> markdownPanelManager.setHtml(command.panelId, command.html)
          is ServerMarkdownEvent.ServerMarkdownSetCssEvent -> markdownPanelManager.setCss(command.panelId, command.css)
          is ServerMarkdownEvent.ServerMarkdownScrollEvent -> markdownPanelManager.scroll(command.panelId, command.scrollOffset)
        }

        is ServerBrowseUriEvent -> UriHandler.browse(command.link)

        is ServerWindowColorsEvent -> {
          ProjectorUI.setColors(command.colors)
          // todo: should WindowManager.lookAndFeelChanged() be called here?
          OnScreenMessenger.lookAndFeelChanged()
        }
      }
    }

    drawCommandsEvents.removeAll {
      val target = it.target as? ServerDrawCommandsEvent.Target.Onscreen ?: return@removeAll false
      target.windowId in windowDataEventsProcessor.excludedWindowIds
    }

    // todo: determine the moment better
    if (drawCommandsEvents.any { it.drawEvents.any { drawEvent -> drawEvent is ServerDrawStringEvent } }) {
      typing.removeSpeculativeImage()
    }

    renderingQueue.add(drawCommandsEvents)
    renderingQueue.drawNewEvents()
  }

  fun onResized() {
    windowDataEventsProcessor.onResized()
  }

}
