import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT

plugins {
    kotlin("js")
}

val bootstrapVersion: String by project
val electronVersion: String by project
val jqueryVersion: String by project
val openVersion: String by project
val popperVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val kotlinExtensionsVersion: String by project

kotlin {
    js {
        // todo: switch to nodejs or electron (KT-35327)
        //       while webpack is not supported in nodejs target, need to override target in webpack.config.d
        browser {
            webpackTask {
                cssSupport.enabled = true
                cssSupport.mode = EXTRACT
            }
        }
    }
}

dependencies {
    implementation(npm("bootstrap", bootstrapVersion))
    implementation(npm("electron", electronVersion))
    implementation(npm("jquery", jqueryVersion))
    implementation(npm("open", openVersion))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
    implementation("org.jetbrains:kotlin-extensions:$kotlinExtensionsVersion")
}


group = "org.jetbrains"
version = "1.0.0"

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlin-js-wrappers")
}

val npmCommand = when (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
    true -> listOf("cmd.exe", "/C", "npm.cmd")
    false -> listOf("npm")
}

val npmInstall by tasks.creating(Exec::class) {
    group = "dist"
    dependsOn(tasks["browserProductionWebpack"])
    workingDir(project.file("build/distributions"))
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
