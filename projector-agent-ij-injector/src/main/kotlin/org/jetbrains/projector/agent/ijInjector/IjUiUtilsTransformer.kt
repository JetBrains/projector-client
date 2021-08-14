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

import javassist.ClassPool
import javassist.LoaderClassPath
import org.jetbrains.projector.agent.common.getClassFromClassfileBuffer
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

internal class IjUiUtilsTransformer private constructor(
  private val ideCp: ClassPool,
) : ClassFileTransformer {

  override fun transform(
    loader: ClassLoader,
    className: String,
    classBeingRedefined: Class<*>?,
    protectionDomain: ProtectionDomain?,
    classfileBuffer: ByteArray,
  ): ByteArray? {
    return transformClass(className, classfileBuffer)
  }

  private fun transformClass(className: String, classfileBuffer: ByteArray): ByteArray? {
    return try {
      when (className) {
        uiUtilPath -> transformUiUtil(className, classfileBuffer)

        else -> classfileBuffer
      }
    }
    catch (e: Exception) {
      logger.error(e) { "Class transform error" }
      null
    }
  }

  private fun transformUiUtil(className: String, classfileBuffer: ByteArray): ByteArray {
    logger.debug { "Transforming UIUtil..." }
    val clazz = getClassFromClassfileBuffer(ideCp, className, classfileBuffer)
    clazz.defrost()

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

  companion object {

    private val logger = Logger<IjUiUtilsTransformer>()

    private const val SE_CONTRIBUTOR_EP = "com.intellij.searchEverywhereContributor"

    private const val uiUtilClass = "com.intellij.util.ui.UIUtil"
    private val uiUtilPath = uiUtilClass.replace('.', '/')

    fun agentmain(
      utils: IjInjector.Utils,
    ) {
      logger.debug { "agentmain start" }

      val extensionPointName = utils.createExtensionPointName(SE_CONTRIBUTOR_EP)
      val extensions = utils.extensionPointNameGetExtensions(extensionPointName)

      val ideClassloader = extensions.filterNotNull().first()::class.java.classLoader

      val ideCp = ClassPool().apply {
        appendClassPath(LoaderClassPath(ideClassloader))
      }
      val transformer = IjUiUtilsTransformer(ideCp)

      utils.instrumentation.addTransformer(transformer, true)

      listOf(
        uiUtilClass,
      ).forEach { clazz ->
        try {
          utils.instrumentation.retransformClasses(Class.forName(clazz, false, ideClassloader))
        }
        catch (t: Throwable) {
          logger.error(t) { "Class retransform error" }
        }
      }

      logger.debug { "agentmain finish" }
    }
  }
}
