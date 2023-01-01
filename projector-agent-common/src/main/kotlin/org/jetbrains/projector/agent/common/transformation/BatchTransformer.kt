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
package org.jetbrains.projector.agent.common.transformation

import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.Instrumentation

/**
 * Transformer setup that simplifies running transformations and logging results of transformers' batch
 */
public open class BatchTransformer<Params, Transformer: TransformerSetup<Params>>(protected val transformerSetups: List<Transformer>) : TransformerSetup<Params> {

  protected val results: MutableList<TransformationResult> = mutableListOf()

  protected open val logger: Logger = Logger<BatchTransformer<Params, Transformer>>()

  final override var transformationResultConsumer: (TransformationResult) -> Unit = { transformationResult: TransformationResult ->
    results += transformationResult
  }

  init {
    transformerSetups.forEach { it.transformationResultConsumer = transformationResultConsumer }
  }

  override fun runTransformations(
    instrumentation: Instrumentation,
    parameters: Params,
    canRetransform: Boolean,
  ) {
    transformerSetups.forEach { it.runTransformations(instrumentation, parameters, canRetransform) }
    logResults("")
  }

  protected fun logResults(typeSuffix: String) {
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
      val message = "$key $typeSuffix: ${value.entries.joinToString(prefix = "[", postfix = "]") { (subKey, subValue) -> "$subKey: ${subValue.joinToString(separator = "; ", prefix = "(", postfix = ")") { it }}" }}"
      when (key) {
        TransformationResult.Error::class.java.simpleName -> logger.error { message }
        else -> logger.debug { message }
      }
    }
  }
}
