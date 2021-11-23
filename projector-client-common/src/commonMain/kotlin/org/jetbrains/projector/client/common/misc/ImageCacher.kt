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

import org.jetbrains.projector.client.common.SingleRenderingSurfaceProcessor
import org.jetbrains.projector.client.common.canvas.Canvas
import org.jetbrains.projector.client.common.canvas.CanvasFactory
import org.jetbrains.projector.client.common.canvas.buffering.UnbufferedRenderingSurface
import org.jetbrains.projector.common.misc.Do
import org.jetbrains.projector.common.protocol.data.ImageData
import org.jetbrains.projector.common.protocol.data.ImageId
import org.jetbrains.projector.common.protocol.toClient.ServerDrawCommandsEvent
import org.jetbrains.projector.common.protocol.toServer.ClientRequestImageDataEvent
import org.jetbrains.projector.util.logging.Logger

class ImageCacher(private val canvasFactory: CanvasFactory) {

  private data class LivingEntity<out EntityType>(var lastUsageTimestamp: Double, val size: Int, val data: EntityType)

  private data class OffscreenImage(
    val width: Int,
    val height: Int,
    val singleRenderingSurfaceProcessor: SingleRenderingSurfaceProcessor,
    val offscreenCanvas: Canvas.ImageSource,
  )

  private var currentSize = 0

  private val cache = mutableMapOf<ImageId, LivingEntity<Canvas.ImageSource>>()

  private val requestedImages = mutableMapOf<ImageId, LivingEntity<Nothing?>>()

  private val imagesToRequest = mutableSetOf<ImageId>()

  private val offscreenImages = mutableMapOf<Long, OffscreenImage>()  // todo: support image removal

  fun collectGarbage() {
    if (currentSize > ParamsProvider.IMAGE_CACHE_SIZE_CHARS) {
      filterDeadEntitiesOutOfMutableMap(cache)
    }
    filterDeadEntitiesOutOfMutableMap(requestedImages)
  }

  fun putImageData(imageId: ImageId, imageData: ImageData) {
    requestedImages[imageId] = LivingEntity(TimeStamp.current, 0, null) // Added new image to requested

    putImageAsync(imageId, imageData)
  }

  fun getImageData(imageId: ImageId): Canvas.ImageSource? = when (imageId) {
    is ImageId.PVolatileImageId -> offscreenImages[imageId.id]?.offscreenCanvas

    is ImageId.BufferedImageId -> {
      val imageData = cache[imageId]?.apply { lastUsageTimestamp = TimeStamp.current }?.data

      if (imageData == null) {
        if (imageId !in requestedImages) {
          imagesToRequest.add(imageId)
          requestedImages[imageId] = LivingEntity(TimeStamp.current, 0, null)
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
      val offScreenCanvas = canvasFactory.create().apply {
        width = offscreenTarget.width
        height = offscreenTarget.height
      }
      val offScreenRenderingSurface = UnbufferedRenderingSurface(offScreenCanvas)

      val offScreenCommandProcessor = SingleRenderingSurfaceProcessor(offScreenRenderingSurface, this)

      offscreenImages[offscreenTarget.pVolatileImageId] = OffscreenImage(
        width = offscreenTarget.width,
        height = offscreenTarget.height,
        singleRenderingSurfaceProcessor = offScreenCommandProcessor,
        offscreenCanvas = offScreenCanvas.imageSource
      )

      return offScreenCommandProcessor
    }

    return image.singleRenderingSurfaceProcessor
  }

  fun extractImagesToRequest(): List<ClientRequestImageDataEvent> {
    // todo: synchronization is needed here
    return imagesToRequest.map(::ClientRequestImageDataEvent).also { imagesToRequest.clear() }
  }

  private fun putImageAsync(imageId: ImageId, imageData: ImageData) {
    val size = when (imageData) {
      is ImageData.PngBase64 -> imageData.pngBase64.length
      is ImageData.Empty -> 0
    }

    fun onLoad(image: Canvas.ImageSource) {
      cache[imageId] = LivingEntity(TimeStamp.current, size, image)
      currentSize += size
    }

    Do exhaustive when (imageData) {
      is ImageData.PngBase64 -> canvasFactory.createImageSource(imageData.pngBase64, ::onLoad)

      is ImageData.Empty -> {
        logger.info { "Empty image received for $imageId" }
        canvasFactory.createEmptyImageSource(::onLoad)
      }
    }
  }

  private fun <KeyType, EntityType> isAlive(entry: Map.Entry<KeyType, LivingEntity<EntityType>>, timestamp: Double): Boolean {
    return entry.value.lastUsageTimestamp + ParamsProvider.IMAGE_TTL > timestamp
  }

  private fun <KeyType, EntityType> filterDeadEntitiesOutOfMutableMap(map: MutableMap<KeyType, LivingEntity<EntityType>>) {
    val timestamp = TimeStamp.current

    val iterator = map.iterator()

    while (iterator.hasNext()) {
      val next = iterator.next()

      if (!isAlive(next, timestamp)) {
        iterator.remove()
        currentSize -= next.value.size
      }
    }
  }

  private val logger = Logger<ImageCacher>()
}
