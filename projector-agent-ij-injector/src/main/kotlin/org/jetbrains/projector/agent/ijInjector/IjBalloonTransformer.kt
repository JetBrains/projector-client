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

import com.intellij.ui.jcef.HwFacadeHelper
import com.intellij.util.lang.UrlClassLoader
import javassist.ClassPool
import javassist.LoaderClassPath
import org.jetbrains.projector.agent.common.getClassFromClassfileBuffer
import org.jetbrains.projector.agent.ijInjector.components.BalloonWindow
import org.jetbrains.projector.agent.ijInjector.components.ProjectorHwFacadeHelper
import org.jetbrains.projector.agent.ijInjector.components.ProjectorHwFacadeJPanel
import org.jetbrains.projector.agent.ijInjector.components.ProjectorHwFacadeNonOpaquePanel
import org.jetbrains.projector.util.loading.ProjectorClassLoader
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

internal class IjBalloonTransformer : ClassFileTransformer {

  override fun transform(
    loader: ClassLoader?,
    className: String?,
    classBeingRedefined: Class<*>?,
    protectionDomain: ProtectionDomain?,
    classfileBuffer: ByteArray,
  ): ByteArray? {

    return try {
      when (className) {
        BALLOON_CLASS_PATH -> transformActual(className, classfileBuffer)
        //BALLOON_COMPONENT_CLASS_PATH -> transformActualComponent(className, classfileBuffer)
        //BALLOON_BUTTON_CLASS_PATH -> transformActualButton(className, classfileBuffer)
        PRJ_FACADE_CLASS_PATH, PRJ_FACADE_HELPER_CLASS_PATH, PRJ_FACADE_NON_OPAQUE_CLASS_PATH, PRJ_BALLOON_WINDOW_CLASS_PATH -> defineWithIdeLoader(className, classfileBuffer)
        //PRJ_FACADE_CLASS_PATH, PRJ_FACADE_HELPER_CLASS_PATH -> transformActualFacade(className, classfileBuffer)
        else -> null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun defineWithIdeLoader(className: String, classfileBuffer: ByteArray,): ByteArray? {
    println("FJJIFJIF: 0 ${javaClass.classLoader}")

    val prjLoader = (javaClass.classLoader as ProjectorClassLoader)

    println("FJJIFJIF: 1111")

    val ideLoader = prjLoader.ideaClassLoader as UrlClassLoader

    println("FJJIFJIF: 222")

    //val newSuperClass = pool.get(ProjectorHwFacadeJPanel::class.java.name)
    //clazz.superclass = newSuperClass

    println("FJJIFJIF: 1")


    val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass",
                                  String::class.java, ByteArray::class.java, Int::class.java, Int::class.java, ProtectionDomain::class.java)

    defineClassMethod.isAccessible = true

    val result = defineClassMethod.invoke(ideLoader, className.replace('/', '.'), classfileBuffer, 0, classfileBuffer.size, null) as Class<*>

    //val result = ideLoader.consumeClassData(className.replace('/', '.'), bytecode, null, null)

    println("FJJIFJIF: ${result.name}")

    return classfileBuffer
  }

  //private fun transformActualComponent(className: String, classfileBuffer: ByteArray,): ByteArray? {
  //
  //  val pool = ClassPool().apply { appendClassPath(LoaderClassPath(this@IjBalloonTransformer.javaClass.classLoader)) }
  //
  //  val clazz = getClassFromClassfileBuffer(pool, className, classfileBuffer)
  //  clazz.defrost()
  //
  //  val newSuperClass = pool.get(ProjectorHwFacadeJPanel::class.java.name)
  //  clazz.superclass = newSuperClass
  //
  //  clazz.debugWriteFile()
  //
  //  println("Kkfdklflkdf")
  //
  //  return try {
  //    clazz.toBytecode()
  //  } catch (e: Exception) {
  //    println("ASSDSDSDSDSDADD")
  //    e.printStackTrace()
  //    null
  //  }
  //}

  private fun transformActualComponent(className: String, classfileBuffer: ByteArray,): ByteArray? {

    val pool = ClassPool().apply { appendClassPath(LoaderClassPath(this@IjBalloonTransformer.javaClass.classLoader)) }

    val clazz = getClassFromClassfileBuffer(pool, className, classfileBuffer)
    clazz.defrost()

    val newSuperClass = pool.get(ProjectorHwFacadeJPanel::class.java.name)
    clazz.superclass = newSuperClass

    clazz.debugWriteFile()

    println("Kkfdklflkdf")

    return try {
      clazz.toBytecode()
    } catch (e: Exception) {
      println("ASSDSDSDSDSDADD")
      e.printStackTrace()
      null
    }
  }

  private fun transformActualButton(className: String, classfileBuffer: ByteArray,): ByteArray? {

    val pool = ClassPool().apply { appendClassPath(LoaderClassPath(this@IjBalloonTransformer.javaClass.classLoader)) }

    val clazz = getClassFromClassfileBuffer(pool, className, classfileBuffer)
    clazz.defrost()

    val newSuperClass = pool.get(ProjectorHwFacadeNonOpaquePanel::class.java.name)
    clazz.superclass = newSuperClass

    clazz.debugWriteFile()

    println("Kkfdklflkdf")

    return try {
      clazz.toBytecode()
    } catch (e: Exception) {
      println("ASSDSDSDSDSDADD")
      e.printStackTrace()
      null
    }
  }

  private fun transformActual(className: String, classfileBuffer: ByteArray,): ByteArray {
    println("BALLOON_CLASS_PATH")
    //println("BALLOON_CLASS_PATH: ${com.intellij.jdkEx.JdkEx::class.java.name}")

    val pool = ClassPool().apply { appendClassPath(LoaderClassPath(this@IjBalloonTransformer.javaClass.classLoader)) }

    val clazz = getClassFromClassfileBuffer(pool, className, classfileBuffer)
    clazz.defrost()


    //try {
    //  val cl = Class.forName("com.intellij.ui.BalloonImpl\$MyComponent")
    //  println("BALLOON_CLASS_PATH: ${cl.name}")
    //  val component = pool.get("com.intellij.ui.BalloonImpl\$MyComponent")
    //  val newSuperClass = pool.get(ProjectorHwFacadeJPanel::class.java.name)
    //  component.superclass = newSuperClass
    //} catch (e: Exception) {
    //  e.printStackTrace()
    //}

    try {

      clazz
        .getDeclaredMethod("activateIfNeeded")
        .setBody(
          //language=java prefix="class HwFacadeHelper { JComponent myTarget; JWindow myHwFacade; Color TRANSPARENT_COLOR; void activateIfNeeded()" suffix="}"
          """
          {
            if (!myTarget.isShowing()) return;
            //if (!myTarget.isShowing()) return;
          
            System.out.println("activateIfNeeded: " + getClass().getClassLoader());
            
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice gd = ge.getDefaultScreenDevice();
            
            boolean isUniformTranslucencySupported = gd.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT);
            boolean isPerPixelTranslucencySupported = gd.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
            boolean isShapedWindowSupported = gd.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
            
            System.out.println("GraphicsEnvironment: " + isUniformTranslucencySupported + ";" + isPerPixelTranslucencySupported + ";" + isShapedWindowSupported + ";" );
            
            java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(myTarget);
            
            java.lang.reflect.Method appClassLoaderMethod = ClassLoader.class.getDeclaredMethod("getBuiltinAppClassLoader", new Class[] {});
            appClassLoaderMethod.setAccessible(true);
            ClassLoader appClassLoader = (ClassLoader) appClassLoaderMethod.invoke(null, new Object[]{});
            
            Class projectorClass = appClassLoader.loadClass("org.jetbrains.projector.util.loading.ProjectorClassLoader");
            java.lang.reflect.Method projectorInstanceMethod = projectorClass.getDeclaredMethod("getInstance", new Class[]{});
            //
            ClassLoader loader = (ClassLoader) projectorInstanceMethod.invoke(null, new Object[]{});

            //Class clazz = loader.loadClass("org.jetbrains.projector.agent.ijInjector.components.DummyClass");

            //System.out.println("activateIfNeeded2: " + clazz.getName());

            Class adapterClass = loader.loadClass("org.jetbrains.projector.agent.ijInjector.components.HwComponentAdapter");  
            java.lang.reflect.Constructor adapterConstructor = adapterClass.getDeclaredConstructor(
                new Class[]{ javax.swing.JComponent.class, getClass() }
            );
            java.awt.event.ComponentAdapter hwComponentAdapter = (java.awt.event.ComponentAdapter) adapterConstructor.newInstance(
                new Object[]{ myTarget, this }
            );

            myHwFacade = new javax.swing.JWindow(owner);
            owner.addComponentListener(myOwnerListener = hwComponentAdapter);
           
            //

            Class panelClass = loader.loadClass("org.jetbrains.projector.agent.ijInjector.components.HwJPanel");
            java.lang.reflect.Constructor panelConstructor = panelClass.getDeclaredConstructor(
                new Class[]{ getClass() }
            );

            javax.swing.JPanel root = (javax.swing.JPanel) panelConstructor.newInstance(new Object[]{ this });
            root.setBackground(TRANSPARENT_COLOR);
            
            myHwFacade.add(root);
            
            //
            com.intellij.jdkEx.JdkEx.setIgnoreMouseEvents(myHwFacade, true);
            
            Class mouseAdapterClass = loader.loadClass("org.jetbrains.projector.agent.ijInjector.components.HwMouseAdapter");
            java.lang.reflect.Constructor mouseAdapterConstructor = mouseAdapterClass.getDeclaredConstructor(new Class[]{});

            java.awt.event.MouseListener mouseAdapter = (java.awt.event.MouseListener) mouseAdapterConstructor.newInstance(new Object[]{});

            myHwFacade.addMouseListener(mouseAdapter); 

            myHwFacade.setBounds(new java.awt.Rectangle(myTarget.getLocationOnScreen(), myTarget.getSize()));
            myHwFacade.setFocusableWindowState(false);
            myHwFacade.setBackground(TRANSPARENT_COLOR);
            myHwFacade.setVisible(true);
            
            //myTarget.getParent().remove(myTarget);
          }
        """.trimIndent()
        )

    } catch (e: Exception) {
      e.printStackTrace()
    }

    clazz
      .getDeclaredMethod("isCefAppActive")
      .setBody(
        //language=java prefix="class HwFacadeHelper { boolean isCefAppActive()" suffix="}"
        """
          {
            return true;
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("onShowing")
      .insertBefore(
        //language=java prefix="class HwFacadeHelper { void onShowing()" suffix="}"
        """
          {
            System.out.println("onShowing");
          }
        """.trimIndent()
      )

    //clazz
    //  .getDeclaredMethod("paint")
    //  .setBody(
    //    //language=java prefix="class HwFacadeHelper { void paint()" suffix="}"
    //    """
    //      {
    //        System.out.println("painttttt");
    //      }
    //    """.trimIndent()
    //  )

    println("DebugFilePath: " + File(".").absolutePath)
    clazz.debugWriteFile()

    println("DebugFilePathAfter: " + File(".").absolutePath)

    return clazz.toBytecode()
  }

  companion object {

    private val BALLOON_CLASS = HwFacadeHelper::class.java
    private val BALLOON_CLASS_PATH = BALLOON_CLASS.name.replace('.', '/')

    private val PRJ_FACADE_CLASS = ProjectorHwFacadeJPanel::class.java
    private val PRJ_FACADE_CLASS_PATH = PRJ_FACADE_CLASS.name.replace('.', '/')

    private val PRJ_FACADE_NON_OPAQUE_CLASS = ProjectorHwFacadeNonOpaquePanel::class.java
    private val PRJ_FACADE_NON_OPAQUE_CLASS_PATH = PRJ_FACADE_NON_OPAQUE_CLASS.name.replace('.', '/')

    private val PRJ_FACADE_HELPER_CLASS = ProjectorHwFacadeHelper::class.java
    private val PRJ_FACADE_HELPER_CLASS_PATH = PRJ_FACADE_HELPER_CLASS.name.replace('.', '/')

    private val PRJ_BALLOON_WINDOW_CLASS = BalloonWindow::class.java
    private val PRJ_BALLOON_WINDOW_CLASS_PATH = PRJ_FACADE_HELPER_CLASS.name.replace('.', '/')

    private val BALLOON_COMPONENT_CLASS_NAME = "com.intellij.ui.BalloonImpl\$MyComponent"
    private val BALLOON_COMPONENT_CLASS_PATH = BALLOON_COMPONENT_CLASS_NAME.replace('.', '/')

    //private val BALLOON_BUTTON_CLASS = BalloonImpl.ActionButton::class.java
    private val BALLOON_BUTTON_CLASS_NAME = "com.intellij.ui.BalloonImpl\$ActionButton"
    private val BALLOON_BUTTON_CLASS_PATH = BALLOON_BUTTON_CLASS_NAME.replace('.', '/')

    fun transform(utils: IjInjector.Utils) {

      val transformer = IjBalloonTransformer()

      utils.instrumentation.apply {
        addTransformer(transformer, true)
        retransformClasses(PRJ_BALLOON_WINDOW_CLASS, PRJ_FACADE_HELPER_CLASS, PRJ_FACADE_CLASS, PRJ_FACADE_NON_OPAQUE_CLASS)
        retransformClasses(Class.forName(BALLOON_BUTTON_CLASS_NAME))
        retransformClasses(BALLOON_CLASS, Class.forName(BALLOON_COMPONENT_CLASS_NAME))
      }

    }

  }
}
