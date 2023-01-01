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

import javassist.ClassPool
import javassist.CtClass
import javassist.LoaderClassPath
import java.lang.instrument.ClassDefinition
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation

public interface TransformerSetup<Params> {

  /**
   * Function to be invoked when transformation finishes.
   */
  public var transformationResultConsumer: (TransformationResult) -> Unit

  /**
   * Maps class to function that is invoked to get transformed class bytecode.
   * Override this method if your transformations are agnostic of agent parameters and classloader.
   *
   * @return mapping from class to function that is invoked to get transformed class bytecode
   */
  public fun getTransformations(): Map<Class<*>, (CtClass) -> ByteArray?> = emptyMap()

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
  public fun getTransformations(parameters: Params, classLoader: ClassLoader): Map<Class<*>, (CtClass) -> ByteArray?> = getTransformations()

  /**
   * Classloader that will be passed to [getTransformations] method. This method is useful if you need to transform classes of a plugin.
   * Classpath of the returned classloader is also used to get bytecode of classes that will be transformed.
   *
   * @param parameters agent parameters that are passed from server
   * @return Classloader that will be passed to [getTransformations] method and used to get bytecode of classes that will be transformed
   */
  public fun getClassLoader(parameters: Params): ClassLoader = javaClass.classLoader

  /**
   * Reports whether transformations of thus transformer are applicable for given parameters
   *
   * @param parameters agent parameters that are passed from server
   * @return true if transformations of thus transformer are applicable for given parameters, false otherwise
   */
  public fun isTransformerAvailable(parameters: Params): Boolean = true

  /**
   * Reports whether transformations of thus transformer are applicable for given parameters
   *
   * @param parameters agent parameters that are passed from server
   * @param unavailableReasonConsumer pass a reason why you return false. Only last passed value is used
   * @return true if transformations of thus transformer are applicable for given parameters, false otherwise
   */
  public fun isTransformerAvailable(parameters: Params, unavailableReasonConsumer: (String) -> Unit): Boolean =
    isTransformerAvailable(parameters)

  /**
   * Runs transformation (returned from [getTransformations]) of this transformer.
   *
   * @param instrumentation services needed to instrument Java bytecode
   * @param parameters agent parameters that are passed from server
   * @param canRetransform can this transformer's transformations be retransformed (from Java)
   */
  public fun runTransformations(
    instrumentation: Instrumentation,
    parameters: Params,
    canRetransform: Boolean = true,
  ) {

    fun skipAllClasses(reason: String) {
      transformationResultConsumer(TransformationResult.Skip(this, "All classes", reason))
    }

    var unsupportedReason: String? = null
    fun unsupportedReasonSaver(reason: String) {
      unsupportedReason = reason
    }

    if (!isTransformerAvailable(parameters, ::unsupportedReasonSaver)) {
      val reason = unsupportedReason ?: "Transformer is not available for provided parameters or environment setup"
      skipAllClasses(reason)
      return
    }

    val loader = getClassLoader(parameters)

    val transformations = getTransformations(parameters, loader)
    if (transformations.isEmpty()) {
      skipAllClasses("No transformations found")
      return
    }

    val transformer = createTransformer(transformations, loader)

    instrumentation.apply {
      addTransformer(transformer, canRetransform)
      retransformClasses(*transformations.keys.toTypedArray())

      val pool = ClassPool().apply { appendClassPath(LoaderClassPath(loader)) }

      redefineClasses(*transformations.map { ClassDefinition(it.key, it.value(pool[it.key.name])) }.toTypedArray())
    }
  }
}

public abstract class TransformerSetupBase<Params> : TransformerSetup<Params> {

  override var transformationResultConsumer: (TransformationResult) -> Unit = {}
}

public fun classForNameOrNull(name: String, classLoader: ClassLoader): Class<*>? = try {
  Class.forName(name, false, classLoader)
} catch (e: ClassNotFoundException) {
  null
}

private fun TransformerSetup<*>.createTransformer(
  transformations: Map<Class<*>, (CtClass) -> ByteArray?>,
  classLoader: ClassLoader,
): ClassFileTransformer {

  val classNameToTransform = transformations.map { (clazz, transform) -> clazz.name to transform }.toMap()
  val classPool = ClassPool().apply { appendClassPath(LoaderClassPath(classLoader)) }

  return ProjectorClassTransformer(this, classNameToTransform, classPool)
}
