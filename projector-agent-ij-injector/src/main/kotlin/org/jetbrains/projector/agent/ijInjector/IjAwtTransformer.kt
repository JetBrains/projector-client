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

import javassist.CtClass
import org.jetbrains.projector.util.loading.ProjectorClassLoader
import org.jetbrains.projector.util.loading.state.IdeState
import java.awt.GraphicsEnvironment

internal object IjAwtTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  // language=java prefix="import " suffix=;
  private const val PROJECTOR_GRAPHICS_ENVIRONMENT_NAME = "org.jetbrains.projector.awt.image.PGraphicsEnvironment"

  override val loadingState: IdeState? = null

  override fun getTransformations(): Map<Class<*>, (CtClass) -> ByteArray?> = mapOf(
    GraphicsEnvironment::class.java to ::transformGraphicsEnvironment,
  )

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters): Boolean = !parameters.isAgent

  private fun transformGraphicsEnvironment(ctClass: CtClass): ByteArray {

    @Suppress("rawtypes", "unchecked", "RedundantArrayCreation") // for body injection
    ctClass
      .getDeclaredMethod("getLocalGraphicsEnvironment")
      .setBody(
        // language=java prefix="class GraphicsEnvironment { private static GraphicsEnvironment createGE()" suffix="}"
        """
          {
            Class prjClassLoaderClazz = ClassLoader.getSystemClassLoader().loadClass("${ProjectorClassLoader::class.java.name}");
            ClassLoader loader = (ClassLoader) prjClassLoaderClazz
              .getDeclaredMethod("getInstance", new Class[0])
              .invoke(null, new Object[0]);
            Class envClass = loader.loadClass("$PROJECTOR_GRAPHICS_ENVIRONMENT_NAME");
            Object envInstance = envClass.getDeclaredMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            return (java.awt.GraphicsEnvironment) envInstance;
          }
        """.trimIndent()
      )

    return ctClass.toBytecode()
  }

}
