import Electron.*

class ElectronUtil {
    companion object {
        var app: App = Electron.app

        public fun disableAllStandardShortcuts() {
            Menu.setApplicationMenu(null);
        }

        public fun disableWindowShortcuts (browserWindow: BrowserWindow) {
            browserWindow.setMenu(null)
        }

        public fun registerGlobalShortcut(recipient: App, activationKey: dynamic, callback: () -> Unit) {
            app.whenReady().then {
                globalShortcut.register(activationKey, callback)
            };
        }

        public fun registerGlobalShortcut(recipient: BrowserWindow, activationKey: Accelerator, callback: () -> Unit) {
            app.whenReady().then {
                globalShortcut.register(activationKey, callback)
            };
        }
    }
}
