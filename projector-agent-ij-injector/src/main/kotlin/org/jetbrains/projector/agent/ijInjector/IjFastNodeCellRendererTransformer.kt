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

import javassist.ClassPool
import javassist.CtClass
import javassist.LoaderClassPath
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import javax.swing.RepaintManager

// TODO remove after https://youtrack.jetbrains.com/issue/PRJ-22 is fixed
internal object IjFastNodeCellRendererTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  private const val FAST_NODE_RENDERER_CLASS_NAME = "com.intellij.openapi.graph.impl.view.FastNodeCellRendererPainter"

  override fun getTransformations(): Map<Class<*>, (CtClass) -> ByteArray?> = mapOf(
    Class.forName(FAST_NODE_RENDERER_CLASS_NAME) to ::transformFastRenderer
  )

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters): Boolean {
    /**
     * TODO recheck agent mode
     * There was a problem (https://youtrack.jetbrains.com/issue/PRJ-663#focus=Comments-27-5145284.0-0),
     * but then disappeared (https://github.com/JetBrains/projector-client/pull/96#discussion_r740151481)
     * Also, if you remove the check, then in agent mode there will be NPE and graphical artifacts
     */
    if (parameters.isAgent) return false
    return try {
      Class.forName(FAST_NODE_RENDERER_CLASS_NAME) // this class is not available in Idea Community
      true
    } catch (t: Throwable) {
      false
    }
  }

  private fun transformFastRenderer(clazz: CtClass): ByteArray {

    val classPool = ClassPool().apply { appendClassPath(LoaderClassPath(javaClass.classLoader)) }

    clazz
      .getDeclaredMethod("paintNode")
      .instrument(DoubleBufferingRemover(classPool))

    return clazz.toBytecode()
  }

  private class DoubleBufferingRemover(classPool: ClassPool) : ExprEditor() {

    private val repaintManagerCtClass = classPool[RepaintManager::class.java.name]

    override fun edit(m: MethodCall) {
      val ctMethod = m.method
      if (ctMethod.declaringClass.subclassOf(repaintManagerCtClass) && ctMethod.name == DOUBLE_BUFFERING_METHOD_NAME) {
        m.replace("")
      }
    }

    companion object {
      private val DOUBLE_BUFFERING_METHOD_NAME = RepaintManager::setDoubleBufferingEnabled.name
    }
  }

}
