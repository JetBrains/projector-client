/*
 * Copyright (c) 2019-2021, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
package org.jetbrains.projector.agent.ijInjector

import javassist.ClassPool
import javassist.CtMethod
import javassist.LoaderClassPath
import org.jetbrains.projector.agent.init.IjArgs

internal object ClassloaderFacilities {

  public lateinit var utilsClass: Class<*>

  fun invoke(utils: IjInjector.Utils) {

    val classloaderProviderClass = utils.args.getValue(IjArgs.IJ_CL_PROVIDER_CLASS)
    val classloaderProviderMethod = utils.args.getValue(IjArgs.IJ_CL_PROVIDER_METHOD)
    val ideClassLoader = Class.forName(classloaderProviderClass).getDeclaredMethod(classloaderProviderMethod).invoke(null) as ClassLoader

    println("ideClassLoader: ${ideClassLoader}")

    val classPool = ClassPool().apply { appendClassPath(LoaderClassPath(ideClassLoader)) }

    val ctClass = classPool.makeClass("com.intellij.prj.Utils")

    //ctClass.addMethod(
    //  CtMethod.make(
    //    //language=java prefix="class Utils {" suffix="}"
    //    """
    //      public void test() {
    //        Class clazz = org.jetbrains.projector.util.loading.ProjectorClassLoader.class;
    //      }
    //    """.trimIndent(), ctClass
    //  )
    //)
    //
    //ctClass.addMethod(
    //  CtMethod.make(
    //    //language=java prefix="class Utils {" suffix="}"
    //    """
    //      public Class loadByProjector(String name) {
    //        org.jetbrains.projector.util.loading.ProjectorClassLoader loader =
    //             org.jetbrains.projector.util.loading.ProjectorClassLoader.getInstance();
    //
    //        return loader.loadClass(name);
    //      }
    //    """.trimIndent(), ctClass
    //  )
    //)

    ctClass.debugWriteFile()

    utilsClass = ctClass.toClass(ideClassLoader, null)
  }

}
