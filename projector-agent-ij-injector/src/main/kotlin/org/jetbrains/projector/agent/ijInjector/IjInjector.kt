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

internal typealias ExtensionPointName = Any
internal typealias ExtensionPointId = String

internal object IjInjector {

  class Utils(
    val instrumentation: Instrumentation,
    val createExtensionPointName: (ExtensionPointId) -> ExtensionPointName,
    val extensionPointNameGetExtensions: (ExtensionPointName) -> Array<*>,
    val args: Map<String, String>,
  )

  private fun createUtils(instrumentation: Instrumentation, args: Map<String, String>): Utils {

    val ijClProviderClass = args.getValue(IjArgs.IJ_CL_PROVIDER_CLASS)
    val ijClProviderMethod = args.getValue(IjArgs.IJ_CL_PROVIDER_METHOD)

    val ijCl = Class.forName(ijClProviderClass).getDeclaredMethod(ijClProviderMethod).invoke(null) as ClassLoader
    val extensionPointNameClass = Class.forName("com.intellij.openapi.extensions.ExtensionPointName", false, ijCl)
    val extensionPointNameCreateMethod = extensionPointNameClass.getDeclaredMethod("create", String::class.java)
    val extensionPointNameGetExtensionsMethod = extensionPointNameClass.getDeclaredMethod("getExtensions")

    return Utils(
      instrumentation = instrumentation,
      createExtensionPointName = { extensionPointNameCreateMethod.invoke(null, it) },
      extensionPointNameGetExtensions = { extensionPointNameGetExtensionsMethod.invoke(it) as Array<*> },
      args = args
    )
  }

  fun agentmain(instrumentation: Instrumentation, args: Map<String, String>) {
    val utils = createUtils(instrumentation, args)

    IjLigaturesDisablerTransformer.agentmain(utils)

    val isAgent = args[IjArgs.IS_AGENT] == "true"
    if (!isAgent) {  // todo: support variant for agent too
      IjMdTransformer.agentmain(utils)
      IjBrowserUtilTransformer.agentmain(utils)
    }
  }
}
