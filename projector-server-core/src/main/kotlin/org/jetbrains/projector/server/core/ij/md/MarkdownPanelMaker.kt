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
package org.jetbrains.projector.server.core.ij.md

import javassist.*

public object MarkdownPanelMaker {

  // todo: this static initialization won't support reloaded MD plugin because classloader there will change
  private lateinit var classPool: ClassPool

  private lateinit var neighbor: Class<*>

  private val markdownHtmlPanelClass by lazy {
    val ourClassName = "ProjectorMarkdownHtmlPanel"
    val ourClassFqnName = "${neighbor.packageName}.$ourClassName"

    classPool.makeClass(ourClassFqnName).apply {
      interfaces = arrayOf(classPool.get("org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel"))

      val delegateClass = """this.delegateClass"""
      val delegate = """this.delegate"""

      addField(
        CtField.make(
          """private final Class delegateClass;""",
          this,
        )
      )

      addField(
        CtField.make(
          """private final Object delegate;""",
          this,
        )
      )

      // todo: not sure that context classloader will work for launch with UI (projector-agent)
      addConstructor(
        CtNewConstructor.make(
          """
            public $ourClassName() {
              final java.util.List threads = new java.util.ArrayList(Thread.getAllStackTraces().keySet());
              for (int i = 0; i < threads.size(); ++i) {
                final Thread thread = (Thread) threads.get(i);
                if (thread.getName().toLowerCase().contains("websocket")) {
                  final ClassLoader projectorClassLoader = thread.getContextClassLoader();
                  $delegateClass = Class.forName("${PanelDelegate::class.qualifiedName}", false, projectorClassLoader);
                }
              }
              $delegate = this.delegateClass.newInstance();
            }
          """.trimIndent(),
          this,
        )
      )

      //@Override
      addMethod(
        CtNewMethod.make(
          """
            public void dispose() {
              $delegateClass.getDeclaredMethod("dispose", new Class[0]).invoke($delegate, new Object[0]);
            }
          """.trimIndent(),
          this,
        )
      )

      //@NotNull
      //@Override
      addMethod(
        CtNewMethod.make(
          """
            public javax.swing.JComponent getComponent() {
              return (javax.swing.JComponent) $delegateClass.getDeclaredMethod("getComponent", new Class[0]).invoke($delegate, new Object[0]);
            }
          """.trimIndent(),
          this,
        )
      )

      //@Override
      //@NotNull String html
      addMethod(
        CtNewMethod.make(
          """
            public void setHtml(String html) {
              $delegateClass.getDeclaredMethod("setHtml", new Class[] { String.class }).invoke($delegate, new Object[] { html });
            }
          """.trimIndent(),
          this,
        )
      )

      //added in JetBrains IDEs 2020.3+
      //@Override
      //@NotNull String html, int initialScrollOffset
      addMethod(
        CtNewMethod.make(
          """
            public void setHtml(String html, int initialScrollOffset) {
              $delegateClass.getDeclaredMethod("setHtml", new Class[] { String.class, int.class }).invoke($delegate, new Object[] { html, Integer.valueOf(initialScrollOffset) });
            }
          """.trimIndent(),
          this,
        )
      )

      //@Override
      //@Nullable String inlineCss, String... fileUris
      addMethod(
        CtNewMethod.make(
          """
            public void setCSS(String inlineCss, String[] fileUris) {
              $delegateClass.getDeclaredMethod("setCSS", new Class[] { String.class, String[].class }).invoke($delegate, new Object[] { inlineCss, fileUris });
            }
          """.trimIndent(),
          this,
        )
      )

      //@Override
      addMethod(
        CtNewMethod.make(
          """
            public void render() {
              $delegateClass.getDeclaredMethod("render", new Class[0]).invoke($delegate, new Object[0]);
            }
          """.trimIndent(),
          this,
        )
      )

      //@Override
      addMethod(
        CtNewMethod.make(
          """
            public void scrollToMarkdownSrcOffset(int offset) {
              $delegateClass.getDeclaredMethod("scrollToMarkdownSrcOffset", new Class[] { int.class }).invoke($delegate, new Object[] { Integer.valueOf(offset) });
            }
          """.trimIndent(),
          this,
        )
      )
    }
      .toClass(
        neighbor)  // without passing a neighbor, there is strange java.lang.ClassNotFoundException: org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel at com.intellij.util.lang.UrlClassLoader.findClass(UrlClassLoader.java:357)
  }

  @JvmStatic
  public fun createMarkdownHtmlPanel(mdClassLoader: ClassLoader): Any {
    if (!::classPool.isInitialized) {
      classPool = ClassPool().apply {
        appendClassPath(LoaderClassPath(mdClassLoader))
      }
      neighbor = Class.forName("org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel", false, mdClassLoader)
    }

    return markdownHtmlPanelClass.getDeclaredConstructor().newInstance()
  }
}
