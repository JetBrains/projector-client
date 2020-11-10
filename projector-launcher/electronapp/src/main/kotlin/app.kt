import Electron.*
external fun require(module: String): dynamic
external val process: dynamic
external val __dirname: dynamic
external val JSON: dynamic

open external class URL(a: String) {
    var searchParams: dynamic
}

fun main(args: Array<String>) {
    val argv = commandLineArguments();
    var url = argv.last();

    if (url.endsWith("projector.exe")) {
        url = ""
    }

    console.log("URL: $url")
    val eapp = ElectronApp(url);
    eapp.start();
}

fun commandLineArguments(): Array<String> {
    return process.argv as Array<String>;
}
