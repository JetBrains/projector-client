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

import org.jetbrains.projector.common.ij.IjArgs
import org.jetbrains.projector.common.ij.toIjArgs
import org.jetbrains.projector.server.core.ij.md.MarkdownPanelMaker
import org.jetbrains.projector.util.agent.copyAgentToTempJarAndAttach
import java.lang.ref.WeakReference

public object IjInjectorAgentInitializer {

  private lateinit var ijClassLoader: WeakReference<ClassLoader>

  @JvmStatic
  public fun getIdeClassloader(): ClassLoader? {
    return ijClassLoader.get()
  }

  @OptIn(ExperimentalStdlibApi::class)
  public fun init(isAgent: Boolean) {
    invokeWhenIdeaIsInitialized("attach IJ injector agent") { ideClassLoader ->
      this.ijClassLoader = WeakReference(ideClassLoader)

      val ijClProviderClass = IjInjectorAgentInitializer::class.java.name
      val ijClProviderMethod = IjInjectorAgentInitializer::getIdeClassloader.name
      val mdPanelMakerClass = MarkdownPanelMaker::class.java.name
      val mdPanelMakerMethod = MarkdownPanelMaker::createMarkdownHtmlPanel.name

      val args = mapOf(
        IjArgs.isAgent to isAgent,
        IjArgs.ijClProviderClass to ijClProviderClass,
        IjArgs.ijClProviderMethod to ijClProviderMethod,
        IjArgs.mdPanelMakerClass to mdPanelMakerClass,
        IjArgs.mdPanelMakerMethod to mdPanelMakerMethod,
      ).toIjArgs()

      copyAgentToTempJarAndAttach(
        agentJar = this::class.java.getResourceAsStream("/projector-agent/projector-agent-ij-injector.jar"),
        args = args,
      )
    }
  }
}
