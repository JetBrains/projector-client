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
package org.jetbrains.projector.server.core.ij

import org.jetbrains.projector.server.core.classloader.ProjectorClassLoaderSetup
import org.jetbrains.projector.util.logging.Logger
import kotlin.concurrent.thread

private fun isIdeaInProperState(ideaClassLoader: ClassLoader?): Boolean {
  val loadingStateClass = Class.forName("com.intellij.diagnostic.LoadingState", false, ideaClassLoader)

  val loadingState = loadingStateClass
    .getDeclaredField("CONFIGURATION_STORE_INITIALIZED")
    .get(null)

  return loadingStateClass
    .getDeclaredMethod("isOccurred")
    .invoke(loadingState) as Boolean
}

private val logger = Logger("IdeState")

/**
 * Invokes given function when one of IDEA state is meet:
 * 1. no running IDEA instance is found
 * 2. IDEA ClassLoader is instantiated, initialized and fetched
 * 3. IDEA is fully initialized
 *
 * You can combine querying for states 1 and 2 or 1 and 3, but not 2 and 3,
 * as the second takes precedence over the third
 *
 * @param purpose               purpose of querying initialization state, used for logging
 * @param onNoIdeaFound         function invoked when no IDEA instance cannot be found
 * @param onClassLoaderFetched  function invoked when IDEA ClassLoader is initialized
 * @param onInitialized         function invoked when IDEA is fully initialized
 */
public fun invokeWhenIdeaIsInitialized(
  purpose: String,
  onNoIdeaFound: (() -> Unit)? = null,
  onClassLoaderFetched: ((ideaClassLoader: ClassLoader) -> Unit)? = null,
  onInitialized: ((ideaClassLoader: ClassLoader) -> Unit)? = null,
) {

  fun logJobDone() {
    if (onNoIdeaFound == null) {
      logger.debug { "\"$purpose\" is done" }
    }
  }

  thread(isDaemon = true) {
    //val logger = Logger("invokeWhenIdeaIsInitialized: $purpose")  // todo: can't do that because Logger calls this fun recursively

    if (onNoIdeaFound == null) {
      logger.debug { "Starting attempts to $purpose" }
    }

    while (true) {
      try {
        val ideaMainClassWithIdeaClassLoader = Class.forName("com.intellij.ide.WindowsCommandLineProcessor")
          .getDeclaredField("ourMainRunnerClass")
          .get(null) as Class<*>?

        if (ideaMainClassWithIdeaClassLoader != null) {  // null means we run with IDEA but it's not initialized yet
          val ideaClassLoader = ideaMainClassWithIdeaClassLoader.classLoader

          if (onClassLoaderFetched != null) {
            onClassLoaderFetched(ideaClassLoader)

            logJobDone()
            break
          }

          if (ProjectorClassLoaderSetup.ideaClassLoaderInitialized && isIdeaInProperState(ideaClassLoader)) {
            onInitialized!!(ideaClassLoader)

            logJobDone()
            break
          }
        }
      }
      catch (t: Throwable) {
        if (onNoIdeaFound == null) {
          logger.info(t) { "Can't $purpose. It's OK if you don't run an IntelliJ platform based app." }
        }
        else {
          onNoIdeaFound()
        }
        break
      }

      Thread.sleep(1)
    }
  }
}
