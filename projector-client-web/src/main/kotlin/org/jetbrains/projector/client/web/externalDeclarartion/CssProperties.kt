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
package org.jetbrains.projector.client.web.externalDeclarartion

import org.w3c.dom.css.CSSStyleDeclaration

// TODO remove after https://youtrack.jetbrains.com/issue/KT-49410 is implemented
inline var CSSStyleDeclaration.textDecorationThickness: String?
  get() = asDynamic().textDecorationThickness.unsafeCast<String?>()
  set(value) {
    asDynamic().textDecorationThickness = value
  }

// todo: https://youtrack.jetbrains.com/issue/KT-51370
inline var CSSStyleDeclaration.overscrollBehaviorX: String?
  get() = asDynamic().overscrollBehaviorX.unsafeCast<String?>()
  set(value) {
    asDynamic().overscrollBehaviorX = value
  }

// todo: https://youtrack.jetbrains.com/issue/KT-51370
inline var CSSStyleDeclaration.overscrollBehaviorY: String?
  get() = asDynamic().overscrollBehaviorY.unsafeCast<String?>()
  set(value) {
    asDynamic().overscrollBehaviorY = value
  }

// todo: https://youtrack.jetbrains.com/issue/KT-51370
inline var CSSStyleDeclaration.touchAction: String?
  get() = asDynamic().touchAction.unsafeCast<String?>()
  set(value) {
    asDynamic().touchAction = value
  }

// todo: https://youtrack.jetbrains.com/issue/KT-51370
inline var CSSStyleDeclaration.pointerEvents: String?
  get() = asDynamic().pointerEvents.unsafeCast<String?>()
  set(value) {
    asDynamic().pointerEvents = value
  }
