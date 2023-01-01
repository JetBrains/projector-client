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

import kotlinx.coroutines.*
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.logging.Logger

// This must be before logger declaration, as loggers will try to call other methods in this file
@OptIn(DelicateCoroutinesApi::class)
private val scope = CoroutineScope(newSingleThreadContext("stateListenerThread"))

private val logger = Logger("IdeState")

/**
 * Invokes given function when IDEA is at certain loading state.
 * Function will be invoked on the dedicated state listener thread.
 *
 * To check whether IDE classes available, use [IdeState.isIdeAttached]
 *
 * @receiver IDEA state at which function will be executed
 * @param purpose               purpose of state querying, used for logging. Pass `null` to disable logging
 * @param onStateOccurred       function invoked when target state occurred
 */
public fun IdeState.whenOccurred(
  purpose: String?,
  onStateOccurred: () -> Unit,
) {
  val listener = IdeStateListener(this) { onStateOccurred() }
  registerStateListener(purpose, listener)
}

/**
 * Registers listener to be invoked on state change.
 * Listener function will be invoked on the dedicated state listener thread.
 *
 * @param purpose               purpose of state querying, used for logging. Pass `null` to disable logging
 * @param listener              listener to register
 */
public fun registerStateListener(purpose: String?, listener: IdeStateListener) {
  if (!IdeState.isIdeAttached) {
    if (purpose != null && IdeState.attachToIde) {
      logger.info { "Can't $purpose. It's OK if you don't run an IntelliJ platform based app." }
    }
    return
  }

  scope.launch { runLoopForListener(purpose, listener) }
}

private suspend fun runLoopForListener(purpose: String?, listener: IdeStateListener) {

  if (purpose != null) {
    logger.debug { "Starting attempts to $purpose" }
  }

  val sortedStates = listener.requiredStates.toSortedSet()

  listenerLoop@ while (true) {
    val stateIterator = sortedStates.iterator()
    iterationLoop@ while (stateIterator.hasNext()) {
      val state = stateIterator.next()
      if (state.isOccurred) {
        try {
          listener.onStateOccurred(state)
        } catch (t: Throwable) {
          logger.error(t) { "Error in IDE state listener" }
        }

        stateIterator.remove()

        if (sortedStates.isEmpty()) {
          if (purpose != null) {
            logger.debug { "\"$purpose\" is done" }
          }
          break@listenerLoop
        }

      } else { // if state is not occurred yet, then all next didn't occur as well
        break@iterationLoop
      }
    }

    delay(5)
  }
}
