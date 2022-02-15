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

import com.intellij.ui.jcef.*
import javassist.*
import javassist.expr.*
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowserFactory
import org.cef.browser.CefRendering
import org.cef.browser.CefRequestContext
import org.cef.handler.CefClientHandler
import org.jetbrains.projector.agent.common.*
import org.jetbrains.projector.common.intellij.buildAtLeast
import org.jetbrains.projector.ij.jcef.CefHandlers
import org.jetbrains.projector.ij.jcef.ProjectorCefBrowser
import org.jetbrains.projector.util.loading.state.IdeState

internal object IjJcefTransformer : IdeTransformerSetup<IjInjector.AgentParameters>() {

  private var isAgent = false

  override fun getTransformations(
    parameters: IjInjector.AgentParameters,
    classLoader: ClassLoader
  ): Map<Class<*>, (CtClass) -> ByteArray?> {

    isAgent = parameters.isAgent

    val transformations = mutableMapOf<Class<*>, (CtClass) -> ByteArray?>(
      CefApp::class.java to ::transformCefApp,
      CefBrowserFactory::class.java to ::transformCefBrowserFactory,
      CefClientHandler::class.java to ::transformCefClientHandler,
      Class.forName("org.cef.browser.CefMessageRouter_N") to ::transformNativeCefMessageRouter,
      JBCefClient::class.java to ::transformJBCefClient,
      JBCefJSQuery::class.java to ::transformJBCefJSQuery,
      Class.forName("com.intellij.ui.jcef.JBCefFileSchemeHandlerFactory") to ::transformJBSchemeHandlerFactory,
    )

    if (buildAtLeast("211")) {
      transformations += JBCefBrowserBase::class.java to ::transformJBCefBrowserBase
    }

    return transformations
  }

  override fun isTransformerAvailable(parameters: IjInjector.AgentParameters): Boolean {
    return parameters.jcefTransformerInUse
  }

  override val loadingState: IdeState
    get() = IdeState.CONFIGURATION_STORE_INITIALIZED

  private fun transformJBSchemeHandlerFactory(clazz: CtClass): ByteArray {

    if (isAgent) {

      val nativeBrowserCode = ProjectorCefBrowser::originalBrowser.getter.getJavaCallString("$1")

      @Suppress("UnusedAssignment")
      clazz
        .getDeclaredMethod("registerLoadHTMLRequest")
        .insertBefore(
          // language=java prefix="class JBCefFileSchemeHandlerFactory { public static String registerLoadHTMLRequest(@NotNull org.cef.browser.CefBrowser $1, @NotNull String $2, @NotNull String $3)" suffix="}"
          """
          {
            // in agent mode native browser is saved to map, but ProjectorCefBrowser is passed to getter
            if ($1.getClass().getName().equals("${ProjectorCefBrowser::class.java.name}")) {
              $1 = $nativeBrowserCode;
            }
          }
        """.trimIndent()
        )
    }

    return clazz.toBytecode()
  }

  private fun transformJBCefJSQuery(clazz: CtClass): ByteArray {

    if (buildAtLeast("211")) { // version where warning appeared

      /**
       *  Get rid of warning with stacktrace due to [JBCefBrowserBase.isCefBrowserCreated] made always return true
       */
      clazz
        .getDeclaredMethods("create") // there are two of them in 221
        .forEach {
          it.setBody(
            // language=java prefix="class JBCefJSQuery { public static com.intellij.ui.jcef.JBCefJSQuery create(@NotNull com.intellij.ui.jcef.JBCefBrowserBase $1)" suffix="}"
            """
              {
                return new com.intellij.ui.jcef.JBCefJSQuery($1, new com.intellij.ui.jcef.JBCefJSQuery.JSQueryFunc($1.getJBCefClient()));
              }
            """.trimIndent()
          )
        }

    }

    return clazz.toBytecode()
  }

  private fun CtBehavior.setBodyIfHeadless(body: String) {
    if (isAgent) return
    setBody(body)
  }

  private fun CtBehavior.insertAfterIfHeadless(body: String) {
    if (isAgent) return
    insertAfter(body)
  }

  private fun CtBehavior.setBodyOrInsertBefore(body: String) {
    when (isAgent) {
      true -> insertBefore(body)
      false -> setBody(body)
    }
  }

  private fun CtBehavior.setBodyOrInsertAfter(body: String) {
    when (isAgent) {
      true -> insertAfter(body)
      false -> setBody(body)
    }
  }

  private fun transformCefClientHandler(clazz: CtClass): ByteArray {

    @Suppress("rawtypes")
    clazz
      .getDeclaredConstructor()
      .setBodyIfHeadless(
        // language=java prefix="class CefClientHandler { public CefClientHandler()" suffix="}"
        """
          {
            // Remove call to native method
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("addMessageRouter")
      // language=java prefix="class CefClientHandler { protected synchronized void addMessageRouter(org.cef.browser.CefMessageRouter $1) {" suffix="}}"
      .setBodyOrInsertBefore(CefHandlers::onMessageRouterAdded.getJavaCallString("$0", "$1"))

    clazz
      .getDeclaredMethod("removeMessageRouter")
      // language=java prefix="class CefClientHandler { protected synchronized void removeMessageRouter(org.cef.browser.CefMessageRouter $1) {" suffix="}}"
      .setBodyOrInsertBefore(CefHandlers::onMessageRouterRemoved.getJavaCallString("$0", "$1"))

    return clazz.toBytecode()
  }

  private fun transformCefApp(clazz: CtClass): ByteArray {

    @Suppress("rawtypes")
    clazz
      .getDeclaredConstructor(Array<String>::class.java, CefSettings::class.java)
      .setBodyIfHeadless(
        // language=java prefix="class CefApp { private CefApp(String[] $1, org.cef.CefSettings $2)" suffix="}"
        """
          {
            super($1);
            if ($2 != null) settings_ = $2.clone();
            clients_ = new java.util.HashSet();
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("startup")
      // language=java prefix="class CefApp { public static final boolean startup(String[] $1)" suffix="}"
      .setBodyIfHeadless(
        """
          {
            return true;
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("initialize")
      .setBodyIfHeadless(
        // language=java prefix="class CefMessageRouter_N { public boolean addHandler(CefMessageRouterHandler $1, boolean $2)" suffix="}"
        """
          {
            setState(org.cef.CefApp.CefAppState.INITIALIZED);
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }

  private fun transformNativeCefMessageRouter(clazz: CtClass): ByteArray {

    // javassist can't automatically box primitives..
    val addHandlerCode = CefHandlers::addMessageRouterHandler.getJavaCallString("this", "$1", "Boolean.valueOf($2)")
    val removeHandlerCode = CefHandlers::removeMessageRouterHandler.getJavaCallString("this", "$1")
    val clearRouterHandlersCode = CefHandlers::clearMessageRouterHandlers.getJavaCallString("this")

    @Suppress("rawtypes", "unchecked")
    clazz
      .getDeclaredMethod("addHandler")
      .apply {
        if (isAgent) {
          insertBefore(
            """
              {
                $addHandlerCode;
              }
            """.trimIndent()
          )
        } else {
          setBody(
            // language=java prefix="class CefMessageRouter_N { public boolean addHandler(CefMessageRouterHandler $1, boolean $2)" suffix="}"
            """
          {
            return $addHandlerCode.booleanValue(); // javassist can't automatically unbox primitive wrapper..
          }
        """.trimIndent()
          )
        }
      }

    clazz
      .getDeclaredMethod("removeHandler")
      .apply {
        if (isAgent) {
          insertBefore(
            // language=java prefix="class CefMessageRouter_N { public boolean removeHandler(CefMessageRouterHandler $1)" suffix="}"
            """
              {
                $removeHandlerCode;
              }
            """.trimIndent()
          )
        } else {
          setBody(
            // language=java prefix="class CefMessageRouter_N { public boolean removeHandler(CefMessageRouterHandler $1)" suffix="}"
            """
              {
                return $removeHandlerCode.booleanValue(); // javassist can't automatically unbox primitive wrapper..
              }
            """.trimIndent()
          )
        }
      }

    clazz
      .getDeclaredMethod("createNative")
      .setBodyIfHeadless(
        // language=java prefix="class CefMessageRouter_N { public static final org.cef.browser.CefMessageRouter createNative(org.cef.browser.CefMessageRouterConfig $1)" suffix="}"
        """
          {
            org.cef.browser.CefMessageRouter instance = new org.cef.browser.CefMessageRouter_N();
            instance.setMessageRouterConfig($1);
            return instance;
          }
        """.trimIndent()
      )

    clazz
      .getDeclaredMethod("dispose")
      .setBodyOrInsertBefore(clearRouterHandlersCode)

    return clazz.toBytecode()
  }

  private fun transformJBCefClient(clazz: CtClass): ByteArray {

    clazz
      .getDeclaredMethod("addLifeSpanHandler")
      .insertAfterIfHeadless(CefHandlers::onLifeSpanHandlerAdded.getJavaCallString("myCefClient", "$1"))

    clazz
      .getDeclaredMethod("removeLifeSpanHandler")
      .insertAfterIfHeadless(CefHandlers::onLifeSpanHandlerRemoved.getJavaCallString("myCefClient"))

    return clazz.toBytecode()
  }

  private fun transformJBCefBrowserBase(clazz: CtClass): ByteArray {

    if (buildAtLeast("212")) {

      clazz
        .getDeclaredConstructor(JBCefBrowserBuilder::class.java)
        .instrument(JBCefBrowserBaseProjectorInserter())

    }

    if (buildAtLeast("213")) {

      fun sendSetOpenLinksInExternalBrowserString(openLinksInExternalBrowser: Boolean): String {
        val booleanParam = "Boolean.valueOf($openLinksInExternalBrowser)"
        return ProjectorCefBrowser::setOpenLinksInExternalBrowser.getJavaCallString("getCefBrowser()", booleanParam)
      }

      clazz
        .getDeclaredMethod("enableExternalBrowserLinks")
        .setBodyOrInsertBefore(sendSetOpenLinksInExternalBrowserString(true))

      clazz
        .getDeclaredMethod("disableExternalBrowserLinks")
        .setBodyOrInsertBefore(sendSetOpenLinksInExternalBrowserString(false))
    }

    // Get rid of checking by referencing to underlying CEF browser pointer
    clazz
      .getDeclaredMethods("isCefBrowserCreated")
      .forEach {
        it.setBodyIfHeadless(
          // language=java prefix="class JBCefBrowserBase { final boolean isCefBrowserCreated()" suffix="}"
          """
          {
            return true;
          }
        """.trimIndent()
        )
      }

    return clazz.toBytecode()
  }

  private fun getProjectorCefBrowserInstantiationCode(vararg params: String): String {
    return "(org.cef.browser.CefBrowser) ${::ProjectorCefBrowser.getJavaCallString(*params, autoCast = false)}"
  }

  private fun transformCefBrowserFactory(clazz: CtClass): ByteArray {

    val originalBrowserParameter = if (isAgent) "${'$'}_" else "null"
    val newBrowserInstanceCode = getProjectorCefBrowserInstantiationCode("$1", "$2", originalBrowserParameter)

    val thirdParameterType = if (buildAtLeast("211")) CefRendering::class.java else Boolean::class.java

    clazz
      .getDeclaredMethod("create",
                         CefClient::class.java, String::class.java,
                         thirdParameterType, Boolean::class.java, CefRequestContext::class.java)
      .setBodyOrInsertAfter(
        """
          {
            return $newBrowserInstanceCode;
          }
        """.trimIndent()
      )

    return clazz.toBytecode()
  }

  /**
   * Replace instantiation of CEF browser in JBCefBrowserBase with ProjectorCefBrowser
   */
  private class JBCefBrowserBaseProjectorInserter() : ExprEditor() {

    private val methodDeclaringClass = projectorClassPool[METHOD_DECLARING_CLASS]

    private val originalBrowserParameter = if (isAgent) "${'$'}proceed($$)" else "null"
    private val newBrowserInstanceCode = getProjectorCefBrowserInstantiationCode("$2", "$3", originalBrowserParameter)

    override fun edit(m: MethodCall) {
      val ctMethod = m.method
      if (ctMethod.declaringClass.subclassOf(methodDeclaringClass) && ctMethod.name == OSR_CREATOR_METHOD_NAME) {

        @Suppress("rawtypes", "unchecked")
        //language=java prefix="class JBCefBrowserBase { private @NotNull CefBrowserOsrWithHandler createOsrBrowser(...)" suffix="}"
        val code =  """
            {
              $assign $newBrowserInstanceCode;
            }
          """.trimIndent()

        // replace method call instead of called method body because method returns CefBrowserOsrWithHandler, but we only have CefBrowser
        m.replace(code)
      }
    }

    companion object {
      private const val OSR_CREATOR_METHOD_NAME = "createOsrBrowser"
      private val METHOD_DECLARING_CLASS = JBCefBrowserBase::class.java.name
    }
  }

}
