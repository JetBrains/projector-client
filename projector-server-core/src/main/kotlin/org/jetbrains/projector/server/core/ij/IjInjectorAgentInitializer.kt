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
package org.jetbrains.projector.server.core.ij

import org.jetbrains.projector.agent.init.IjArgs
import org.jetbrains.projector.agent.init.toIjArgs
import org.jetbrains.projector.util.agent.copyAgentToTempJarAndAttach

@Suppress("unused") // Used in projector-server
public object IjInjectorAgentInitializer {

  // raw string because it must be loaded with Markdown PluginClassLoader
  internal const val MD_PANEL_CLASS_NAME = "org.jetbrains.projector.server.core.ij.md.ProjectorMarkdownPanel"

  @Suppress("unused") // Called from projector-server, don't trigger linter that doesn't know it
  public fun init(isAgent: Boolean, isIdeAttached: Boolean) {
    val args = mapOf(
      IjArgs.IS_AGENT to isAgent,
      IjArgs.MD_PANEL_CLASS to MD_PANEL_CLASS_NAME,
      IjArgs.IS_IDE_ATTACHED to isIdeAttached,
    ).toIjArgs()

    copyAgentToTempJarAndAttach(
      agentJar = this::class.java.getResourceAsStream("/projector-agent/projector-agent-ij-injector.jar")!!,
      args = args,
    )
  }
}
