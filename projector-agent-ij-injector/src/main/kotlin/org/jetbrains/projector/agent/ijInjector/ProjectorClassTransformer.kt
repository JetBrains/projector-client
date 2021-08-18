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
package org.jetbrains.projector.agent.ijInjector

import javassist.ClassPool
import javassist.CtClass
import org.jetbrains.projector.agent.common.getClassFromClassfileBuffer
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

internal class ProjectorClassTransformer(
  private val transformations: Map<String, (CtClass) -> ByteArray?>,
  private val classPool: ClassPool,
  private val transformationResultConsumer: (TransformationResult) -> Unit,
) : ClassFileTransformer {

  override fun transform(
    loader: ClassLoader,
    className: String,
    classBeingRedefined: Class<*>?,
    protectionDomain: ProtectionDomain?,
    classfileBuffer: ByteArray,
  ): ByteArray? {

    val dottedClassName = className.replace('/', '.')

    return try {
      val transform = transformations[dottedClassName] ?: return null
      val clazz = getClassFromClassfileBuffer(classPool, dottedClassName, classfileBuffer)
      val result = transform.invoke(clazz)

      transformationResultConsumer(TransformationResult.Success(dottedClassName))
      result
    }
    catch (e: Exception) {
      transformationResultConsumer(TransformationResult.Error(dottedClassName, e))
      null
    }
  }

  sealed class TransformationResult(val className: String) {

    class Success(className: String): TransformationResult(className)
    class Error(className: String, val throwable: Throwable): TransformationResult(className)

  }
}
