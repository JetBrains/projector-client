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
package org.jetbrains.projector.util.agent

import com.sun.tools.attach.VirtualMachine
import org.jetbrains.projector.util.logging.Logger
import java.io.File
import java.io.InputStream
import java.lang.management.ManagementFactory

private val logger = Logger("util.agent.MiscKt")

public fun attachAgent(agentJarPath: String, args: String?) {
  logger.debug { "dynamically attaching agent: jar=$agentJarPath, args=$args" }

  val pidOfRunningVM = ManagementFactory.getRuntimeMXBean().name.substringBefore('@')

  try {
    VirtualMachine.attach(pidOfRunningVM)!!.apply {
      loadAgent(agentJarPath, args)
      detach()
    }
  }
  catch (e: Exception) {
    throw RuntimeException(e)
  }

  logger.debug { "dynamically attaching agent is done: jar=$agentJarPath, args=$args" }
}

public fun copyAgentToTempJarAndAttach(agentJar: InputStream, args: String?) {
  val tempJar = File.createTempFile("projector-agent", ".jar").apply {
    deleteOnExit()
  }
  agentJar.transferTo(tempJar.outputStream())
  attachAgent(agentJarPath = tempJar.absolutePath, args = args)
}
