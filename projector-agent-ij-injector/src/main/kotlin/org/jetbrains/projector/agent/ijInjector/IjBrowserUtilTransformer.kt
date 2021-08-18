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

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import javassist.CtClass
import org.jetbrains.projector.agent.common.getDeclaredMethod
import org.jetbrains.projector.util.logging.Logger

internal object IjBrowserUtilTransformer : TransformerSetup {

  override val logger = Logger<IjBrowserUtilTransformer>()

  override val classTransformations: Map<Class<*>, (CtClass) -> ByteArray?> = mapOf(
    BrowserUtil::class.java to ::transformBrowserUtil,
  )

  private fun transformBrowserUtil(clazz: CtClass): ByteArray {

    clazz
      .getDeclaredMethod("browse", String::class.java)
      .setBody(
        // language=java prefix="class BrowserUtil { public static void browse(@NotNull String $1)" suffix="}"
        """
          {
            java.awt.Desktop.getDesktop().browse(new java.net.URI($1));
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("browse", String::class.java, Project::class.java)
      .setBody(
        // language=java prefix="class BrowserUtil { public static void browse(@NotNull String $1, @Nullable Project $2)" suffix="}"
        """
          {
            java.awt.Desktop.getDesktop().browse(new java.net.URI($1));
          }
        """.trimIndent()
      )

    // todo: need to support other BrowserUtil.browse methods

    return clazz.toBytecode()
  }
}
