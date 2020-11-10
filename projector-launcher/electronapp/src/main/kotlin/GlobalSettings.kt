class GlobalSettings {
    companion object {
        var node_path = require("path")
        var node_os = require("os")

        var LOG_LEVEL:String = "debug"
        var DEVELOPER_TOOLS_ENABLED: Boolean = false
        var USER_CONFIG_DIR: String = node_os.homedir() + node_path.sep + ".projector-launcher"
        var USER_CONFIG_FILE: String = node_os.homedir() + node_path.sep + ".projector-launcher" + node_path.sep + "settings.json"
    }
}
