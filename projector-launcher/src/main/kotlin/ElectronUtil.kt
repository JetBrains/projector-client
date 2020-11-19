import Electron.*

class ElectronUtil {
  companion object {
    var app: App = Electron.app

    fun disableAllStandardShortcuts() {
      Menu.setApplicationMenu(null)
    }

    fun disableWindowShortcuts(browserWindow: BrowserWindow) {
      browserWindow.setMenu(null)
    }

    fun registerGlobalShortcut(recipient: App, activationKey: dynamic, callback: () -> Unit) {
      app.whenReady().then {
        globalShortcut.register(activationKey, callback)
      }
    }

    fun registerGlobalShortcut(recipient: BrowserWindow, activationKey: Accelerator, callback: () -> Unit) {
      app.whenReady().then {
        globalShortcut.register(activationKey, callback)
      }
    }
  }
}
