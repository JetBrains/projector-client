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
package org.jetbrains.projector.client.web.ui

import kotlinx.css.*
import org.jetbrains.projector.client.web.externalDeclarartion.loadingIndicator
import org.jetbrains.projector.client.web.externalDeclarartion.styleRoot
import react.*
import styled.css
import styled.styledH1
import styled.styledSpan

external interface ReconnectionMessageProps : Props {

  var message: String?
}

class ReconnectionMessage : RComponent<ReconnectionMessageProps, State>() {

  override fun RBuilder.render() {
    props.message?.let { message ->
      styledSpan {
        css {
          position = Position.absolute
          width = 100.pct
          height = 100.pct
          top = 0.px
          left = 0.px
          pointerEvents = PointerEvents.none

          background = "rgba(127, 127, 127, 0.5)"
          display = Display.block

          textAlign = TextAlign.center

          classes.add("connection-watcher-warning")
        }

        styledH1 {
          css {
            put("text-shadow", "-1px 0 #ccc, 0 1px #ccc, 1px 0 #ccc, 0 -1px #ccc")  // border
          }

          +message
        }

        styleRoot {
          loadingIndicator {
            attrs {
              segmentLength = 50.0
              segmentWidth = 10.0
              spacing = 20.0
            }
          }
        }
      }
    }
  }
}
