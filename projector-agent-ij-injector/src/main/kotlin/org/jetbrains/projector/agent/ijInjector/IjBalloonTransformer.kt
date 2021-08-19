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

import com.intellij.ui.BalloonImpl
import com.intellij.ui.jcef.HwFacadeHelper
import javassist.ClassPool
import javassist.LoaderClassPath
import org.jetbrains.projector.agent.common.getClassFromClassfileBuffer
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

    if (className == BALLOON_CLASS_PATH) {

      return try {
        transformActual(className, classfileBuffer)
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }

    }

    return super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
  }

  private fun transformActual(className: String, classfileBuffer: ByteArray,): ByteArray {
    println("BALLOON_CLASS_PATH")
    println("BALLOON_CLASS_PATH: ${com.intellij.jdkEx.JdkEx::class.java.name}")

    val pool = ClassPool().apply { appendClassPath(LoaderClassPath(javaClass.classLoader)) }

    val clazz = getClassFromClassfileBuffer(pool, className, classfileBuffer)
    clazz.defrost()

    try {

      clazz
        .getDeclaredMethod("activateIfNeeded")
        .setBody(
          //language=java prefix="class HwFacadeHelper { JComponent myTarget; JWindow myHwFacade; Color TRANSPARENT_COLOR; void activateIfNeeded()" suffix="}"
          """
          {
            if (!myTarget.isShowing()) return;
          
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
            //
            //com.intellij.jdkEx.JdkEx.setIgnoreMouseEvents(myHwFacade, true);
            
            Class mouseAdapterClass = loader.loadClass("org.jetbrains.projector.agent.ijInjector.components.HwMouseAdapter");
            java.lang.reflect.Constructor mouseAdapterConstructor = mouseAdapterClass.getDeclaredConstructor(new Class[]{});

            java.awt.event.MouseListener mouseAdapter = (java.awt.event.MouseListener) mouseAdapterConstructor.newInstance(new Object[]{});

            myHwFacade.addMouseListener(mouseAdapter); 

            myHwFacade.setBounds(new java.awt.Rectangle(myTarget.getLocationOnScreen(), myTarget.getSize()));
            myHwFacade.setFocusableWindowState(false);
            myHwFacade.setBackground(TRANSPARENT_COLOR);
            myHwFacade.setVisible(true);
            
            myTarget.getParent().remove(myTarget);
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

    fun transform(utils: IjInjector.Utils) {

      val transformer = IjBalloonTransformer()

      utils.instrumentation.apply {
        addTransformer(transformer, true)
        retransformClasses(BALLOON_CLASS)
      }

    }

  }
}
