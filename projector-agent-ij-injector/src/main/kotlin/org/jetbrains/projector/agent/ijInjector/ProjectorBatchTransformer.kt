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

import kotlinx.coroutines.*
import org.jetbrains.projector.agent.common.transformation.BatchTransformer
import org.jetbrains.projector.util.loading.state.IdeState
import org.jetbrains.projector.util.loading.state.IdeStateListener
import org.jetbrains.projector.util.loading.state.registerStateListener
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.Instrumentation

internal class ProjectorBatchTransformer(transformerSetups: List<IdeTransformerSetup<IjInjector.AgentParameters>>) :
  BatchTransformer<IjInjector.AgentParameters, IdeTransformerSetup<IjInjector.AgentParameters>>(transformerSetups) {

  override val logger: Logger = Logger<ProjectorBatchTransformer>()

  @OptIn(DelicateCoroutinesApi::class)
  private val scope = CoroutineScope(newSingleThreadContext("transformer"))

  override fun runTransformations(
    instrumentation: Instrumentation,
    parameters: IjInjector.AgentParameters,
    canRetransform: Boolean,
  ) {

    val groups = transformerSetups.groupBy { it.loadingState }.toMutableMap()

    fun runForState(state: IdeState?): Boolean {
      val transformers = groups[state] ?: return groups.isEmpty()

      scope.launch { // run on another thread to prevent hanging
        transformers.forEach { it.runTransformations(instrumentation, parameters, canRetransform) }
        logResults(state)
      }

      groups.remove(state)
      return groups.isEmpty()
    }

    runForState(null)

    val requiredStates = groups.keys.filterNotNullTo(mutableSetOf())
    registerStateListener("run transformations", IdeStateListener(requiredStates, ::runForState))
  }

  private fun logResults(state: IdeState?) {
    logResults("(State: ${state?.name ?: "Initial"})")
    results.clear()
  }

}
