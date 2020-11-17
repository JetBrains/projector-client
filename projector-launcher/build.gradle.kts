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

val npmInstall by tasks.creating(Exec::class) {
    group = "dist"
    workingDir(project.file("electronapp/build/distributions"))
    commandLine(npmCommand + listOf("install"))
}

val runPackager by tasks.creating(Exec::class) {
    group = "dist"
    dependsOn(npmInstall)
    workingDir(project.projectDir)
    commandLine(npmCommand + listOf("run", "packager"))
}

tasks.create("dist") {
    group = "dist"
    dependsOn(runPackager)
}
