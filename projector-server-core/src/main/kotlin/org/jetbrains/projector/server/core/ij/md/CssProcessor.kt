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
package org.jetbrains.projector.server.core.ij.md

import org.jetbrains.projector.util.logging.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

internal object CssProcessor {

  private val logger = Logger<CssProcessor>()

  fun makeCss(inlineCss: String?, cssFileUrls: List<String?>): String = buildString {
    appendLine(inlineCss.orEmpty())

    cssFileUrls.forEach {
      if (it.isNullOrEmpty()) return@forEach

      try {
        val connection = URL(it).openConnection()

        val reader = BufferedReader(InputStreamReader(connection.inputStream))

        while (true) {
          val line = reader.readLine() ?: break

          appendLine(line)
        }
      }
      catch (t: Throwable) {
        logger.error(t) { "Can't process file '$it', skipping" }
      }
    }
  }
}
