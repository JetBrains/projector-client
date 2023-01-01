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
@file:Suppress("JSCODE_ARGUMENT_SHOULD_BE_CONSTANT")

import Electron.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL

@OptIn(DelicateCoroutinesApi::class)
class ElectronApp(val url: String) {
  var that = this
  var configData = js("{}")
  var toolboxInfoWasActivated: Boolean = false

  var path = require("path")
  //var node_url = require("url") //uncomment to enable "url" module
  var node_fs = require("fs")

  var app: App = Electron.app
  lateinit var mainWindow: BrowserWindow
  lateinit var mainWindowUrl: String
  lateinit var mainWindowPrevUrl: String
  lateinit var mainWindowNextUrl: String
  var initialized = false

  fun navigateMainWindow(url: String) {
    GlobalScope.launch(block = {
      that.mainWindowNextUrl = url
      try {
        that.mainWindow.loadURL(url).await()
        that.mainWindowPrevUrl = that.mainWindowUrl
        that.mainWindowUrl = url
      }
      catch (e: dynamic) {
        Logger.debug(e)
      }
    })
  }

  fun navigateMainWindowFromFile(url: String, saveToHistory: Boolean) {
    GlobalScope.launch(block = {
      if (saveToHistory) {
        that.mainWindowNextUrl = url
        try {
          that.mainWindow.loadFile(url).await()
          that.mainWindowPrevUrl = that.mainWindowUrl
          that.mainWindowUrl = url
        }
        catch (e: dynamic) {
          Logger.debug(e)
        }
      } else {
        try {
          that.mainWindow.loadFile(url).await()
        }
        catch (e: dynamic) {
          Logger.debug(e)
        }
      }
    })
  }

  fun createWindow() {
    val workAreaSize = screen.getPrimaryDisplay().workAreaSize

    val windowOptions = """
            function(width,height,preloadPath) {
                return {  
                    width: width,
                    height: height,
                    webPreferences: {
                        nodeIntegration: false,
                        contextIsolation: true,
                        enableRemoteModule: true,
                        webSecurity: false,
                        worldSafeExecuteJavaScript: true,
                        preload: preloadPath
                    }
                }
            }
        """

    val preloadPath = path.normalize(path.join(__dirname, "./assets/js/preload.js"))
    this.mainWindow = BrowserWindow(js(windowOptions)(workAreaSize.width, workAreaSize.height, preloadPath))

    if (process.platform.unsafeCast<String>().lowercase() !in setOf("win32", "darwin")) {
      // change icon for Linux and other systems:
      val iconPath = path.normalize(path.join(__dirname, "assets/img/electron-icon.png")).unsafeCast<String>()
      val iconImage = NativeImage.createFromPath(iconPath)
      this.mainWindow.setIcon(iconImage)
    }

    this.mainWindow.webContents.on("did-navigate-in-page")
    {
      _: Event,
      url: String,
      isMainFrame: Boolean,
      _: Number,
      _: Number,
      ->

      initialized = true
      if (isMainFrame) {
        Logger.debug("Navigation: go to $url")
      }
    }

    this.mainWindow.webContents.on("did-fail-load",
                                   didFailLoadListener = { _: Event, errorCode: Number, errorDescription: String, validatedURL: String, _: Boolean, _: Number, _: Number ->
                                     GlobalScope.launch(block = {
                                       if (!that.initialized) {
                                         if (validatedURL.isNotBlank()) {
                                           messageInvalidURL(validatedURL)
                                         }

                                         console.log("Can't load the URL: $validatedURL")
                                         console.log("errorDescription: $errorDescription, errorCode: $errorCode")

                                         val dontShowToolboxInfo = (that.configData.dontShowToolboxInfo ?: false) as Boolean
                                         if (!that.toolboxInfoWasActivated && !dontShowToolboxInfo) {
                                           that.toolboxInfoWasActivated = true
                                           that.mainWindow.loadFile("toolboxinfo.html").await()
                                         } else {
                                           that.mainWindow.loadFile("openurl.html").await()
                                         }
                                       }
                                       else {
                                         Logger.direct(
                                           "Loading failed, fallback activated: ${that.mainWindowNextUrl} -> ${that.mainWindowUrl}")
                                         that.navigateMainWindow(that.mainWindowUrl)
                                       }
                                     })
                                   })

    this.mainWindow.on("closed", genericListener = {
      this.app.quit()
    })

    this.navigateMainWindow(url)
  }

  fun messageInvalidURL(validatedURL: String) {
    dialog.showMessageBoxSync(that.mainWindow, object : MessageBoxSyncOptions {
      override var message: String
        get() = "Can't load the URL"
        set(_) {}
      override var detail: String?
        get() = validatedURL
        set(_) {}
      override var type: String?
        get() = "error"
        set(_) {}
      override var title: String?
        get() = "Invalid URL"
        set(_) {}
    })
  }

  fun testUrl(url: String): Boolean {
    var result = true
    try {
      URL(url)
    }
    catch (e: Throwable) {
      result = false
    }
    return result
  }

  fun registerApplicationLevelEvents() {
    app.whenReady().then {
      this.createWindow()
      ElectronUtil.disableAllStandardShortcuts()

      if (GlobalSettings.DEVELOPER_TOOLS_ENABLED) {
        this.mainWindow.webContents.openDevTools()
      }
    }

    ipcMain.on("projector-connect") { _, arg: dynamic ->
      this.connect(arg)
    }

    ipcMain.on("toolboxinfo-ok") { _, arg: dynamic ->
      this.toolboxInfoOK((arg ?: false) as Boolean)
    }

    ipcMain.on("projector-dom-ready") { event, _ ->
      ElectronUtil.disableAllStandardShortcuts()

      val defaultUrl = this.configData.defaultUrl
      if (null != defaultUrl) {
        event.sender.send("projector-set-url", defaultUrl)
      }
    }

    app.on("window-all-closed", listenerFunction = {
      if (!process.platform.equals("darwin")) {
        quitApp()
      }
    })

    app.on("web-contents-created", listener = { _: Event, contents: WebContents ->
      contents.on("new-window", listener = { e: Event, url: String ->
        e.preventDefault()
        require("open")(url)
      })


      contents.on("will-navigate", navigationListener = { e: Event, url: String ->
        if (url !== contents.getURL()) {
          e.preventDefault()
          require("open")(url)
        }
      })

      contents.on("before-input-event") handleExitHotkey@ { _: Event, input: Input ->
        if (input.type != "keyDown") {
          // For every input, two events can be generated: keyDown and keyUp.
          // When we close an app above our electron app, keyUp can be sent to our app because
          // the hotkey can be released after the app above is closed.
          // So need to react only to keyDown event.
          return@handleExitHotkey
        }

        if (process.platform.equals("darwin")) {
          if (input.key === "Q" && !input.control && !input.alt && input.meta && !input.shift) {
            this.quitApp()
          }
        } else {
          if (input.key === "F4" && !input.control && input.alt && !input.meta && !input.shift) {
            this.quitApp()
          }
        }
      }
    })

    // todo: to allow self-signed certificates, we just allow all certificates.
    //       it seems to be quite unsecure. we should probably show a dialog like "bad cert, do you wish to go on?"
    // from: https://stackoverflow.com/a/46789486/6639500
    // SSL/TSL: this is the self signed certificate support
    app.on("certificate-error") { event, _, _, _, _, callback ->
      // On certificate error we disable default behaviour (stop loading the page)
      // and we then say "it is all fine - true" to the callback
      event.preventDefault()
      callback(true)
    }
  }

  fun toolboxInfoOK(dontShowAgain: Boolean) {
    that.configData.dontShowToolboxInfo = dontShowAgain
    savedb()
    Logger.debug("Will navigate to the default page")
    this.navigateMainWindowFromFile("openurl.html", false)
  }

  fun connect(newUrl: String, password: String? = null) {
    if (!this.testUrl(newUrl)) {
      messageInvalidURL(newUrl)
      return
    }
    val urlToAddOptions = URL(newUrl)
    urlToAddOptions.searchParams.append("blockClosing", "false")
    urlToAddOptions.searchParams.append("notSecureWarning", "false")
    if (!password.isNullOrBlank()) {
      urlToAddOptions.searchParams.append("token", password)
    }
    this.configData.defaultUrl = newUrl
    this.savedb()
    this.navigateMainWindow(urlToAddOptions.toString())
  }

  fun quitApp() {
    app.exit(0)
  }

  fun start() {
    loaddb()
    registerApplicationLevelEvents()
  }

  fun savedb() {
    val data = JSON.stringify(this.configData)

    if (!node_fs.existsSync(GlobalSettings.USER_CONFIG_DIR)) {
      node_fs.mkdirSync(GlobalSettings.USER_CONFIG_DIR)
    }
    node_fs.writeFileSync(GlobalSettings.USER_CONFIG_FILE, data)
  }

  fun loaddb() {
    if (node_fs.existsSync(GlobalSettings.USER_CONFIG_FILE)) {
      val buffer = node_fs.readFileSync(GlobalSettings.USER_CONFIG_FILE)
      this.configData = JSON.parse(buffer.toString())
    }
  }
}
