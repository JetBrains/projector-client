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
package org.jetbrains.projector.agent.common

import javassist.*

public fun getClassFromClassfileBuffer(pool: ClassPool, className: String, classfileBuffer: ByteArray): CtClass {
  pool.insertClassPath(ByteArrayClassPath(className, classfileBuffer))
  return pool.get(className).apply(CtClass::defrost)
}

private val currentClassPool: ClassPool by lazy { ClassPool().apply { appendClassPath(LoaderClassPath(object {}.javaClass.classLoader)) } }

private fun CtClass.getDeclaredMethodImpl(name: String, classPool: ClassPool, params: Array<out Class<*>>): CtMethod =
  getDeclaredMethod(name, params.map { classPool[it.name] }.toTypedArray())

public fun CtClass.getDeclaredMethod(name: String, vararg params: Class<*>): CtMethod =
  getDeclaredMethodImpl(name, currentClassPool, params)
