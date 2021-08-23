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
package org.jetbrains.projector.util.loading

import org.jetbrains.projector.util.logging.Logger
import java.io.File
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URL
import java.util.jar.JarFile

@Suppress("unused", "MemberVisibilityCanBePrivate", "RedundantVisibilityModifier") // public to be accessible for additional setup
public class ProjectorClassLoader constructor(parent: ClassLoader? = null) : ClassLoader(parent) {

  @Suppress("RedundantVisibilityModifier") // Once JB IDE is loaded we must assign this field from outside
  public var ideaClassLoader: ClassLoader? = null

  private val loadMethod = ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java, Boolean::class.java).apply(Method::unprotect)

  private val myAppClassLoader: ClassLoader by lazy {
    ClassLoader::class.java
      .getDeclaredMethod("getBuiltinAppClassLoader")
      .apply(Method::unprotect)
      .invoke(null) as ClassLoader
  }

  private val myIdeaLoader: ClassLoader get() = ideaClassLoader ?: myAppClassLoader

  private val forceLoadByPlatform = mutableSetOf<String>()

  private val forceLoadByOurselves = mutableSetOf<String>()

  private val forceLoadByIdea = mutableSetOf<String>()

  private val jarFiles = mutableSetOf<String>()

  init {
    println("NewProjectorLoader: ${javaClass.classLoader}")
    if (myInstance == null) myInstance = this
  }

  @Suppress("RedundantVisibilityModifier") // public to be accessible for additional setup
  public fun forceLoadByPlatform(className: String) {
    forceLoadByPlatform += className
  }

  @Suppress("RedundantVisibilityModifier") // public to be accessible for additional setup
  public fun forceLoadByProjectorClassLoader(className: String) {
    forceLoadByOurselves += className
  }

  @Suppress("unused", "RedundantVisibilityModifier") // public to be accessible for additional setup
  public fun forceLoadByIdea(className: String) {
    forceLoadByIdea += className
  }

  private fun mustBeLoadedByPlatform(name: String): Boolean = forceLoadByPlatform.any { name.startsWith(it) }
  private fun mustBeLoadedByOurselves(name: String): Boolean = forceLoadByOurselves.any { name.startsWith(it) }
  private fun mustBeLoadedByIdea(name: String): Boolean = forceLoadByIdea.any { name.startsWith(it) }

  private fun isProjectorAgentClass(name: String): Boolean = name.startsWith(PROJECTOR_AGENT_PACKAGE_PREFIX)
  private fun isProjectorClass(name: String): Boolean = name.startsWith(PROJECTOR_PACKAGE_PREFIX)
  private fun isIntellijClass(name: String): Boolean = name.startsWith(INTELLIJ_PACKAGE_PREFIX) || (name.startsWith(JETBRAINS_PACKAGE_PREFIX) && !isProjectorClass(name))

  private fun debug(name: String, messageSupplier: () -> String) {
    val test = "org.jetbrains.projector.util.loading"
    val testPath = test.replace('.', '/')
    if (name.contains(test) || name.contains(testPath)) {
      logger.debug(null, messageSupplier)
    }
  }

  override fun loadClass(name: String, resolve: Boolean): Class<*> {

    debug(name) { "loadClass [$name] by classloader $this" }

    synchronized(getClassLoadingLock(name)) {

      val found = findLoadedClass(name)
      if (found != null) return found

      return when {
        mustBeLoadedByPlatform(name) -> myAppClassLoader.loadClass(name, resolve)
        isProjectorClass(name) -> {

          if (isProjectorAgentClass(name)) {
            try {
              loadProjectorAgentClass(name, resolve)
            } catch (e: ClassNotFoundException) {
              loadProjectorClass(name, resolve)
            }
          } else {
            loadProjectorClass(name, resolve)
          }
        }
        mustBeLoadedByOurselves(name) -> defineClass(name, resolve, ::getResourceAsStream)
        isIntellijClass(name) || mustBeLoadedByIdea(name) -> myIdeaLoader.loadClass(name, resolve)
        else -> myAppClassLoader.loadClass(name, resolve)
      }.apply {
        debug(name) { "loadClass done [${name}]" }
      }
    }
  }

  private fun ClassLoader.loadClass(name: String, resolve: Boolean): Class<*> = try {
    debug(name) { "ClassLoader[$this].loadClass [${name}]" }
    loadMethod.invoke(this, name, resolve) as Class<*>
  } catch (e: InvocationTargetException) {
    debug(name) { "InvocationTargetException [${name}]" }
    throw e.targetException
  } catch (e: Throwable) {
    debug(name) { "Exception [${name}]" }
    throw e
  }

  private fun String.toClassFileName() = "${replace('.', '/')}.class"

  private fun loadProjectorClass(name: String, resolve: Boolean): Class<*> = defineClass(name, resolve, myAppClassLoader::getResourceAsStream)

  private fun loadProjectorAgentClass(name: String, resolve: Boolean): Class<*> {
    return defineClass(name, resolve) { classFileName ->

      jarFiles.mapNotNull { jarPath ->
        val file = File(jarPath)
        if (!file.exists()) return@mapNotNull null

        val jarFile = JarFile(file)
        val entry = jarFile.getJarEntry(classFileName) ?: return@mapNotNull null

        jarFile.getInputStream(entry)
      }.firstOrNull()
    }
  }

  private fun defineClass(name: String, resolve: Boolean, inputStreamProvider: (String) -> InputStream?): Class<*> {
    val bytes = inputStreamProvider(name.toClassFileName())?.use { it.readAllBytes() } ?: throw ClassNotFoundException(name)

    val clazz = defineClass(name, bytes, 0, bytes.size)
    if (resolve) {
      resolveClass(clazz)
    }

    return clazz
  }

  override fun getResourceAsStream(name: String): InputStream? {
    logger.debug { "getResourceAsStream [$name]" }
    val resourceStream = listOf(myAppClassLoader, ideaClassLoader).fold(null as InputStream?) { stream, loader ->
      stream ?: loader?.getResourceAsStream(name)
    }
    if (resourceStream != null) return resourceStream

    return super.getResourceAsStream(name)
  }

  override fun getResource(name: String): URL? {
    logger.debug { "getResource [$name]" }
    val resource = listOf(myAppClassLoader, ideaClassLoader).fold(null as URL?) { stream, loader ->
      stream ?: loader?.getResource(name)
    }
    if (resource != null) return resource

    return super.getResource(name)
  }

  private fun appendToClassPathForInstrumentation(jarPath: String) {
    jarFiles += jarPath
  }

  @Suppress("RedundantVisibilityModifier") // public so that `instance` is accessible for additional setup
  public companion object {

    private const val INTELLIJ_PACKAGE_PREFIX = "com.intellij."
    private const val JETBRAINS_PACKAGE_PREFIX = "org.jetbrains."
    private const val PROJECTOR_PACKAGE_PREFIX = "org.jetbrains.projector."
    private const val PROJECTOR_AGENT_PACKAGE_PREFIX = "org.jetbrains.projector.agent."

    private val logger = Logger<ProjectorClassLoader>()

    private var myInstance: ProjectorClassLoader? = null

    @Suppress("RedundantVisibilityModifier") // public to be accessible for additional setup
    @JvmStatic
    public val instance: ProjectorClassLoader get() {
      return myInstance ?: ProjectorClassLoader()
    }

    //internal object Fixer {
    //
    //  public val instance: ProjectorClassLoader get() {
    //
    //  }
    //
    //}
  }
}
