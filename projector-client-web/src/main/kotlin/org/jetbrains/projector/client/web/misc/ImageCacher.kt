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
package org.jetbrains.projector.client.web.misc

import org.jetbrains.projector.client.common.canvas.DomCanvas
import org.jetbrains.projector.client.common.canvas.buffering.UnbufferedRenderingSurface
import org.jetbrains.projector.client.common.misc.Logger
import org.jetbrains.projector.client.common.misc.ParamsProvider
import org.jetbrains.projector.client.web.canvas.SingleRenderingSurfaceProcessor
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.ImageData
import org.jetbrains.projector.common.protocol.data.ImageId
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toServer.ClientRequestImageDataEvent
import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import kotlin.browser.document

object ImageCacher {

  private data class LivingEntity<out EntityType>(var lastUsageTimestamp: Double, val data: EntityType)

  private data class OffscreenImage(
    val width: Int,
    val height: Int,
    val singleRenderingSurfaceProcessor: SingleRenderingSurfaceProcessor,
    val offscreenCanvas: CanvasImageSource
  )

  private val cache = mutableMapOf<ImageId, LivingEntity<CanvasImageSource>>()

  private val requestedImages = mutableMapOf<ImageId, LivingEntity<Nothing?>>()

  private val imagesToRequest = mutableSetOf<ImageId>()

  private val offscreenImages = mutableMapOf<Long, OffscreenImage>()  // todo: support image removal

  fun collectGarbage() {
    filterDeadEntitiesOutOfMutableMap(cache)
    filterDeadEntitiesOutOfMutableMap(requestedImages)
  }

  fun putImageData(imageId: ImageId, imageData: ImageData, repainter: () -> Unit) {
    putImageAsync(imageId, imageData, repainter)
  }

  fun getImageData(imageId: ImageId): CanvasImageSource? = when (imageId) {
    is ImageId.PVolatileImageId -> offscreenImages[imageId.id]?.offscreenCanvas

    is ImageId.BufferedImageId -> {
      val imageData = cache[imageId]?.apply { lastUsageTimestamp = currentTimeStamp }?.data

      if (imageData == null) {
        if (imageId !in requestedImages) {
          imagesToRequest.add(imageId)
          requestedImages[imageId] = LivingEntity(currentTimeStamp, null)
        }

        null
      }
      else {
        imageData
      }
    }

    is ImageId.Unknown -> {
      logger.info { "Can't draw unknown image: $imageId" }

      null
    }
  }

  fun getOffscreenProcessor(offscreenTarget: ServerDrawCommandsEvent.Target.Offscreen): SingleRenderingSurfaceProcessor {
    val image = offscreenImages[offscreenTarget.pVolatileImageId]

    if (image == null || image.width != offscreenTarget.width || image.height != offscreenTarget.height) {
      val offScreenCanvas = (document.createElement("canvas") as HTMLCanvasElement).apply {
        width = offscreenTarget.width
        height = offscreenTarget.height
      }
      val offScreenRenderingSurface = UnbufferedRenderingSurface(DomCanvas(offScreenCanvas))

      val offScreenCommandProcessor = SingleRenderingSurfaceProcessor(offScreenRenderingSurface)

      offscreenImages[offscreenTarget.pVolatileImageId] = OffscreenImage(
        width = offscreenTarget.width,
        height = offscreenTarget.height,
        singleRenderingSurfaceProcessor = offScreenCommandProcessor,
        offscreenCanvas = offScreenCanvas
      )

      return offScreenCommandProcessor
    }

    return image.singleRenderingSurfaceProcessor
  }

  fun extractImagesToRequest(): List<ClientRequestImageDataEvent> {
    synchronized(imagesToRequest) {
      return imagesToRequest.map(::ClientRequestImageDataEvent).also { imagesToRequest.clear() }
    }
  }

  private fun putImageAsync(imageId: ImageId, imageData: ImageData, repainter: () -> Unit) {
    fun onLoad(image: CanvasImageSource) {
      cache[imageId] = LivingEntity(currentTimeStamp, image)
      repainter()
    }

    Do exhaustive when (imageData) {
      is ImageData.PngBase64 -> Image().apply {
        src = "data:image/png;base64,${imageData.pngBase64}"
        onload = { onLoad(this) }
      }

      is ImageData.Empty -> {
        logger.info { "Empty image received for $imageId" }

        onLoad(createEmptyImage())
      }
    }
  }

  private fun <KeyType, EntityType> isAlive(entry: Map.Entry<KeyType, LivingEntity<EntityType>>, timestamp: Double): Boolean {
    return entry.value.lastUsageTimestamp + ParamsProvider.IMAGE_TTL > timestamp
  }

  private fun <KeyType, EntityType> filterDeadEntitiesOutOfMutableMap(map: MutableMap<KeyType, LivingEntity<EntityType>>) {
    val timestamp = currentTimeStamp

    val iterator = map.iterator()

    while (iterator.hasNext()) {
      val next = iterator.next()

      if (!isAlive(next, timestamp)) {
        iterator.remove()
      }
    }
  }

  private fun createEmptyImage(): CanvasImageSource = (document.createElement("div") as HTMLCanvasElement).apply {
    width = 20
    height = 20

    (getContext("2d") as CanvasRenderingContext2D).apply {
      fillStyle = "pink"
      fillRect(0.0, 0.0, 20.0, 20.0)
    }
  }

  private val logger = Logger(ImageCacher::class.simpleName!!)
}
