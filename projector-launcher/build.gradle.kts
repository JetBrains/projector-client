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

val distElectron by tasks.creating(Exec::class) {
    group = "dist"
    workingDir(project.projectDir)
    commandLine(npmCommand + listOf("run", "packager"))
}

val copyDeps by tasks.creating(Copy::class) {
    group = "dist"
    dependsOn(distElectron)

    // inspired by https://stackoverflow.com/a/42133267/6639500
    val filesToCopy = copySpec {
        from("build/js/node_modules")
    }

    into("out")

    val destinations = listOf(
            "projector-linux-x64/resources/app/node_modules",
            "projector-win32-x64/resources/app/node_modules",
            "projector-darwin-x64/projector.app/Contents/Resources/app/node_modules"
    )
    destinations.forEach { into(it) { with(filesToCopy) } }
}

tasks.create("dist") {
    group = "dist"
    dependsOn(copyDeps)
}
