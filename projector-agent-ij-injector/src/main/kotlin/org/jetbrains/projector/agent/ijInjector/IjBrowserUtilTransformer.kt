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

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import javassist.CtClass
import org.jetbrains.projector.agent.common.getDeclaredMethod
import java.net.URI

internal object IjBrowserUtilTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  override fun getTransformations(): Map<Class<*>, (CtClass) -> ByteArray?> = mapOf(
    BrowserUtil::class.java to ::transformBrowserUtil,
  )

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters): Boolean {
    return !parameters.isAgent
  }

  private fun transformBrowserUtil(clazz: CtClass): ByteArray {

    clazz
      .getDeclaredMethod("browse", String::class.java, Project::class.java)
      .setBody(
        // language=java prefix="class BrowserUtil { public static void browse(@NotNull String $1, @Nullable Project $2)" suffix="}"
        """
          {
            if ($1.contains("account.jetbrains.com") && $1.contains("redirect_uri")) {
              // fallback to token copy-pasting
              throw new IllegalArgumentException("Projector cannot automatically process IDE licensing. See PRJ-691; PRJ-750; PRJ-779");
            }
            java.net.URI uri = com.intellij.openapi.vfs.VfsUtil.toUri($1);
            java.awt.Desktop.getDesktop().browse(uri);
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("browse", URI::class.java)
      .setBody(
        // language=java prefix="class BrowserUtil { public static void browse(@NotNull java.net.URI $1)" suffix="}"
        """
          {
            java.awt.Desktop.getDesktop().browse($1);
          }
        """.trimIndent()
      )

    // todo: need to support other BrowserUtil.browse methods

    return clazz.toBytecode()
  }
}
