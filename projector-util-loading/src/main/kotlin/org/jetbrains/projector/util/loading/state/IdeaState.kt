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
package org.jetbrains.projector.util.loading.state

import com.intellij.diagnostic.LoadingState
import com.intellij.ide.WindowsCommandLineProcessor
import org.jetbrains.projector.util.loading.ProjectorClassLoaderSetup
import org.jetbrains.projector.util.loading.UseProjectorLoader

/**
 * Mirrors states from com.intellij.diagnostic.LoadingState and introduces some helper ones
 */
@UseProjectorLoader
public enum class IdeaState {
  IDE_CLASSLOADER_INSTANTIATED, // Helper state to indicate whether IDE classloader can be fetched
  IDE_CLASSLOADER_FETCHED, // Helper state to indicate whether IDE classes can be used (IDE classloader is fetched by ProjectorClassLoader)
  BOOTSTRAP,
  LAF_INITIALIZED,
  COMPONENTS_REGISTERED,
  CONFIGURATION_STORE_INITIALIZED,
  COMPONENTS_LOADED,
  APP_STARTED,
  PROJECT_OPENED,
  INDEXING_FINISHED;

  public val asLoadingState: LoadingState
    get() = when (this) {
      BOOTSTRAP -> LoadingState.BOOTSTRAP
      LAF_INITIALIZED -> LoadingState.LAF_INITIALIZED
      COMPONENTS_REGISTERED -> LoadingState.COMPONENTS_REGISTERED
      CONFIGURATION_STORE_INITIALIZED -> LoadingState.CONFIGURATION_STORE_INITIALIZED
      COMPONENTS_LOADED -> LoadingState.COMPONENTS_LOADED
      APP_STARTED -> LoadingState.APP_STARTED
      PROJECT_OPENED -> LoadingState.PROJECT_OPENED
      INDEXING_FINISHED -> LoadingState.INDEXING_FINISHED
      else -> throw IllegalStateException("IdeaState $name doesn't have backing com.intellij.diagnostic.LoadingState instance")
    }

  public val isOccurred: Boolean
    get() = when (this) {
      IDE_CLASSLOADER_INSTANTIATED -> isIdeClassLoaderInstantiated
      IDE_CLASSLOADER_FETCHED -> ProjectorClassLoaderSetup.ideaClassLoaderInitialized
      else -> IDE_CLASSLOADER_FETCHED.isOccurred && asLoadingState.isOccurred
    }

  public companion object {
    public val isIdeAttached: Boolean get() = try {
      WindowsCommandLineProcessor.ourMainRunnerClass
      true
    } catch (t: Throwable) {
      t.printStackTrace()
      false
    }

    private val isIdeClassLoaderInstantiated: Boolean
      get() = isIdeAttached && WindowsCommandLineProcessor.ourMainRunnerClass != null
  }
}
