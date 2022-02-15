/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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

@file:UseProjectorLoader

package org.jetbrains.projector.agent.common

import javassist.*
import org.jetbrains.projector.util.loading.ProjectorClassLoader
import org.jetbrains.projector.util.loading.UseProjectorLoader
import java.lang.IllegalArgumentException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

internal fun getClassFromClassfileBuffer(pool: ClassPool, className: String, classfileBuffer: ByteArray): CtClass {
  pool.insertClassPath(ByteArrayClassPath(className, classfileBuffer))
  return pool.get(className).apply(CtClass::defrost)
}

public val projectorClassPool: ClassPool by lazy { ClassPool().apply { appendClassPath(LoaderClassPath(ProjectorClassLoader.instance)) } }

private fun CtClass.getDeclaredMethodImpl(name: String, classPool: ClassPool, params: Array<out Class<*>>): CtMethod =
  getDeclaredMethod(name, params.map { classPool[it.name] }.toTypedArray())

public fun CtClass.getDeclaredMethod(name: String, vararg params: Class<*>): CtMethod =
  getDeclaredMethodImpl(name, projectorClassPool, params)

private fun CtClass.getDeclaredConstructorImpl(classPool: ClassPool, params: Array<out Class<*>>): CtConstructor =
  getDeclaredConstructor(params.map { classPool[it.name] }.toTypedArray())

public fun CtClass.getDeclaredConstructor(vararg params: Class<*>): CtConstructor =
  getDeclaredConstructorImpl(projectorClassPool, params)

public inline operator fun <reified T> ClassPool.invoke(): CtClass = this[T::class.java.name]

public fun Method.toGetDeclaredMethodFormat(): String {

  //val parameterClasses = parameterTypes.joinToString(", ") { "${it.kotlin.javaObjectType.name}.class" }
  val parameterClasses = parameterTypes.joinToString(", ") { "${it.name}.class" }
  val parametersString = "new Class[] { $parameterClasses }"

  return "\"$name\", $parametersString"
}

public fun Constructor<*>.toGetDeclaredMethodFormat(): String {
  val parameterClasses = parameterTypes.joinToString(", ") { "${it.name}.class" }
  return "new Class[] { $parameterClasses }"
}

public fun loadClassWithProjectorLoader(clazz: Class<*>): String = loadClassWithProjectorLoader(clazz.name, true)

private fun loadClassWithProjectorLoader(className: String, trim: Boolean): String = """
        $commonClassLoadCode
          .loadClass("$className")
    """.let { if (!trim) it.trimIndent() else it }

private val unitType by lazy { Unit::class.createType() }

public fun <T : KFunction<*>> T.getJavaCallString(
  vararg params: String,
  finishedExpression: Boolean = this.returnType == unitType,
  autoCast: Boolean = true,
): String {

  val mainPart = when {
    javaMethod != null -> getJavaCallString(javaMethod!!, autoCast, *params)
    javaConstructor != null -> getJavaCallString(javaConstructor!!, autoCast, *params)
    else -> throw IllegalArgumentException("Cannot convert Kotlin function $this to Java method")
  }

  return if (finishedExpression)
    """
          {
            $mainPart;
          }
        """.trimIndent()
  else mainPart
}

private fun getJavaCallString(asJavaMethod: Method, cast: Boolean = true, vararg params: String): String {

  val isStatic = Modifier.isStatic(asJavaMethod.modifiers)
  val instance = if (isStatic) "null" else params.first()
  val otherParams = if (isStatic) params.toList() else params.drop(1)

  require(otherParams.size == asJavaMethod.parameterCount) {
    "Cannot create Java method call string: expected ${asJavaMethod.parameterCount} parameters, got ${otherParams.size}"
  }

  val castString = if (!cast || asJavaMethod.returnType == Void.TYPE) "" else "(${asJavaMethod.returnType.objectType.name})"

  return """
           ($castString ${loadClassWithProjectorLoader(asJavaMethod.declaringClass.name, false)}
            .getDeclaredMethod(${asJavaMethod.toGetDeclaredMethodFormat()})
            .invoke($instance, new Object[] { ${otherParams.joinToString(", ")} }))
      """.trimIndent()
}

private fun getJavaCallString(asJavaCtor: Constructor<*>, cast: Boolean = true, vararg params: String): String {
  require(params.size == asJavaCtor.parameterCount) {
    "Cannot create Java method call string: expected ${asJavaCtor.parameterCount} parameters, got ${params.size}"
  }

  val castString = if (!cast) "" else "(${asJavaCtor.declaringClass.name})"

  return """
          ($castString ${loadClassWithProjectorLoader(asJavaCtor.declaringClass)}
            .getDeclaredConstructor(${asJavaCtor.toGetDeclaredMethodFormat()})
            .newInstance(new Object[] { ${params.joinToString(", ")} }))
      """.trimIndent()
}

private val Class<*>.objectType get() = kotlin.javaObjectType

public const val assign: String = "${'$'}_ = "

private val PROJECTOR_LOADER_CLASS_NAME: String = ProjectorClassLoader::class.java.name

private val PROJECTOR_LOADER_INSTANCE_GETTER_NAME: String = ProjectorClassLoader.Companion::instance.getter.javaMethod!!.name

private val commonClassLoadCode = """
      ((ClassLoader) ClassLoader
        .getSystemClassLoader()
        .loadClass("$PROJECTOR_LOADER_CLASS_NAME")
        .getDeclaredMethod("$PROJECTOR_LOADER_INSTANCE_GETTER_NAME", new Class[0])
        .invoke(null, new Object[0]))
    """.trimIndent()
