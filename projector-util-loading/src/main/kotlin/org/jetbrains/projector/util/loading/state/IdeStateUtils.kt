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

import org.jetbrains.projector.common.misc.rangeTo
import org.jetbrains.projector.common.misc.until
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.logging.Logger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private val logger = Logger("IdeState")

/**
 * Invokes given function when IDEA is at certain loading state.
 * Function will be invoked immediately if the state already occurred
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
  val listener = IdeTargetStateListener(this, onStateOccurred)
  registerStateListener(purpose, listener)
}
/**
 * Registers listener to be invoked on state change.
 * Listener function will be invoked immediately on the current thread for already occurred states.
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

  lock.withLock {
    if (listeners.contains(listener)) return

    if (purpose != null) {
      logger.debug { "Starting attempts to $purpose" }
    }

    if (!listenerThreadStarted) {
      listenerThreadStarted = true
      runListenerThread()
    } else {
      for (state in IdeState.values().first() until startingState) {
        if (listener.onStateOccurred(state)) {
          logJobDone(purpose)
          return
        }
      }
    }
    listeners.add(IdeStateListenerWithPurpose(purpose, listener))
    condition.signal()
  }
}

private val lock = ReentrantLock()
private val condition = lock.newCondition()
private val listeners = mutableSetOf<IdeStateListenerWithPurpose>()

private var startingState = IdeState.values().first()
private var listenerThreadStarted = false

private fun runListenerThread() {
  thread {
    while (true) {
      try {
        lock.withLock {

          for (state in startingState..IdeState.values().last()) {
            if (state.isOccurred) {
              listeners.removeIf {
                withCondition(it.onStateOccurred(state)) {
                  logJobDone(it.purpose)
                }
              }
              startingState = IdeState.values()[state.ordinal + 1]
            } else { // if state is not occurred yet, then all next didn't occur as well
              break
            }
          }

          if (listeners.isEmpty()) {
            condition.await()
          }
        }

        Thread.sleep(5)
      }
      catch (t: Throwable) {
        t.printStackTrace()
      }
    }
  }
}

private fun logJobDone(purpose: String?) {
  if (purpose != null) {
    logger.debug { "\"$purpose\" is done" }
  }
}

private inline fun withCondition(condition: Boolean, action: () -> Unit): Boolean {
  if (condition) { action() }
  return condition
}

@UseProjectorLoader
private class IdeTargetStateListener(
  private val targetState: IdeState,
  private val onStateOccurred: () -> Unit,
) : IdeStateListener {

  override fun onStateOccurred(state: IdeState): Boolean {
    if (state == targetState) {
      onStateOccurred()
      return true
    }

    return false
  }
}

@UseProjectorLoader
private class IdeStateListenerWithPurpose(
  val purpose: String?,
  private val listener: IdeStateListener,
) : IdeStateListener by listener
