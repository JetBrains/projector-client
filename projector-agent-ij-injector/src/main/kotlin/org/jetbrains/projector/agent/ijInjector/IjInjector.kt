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

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.projector.agent.init.IjArgs
import java.lang.instrument.Instrumentation

internal object IjInjector {

  class Utils(
    val instrumentation: Instrumentation,
    val createExtensionPointName: (String) -> ExtensionPointName<*>,
    val extensionPointNameGetExtensions: (ExtensionPointName<*>) -> Array<*>,
    val args: Map<String, String>,
  )

  private fun createUtils(instrumentation: Instrumentation, args: Map<String, String>): Utils {

    return Utils(
      instrumentation = instrumentation,
      createExtensionPointName = { ExtensionPointName.create<Any>(it) },
      extensionPointNameGetExtensions = { it.extensions },
      args = args
    )
  }

  @JvmStatic
  fun agentmain(instrumentation: Instrumentation, args: Map<String, String>) {
    val utils = createUtils(instrumentation, args)

    IjLigaturesDisablerTransformer.agentmain(utils)

    val isAgent = args[IjArgs.IS_AGENT] == "true"
    if (!isAgent) {  // todo: support variant for agent too
      IjMdTransformer.agentmain(utils)
      IjBrowserUtilTransformer.agentmain(utils)
      IjUiUtilsTransformer.agentmain(utils)
    }
  }
}
