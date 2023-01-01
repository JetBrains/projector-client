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
package org.jetbrains.projector.util.loading

/**
 * Annotation to mark classes that should be loaded by one of [ProjectorClassLoader] underlying ClassLoaders.
 * You should also mark all classes in which marked one is instantiated, and go on up to the place where some class
 * is loaded by ProjectorClassLoader for this annotation to make effect
 * (alternatively you can specify those classes in projector-server/org.jetbrains.projector.server.core.classloader.ProjectorClassLoaderSetup).
 *
 * @param loader classloader which will actually load the class
 * @param attachPackage whether only marked class (and its inner classes) should be loaded by ProjectorClassLoader or the whole package
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
public annotation class UseProjectorLoader(
  val loader: ProjectorClassLoader.ActualLoader = ProjectorClassLoader.ActualLoader.PROJECTOR,
  val attachPackage: Boolean = false,
)
