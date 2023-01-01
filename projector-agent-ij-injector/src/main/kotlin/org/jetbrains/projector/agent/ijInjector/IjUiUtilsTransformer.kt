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

import com.intellij.util.ui.UIUtil
import javassist.CtClass

internal object IjUiUtilsTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  override fun getTransformations(): Map<Class<*>, (CtClass) -> ByteArray?> = mapOf(
    UIUtil::class.java to ::transformUiUtil,
  )

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters): Boolean {
    return !parameters.isAgent
  }

  private fun transformUiUtil(clazz: CtClass): ByteArray {

    // like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction:
    clazz
      .getDeclaredMethod("isReallyTypedEvent")
      .setBody(
        // language=java prefix="class UIUtil { public static boolean isReallyTypedEvent(java.awt.event.KeyEvent $1)" suffix="}"
        """
          {
            boolean isPrintableMask = true;
            final java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
            if (tk instanceof sun.awt.SunToolkit) {
              //noinspection deprecation // copied from javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
              final int mod = $1.getModifiers();
              isPrintableMask = ((sun.awt.SunToolkit) tk).isPrintableCharacterModifiersMask(mod);
            }

            final char c = $1.getKeyChar();
            return (isPrintableMask && (c >= 0x20) && (c != 0x7F)) || (!isPrintableMask && (c >= 0x200C) && (c <= 0x200D));
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }
}
