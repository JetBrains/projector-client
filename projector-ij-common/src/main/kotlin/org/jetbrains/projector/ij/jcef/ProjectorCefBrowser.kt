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
package org.jetbrains.projector.ij.jcef

import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.*
import org.cef.handler.*
import org.cef.misc.CefPdfPrintSettings
import org.cef.network.CefRequest
import org.jetbrains.projector.common.EventSender
import org.jetbrains.projector.common.event.BrowserShowEventPart
import org.jetbrains.projector.common.protocol.data.CommonIntSize
import org.jetbrains.projector.common.protocol.toClient.ServerBrowserEvent
import org.jetbrains.projector.common.protocol.toServer.ClientJcefEvent
import org.jetbrains.projector.util.loading.UseProjectorLoader
import org.jetbrains.projector.util.logging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import org.jetbrains.projector.common.protocol.data.Point as PrjPoint

@UseProjectorLoader
public class ProjectorCefBrowser @JvmOverloads constructor(
  public val cefClient: CefClient,
  url: String? = null,
  public val originalBrowser: CefBrowser? = null,
) : CefNativeAdapter(), CefBrowser {

  private var state = BrowserState("", emptyList(), false, "")

  private val id = ID_COUNTER.incrementAndGet()

  private val lifeSpanHandlers = mutableListOf<CefLifeSpanHandler>()

  private val headlessBackingComponent = JPanel().apply {
    val header = JLabel("If you see this then current IDE version is not supported")
    add(header, BorderLayout.NORTH)
  }

  private fun sendMoveResizeEvent() {
    sendMoveEvent()
    sendEvent(ServerBrowserEvent.ResizeEvent(id, CommonIntSize(uiComponent.width, uiComponent.height)))
  }

  private fun sendMoveEvent() {
    if (!uiComponent.isShowing) return

    var parent = uiComponent
    var previousParent = parent
    while (parent !is Window) {
      previousParent = parent
      parent = parent.parent
    }

    val locationInWindow = SwingUtilities.convertPoint(uiComponent, uiComponent.location, parent)

    // in agent mode rootPane for some reason has vertical offset of about 30 pixels (window header height?)
    val realY = locationInWindow.y - previousParent.y
    sendEvent(ServerBrowserEvent.MoveEvent(id, PrjPoint(locationInWindow.x, realY)))
  }

  private fun sendShowEvent(shown: Boolean?) {
    val valueToSend = when (shown) {
      null -> uiComponent.isShowing
      else -> shown
    }
    eventSender.sendEventPart(BrowserShowEventPart(id, valueToSend, uiComponent.takeIf { valueToSend }))
  }

  private fun sendJs(code: String, url: String? = null, line: Int = 0) {
    if (state.html.isEmpty()) {
      logger.error(IllegalStateException("Sending js before html is not supported")) { "js code: ${code.take(200)}" }
      return
    }

    if (code.contains("const render = ;")) {
      return // TODO seems like markdown bug
    }
    sendEvent(ServerBrowserEvent.ExecuteJsEvent(id, code, url, line))
  }

  private fun sendHtml(html: String) {
    sendEvent(ServerBrowserEvent.SetHtmlEvent(id, html))
  }

  private fun sendSetOpenLinksInExternalBrowser(openLinksInExternalBrowser: Boolean) {
    sendEvent(ServerBrowserEvent.SetOpenLinksInExternalBrowserEvent(id, openLinksInExternalBrowser))
  }

  private fun ifHeadless(foo: () -> Unit) {
    if (originalBrowser == null) {
      foo()
    }
  }

  init {
    instances[id] = this

    uiComponent.addComponentListener(object : ComponentAdapter() {

      override fun componentResized(event: ComponentEvent) {
        sendMoveResizeEvent()
      }

      override fun componentMoved(e: ComponentEvent?) {
        sendMoveEvent()
      }
    })

    (uiComponent as? JComponent)?.addAncestorListener(object : AncestorListener {

      override fun ancestorAdded(event: AncestorEvent?) {
        instances[id] = this@ProjectorCefBrowser

        sendMoveResizeEvent()
        sendShowEvent(true)
        // in Python Packages browser created and this option is set before component is attached, so resendState won't be called
        if (state.openInExternalBrowser) {
          sendSetOpenLinksInExternalBrowser(true)
        }
      }

      override fun ancestorRemoved(event: AncestorEvent?) {
        instances.remove(id)

        sendShowEvent(false)
      }

      override fun ancestorMoved(event: AncestorEvent?) {
        sendMoveEvent()
      }
    })

    logger.debug { "createdPrjBrowser: ${cefClient.hashCode()}: ${instances.size} : $originalBrowser" }

    loadURL(url)
  }

  override fun getNativeRef(identifer: String?): Long {
    return (originalBrowser as? CefNative)?.getNativeRef(identifer) ?: super.getNativeRef(identifer)
  }

  override fun setNativeRef(identifer: String?, nativeRef: Long) {
    (originalBrowser as? CefNative)?.setNativeRef(identifer, nativeRef)
  }

  override fun createImmediately() {
    originalBrowser?.createImmediately()
  }

  override fun getUIComponent(): Component {
    return originalBrowser?.uiComponent ?: headlessBackingComponent
  }

  override fun getClient(): CefClient = cefClient

  override fun getRenderHandler(): CefRenderHandler {
    return originalBrowser?.renderHandler ?: TODO("Not yet implemented")
  }

  override fun getWindowHandler(): CefWindowHandler {
    return originalBrowser?.windowHandler ?: TODO("Not yet implemented")
  }

  override fun canGoBack(): Boolean {
    return originalBrowser?.canGoBack() ?: false
  }

  override fun goBack() {
    originalBrowser?.goBack()
  }

  override fun canGoForward(): Boolean {
    return originalBrowser?.canGoForward() ?: false
  }

  override fun goForward() {
    originalBrowser?.goForward()
  }

  override fun isLoading(): Boolean {
    return originalBrowser?.isLoading ?: false
  }

  override fun reload() {
    originalBrowser?.reload()
  }

  override fun reloadIgnoreCache() {
    originalBrowser?.reloadIgnoreCache()
  }

  override fun stopLoad() {
    originalBrowser?.stopLoad()
  }

  override fun getIdentifier(): Int {
    return originalBrowser?.identifier ?: id
  }

  override fun getMainFrame(): CefFrame {
    return originalBrowser?.mainFrame ?: TODO("Not yet implemented")
  }

  override fun getFocusedFrame(): CefFrame {
    return originalBrowser?.focusedFrame ?: TODO("Not yet implemented")
  }

  override fun getFrame(identifier: Long): CefFrame {
    return originalBrowser?.getFrame(identifier) ?: TODO("Not yet implemented")
  }

  override fun getFrame(name: String?): CefFrame {
    return originalBrowser?.getFrame(name) ?: TODO("Not yet implemented")
  }

  override fun getFrameIdentifiers(): Vector<Long> {
    return originalBrowser?.frameIdentifiers ?: Vector()
  }

  override fun getFrameNames(): Vector<String> {
    return originalBrowser?.frameNames ?: Vector()
  }

  override fun getFrameCount(): Int {
    return originalBrowser?.frameCount ?: 0
  }

  override fun isPopup(): Boolean {
    return originalBrowser?.isPopup ?: false
  }

  override fun hasDocument(): Boolean {
    return originalBrowser?.hasDocument() ?: true
  }

  override fun viewSource() {
    originalBrowser?.viewSource()
  }

  override fun getSource(visitor: CefStringVisitor?) {
    originalBrowser?.getSource(visitor)
  }

  override fun getText(visitor: CefStringVisitor?) {
    originalBrowser?.getText(visitor)
  }

  override fun loadRequest(request: CefRequest?) {
    originalBrowser?.loadRequest(request)
  }

  override fun loadURL(url: String?) {
    originalBrowser?.loadURL(url)

    if (url == null || url == "about:blank") return

    val parsedUrl = URL(url)

    if (parsedUrl.protocol == "file") {
      // CEF allow only urls, so in Intellij platform there is a workaround to support direct rendering of html
      val html = getHtmlMap(this)?.get(url)
      if (html != null) {
        loadHtml(html)
        return
      }
    }

    if (parsedUrl.protocol.startsWith("http") && parsedUrl.host != "localhost") {
      beforeLoad()
      state = state.copy(externalUrl = url)
      sendEvent(ServerBrowserEvent.LoadUrlEvent(id, url, true))
      afterLoad()
      return
    }

    val result = readUrlAsText(url) ?: run {
      val code = CefLoadHandler.ErrorCode.ERR_CONNECTION_FAILED
      cefClient.onLoadError(this, DEFAULT_FRAME, code, "Cannot read url content", url)
      return
    }
    loadHtml(result)
  }

  private fun <F, T> F.tryOrNull(callable: (F) -> T) = try {
    callable(this)
  }
  catch (_: Throwable) {
    null
  }

  private fun readUrlAsText(url: String) = tryOrNull { URL(url) }?.openConnection()?.tryOrNull { connection ->
    val reader = BufferedReader(InputStreamReader(connection.inputStream))
    reader.readText()
  }

  /**
   * JS functions to communicate with IDE
   */
  private fun getRequiredJs() = cefClient.getMessageRouters().mapNotNull {
    val jsQueryFunctionName = it.messageRouterConfig?.jsQueryFunction ?: return@mapNotNull null

    @Suppress("JSUnresolvedVariable", "JSUnresolvedFunction")
    // language=js
    """
      window.$jsQueryFunctionName = function(obj) {
        const prjWebModule = parent["projector-client-web"];
        const prjWebPackage = prjWebModule.org.jetbrains.projector.client.web;
        const addEvent = prjWebPackage.state.ClientAction.AddEvent;
        const jcefEvent = prjWebModule.${ClientJcefEvent::class.java.name};
        prjWebPackage.Application.fireAction(new addEvent(new jcefEvent($id, '$jsQueryFunctionName', obj.request)));
      }
    """
  }

  private fun prepareHtml(html: String): String {
    val doc = Jsoup.parse(html)
    doc.head().apply {

      // inline css
      getElementsByTag("link").forEach {
        val href = it.attr("abs:href")
        if (href.isNotEmpty() && it.attr("rel") == "stylesheet") {
          val styles = readUrlAsText(href) ?: return@forEach

          val link = Element(Tag.valueOf("style"), "")
            .text(styles)
            .attr("type", "text/css")

          it.replaceWith(link)
        }
      }

      // inline js
      getElementsByTag("script").forEach {
        val src = it.attr("src")
        if (src.isNotEmpty()) {
          val script = readUrlAsText(src) ?: return@forEach
          it.removeAttr("src")
          it.append(script)
        }
      }

      // allow all resources to support inlined css and js
      getElementsByTag("meta").forEach {
        if (it.attr("http-equiv") == "Content-Security-Policy") {
          it.remove()
        }
      }
    }

    val correctedHtml = doc.toString()

    return correctedHtml
      .replace(
        "<head>",
        """
          <head>
              ${getRequiredJs().joinToString("\n") { "<script type=\"text/javascript\">$it</script>" }}
        """.trimIndent()
      )
  }

  private fun loadHtml(html: String) {
    if (html.isEmpty()) return

    beforeLoad()

    val preparedHtml = prepareHtml(html)
    val executedJs = if (state.html.isEmpty()) state.executedJs else emptyList()
    state = state.copy(html = preparedHtml, executedJs = executedJs)

    sendHtml(preparedHtml)
    executedJs.forEach { (code, url, line) ->
      sendJs(code, url, line) // send all code that tried to be sent before html
    }

    afterLoad()
  }

  private fun beforeLoad() {
    // TODO research what should be called before load
    //cefClient.onLoadStart(this, DEFAULT_FRAME, CefRequest.TransitionType.TT_EXPLICIT) // calls methods in frame, but seems not obligatory
  }

  private fun afterLoad() = ifHeadless {
    cefClient.onLoadingStateChange(this, false, false, false)
    cefClient.onLoadEnd(this, DEFAULT_FRAME, 200)
  }

  override fun executeJavaScript(code: String?, url: String?, line: Int) {
    originalBrowser?.executeJavaScript(code, url, line)

    if (code == null) return
    state = state.copy(executedJs = state.executedJs + JsCode(code, url, line))
    sendJs(code, url, line)
  }

  override fun getURL(): String {
    return originalBrowser?.url ?: "https://example.com"
  }

  override fun close(force: Boolean) {
    originalBrowser?.close(force)
    instances.remove(id)
  }

  override fun setCloseAllowed() {
    originalBrowser?.setCloseAllowed()
  }

  override fun doClose(): Boolean {
    instances.remove(id)
    return originalBrowser?.doClose() ?: true
  }

  override fun onBeforeClose() {
    originalBrowser?.onBeforeClose()
  }

  override fun setFocus(enable: Boolean) {
    originalBrowser?.setFocus(enable)
  }

  override fun setWindowVisibility(visible: Boolean) {
    originalBrowser?.setWindowVisibility(visible)
  }

  override fun getZoomLevel(): Double {
    return originalBrowser?.zoomLevel ?: 1.0
  }

  override fun setZoomLevel(zoomLevel: Double) {
    originalBrowser?.zoomLevel = zoomLevel
  }

  override fun runFileDialog(
    mode: CefDialogHandler.FileDialogMode?,
    title: String?,
    defaultFilePath: String?,
    acceptFilters: Vector<String>?,
    selectedAcceptFilter: Int,
    callback: CefRunFileDialogCallback?,
  ) {
    originalBrowser?.runFileDialog(mode, title, defaultFilePath, acceptFilters, selectedAcceptFilter, callback)
  }

  override fun startDownload(url: String?) {
    originalBrowser?.startDownload(url)
  }

  override fun print() {
    originalBrowser?.print()
  }

  override fun printToPDF(path: String?, settings: CefPdfPrintSettings?, callback: CefPdfPrintCallback?) {
    originalBrowser?.printToPDF(path, settings, callback)
  }

  override fun find(identifier: Int, searchText: String?, forward: Boolean, matchCase: Boolean, findNext: Boolean) {
    originalBrowser?.find(identifier, searchText, forward, matchCase, findNext)
  }

  override fun stopFinding(clearSelection: Boolean) {
    originalBrowser?.stopFinding(clearSelection)
  }

  override fun getDevTools(): CefBrowser {
    return originalBrowser?.devTools ?: TODO("Not yet implemented")
  }

  override fun getDevTools(inspectAt: Point?): CefBrowser {
    return originalBrowser?.getDevTools(inspectAt) ?: TODO("Not yet implemented")
  }

  override fun replaceMisspelling(word: String?) {
    originalBrowser?.replaceMisspelling(word)
  }

  override fun wasResized(width: Int, height: Int) {
    originalBrowser?.wasResized(width, height)
  }

  override fun sendKeyEvent(e: KeyEvent?) {
    originalBrowser?.sendKeyEvent(e)
  }

  override fun sendMouseEvent(e: MouseEvent?) {
    originalBrowser?.sendMouseEvent(e)
  }

  override fun sendMouseWheelEvent(e: MouseWheelEvent?) {
    originalBrowser?.sendMouseWheelEvent(e)
  }

  override fun createScreenshot(nativeResolution: Boolean): CompletableFuture<BufferedImage> {
    return originalBrowser?.createScreenshot(nativeResolution) ?: TODO("Not yet implemented")
  }

  override fun equals(other: Any?): Boolean {
    return originalBrowser?.equals(other) ?: super.equals(other)
  }

  override fun hashCode(): Int {
    return originalBrowser?.hashCode() ?: super.hashCode()
  }

  public fun resendState() {
    if (state.html.isNotEmpty()) {
      sendHtml(state.html)
      state.executedJs.forEach { (code, url, line) ->
        sendJs(code, url, line)
      }
    }
    else if (state.externalUrl.isNotEmpty()) {
      sendEvent(ServerBrowserEvent.LoadUrlEvent(id, state.externalUrl, false))
    }
    else return

    sendMoveResizeEvent()
    sendShowEvent(null)
    sendSetOpenLinksInExternalBrowser(state.openInExternalBrowser)
  }

  public fun setOpenLinksInExternalBrowser(openLinksInExternalBrowser: Boolean) {
    state = state.copy(openInExternalBrowser = openLinksInExternalBrowser)
    sendSetOpenLinksInExternalBrowser(openLinksInExternalBrowser)
  }

  // Called only in headless
  public fun onLifeSpanHandlerAdded(lifeSpanHandler: CefLifeSpanHandler) {
    lifeSpanHandlers += lifeSpanHandler
    lifeSpanHandler.onAfterCreated(this) // html loading starts only after browser creation
  }

  // Called only in headless
  public fun onLifeSpanHandlerRemoved() {
    lifeSpanHandlers.clear()
  }

  public companion object {

    private val logger = Logger<ProjectorCefBrowser>()

    private val ID_COUNTER = AtomicInteger()

    private val eventSender = EventSender.instance

    private val htmlMapField by lazy {
      val clazz = Class.forName("com.intellij.ui.jcef.JBCefFileSchemeHandlerFactory")

      MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()).findStaticVarHandle(clazz, "LOADHTML_REQUEST_MAP", Map::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getHtmlMap(browser: CefBrowser): Map<String, String>? =
      (htmlMapField.get() as Map<CefBrowser, Map<String, String>>)[browser]

    private fun sendEvent(event: ServerBrowserEvent) = eventSender.sendEvent(event)

    private val instances = Collections.synchronizedMap(mutableMapOf<Int, ProjectorCefBrowser>())

    @Suppress("unused") // used in server
    public fun getInstance(id: Int): ProjectorCefBrowser? = instances[id]

    public fun getClientInstances(client: CefClient): List<ProjectorCefBrowser> = instances.values.filter { it.cefClient === client }

    @Suppress("unused") // used in server
    public fun updateAll() {
      instances.values.forEach {
        it.resendState()
      }
    }

  }
}
