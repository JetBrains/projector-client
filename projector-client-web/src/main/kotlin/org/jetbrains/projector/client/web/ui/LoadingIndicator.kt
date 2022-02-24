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
package org.jetbrains.projector.client.web.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.Nth.Companion.Functional
import org.jetbrains.compose.web.dom.Div

private const val CHILDREN_COUNT = 12

private object LoadingIndicatorStyleSheet : StyleSheet() {

  private val ldsSpinnerFrames by keyframes {
    0.percent {
      opacity(1)
    }

    100.percent {
      opacity(0)
    }
  }

  val ldsSpinner by style {
    color(Color.white)
    display(DisplayStyle.InlineBlock)
    position(Position.Relative)
    width(80.px)
    height(80.px)

    type("div") style {
      property("transform-origin", "40px 40px")
      animation(ldsSpinnerFrames) {
        duration(1.2.s)
        timingFunction(AnimationTimingFunction.Linear)
        iterationCount(null)  // null is infinite
      }
    }

    type("div") + after style {
      property("content", """" """")
      display(DisplayStyle.Block)
      position(Position.Absolute)
      top(3.px)
      left(37.px)
      width(6.px)
      height(18.px)
      borderRadius(20.percent)
      background("#fff")
    }

    (1..CHILDREN_COUNT).forEach { n ->
      type("div") + nthChild(Functional(b = n)) style {
        property("transform", "rotate(${(n - 1) * 30}deg)")
        property("animation-delay", "${(-1.2 + n * 0.1)}s")
      }
    }
  }
}

@Composable
fun LoadingIndicator() {
  Style(LoadingIndicatorStyleSheet)

  Div({ classes(LoadingIndicatorStyleSheet.ldsSpinner) }) {
    repeat(CHILDREN_COUNT) {
      Div()
    }
  }
}
