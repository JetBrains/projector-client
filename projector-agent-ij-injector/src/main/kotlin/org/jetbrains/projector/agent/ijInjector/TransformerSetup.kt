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
import javassist.LoaderClassPath
import org.jetbrains.projector.util.logging.Logger
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation

internal interface TransformerSetup {

  /**
   * Logger to be used for reporting transformation result
   */
  val logger: Logger

  /**
   * Maps class to function that is invoked to get transformed class bytecode.
   * Override this property if your transformations are agnostic of agent parameters and classloader.
   */
  val classTransformations: Map<Class<*>, (CtClass) -> ByteArray?>
    get() = emptyMap()

  /**
   * Maps class to function that is invoked to get transformed class bytecode.
   * Override this method if you need either agent parameters or classloader (returned by [getClassLoader])
   * to describe transformations.
   *
   * @see [getClassLoader]
   *
   * @param parameters agent parameters that are passed from server
   * @param classLoader classloader returned by [getClassLoader] method
   * @return mapping from class to function that is invoked to get transformed class bytecode
   */
  fun getTransformations(parameters: IjInjector.AgentParameters, classLoader: ClassLoader): Map<Class<*>, (CtClass) -> ByteArray?> = classTransformations

  /**
   * Classloader that will be passed to [getTransformations] method. This method is useful if you need to transform classes of a plugin.
   * Classpath of the returned classloader is also used to get bytecode of classes that will be transformed.
   *
   * @see [IjMdTransformer.getClassLoader]
   *
   * @param parameters agent parameters that are passed from server
   * @return Classloader that will be passed to [getTransformations] method and used to get bytecode of classes that will be transformed
   */
  fun getClassLoader(parameters: IjInjector.AgentParameters): ClassLoader? = javaClass.classLoader
}

internal fun TransformerSetup.runTransformations(
  instrumentation: Instrumentation,
  parameters: IjInjector.AgentParameters,
  canRetransform: Boolean = true,
) {

  val loader = getClassLoader(parameters) ?: return

  val transformations = getTransformations(parameters, loader)
  if (transformations.isEmpty()) return

  val transformer = createTransformer(transformations, loader)

  instrumentation.apply {
    addTransformer(transformer, canRetransform)
    retransformClasses(*transformations.keys.toTypedArray())
  }
}

internal fun TransformerSetup.classForNameOrNull(name: String, classLoader: ClassLoader): Class<*>? = try {
  Class.forName(name, false, classLoader)
} catch (e: ClassNotFoundException) {
  logger.info { "Transformation of class '$name': Class not found" }
  null
}

private fun TransformerSetup.createTransformer(
  transformations: Map<Class<*>, (CtClass) -> ByteArray?>,
  classLoader: ClassLoader,
): ClassFileTransformer {

  val classNameToTransform = transformations.map { (clazz, transform) -> clazz.name to transform }.toMap()
  val classPool = ClassPool().apply { appendClassPath(LoaderClassPath(classLoader)) }

  return ProjectorClassTransformer(classNameToTransform, classPool) {
    when (it) {
      is ProjectorClassTransformer.TransformationResult.Error -> logger.info(it.throwable) { "Transformation of class '${it.className}': Error" }
      is ProjectorClassTransformer.TransformationResult.Success -> logger.debug { "Transformation of class '${it.className}': Success" }
    }
  }
}
