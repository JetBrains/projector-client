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
@file:UseProjectorLoader

package org.jetbrains.projector.util.loading.state

import kotlinx.coroutines.*
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.logging.Logger
import java.util.*

private val logger = Logger("IdeState")

/**
 * Invokes given function when IDEA is at certain loading state.
 * Function will be invoked immediately on the **current thread** if the state already occurred
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
 * Listener function will be invoked immediately on the **current thread** for already occurred states.
 *
 * @param purpose               purpose of state querying, used for logging. Pass `null` to disable logging
 * @param listener              listener to register
 */
public fun registerStateListener(purpose: String?, listener: IdeStateListener) {
  if (!IdeState.isIdeAttached) {
    if (purpose != null) {
      logger.info { "Can't $purpose. It's OK if you don't run an IntelliJ platform based app." }
    }
    return
  }

  val listenerWithPurpose = IdeStateListenerWithPurpose(purpose, listener)
  scope.launch { runLoopForListener(listenerWithPurpose) }
}

@OptIn(DelicateCoroutinesApi::class)
private val scope = CoroutineScope(newSingleThreadContext("stateListenerThread"))

private suspend fun runLoopForListener(listenerWithPurpose: IdeStateListenerWithPurpose) {

  listenerWithPurpose.purpose?.also {
    logger.debug { "Starting attempts to $it" }
  }

  threadLoop@ while (true) {
    val stateIterator = listenerWithPurpose.requiredStates.iterator()
    iterationLoop@ while (stateIterator.hasNext()) {
      val state = stateIterator.next()
      if (state.isOccurred) {
        try {
          listenerWithPurpose.onStateOccurred(state)
        } catch (t: Throwable) {
          logger.error(t) { "Error in IDE state listener" }
        }

        stateIterator.remove()

        if (listenerWithPurpose.requiredStates.isEmpty()) {
          logJobDone(listenerWithPurpose.purpose)
          break@threadLoop
        }

      } else { // if state is not occurred yet, then all next didn't occur as well
        break@iterationLoop
      }
    }

    delay(5)
  }
}

private fun logJobDone(purpose: String?) {
  if (purpose != null) {
    logger.debug { "\"$purpose\" is done" }
  }
}

@UseProjectorLoader
private class IdeStateListenerWithPurpose(
  val purpose: String?,
  private val listener: IdeStateListener,
) : IdeStateListener by listener {

  override val requiredStates: SortedSet<IdeState> = listener.requiredStates.toSortedSet()
}
