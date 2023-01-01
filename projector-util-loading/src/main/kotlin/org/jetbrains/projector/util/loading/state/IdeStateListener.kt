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
@file:UseProjectorLoader

package org.jetbrains.projector.util.loading.state

import org.jetbrains.projector.util.loading.UseProjectorLoader

@UseProjectorLoader
public interface IdeStateListener {

  /**
   * States occurrence of which should be listened.
   */
  public val requiredStates: Set<IdeState>

  /**
   * Function to be invoked when state occurred.
   *
   * @param state state that occurred
   */
  public fun onStateOccurred(state: IdeState)

}

@Suppress("FunctionName")
public fun IdeStateListener(vararg ideState: IdeState, callback: (IdeState) -> Unit): IdeStateListener =
  IdeStateListener(setOf(*ideState), callback)

@Suppress("FunctionName")
public fun IdeStateListener(ideState: Set<IdeState>, callback: (IdeState) -> Unit): IdeStateListener = object : IdeStateListener {

  override val requiredStates: Set<IdeState> = ideState

  override fun onStateOccurred(state: IdeState) = callback(state)
}
