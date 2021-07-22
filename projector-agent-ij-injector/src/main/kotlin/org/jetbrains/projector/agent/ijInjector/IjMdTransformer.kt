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
import org.jetbrains.projector.agent.init.IjArgs
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.ClassFileTransformer
import java.lang.reflect.InvocationTargetException
import java.security.ProtectionDomain

internal class IjMdTransformer private constructor(
  private val mdCp: ClassPool,
  private val mdPanelMakerClass: String,
  private val mdPanelMakerMethod: String,
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
        javaFxPath -> transformMdHtmlPanelProvider(MdPreviewType.JAVAFX, className, classfileBuffer)
        jcefPath -> transformMdHtmlPanelProvider(MdPreviewType.JCEF, className, classfileBuffer)

        else -> classfileBuffer
      }
    }
    catch (e: Exception) {
      logger.error(e) { "Class transform error" }
      null
    }
  }

  private fun transformMdHtmlPanelProvider(previewType: MdPreviewType, className: String, classfileBuffer: ByteArray): ByteArray {
    logger.debug { "Transforming MdHtmlPanelProvider (${previewType.displayName})..." }
    val clazz = getClassFromClassfileBuffer(mdCp, className, classfileBuffer)
    clazz.defrost()

    clazz
      .getDeclaredMethod("isAvailable")
      .setBody(
        """
          {
            return org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider.AvailabilityInfo.AVAILABLE;
          }
        """.trimIndent()
      )

    // todo: use correct className, not hardcoded
    clazz
      .getDeclaredMethod("getProviderInfo")
      .setBody(
        """
          {
            final java.lang.String className = org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider.class.getName();
            return new org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider.ProviderInfo("Projector (${previewType.displayName})", className);
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("createHtmlPanel")
      .setBody(
        """
          {
            final java.lang.ClassLoader mdClassLoader = org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel.class.getClassLoader();
            final Object panel = Class.forName("$mdPanelMakerClass")
              .getDeclaredMethod("$mdPanelMakerMethod", new java.lang.Class[] { java.lang.ClassLoader.class })
              .invoke(null, new java.lang.Object[] { mdClassLoader });
            return (org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel) panel;
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }

  private enum class MdPreviewType(val displayName: String) {

    JAVAFX("JavaFX WebView"),
    JCEF("JCEF Browser"),
  }

  companion object {

    private val logger = Logger<IjMdTransformer>()

    private const val MD_EXTENSION_ID = "org.intellij.markdown.html.panel.provider"

    private const val javaFxClass = "org.intellij.plugins.markdown.ui.preview.javafx.JavaFxHtmlPanelProvider"
    private val javaFxPath = javaFxClass.replace('.', '/')
    private const val jcefClass = "org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider"
    private val jcefPath = jcefClass.replace('.', '/')

    fun agentmain(utils: IjInjector.Utils) {
      logger.debug { "IjMdTransformer agentmain start" }

      val extensionPointName = utils.createExtensionPointName(MD_EXTENSION_ID)
      val extensions = try {
        utils.extensionPointNameGetExtensions(extensionPointName)
      } catch (e: InvocationTargetException) {
        logger.debug { "Markdown plugin is not installed. Skip the transform" }
        return
      }

      val mdClassloader = extensions.filterNotNull().first()::class.java.classLoader

      val mdCp = ClassPool().apply {
        appendClassPath(LoaderClassPath(mdClassloader))
      }

      val mdPanelMakerClass = utils.args.getValue(IjArgs.MD_PANEL_MAKER_CLASS)
      val mdPanelMakerMethod = utils.args.getValue(IjArgs.MD_PANEL_MAKER_METHOD)

      val transformer = IjMdTransformer(mdCp, mdPanelMakerClass = mdPanelMakerClass, mdPanelMakerMethod = mdPanelMakerMethod)

      utils.instrumentation.addTransformer(transformer, true)

      listOf(
        javaFxClass,
        jcefClass,
      ).forEach { clazz ->
        try {
          utils.instrumentation.retransformClasses(Class.forName(clazz, false, mdClassloader))
        }
        catch (t: Throwable) {
          logger.info(t) { "Class retransform error" }
        }
      }

      logger.debug { "IjMdTransformer agentmain finish" }
    }
  }
}
