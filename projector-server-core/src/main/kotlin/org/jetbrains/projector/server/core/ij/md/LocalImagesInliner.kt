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

import org.jsoup.Jsoup
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

internal object LocalImagesInliner {

  fun inlineLocalImages(html: String): String {
    val doc = Jsoup.parse(html)
    val images = doc.getElementsByTag("img")

    images.forEach {
      val src = it.attr("src")

      if (src.startsWith("file:")) {
        val extension = src.substringAfterLast('.', missingDelimiterValue = "")
        val path = Path.of(URI(src.substringBefore('?')))

        val inlinedImage = inlineImage(path, extension)

        it.attr("src", inlinedImage)
      }
    }

    return doc.toString()
  }

  private fun inlineImage(localPath: Path, extension: String): String {
    return try {
      val bytes = Files.readAllBytes(localPath)
      val base64Content = Base64.getEncoder().encodeToString(bytes)

      when (extension.lowercase()) {
        "svg" -> "data:image/svg+xml;base64,$base64Content"

        else -> "data:image/$extension;base64,$base64Content"
      }
    }
    catch (t: Throwable) {
      "Can't inline image because of $t"  // maybe file does not exist so we don't need to care
    }
  }
}
