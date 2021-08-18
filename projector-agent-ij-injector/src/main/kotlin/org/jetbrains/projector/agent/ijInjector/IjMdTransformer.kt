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

import com.intellij.openapi.extensions.ExtensionPointName
import javassist.CtClass
import org.jetbrains.projector.agent.init.IjArgs
import org.jetbrains.projector.util.logging.Logger

internal object IjMdTransformer : TransformerSetup {

  override val logger = Logger<IjMdTransformer>()

  private const val MD_EXTENSION_ID = "org.intellij.markdown.html.panel.provider"

  // language=java prefix="import " suffix=";"
  private const val javaFxClass = "org.intellij.plugins.markdown.ui.preview.javafx.JavaFxHtmlPanelProvider"
  // language=java prefix="import " suffix=";"
  private const val jcefClass = "org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider"

  override fun getTransformations(utils: IjInjector.Utils, classLoader: ClassLoader): Map<Class<*>, (CtClass) -> ByteArray?> {

    val mdPanelMakerClass = utils.args.getValue(IjArgs.MD_PANEL_MAKER_CLASS)
    val mdPanelMakerMethod = utils.args.getValue(IjArgs.MD_PANEL_MAKER_METHOD)
    val projectorMarkdownPanel = ProjectorMarkdownPanel(mdPanelMakerClass, mdPanelMakerMethod)

    return listOf(
      javaFxClass to MdPreviewType.JAVAFX,
      jcefClass to MdPreviewType.JCEF,
    ).mapNotNull {
      val clazz = classForNameOrNull(it.first, classLoader) ?: return@mapNotNull null
      clazz to it.second
    }.associate { it.first to { clazz: CtClass -> transformMdHtmlPanelProvider(it.second, clazz, projectorMarkdownPanel) } }
  }

  override fun getClassLoader(utils: IjInjector.Utils): ClassLoader? {
    val extensionPointName = ExtensionPointName.create<Any>(MD_EXTENSION_ID)
    val extensions = try {
      extensionPointName.extensions
    } catch (e: IllegalArgumentException) {
      logger.debug { "Markdown plugin is not installed. Skip the transform" }
      return null
    }

    return extensions.filterNotNull().first()::class.java.classLoader
  }

  private fun transformMdHtmlPanelProvider(previewType: MdPreviewType, clazz: CtClass, projectorMarkdownPanel: ProjectorMarkdownPanel): ByteArray {
    clazz
      .getDeclaredMethod("isAvailable")
      .setBody(
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract AvailabilityInfo isAvailable()" suffix="}"
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
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract ProviderInfo getProviderInfo()" suffix="}"
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
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract MarkdownHtmlPanel createHtmlPanel()" suffix="}"
        """
          {
            final java.lang.ClassLoader mdClassLoader = org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel.class.getClassLoader();
            //noinspection RedundantArrayCreation
            final Object panel = Class.forName("${projectorMarkdownPanel.className}")
              .getDeclaredMethod("${projectorMarkdownPanel.methodName}", new java.lang.Class[] { java.lang.ClassLoader.class })
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

  private data class ProjectorMarkdownPanel(val className: String, val methodName: String)
}
