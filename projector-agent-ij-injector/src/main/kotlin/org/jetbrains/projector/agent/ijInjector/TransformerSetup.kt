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

internal interface TransformerSetup {

  val logger: Logger

  val classTransformations: Map<Class<*>, (CtClass) -> ByteArray?>
    get() = emptyMap()

  fun getTransformations(utils: IjInjector.Utils, classLoader: ClassLoader): Map<Class<*>, (CtClass) -> ByteArray?> = classTransformations

  fun getClassLoader(utils: IjInjector.Utils): ClassLoader? = javaClass.classLoader
}

internal fun TransformerSetup.runTransformations(utils: IjInjector.Utils, canRetransform: Boolean = true) {

  val loader = getClassLoader(utils) ?: return

  val transformations = getTransformations(utils, loader)
  if (transformations.isEmpty()) return

  val transformer = createTransformer(transformations, loader)

  utils.instrumentation.apply {
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
