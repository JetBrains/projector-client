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

import org.jetbrains.projector.agent.common.transformation.TransformationResult
import org.jetbrains.projector.agent.common.transformation.TransformerSetup
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.Instrumentation

internal class BatchTransformer(private val transformerSetups: List<TransformerSetup<IjInjector.AgentParameters>>) : TransformerSetup<IjInjector.AgentParameters> {

  private val results = mutableListOf<TransformationResult>()

  private val logger: Logger = Logger<BatchTransformer>()

  override var transformationResultConsumer = { transformationResult: TransformationResult ->
    results += transformationResult
  }

  init {
    transformerSetups.forEach { it.transformationResultConsumer = transformationResultConsumer }
  }

  override fun runTransformations(
    instrumentation: Instrumentation,
    parameters: IjInjector.AgentParameters,
    canRetransform: Boolean,
  ) {
    transformerSetups.forEach { it.runTransformations(instrumentation, parameters, canRetransform) }

    // resultKind -> transformer name -> message for each class
    val map = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
    results.forEach { result ->
      val resultKind = result.javaClass.simpleName
      val subMap = map.getOrPut(resultKind) { mutableMapOf() }
      val transformerName = result.setup.javaClass.simpleName
      val classNames = subMap.getOrPut(transformerName) { mutableSetOf() }
      val classNameAndInfo = when (result) {
        is TransformationResult.Success -> result.className
        is TransformationResult.Skip -> "${result.className}. Reason: ${result.reason}"
        is TransformationResult.Error -> "${result.className}. Message: ${result.throwable.message}"
      }
      classNames.add(classNameAndInfo)
    }

    map.entries.forEach { (key, value) ->
      val message = "$key: ${value.entries.joinToString(prefix = "[", postfix = "]") { (subKey, subValue) -> "$subKey: ${subValue.joinToString(separator = "; ", prefix = "(", postfix = ")") { it }}" }}"
      when (key) {
        TransformationResult.Error::class.java.simpleName -> logger.error { message }
        else -> logger.debug { message }
      }
    }
  }
}
