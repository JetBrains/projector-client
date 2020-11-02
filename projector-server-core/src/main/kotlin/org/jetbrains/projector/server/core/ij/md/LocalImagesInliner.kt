/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal object LocalImagesInliner {

  private val docFactory = DocumentBuilderFactory.newInstance()
  private val docBuilder = docFactory.newDocumentBuilder()

  private val transformerFactory = TransformerFactory.newInstance()
  private val transformer = transformerFactory.newTransformer().apply {
    setOutputProperty(OutputKeys.INDENT, "no")  // this is mandatory because extra indents spoil blocks of code
    setOutputProperty(OutputKeys.METHOD, "xml")  // previous line won't work without this one
    setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")  // previous line will add header so we have o disable it explicitly
  }

  fun inlineLocalImages(html: String): String {
    val input = ByteArrayInputStream(html.toByteArray())

    val doc = docBuilder.parse(input)
    val images = doc.getElementsByTagName("img")

    for (imageId in 0 until images.length) {
      val image = images.item(imageId)

      val srcAttribute = image.attributes.getNamedItem("src") ?: continue

      val src = srcAttribute.textContent

      if (!src.startsWith("file:")) {
        continue
      }

      val extension = src.substringAfterLast('.', missingDelimiterValue = "")
      val path = Path.of(URI(src))

      val inlinedImage = inlineImage(path, extension)

      srcAttribute.textContent = inlinedImage
    }

    val source = DOMSource(doc)
    val stringWriter = StringWriter(html.length)
    transformer.transform(source, StreamResult(stringWriter))

    return stringWriter.toString()
  }

  private fun inlineImage(localPath: Path, extension: String): String {
    return try {
      val bytes = Files.readAllBytes(localPath)
      val base64Content = Base64.getEncoder().encodeToString(bytes)

      when (extension.toLowerCase()) {
        "svg" -> "data:image/svg+xml;base64,$base64Content"

        else -> "data:image/$extension;base64,$base64Content"
      }
    }
    catch (t: Throwable) {
      "Can't inline image because of $t"  // maybe file does not exist so we don't need to care
    }
  }
}
