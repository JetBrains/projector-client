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
package org.jetbrains.projector.server.core.ij.log

import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.loading.state.IdeState
import org.jetbrains.projector.util.loading.state.whenOccurred
import org.jetbrains.projector.util.logging.ConsoleJvmLogger
import org.jetbrains.projector.util.logging.Logger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// todo: make it internal after moving ProjectorServer to this
@UseProjectorLoader
public class DelegatingJvmLogger(tag: String) : Logger {

  private var ideaLoggerState: IdeaLoggerState = IdeaLoggerState.WaitingIdeaLoggerState()

  private val ideaLoggerStateLock = ReentrantReadWriteLock(true)

  private val consoleJvmLogger = ConsoleJvmLogger(tag)

  init {
    if (IdeState.isIdeAttached) {
      IdeState.BOOTSTRAP.whenOccurred(purpose = null) {
        ideaLoggerStateLock.write {
          val notLoggedEvents = (ideaLoggerState as IdeaLoggerState.WaitingIdeaLoggerState).notLoggedEvents

          val initializedIdeaLoggerState = IdeaLoggerState.InitializedIdeaLoggerState(tag)

          notLoggedEvents.forEach {
            Do exhaustive when (it) {
              is LoggingEvent.Error -> initializedIdeaLoggerState.ijJvmLogger.error(it.t, it.lazyMessage)
              is LoggingEvent.Info -> initializedIdeaLoggerState.ijJvmLogger.info(it.t, it.lazyMessage)
              is LoggingEvent.Debug -> initializedIdeaLoggerState.ijJvmLogger.debug(it.t, it.lazyMessage)
            }
          }

          ideaLoggerState = initializedIdeaLoggerState
        }
      }
    } else {
      ideaLoggerStateLock.write {
        ideaLoggerState = IdeaLoggerState.NoIj
      }
    }
  }

  override fun error(t: Throwable?, lazyMessage: () -> String): Unit = ideaLoggerStateLock.read {
    log(
      consoleJvmLogger = consoleJvmLogger,
      ideaLoggerState = ideaLoggerState,
      logFunction = Logger::error,
      loggingEventConstructor = LoggingEvent::Error,
      t = t,
      lazyMessage = lazyMessage,
    )
  }

  override fun info(t: Throwable?, lazyMessage: () -> String): Unit = ideaLoggerStateLock.read {
    log(
      consoleJvmLogger = consoleJvmLogger,
      ideaLoggerState = ideaLoggerState,
      logFunction = Logger::info,
      loggingEventConstructor = LoggingEvent::Info,
      t = t,
      lazyMessage = lazyMessage,
    )
  }

  override fun debug(t: Throwable?, lazyMessage: () -> String): Unit = ideaLoggerStateLock.read {
    log(
      consoleJvmLogger = consoleJvmLogger,
      ideaLoggerState = ideaLoggerState,
      logFunction = Logger::debug,
      loggingEventConstructor = LoggingEvent::Debug,
      t = t,
      lazyMessage = lazyMessage,
    )
  }

  private companion object {

    private fun log(
      consoleJvmLogger: ConsoleJvmLogger,
      ideaLoggerState: IdeaLoggerState,
      logFunction: Logger.(t: Throwable?, lazyMessage: () -> String) -> Unit,
      loggingEventConstructor: (t: Throwable?, lazyMessage: () -> String) -> LoggingEvent,
      t: Throwable?,
      lazyMessage: () -> String,
    ) {
      consoleJvmLogger.logFunction(t, lazyMessage)

      Do exhaustive when (ideaLoggerState) {
        is IdeaLoggerState.WaitingIdeaLoggerState -> ideaLoggerState.notLoggedEvents.add(loggingEventConstructor(t, lazyMessage))
        is IdeaLoggerState.InitializedIdeaLoggerState -> ideaLoggerState.ijJvmLogger.logFunction(t, lazyMessage)
        is IdeaLoggerState.NoIj -> Unit
      }
    }

    private sealed class LoggingEvent(val t: Throwable?, val lazyMessage: () -> String) {

      class Error(t: Throwable?, lazyMessage: () -> String) : LoggingEvent(t, lazyMessage)
      class Info(t: Throwable?, lazyMessage: () -> String) : LoggingEvent(t, lazyMessage)
      class Debug(t: Throwable?, lazyMessage: () -> String) : LoggingEvent(t, lazyMessage)
    }

    private sealed class IdeaLoggerState {

      class WaitingIdeaLoggerState : IdeaLoggerState() {

        val notLoggedEvents = ArrayDeque<LoggingEvent>()
      }

      class InitializedIdeaLoggerState(tag: String) : IdeaLoggerState() {

        val ijJvmLogger: Logger = IjJvmLogger(tag)
      }

      object NoIj : IdeaLoggerState()
    }
  }
}
