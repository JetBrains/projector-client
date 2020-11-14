import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

subprojects {
    group = "org.jetbrains"
    version = "1.0.0"

    repositories {
        jcenter()
        maven("https://kotlin.bintray.com/kotlin-js-wrappers")
    }
}

val npmCommand = when (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
    true -> listOf("cmd.exe", "/C", "npm.cmd")
    false -> listOf("npm")
}

val distElectron = task<Exec>("distElectron") {
    workingDir(project.projectDir)
    commandLine(npmCommand + listOf("run", "packager"))
}

val dist = task<Exec>("dist") {
    dependsOn(distElectron)
    workingDir(project.projectDir)
    listOf("copy-deps-mac", "copy-deps-win", "copy-deps-lin").forEach {
        commandLine(npmCommand + listOf("run", it))
    }
}
