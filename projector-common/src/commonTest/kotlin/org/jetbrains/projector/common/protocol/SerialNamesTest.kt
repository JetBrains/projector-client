/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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
package org.jetbrains.projector.common.protocol

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import org.jetbrains.projector.common.protocol.toClient.ServerEvent

@OptIn(ExperimentalSerializationApi::class)
class SerialNamesTest : FunSpec() {
  init {
    test("element names should be different and contain only one symbol") {
      listOf(
        ServerEvent.serializer()
      )
        .map(KSerializer<*>::descriptor)
        .forEach { checkProjectorDescriptor(it) }
    }
  }

  private fun checkProjectorDescriptor(descriptor: SerialDescriptor) {
    if (descriptor.kind is PrimitiveKind) {
      return
    }

    if (descriptor.kind in DESCRIPTOR_KINDS_TO_CHECK) {
      val elementNames = descriptor.elementNames.toList()
      val elementNamesSet = elementNames.toSet()

      withClue("Similar element names $descriptor: $elementNames") {
        elementNamesSet.shouldBeSameSizeAs(elementNames)
      }

      val singleSymbolElementNames = mutableSetOf<Char>()

      elementNames.forEach { name ->
        val singleSymbol = name.singleOrNull()

        withClue("Not single-symbol element name in $descriptor: $name") {
          singleSymbol.shouldNotBeNull()
          singleSymbolElementNames.add(singleSymbol)
        }
      }

      val expectedNames = generateSequence('a') { it + 1 }.take(singleSymbolElementNames.size).toSet()

      withClue("Not alphabetical single-symbol element names in $descriptor") {
        singleSymbolElementNames.sorted() shouldBe expectedNames.sorted()
      }
    }

    if (descriptor.kind !is SerialKind.ENUM) {
      descriptor.elementDescriptors.forEach(::checkProjectorDescriptor)
    }
  }

  companion object {

    private val DESCRIPTOR_KINDS_TO_CHECK = setOf(
      StructureKind.OBJECT,  // our objects
      StructureKind.CLASS,  // our data classes
      SerialKind.ENUM,  // our enums
      SerialKind.CONTEXTUAL  // generated info about children of our sealed class
    )
  }
}
