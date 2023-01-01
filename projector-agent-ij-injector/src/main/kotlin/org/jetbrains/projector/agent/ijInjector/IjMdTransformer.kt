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

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.BuildNumber
import com.intellij.ui.jcef.JBCefApp
import javassist.CtClass
import org.jetbrains.projector.agent.common.transformation.TransformationResult
import org.jetbrains.projector.agent.common.transformation.classForNameOrNull
import org.jetbrains.projector.ij.md.markdownPlugin
import org.jetbrains.projector.util.loading.ProjectorClassLoader
import org.jetbrains.projector.util.loading.state.IdeState

internal object IjMdTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  // language=java prefix="import " suffix=";"
  private const val javaFxClass = "org.intellij.plugins.markdown.ui.preview.javafx.JavaFxHtmlPanelProvider"

  // language=java prefix="import " suffix=";"
  private const val jcefClass = "org.intellij.plugins.markdown.ui.preview.jcef.JCEFHtmlPanelProvider"

  // language=java prefix="import " suffix=";"
  private const val previewFileEditorClass = "org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor"

  override val loadingState: IdeState
    get() = IdeState.CONFIGURATION_STORE_INITIALIZED

  override fun getTransformations(
    parameters: IjInjector.AgentParameters,
    classLoader: ClassLoader,
  ): Map<Class<*>, (CtClass) -> ByteArray?> {

    val projectorClassLoader = ProjectorClassLoader.instance
    projectorClassLoader.addPluginLoader("org.intellij.plugins.markdown", classLoader)

    val transformations = mutableMapOf<Class<*>, (CtClass) -> ByteArray?>()

    transformations += listOf(
      javaFxClass to MdPreviewType.JAVAFX,
      jcefClass to MdPreviewType.JCEF,
    ).mapNotNull { (className, previewType) ->
      val clazz = classForNameOrNull(className, classLoader) ?: run {
        transformationResultConsumer(TransformationResult.Skip(this, className, "Class not found"))
        return@mapNotNull null
      }
      clazz to previewType
    }.associate { (clazz, previewType) ->
      clazz to { ctClass -> transformMdHtmlPanelProvider(previewType, ctClass, parameters.markdownPanelClassName, parameters.isAgent) }
    }

    if (!parameters.isAgent && isPreviewCheckIsBrokenInHeadless()) {
      val previewFileEditor = Class.forName(previewFileEditorClass, false, classLoader)
      transformations[previewFileEditor] = ::fixMarkdownPreviewNPE
    }

    return transformations
  }

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters, unavailableReasonConsumer: (String) -> Unit): Boolean {
    val reason = when {
      markdownPlugin == null -> "Markdown plugin is not installed"
      !markdownPlugin!!.isEnabled -> "Markdown plugin is disabled"
      else -> return true
    }

    unavailableReasonConsumer(reason)
    return false
  }

  override fun getClassLoader(parameters: IjInjector.AgentParameters): ClassLoader {
    return markdownPlugin!!.pluginClassLoader!!
  }

  private fun isPreviewCheckIsBrokenInHeadless(): Boolean {
    val buildNumberMin = BuildNumber.fromString("201.0")!!
    val buildNumberFixed = BuildNumber.fromString("212.0")!!
    val markdownVersionString = PluginManagerCore.getPlugin(PluginId.getId("org.intellij.plugins.markdown"))!!.version
    val markdownBuildNumber = BuildNumber.fromString(markdownVersionString)!!
    return markdownBuildNumber >= buildNumberMin && markdownBuildNumber < buildNumberFixed
  }

  private fun fixMarkdownPreviewNPE(previewFileEditorClazz: CtClass): ByteArray {

    @Suppress("deprecation") // copied from Markdown plugin 212
    previewFileEditorClazz
      .getDeclaredMethod("isPreviewShown")
      .setBody(
        // language=java prefix="class MarkdownPreviewFileEditor { private static boolean isPreviewShown(@NotNull com.intellij.openapi.project.Project $1, @NotNull com.intellij.openapi.vfs.VirtualFile $2)" suffix="}"
        """
          {
            org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditorProvider provider = 
              com.intellij.openapi.fileEditor.FileEditorProvider
                .EP_FILE_EDITOR_PROVIDER
                .findExtension(org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditorProvider.class);
            if (provider == null) {
              return true;
            }
        
            com.intellij.openapi.fileEditor.FileEditorState state = com.intellij.openapi.fileEditor.impl.EditorHistoryManager.getInstance($1).getState($2, provider);
            if (!(state instanceof org.intellij.plugins.markdown.ui.split.SplitFileEditor.MyFileEditorState)) {
              return true;
            }
        
            String layout = ((org.intellij.plugins.markdown.ui.split.SplitFileEditor.MyFileEditorState)state).getSplitLayout();
            return layout == null || !layout.equals("FIRST");
          }
        """.trimIndent()
      )

    return previewFileEditorClazz.toBytecode()
  }

  private fun transformMdHtmlPanelProvider(
    previewType: MdPreviewType,
    clazz: CtClass,
    projectorMarkdownPanelClass: String,
    isAgent: Boolean,
  ): ByteArray {

    val availabilityInfo = when (!isAgent || previewType.isAvailable()) {
      // language=java prefix="class Dummy { var t = " suffix="; }"
      true -> "org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider.AvailabilityInfo.AVAILABLE"
      // language=java prefix="class Dummy { var t = " suffix="; }"
      false -> "org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE"
    }

    clazz
      .getDeclaredMethod("isAvailable")
      .setBody(
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract AvailabilityInfo isAvailable()" suffix="}"
        """
          {
            return ${availabilityInfo};
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("getProviderInfo")
      .setBody(
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract ProviderInfo getProviderInfo()" suffix="}"
        """
          {
            return new org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider.ProviderInfo("Projector (${previewType.displayName})", getClass().getName());
          }
        """.trimIndent()
      )

    @Suppress("rawtypes", "unchecked", "RedundantArrayCreation") // for body injection
    clazz
      .getDeclaredMethod("createHtmlPanel")
      .setBody(
        // language=java prefix="class MarkdownHtmlPanelProvider { @NotNull public abstract MarkdownHtmlPanel createHtmlPanel()" suffix="}"
        """
          {
            // we need the version loaded via system lassLoader
            Class actualPrjClassLoaderClazz = ClassLoader.getSystemClassLoader().loadClass("org.jetbrains.projector.util.loading.ProjectorClassLoader");   
            ClassLoader actualPrjClassLoader = (ClassLoader) actualPrjClassLoaderClazz
                .getDeclaredMethod("getInstance", new Class[0])
                .invoke(null, new Object[0]);
            
            String className = "$projectorMarkdownPanelClass";
            Class mdPanelClazz = actualPrjClassLoader.loadClass(className);
            
            return (org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel) mdPanelClazz.getDeclaredConstructor(new Class[] { boolean.class, String.class }).newInstance(new Object[] { Boolean.valueOf($isAgent), "${previewType.panelClass}" });
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }

  private enum class MdPreviewType(val displayName: String, val panelClass: String) {

    JAVAFX("JavaFX WebView", "org.intellij.plugins.markdown.ui.preview.javafx.MarkdownJavaFxHtmlPanel") {
      override fun isAvailable(): Boolean {
        try {
          Class.forName("javafx.scene.web.WebView", false, javaClass.classLoader)
          return true
        } catch (ignored: ClassNotFoundException) {
        }

        return false
      }
    },
    JCEF("JCEF Browser", "org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel") {
      override fun isAvailable(): Boolean = if (JBCefApp.isSupported()) {
        try {
          JBCefApp.getInstance()
          true
        } catch (e: IllegalStateException) {
          false
        }
      } else false
    };

    abstract fun isAvailable(): Boolean
  }
}
