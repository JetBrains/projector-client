class Logger {
    companion object {
        fun debug(message:dynamic) {
            if (GlobalSettings.LOG_LEVEL.equals( "debug")) {
                console.log(message);
            }
        }

        fun direct(message:dynamic) {
            console.log(message);
        }
    }
}
