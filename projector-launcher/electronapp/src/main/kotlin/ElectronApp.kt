@file:Suppress("JSCODE_ARGUMENT_SHOULD_BE_CONSTANT")

import Electron.*
import kotlinext.js.jsObject
import kotlinext.js.js
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.json

external fun encodeURI(data: String): String

class ElectronApp(val url: String) {
    var that = this
    var configData = js("{}")

    var path = require("path")
    var node_url = require("url")
    var node_fs = require("fs")

    var app: App = Electron.app
    lateinit var mainWindow: BrowserWindow
    lateinit var mainWindowUrl: String
    lateinit var mainWindowPrevUrl: String
    lateinit var mainWindowNextUrl: String
    var initialized = false;

    fun navigateMainWindow(url: String) {
        GlobalScope.launch(block = {
            that.mainWindowNextUrl = url
            try {
                that.mainWindow.loadURL(url).await()
                that.mainWindowPrevUrl = that.mainWindowUrl
                that.mainWindowUrl = url
            } catch (e: dynamic) {

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
                        nodeIntegration: true,
                        webSecurity: false,
                        worldSafeExecuteJavaScript: true,
                        preload: preloadPath
                    }
                }
            }
        """

        var preloadPath = path.normalize(path.join(__dirname, "../assets/js/preload.js"))
        this.mainWindow = BrowserWindow(js(windowOptions)(workAreaSize.width, workAreaSize.height, preloadPath))

        this.mainWindow.webContents.on("did-navigate-in-page")
        { event: Event,
          url: String,
          isMainFrame: Boolean,
          frameProcessId: Number,
          frameRoutingId: Number ->

            initialized = true;
            if (isMainFrame) {
                Logger.debug("Navigation: go to $url")
            }
        }

        this.mainWindow.webContents.on("did-fail-load", didFailLoadListener = { event: Event, errorCode: Number, errorDescription: String, validatedURL: String, isMainFrame: Boolean, frameProcessId: Number, frameRoutingId: Number ->
            GlobalScope.launch(block = {
                if (!that.initialized) {
                    if (!validatedURL.isNullOrBlank()) {
                        messageInvalidURL(validatedURL)
                    }

                    console.log("Can't load the URL: $validatedURL")
                    that.mainWindow.loadFile("openurl.html").await()
                } else {
                    Logger.direct("Loading failed, fallback activated: ${that.mainWindowNextUrl} -> ${that.mainWindowUrl}");
                    that.navigateMainWindow(that.mainWindowUrl)
                }
            })
        })

        this.mainWindow.on("closed", genericListener = {
            this.app.quit()
        });

        this.navigateMainWindow(url);
    }

    fun messageInvalidURL(validatedURL: String) {
        dialog.showMessageBoxSync(that.mainWindow, object : MessageBoxSyncOptions {
            override var message: String
                get() = "Can't load the URL"
                set(value) {}
            override var detail: String?
                get() = validatedURL
                set(value) {}
            override var type: String?
                get() = "error"
                set(value) {}
            override var title: String?
                get() = "Invalid URL"
                set(value) {}
        })
    }

    fun loadMessage(message: String) {
        GlobalScope.launch(block = {
            that.mainWindow.loadURL("about:blank").await()
            var html = "<body><h1>Invalid URL</h1><p>$message</p></body>"
            that.mainWindow.loadURL("data:text/html;charset=utf-8," + encodeURI(html)).await()
        })
    }

    fun testUrl(url: String): Boolean {
        var result: Boolean = true;
        try {
            val newUrl = URL(url)
        } catch (e: Throwable) {
            result = false;
        }
        return result;
    }

    fun registerGlobalShortcuts() {
        ElectronUtil.disableAllStandardShortcuts();

        ElectronUtil.registerGlobalShortcut(app, "Alt+F4") {
            this.quitApp();
        };

        ElectronUtil.registerGlobalShortcut(app, "Cmd+Q") {
            this.quitApp();
        };
    }

    fun registerApplicationLevelEvents() {
        app.whenReady().then {
            this.createWindow()
            this.registerGlobalShortcuts()

            if (GlobalSettings.DEVELOPER_TOOLS_ENABLED) {
                this.mainWindow.webContents.openDevTools();
            }
        }

        ipcMain.on("projector-connect") { event, arg:dynamic ->
            this.connect(arg)
        }

        ipcMain.on("projector-dom-ready") { event, arg:dynamic ->
            registerGlobalShortcuts()

            var defaultUrl = this.configData.defaultUrl
            if (null != defaultUrl) {
                event.sender.send("projector-set-url", defaultUrl);
            }
        }

        app.on("window-all-closed", listenerFunction = {
            if (!process.platform.equals("darwin")) {
                quitApp()
            }
        })

        app.on("web-contents-created", listener = { e: Event, contents: WebContents ->
            contents.on("new-window", listener = { e: Event, url: String ->
                e.preventDefault();
                require("open")(url);
            })


            contents.on("will-navigate", navigationListener = { e: Event, url: String ->
                if (url !== contents.getURL()) {
                    e.preventDefault()
                    require("open")(url);
                }
            })
        })
    }

    fun connect(newUrl: String, password: String? = null) {
        if (!this.testUrl(newUrl)) {
            messageInvalidURL(newUrl)
        }
        val urlToAddOptions = URL(newUrl)
        urlToAddOptions.searchParams.append("blockClosing", "false");
        urlToAddOptions.searchParams.append("notSecureWarning", "false");
        if (!password.isNullOrBlank()) {
            urlToAddOptions.searchParams.append("token", password);
        }
        this.configData.defaultUrl = newUrl
        this.savedb()
        this.navigateMainWindow(urlToAddOptions.toString())
    }

    fun quitApp() {
        app.exit(0)
    }

    open fun start() {
        loaddb()
        registerApplicationLevelEvents()
    }

    fun savedb() {
        var data = JSON.stringify(this.configData);

        if (!node_fs.existsSync(GlobalSettings.USER_CONFIG_DIR)){
            node_fs.mkdirSync(GlobalSettings.USER_CONFIG_DIR);
        }
        node_fs.writeFileSync(GlobalSettings.USER_CONFIG_FILE, data);
    }

    fun loaddb() {
        if (node_fs.existsSync(GlobalSettings.USER_CONFIG_FILE)) {
            var buffer = node_fs.readFileSync(GlobalSettings.USER_CONFIG_FILE);
            this.configData = JSON.parse(buffer.toString());
        }
    }
}
