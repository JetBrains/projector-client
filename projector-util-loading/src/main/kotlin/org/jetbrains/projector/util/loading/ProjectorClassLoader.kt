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

import org.jetbrains.projector.util.logging.Logger
import java.io.File
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URL
import java.util.*
import java.util.jar.JarFile

@Suppress("unused", "MemberVisibilityCanBePrivate", "RedundantVisibilityModifier") // public to be accessible for additional setup
public class ProjectorClassLoader constructor(parent: ClassLoader? = null) : ClassLoader(parent) {

  @Suppress("RedundantVisibilityModifier") // Once JB IDE is loaded we must assign this field from outside
  public var ideaClassLoader: ClassLoader? = null

  private val loadMethod = ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java, Boolean::class.java).apply(Method::unprotect)

  private val myIdeaLoader: ClassLoader get() = ideaClassLoader ?: myAppClassLoader

  private val forceLoadByPlatform = mutableSetOf<String>()

  private val forceLoadByOurselves = mutableSetOf<String>()

  private val forceLoadByIdea = mutableSetOf<String>()

  private val jarFiles = mutableSetOf<String>()

  private val pluginClassLoaders = mutableMapOf<String, ClassLoader>()

  init {
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

  @Suppress("unused", "RedundantVisibilityModifier") // public to be accessible for additional setup
  public fun addPluginLoader(classNamePrefix: String, classLoader: ClassLoader) {
    pluginClassLoaders += classNamePrefix to classLoader
  }

  private fun mustBeLoadedByPlatform(name: String): Boolean = forceLoadByPlatform.any { name.startsWith(it) }
  private fun mustBeLoadedByOurselves(name: String): Boolean = forceLoadByOurselves.any { name.startsWith(it) }
  private fun mustBeLoadedByIdea(name: String): Boolean = forceLoadByIdea.any { name.startsWith(it) }

  override fun loadClass(name: String, resolve: Boolean): Class<*> {
    synchronized(getClassLoadingLock(name)) {

      val found = findLoadedClass(name)
      if (found != null) return found

      return when {
        mustBeLoadedByOurselves(name) -> defineClass(name, resolve, ::getResourceAsStream)
        mustBeLoadedByPlatform(name) -> myAppClassLoader.loadClass(name, resolve)
        isIntellijClass(name) || mustBeLoadedByIdea(name) -> myIdeaLoader.loadClass(name, resolve)
        else -> tryLoadViaPluginClassLoader(name, resolve) ?: redefineClassIfNeeded(name, resolve)
      }
    }
  }

  private fun ClassLoader.loadClass(name: String, resolve: Boolean): Class<*> = try {
    loadMethod.invoke(this, name, resolve) as Class<*>
  } catch (e: InvocationTargetException) {
    throw e.targetException
  }

  private fun String.toClassFileName() = "${replace('.', '/')}.class"

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

  private fun tryLoadViaPluginClassLoader(className: String, resolve: Boolean): Class<*>? {

    val classFromPlugin = try {
      getPluginClassLoader(className)?.loadClass(className, resolve)
    } catch (_: Throwable) {
      null
    }

    return classFromPlugin
  }

  private fun getPluginClassLoader(className: String): ClassLoader? {
    var classLoader: ClassLoader? = null
    var longestSequenceLength = 0

    pluginClassLoaders.entries.forEach {

      if (it.key.length > longestSequenceLength && className.startsWith(it.key)) {
        classLoader = it.value
        longestSequenceLength = it.key.length
      }
    }

    return classLoader
  }

  private fun defineClass(name: String, resolve: Boolean, inputStreamProvider: (String) -> InputStream?): Class<*> {
    val bytes = inputStreamProvider(name.toClassFileName())?.use { it.readAllBytes() } ?: throw ClassNotFoundException(name)

    val clazz = defineClass(name, bytes, 0, bytes.size)
    if (resolve) {
      resolveClass(clazz)
    }

    return clazz
  }

  private fun redefineClassIfNeeded(name: String, resolve: Boolean): Class<*> {
    val appClass = myAppClassLoader.loadClass(name, resolve)
    val loadingSetup = appClass.getAnnotation(UseProjectorLoader::class.java) ?: return appClass
    val scope = if (loadingSetup.attachPackage) "${appClass.packageName}." else appClass.name
    return when (loadingSetup.loader) {
      ActualLoader.PLATFORM -> {
        forceLoadByPlatform(scope)
        myAppClassLoader.loadClass(name, resolve)
      }
      ActualLoader.IDE -> {
        forceLoadByIdea(scope)
        myIdeaLoader.loadClass(name, resolve)
      }
      ActualLoader.PROJECTOR -> {
        forceLoadByProjectorClassLoader(scope)
        defineClass(name, resolve, ::getResourceAsStream)
      }
    }
  }

  private fun <T> findInClassloaders(transform: (ClassLoader) -> T?): T? {
    return listOf(myAppClassLoader, ideaClassLoader).fold(null as T?) { value, loader ->
      value ?: loader?.let(transform)
    }
  }

  override fun getResourceAsStream(name: String?): InputStream? {
    return findInClassloaders { it.getResourceAsStream(name) } ?: super.getResourceAsStream(name)
  }

  override fun getResource(name: String?): URL? {
    return findInClassloaders { it.getResource(name) } ?: super.getResource(name)
  }

  override fun getResources(name: String?): Enumeration<URL> {
    return findInClassloaders { it.getResources(name) } ?: super.getResources(name)
  }

  public fun addJarSource(jarPath: String) {
    jarFiles += jarPath
  }

  private fun appendToClassPathForInstrumentation(jarPath: String) {
    addJarSource(jarPath)
  }

  @Suppress("RedundantVisibilityModifier") // public so that `instance` is accessible for additional setup
  public companion object {

    private const val INTELLIJ_PACKAGE_PREFIX = "com.intellij."
    private const val JETBRAINS_PACKAGE_PREFIX = "org.jetbrains."
    private const val PROJECTOR_PACKAGE_PREFIX = "org.jetbrains.projector."
    private const val PROJECTOR_AGENT_PACKAGE_PREFIX = "org.jetbrains.projector.agent."

    private val logger = Logger<ProjectorClassLoader>()

    private val myAppClassLoader: ClassLoader by lazy {
      val systemClassLoader = getSystemClassLoader()
      // check we won't create infinite recursion when loading app classes
      if (systemClassLoader.javaClass.name != ProjectorClassLoader::class.java.name) {
        systemClassLoader
      } else {
        ClassLoader::class.java
          .getDeclaredMethod("getBuiltinAppClassLoader")
          .apply(Method::unprotect)
          .invoke(null) as ClassLoader
      }
    }

    private var myInstance: ProjectorClassLoader? = null

    @Suppress("RedundantVisibilityModifier") // public to be accessible for additional setup
    @JvmStatic
    public val instance: ProjectorClassLoader
      get() = myInstance ?: ProjectorClassLoader()

    private fun isProjectorClass(name: String): Boolean = name.startsWith(PROJECTOR_PACKAGE_PREFIX)

    private fun isIntellijClass(name: String): Boolean = name.startsWith(INTELLIJ_PACKAGE_PREFIX)
                                                         || (name.startsWith(JETBRAINS_PACKAGE_PREFIX) && !isProjectorClass(name))
  }

  public enum class ActualLoader {
    PLATFORM,
    IDE,
    PROJECTOR,
  }
}
