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
package org.jetbrains.projector.client.common.misc

import org.jetbrains.projector.client.common.canvas.DummyCanvasFactory
import org.jetbrains.projector.common.protocol.data.ImageData
import org.jetbrains.projector.common.protocol.data.ImageId
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageCacherTest {

  private val imageId = ImageId.BufferedImageId(15, 1076959906)
  private val imageData = ImageData.PngBase64("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA10lEQVR4XmNgGBQgKytLIjc31" +
                                              "zcvL68eSG8E0s9BGMoGifmC1KDrgwOgov/EYHR9cICuEITz8/PFQTYDcR1ZBmCTRxZDAeia8SrGBtA1oxsQ" +
                                              "sSJzbcziNElkMRQA1PAHnwGRKzL/A/F7IE5BFocDoIajRBgAw/tjVmWpIMszAEM8FJsBBQUFClgMAOFvkSu" +
                                              "zyhz2N7AgGzIR2QCg5gAg/RyHAWAMDJs5cAOghhQDNf1CMugXDgMwXQADQEPUgXgbCOfk5GiAxNA0Y4YBIQ" +
                                              "DViDsWCAF86QAAK0j+ypzY0swAAAAASUVORK5CYII=")

  @Test
  fun getImageDataShouldNotAddImageToRequestIfImageDataIsAlreadyConvertedFromBase64() {
    val imageCacher = ImageCacher(DummyCanvasFactory)

    imageCacher.putImageData(imageId, imageData)

    imageCacher.getImageData(imageId)

    assertEquals(0, imageCacher.extractImagesToRequest().size)
  }
}
