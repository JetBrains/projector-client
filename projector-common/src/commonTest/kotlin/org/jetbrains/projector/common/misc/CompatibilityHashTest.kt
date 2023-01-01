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
package org.jetbrains.projector.common.misc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompatibilityHashTest {

  @Serializable
  @SerialName(TEST_ENUM_SERIAL_NAME)
  private enum class Variants1 {

    @Suppress("unused")
    ONE,

    @Suppress("unused")
    TWO,
  }

  @Serializable
  @SerialName(TEST_ENUM_SERIAL_NAME)
  private enum class Variants2 {

    @Suppress("unused")
    ONE,
  }

  @Serializable
  @SerialName(TEST_ENUM_SERIAL_NAME)
  private enum class Variants3 {

    @Suppress("unused")
    ONE,

    @Suppress("unused")
    TWO2,
  }

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass1(val a: Int, val b: String, val c: Variants1)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass2(val a: Int, val b: String, val c: Variants1)

  @Serializable
  private data class SimpleClass3(val a: Int, val b: String, val c: Variants1)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass4(val a: Int, val b: String, val c: Variants2)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass5(val a: Int, val b: String, val c: Variants3)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass6(val a: Int, val b: String, val c: Variants1?)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  private data class SimpleClass7(val badName: Int, val b: String, val c: Variants1)

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  sealed class SealedClass1 {

    @Suppress("unused")
    @Serializable
    data class SealedChild1(val a: Int) : SealedClass1()

    @Suppress("unused")
    @Serializable
    object SealedChild2 : SealedClass1()
  }

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  sealed class SealedClass2 {

    @Suppress("unused")
    @Serializable
    data class SealedChild1(val a: Double) : SealedClass2()

    @Suppress("unused")
    @Serializable
    object SealedChild2 : SealedClass2()
  }

  @Serializable
  @SerialName(TEST_CLASS_SERIAL_NAME)
  sealed class SealedClass3 {

    @Suppress("unused")
    @Serializable
    data class SealedChild1(val a: Int) : SealedClass3()

    @Suppress("unused")
    @Serializable
    object BadChild : SealedClass3()
  }

  private data class TestData(val serializer1: KSerializer<*>, val serializer2: KSerializer<*>, val testMessage: String, val equal: Boolean)

  @Test
  fun test() {
    listOf(
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass1.serializer(),
        testMessage = "descriptor must be compatible to itself",
        equal = true
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass2.serializer(),
        testMessage = "descriptors of classes with identical signature must be compatible",
        equal = true
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass3.serializer(),
        testMessage = "descriptors of classes with different names must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass4.serializer(),
        testMessage = "descriptors of enums with different amount of elements must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass5.serializer(),
        testMessage = "descriptors of enums with different elements must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass6.serializer(),
        testMessage = "descriptors of fields with different nullability must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SimpleClass1.serializer(),
        serializer2 = SimpleClass7.serializer(),
        testMessage = "descriptors of fields with different names must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SealedClass1.serializer(),
        serializer2 = SealedClass2.serializer(),
        testMessage = "descriptors of sealed classes with children with different elements must be incompatible",
        equal = false
      ),
      TestData(
        serializer1 = SealedClass1.serializer(),
        serializer2 = SealedClass3.serializer(),
        testMessage = "descriptors of sealed classes with different children must be incompatible",
        equal = false
      )
    )
      .flatMap { (serializer1, serializer2, testMessage, equal) ->
        listOf(
          TestData(
            serializer1 = serializer1,
            serializer2 = serializer2,
            testMessage = testMessage,
            equal = equal
          ),
          TestData(
            serializer1 = ListSerializer(serializer1),
            serializer2 = ListSerializer(serializer2),
            testMessage = "[list] $testMessage",
            equal = equal
          ),
          TestData(
            serializer1 = ListSerializer(ListSerializer(serializer1)),
            serializer2 = ListSerializer(ListSerializer(serializer2)),
            testMessage = "[list x 2] $testMessage",
            equal = equal
          )
        )
      }
      .forEach { (serializer1, serializer2, testMessage, equal) ->
        val hash1 = serializer1.descriptor.compatibilityHash
        val hash2 = serializer2.descriptor.compatibilityHash

        when (equal) {
          true -> assertEquals(hash1, hash2, testMessage)

          false -> assertNotEquals(hash1, hash2, testMessage)
        }

        println("Passed test: $testMessage")
      }
  }

  companion object {

    private const val TEST_CLASS_SERIAL_NAME = "TestClassName"

    private const val TEST_ENUM_SERIAL_NAME = "TestEnumName"
  }
}
