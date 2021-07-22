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

internal class IjLigaturesDisablerTransformer private constructor(
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
        fontPrefsPath -> transformFontPrefs(className, classfileBuffer)

        else -> classfileBuffer
      }
    }
    catch (e: Exception) {
      logger.error(e) { "Class transform error" }
      null
    }
  }

  private fun transformFontPrefs(className: String, classfileBuffer: ByteArray): ByteArray {
    logger.debug { "Transforming fontprefs..." }
    val clazz = getClassFromClassfileBuffer(ideCp, className, classfileBuffer)
    clazz.defrost()

    clazz
      .getDeclaredMethod("useLigatures")
      .setBody(
        """
          {
            return false;
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }

  companion object {

    private val logger = Logger<IjLigaturesDisablerTransformer>()

    private const val SE_CONTRIBUTOR_EP = "com.intellij.searchEverywhereContributor"

    private const val fontPrefsClass = "com.intellij.openapi.editor.colors.impl.FontPreferencesImpl"
    private val fontPrefsPath = fontPrefsClass.replace('.', '/')

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
      val transformer = IjLigaturesDisablerTransformer(ideCp)

      utils.instrumentation.addTransformer(transformer, true)

      listOf(
        fontPrefsClass,
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
