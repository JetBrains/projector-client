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
package org.jetbrains.projector.util.loading

import com.intellij.ide.WindowsCommandLineProcessor
import org.jetbrains.projector.util.loading.state.IdeState
import org.jetbrains.projector.util.loading.state.whenOccurred
import java.lang.reflect.Method

@Suppress("RedundantVisibilityModifier") // Accessed in projector-server, don't trigger linter that doesn't know it
public object ProjectorClassLoaderSetup {

  internal var ideaClassLoaderInitialized = false
    private set

  @Suppress("unused", "RedundantVisibilityModifier") // Called from projector-server, don't trigger linter that doesn't know it
  public fun initClassLoader(classLoader: ClassLoader): ProjectorClassLoader {
    val prjClassLoader = if (classLoader is ProjectorClassLoader) classLoader else ProjectorClassLoader.instance

    // loaded with AppClassLoader in IDE
    prjClassLoader.forceLoadByPlatform("com.intellij.util.lang.UrlClassLoader")
    // loaded with AppClassLoader in IDE
    prjClassLoader.forceLoadByPlatform("com.intellij.util.lang.PathClassLoader")

    // to prevent problems caused by loading classes like kotlin.jvm.functions.Function0 by both ProjectorClassLoader and IDE ClassLoader
    prjClassLoader.forceLoadByProjectorClassLoader("com.intellij.openapi.application.ActionsKt")
    // implements Markdown plugin interface
    prjClassLoader.forceLoadByProjectorClassLoader("org.jetbrains.projector.server.core.ij.md.ProjectorMarkdownPanel")

    val onIdeClassloaderInstantiated = Runnable {
      prjClassLoader.ideaClassLoader = WindowsCommandLineProcessor.ourMainRunnerClass.classLoader
      ideaClassLoaderInitialized = true
    }

    prjClassLoader
      .loadClass("${this::class.java.name}\$IdeaStateUtils")
      .getDeclaredMethod("invokeWhenIdeClassLoaderInstantiated", String::class.java, Runnable::class.java)
      .apply(Method::unprotect)
      .invoke(null, "Init ProjectorClassLoader", onIdeClassloaderInstantiated)

    return prjClassLoader
  }

  @Suppress("unused") // used via reflection
  @UseProjectorLoader
  private object IdeaStateUtils {

    @JvmStatic
    private fun invokeWhenIdeClassLoaderInstantiated(
      purpose: String?,
      onIdeClassloaderInstantiated: Runnable,
    ) {
      IdeState.IDE_CLASSLOADER_INSTANTIATED.whenOccurred(purpose) { onIdeClassloaderInstantiated.run() }
    }
  }
}
