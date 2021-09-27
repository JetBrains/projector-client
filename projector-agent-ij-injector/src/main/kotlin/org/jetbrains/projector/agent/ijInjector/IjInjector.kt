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
package org.jetbrains.projector.agent.ijInjector

import org.jetbrains.projector.agent.init.IjArgs
import java.lang.instrument.Instrumentation

internal object IjInjector {

  class AgentParameters(
    val isAgent: Boolean,
    val classloadersProviderClassName: String,
    val ideaClassloaderProviderMethodName: String,
    val projectorClassloaderProviderMethodName: String,
    val markdownPanelMakerClassName: String,
    val markdownPanelMakerMethodName: String,
  )

  private fun parametersFromArgs(args: Map<String, String>): AgentParameters {

    return AgentParameters(
      isAgent = args[IjArgs.IS_AGENT] == "true",
      classloadersProviderClassName = args.getValue(IjArgs.IJ_CL_PROVIDER_CLASS),
      ideaClassloaderProviderMethodName = args.getValue(IjArgs.IJ_CL_PROVIDER_METHOD),
      projectorClassloaderProviderMethodName = args.getValue(IjArgs.PRJ_CL_PROVIDER_METHOD),
      markdownPanelMakerClassName = args.getValue(IjArgs.MD_PANEL_MAKER_CLASS),
      markdownPanelMakerMethodName = args.getValue(IjArgs.MD_PANEL_MAKER_METHOD),
    )
  }

  @JvmStatic
  fun agentmain(instrumentation: Instrumentation, args: Map<String, String>) {
    val parameters = parametersFromArgs(args)

    // TODO: support same transformers in agent mode too to reach feature parity
    val transformers = listOf(
      IjLigaturesDisablerTransformer,
      IjMdTransformer,
      IjBrowserUtilTransformer,
      IjUiUtilsTransformer,
    )

    BatchTransformer(transformers).runTransformations(instrumentation, parameters)
  }
}
