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

import org.jetbrains.projector.common.protocol.data.PaintValue
import org.jetbrains.projector.common.protocol.toClient.ServerWindowColorsEvent
import org.jetbrains.projector.util.logging.Logger
import java.awt.Color
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Class retrieves color info from IDE settings.
 * Subscribe on LafManager and update colors on LAF change.
 * Calls provided in constructor onColorsChanged action on LAF change.
 */
public class IdeColors(private val onColorsChanged: (ServerWindowColorsEvent.ColorsStorage) -> Unit) {

  private val logger = Logger<IdeColors>()

  public var colors: ServerWindowColorsEvent.ColorsStorage? = null
    private set

  init {
    invokeWhenIdeaIsInitialized("Getting IDE colors") { ideaClassLoader ->
      getColors(ideaClassLoader)?.let {
        colors = it
        onColorsChanged(it)
      }
      subscribeToIdeLafManager(ideaClassLoader)
    }
  }

  private fun subscribeToIdeLafManager(ideaClassLoader: ClassLoader) {
    try {
      val lafManagerClass = Class.forName("com.intellij.ide.ui.LafManager", false, ideaClassLoader)
      val lafManagerListenerClass = Class.forName("com.intellij.ide.ui.LafManagerListener", false, ideaClassLoader)

      val obj = InvocationHandler { _, method, _ ->
        if (method.declaringClass == lafManagerListenerClass && method.name == "lookAndFeelChanged") {
          getColors(ideaClassLoader)?.let {
            colors = it
            onColorsChanged(it)
          }
        }
        null
      }

      val proxy = Proxy.newProxyInstance(ideaClassLoader, arrayOf(lafManagerListenerClass), obj) as Proxy

      val lafManagerInstance = lafManagerClass.getDeclaredMethod("getInstance").invoke(null)
      lafManagerClass.getDeclaredMethod("addLafManagerListener", lafManagerListenerClass).invoke(lafManagerInstance, proxy)
    }
    catch (e: Exception) {
      logger.error(e) { "Failed to subscribe to IDE LAF manager." }
    }
  }

  private fun getColors(ideaClassLoader: ClassLoader): ServerWindowColorsEvent.ColorsStorage? {
    try {
      val popupClass = Class.forName("com.intellij.util.ui.JBUI\$CurrentTheme\$Popup", false, ideaClassLoader)

      val headerBackgroundMethod = popupClass.getDeclaredMethod("headerBackground", Boolean::class.java)
      val windowHeaderActiveBackground = PaintValue.Color((headerBackgroundMethod.invoke(null, true) as Color).rgb)
      val windowHeaderInactiveBackground = PaintValue.Color((headerBackgroundMethod.invoke(null, false) as Color).rgb)

      val borderColorMethod = popupClass.getDeclaredMethod("borderColor", Boolean::class.java)
      val windowActiveBorder = PaintValue.Color((borderColorMethod.invoke(null, true) as Color).rgb)
      val windowInactiveBorder = PaintValue.Color((borderColorMethod.invoke(null, false) as Color).rgb)

      val labelClass = Class.forName("com.intellij.util.ui.JBUI\$CurrentTheme\$Label", false, ideaClassLoader)

      val labelForegroundMethod = labelClass.getDeclaredMethod("foreground")
      val windowHeaderActiveText = PaintValue.Color((labelForegroundMethod.invoke(null) as Color).rgb)
      val windowHeaderInactiveText = windowHeaderActiveText

      return ServerWindowColorsEvent.ColorsStorage(
        windowHeaderActiveBackground = windowHeaderActiveBackground,
        windowHeaderInactiveBackground = windowHeaderInactiveBackground,
        windowActiveBorder = windowActiveBorder,
        windowInactiveBorder = windowInactiveBorder,
        windowHeaderActiveText = windowHeaderActiveText,
        windowHeaderInactiveText = windowHeaderInactiveText
      )
    }
    catch (e: Exception) {
      logger.error(e) { "Failed to get IDE color scheme." }
      return null
    }
  }
}
