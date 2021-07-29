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
package org.jetbrains.projector.server.core.ij

import org.jetbrains.projector.util.loading.unprotect
import org.jetbrains.projector.util.logging.Logger
import java.awt.RenderingHints
import javax.swing.UIManager

public object SettingsInitializer {

  private fun getIdeaComponentAntiAliasing(ideaClassLoader: ClassLoader): Any? {
    // this can't be run before IDEA's settings initialization
    val aaTextInfo = Class.forName("com.intellij.ide.ui.AntialiasingType", false, ideaClassLoader)
      .getDeclaredMethod("getAAHintForSwingComponent")
      .invoke(null)

    return aaTextInfo::class.java.getDeclaredField("aaHint").get(aaTextInfo)
  }

  private fun setDefaultComponentTextAa(aaHint: Any?) {
    // this can't be run before IDEA initialization too:
    // JComponents crash with `no ComponentUI class for: javax.swing.J***` (for example, JLabel)

    UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, aaHint)
  }

  private fun fixComponentAntiAliasing(ideaClassLoader: ClassLoader, defaultAa: Any?) {
    val aaHint = try {
      // todo: we do this only once so this won't work if a user changes AA settings in IDEA
      val ideaComponentAntiAliasing = getIdeaComponentAntiAliasing(ideaClassLoader)

      logger.debug { "Found IDEA aa: $ideaComponentAntiAliasing" }

      ideaComponentAntiAliasing
    }
    catch (t: Throwable) {
      logger.debug(t) { "Can't find IDEA AntiAliasing settings. It's OK if you don't run an IntelliJ platform based app." }

      defaultAa
    }

    setDefaultComponentTextAa(aaHint)
  }

  private fun disableSmoothScrolling(ideaClassLoader: ClassLoader) {
    val uiSettingsClass = Class.forName("com.intellij.ide.ui.UISettings", false, ideaClassLoader)

    val uiSettings = uiSettingsClass
      .getDeclaredMethod("getInstanceOrNull")
      .invoke(null)

    val uiSettingsState = uiSettingsClass
      .getDeclaredField("state").let {
        it.unprotect()

        it.get(uiSettings)
      }

    Class.forName("com.intellij.ide.ui.UISettingsState", false, ideaClassLoader)
      .getDeclaredMethod("setSmoothScrolling", Boolean::class.java)
      .invoke(uiSettingsState, false)
  }

  private fun onIdeaInitialization(ideaClassLoader: ClassLoader, defaultAa: Any?) {
    fixComponentAntiAliasing(ideaClassLoader, defaultAa)
    disableSmoothScrolling(ideaClassLoader)
  }

  public fun addTaskToInitializeIdea(defaultAa: Any?) {
    invokeWhenIdeaIsInitialized("initialize IDEA: fix AA and disable smooth scrolling (at start)") {
      onIdeaInitialization(it, defaultAa)
    }
  }

  private val logger = Logger<SettingsInitializer>()
}
