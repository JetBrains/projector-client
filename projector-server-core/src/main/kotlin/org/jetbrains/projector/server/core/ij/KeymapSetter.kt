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
package org.jetbrains.projector.server.core.ij

import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import org.jetbrains.projector.common.protocol.data.UserKeymap
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.loading.state.IdeState
import org.jetbrains.projector.util.loading.state.whenOccurred
import org.jetbrains.projector.util.logging.Logger
import javax.swing.SwingUtilities

@UseProjectorLoader
public object KeymapSetter {

  private val logger = Logger<KeymapSetter>()

  private fun UserKeymap.toKeyMapManagerFieldName() = when (this) {
    UserKeymap.WINDOWS -> KeymapManager.X_WINDOW_KEYMAP
    UserKeymap.MAC -> KeymapManager.MAC_OS_X_10_5_PLUS_KEYMAP
    UserKeymap.LINUX -> KeymapManager.GNOME_KEYMAP
  }

  public fun setKeymap(keymap: UserKeymap) {
    IdeState.CONFIGURATION_STORE_INITIALIZED.whenOccurred("set keymap to match user's OS ($keymap)") {
      SwingUtilities.invokeLater {
        // it should be done on EDT
        val keymapManagerExInstance = KeymapManagerEx.getInstanceEx()
        val userKeymapName = keymap.toKeyMapManagerFieldName()
        val keymapInstance = keymapManagerExInstance.getKeymap(userKeymapName) ?: run {
          logger.error { "getKeymap($userKeymapName) == null - skipping setting keymap" }
          return@invokeLater
        }
        keymapManagerExInstance.activeKeymap = keymapInstance
      }
    }
  }
}
