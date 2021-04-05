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

internal class IjBrowserUtilTransformer private constructor(
  private val mdCp: ClassPool,
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
        browserUtilPath -> transformBrowserUtil(className, classfileBuffer)

        else -> classfileBuffer
      }
    }
    catch (e: Exception) {
      logger.error(e) { "Class transform error" }
      null
    }
  }

  private fun transformBrowserUtil(className: String, classfileBuffer: ByteArray): ByteArray {
    logger.debug { "Transforming BrowserUtil..." }
    val clazz = getClassFromClassfileBuffer(mdCp, className, classfileBuffer)
    clazz.defrost()

    clazz
      .getDeclaredMethod("browse", arrayOf(mdCp["java.lang.String"]))
      .setBody(
        """
          {
            java.awt.Desktop.getDesktop().browse(new java.net.URI((java.lang.String) $JAVASSIST_ARGS[0]));
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("browse", arrayOf(mdCp["java.lang.String"], mdCp["com.intellij.openapi.project.Project"]))
      .setBody(
        """
          {
            java.awt.Desktop.getDesktop().browse(new java.net.URI((java.lang.String) $JAVASSIST_ARGS[0]));
          }
        """.trimIndent()
      )

    // todo: need to support other BrowserUtil.browse methods

    return clazz.toBytecode()
  }

  companion object {

    private val logger = Logger<IjBrowserUtilTransformer>()

    private const val MD_EXTENSION_ID = "org.intellij.markdown.html.panel.provider"

    private const val browserUtilClass = "com.intellij.ide.BrowserUtil"
    private val browserUtilPath = browserUtilClass.replace('.', '/')

    private const val JAVASSIST_ARGS = "\$args"

    fun agentmain(
      utils: IjInjector.Utils,
    ) {
      logger.debug { "agentmain start" }

      // todo: get rid of md, it's needed only to get IDE classloader
      val extensionPointName = utils.createExtensionPointName(MD_EXTENSION_ID)
      val extensions = utils.extensionPointNameGetExtensions(extensionPointName)

      val mdClassloader = extensions.filterNotNull().first()::class.java.classLoader

      val mdCp = ClassPool().apply {
        appendClassPath(LoaderClassPath(mdClassloader))
      }
      val transformer = IjBrowserUtilTransformer(mdCp)

      utils.instrumentation.addTransformer(transformer, true)

      listOf(
        browserUtilClass,
      ).forEach { clazz ->
        try {
          utils.instrumentation.retransformClasses(Class.forName(clazz, false, mdClassloader))
        }
        catch (t: Throwable) {
          logger.error(t) { "Class retransform error" }
        }
      }

      logger.debug { "agentmain finish" }
    }
  }
}
