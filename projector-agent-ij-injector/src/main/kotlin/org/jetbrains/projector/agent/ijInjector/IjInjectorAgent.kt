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
package org.jetbrains.projector.agent.ijInjector

import org.jetbrains.projector.agent.init.toArgsMap
import org.jetbrains.projector.util.loading.ProjectorClassLoader
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.Instrumentation

@Suppress("unused") // defined as agent entry point in build.gradle
public object IjInjectorAgent {

  private val logger = Logger<IjInjectorAgent>()

  @JvmStatic
  public fun agentmain(args: String, instrumentation: Instrumentation) {
    logger.debug { "IjInjectorAgent agentmain start, args=$args" }

    val argsMap = args.toArgsMap()

    /**
     * AppClassLoader doesn't know any classes outside of Projector code base, so we need ProjectorClassLoader
     * to reference Intellij Platform classes in agent. But firstly we need to load some class with our ProjectorClassLoader,
     * so that all other agent classes will be loaded by ProjectorClassLoader as well
     *
     * [org.jetbrains.projector.agent.ijInjector.IjInjector]
     */
    val injectorClazz = Class.forName("${javaClass.packageName}.IjInjector", false, ProjectorClassLoader.instance)

    /**
     * [org.jetbrains.projector.agent.ijInjector.IjInjector.agentmain]
     */
    injectorClazz
      .getDeclaredMethod("agentmain", Instrumentation::class.java, Map::class.java)
      .invoke(null, instrumentation, argsMap)

    logger.debug { "IjInjectorAgent agentmain finish" }
  }
}
