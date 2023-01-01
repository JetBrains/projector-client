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
package org.jetbrains.projector.agent.ijInjector

import org.jetbrains.projector.agent.init.IjArgs
import org.jetbrains.projector.util.loading.UseProjectorLoader
import java.lang.instrument.Instrumentation

@UseProjectorLoader(attachPackage = true)
internal object IjInjector {

  @Suppress("unused") // classloader provider class name and method names are not currently used, but may be in the future
  class AgentParameters(
    val isAgent: Boolean,
    val markdownPanelClassName: String,
    val isIdeAttached: Boolean,
  )

  private fun parametersFromArgs(args: Map<String, String>): AgentParameters {

    return AgentParameters(
      isAgent = args[IjArgs.IS_AGENT] == "true",
      markdownPanelClassName = args.getValue(IjArgs.MD_PANEL_CLASS),
      isIdeAttached = args[IjArgs.IS_IDE_ATTACHED] == "true",
    )
  }

  @JvmStatic
  fun agentmain(instrumentation: Instrumentation, args: Map<String, String>) {
    val parameters = parametersFromArgs(args)

    // TODO: support same transformers in agent mode too to reach feature parity
    val transformers = listOf(
      IjAwtTransformer,
      IjLigaturesDisablerTransformer,
      IjMdTransformer,
      IjBrowserUtilTransformer,
      IjUiUtilsTransformer,
      IjFastNodeCellRendererTransformer,
      IjRestartDisablerTransformer,
    )

    ProjectorBatchTransformer(transformers).runTransformations(instrumentation, parameters)
  }
}
