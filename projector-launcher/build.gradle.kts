/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import com.google.gson.GsonBuilder
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT

plugins {
  kotlin("js")
}

val bootstrapVersion: String by project
val coroutinesVersion: String by project
val electronVersion: String by project
val electronPackagerVersion: String by project
val jqueryVersion: String by project
val kotlinVersion: String by project
val kotlinExtensionsVersion: String by project
val openVersion: String by project

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
  implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions:$kotlinExtensionsVersion")
}

version = "1.0.2"  // todo: npm doesn't support versions like "1.0-SNAPSHOT"

val npmCommand = when (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
  true -> listOf("cmd.exe", "/C", "npm.cmd")
  false -> listOf("npm")
}

val distDir = "build/distributions"

fun Map<String, Any>.toJsonString(): String {
  val gson = GsonBuilder().setPrettyPrinting().create()!!
  return gson.toJson(this)!!
}

val generateDistPackageJson by tasks.creating(Task::class) {
  group = "dist"
  dependsOn(tasks["browserProductionWebpack"])

  doLast {
    val packageJson = mapOf(
      "main" to "projector-launcher.js",
      "scripts" to mapOf(
        "electron" to "electron",
        "electron-packager" to "electron-packager"
      ),
      "devDependencies" to mapOf(
        "electron" to electronVersion,
        "electron-packager" to electronPackagerVersion
      ),
      "keywords" to listOf(
        "projector",
        "jetbrains",
        "kotlin",
        "kotlin-js"
      ),
      "author" to "projector",
      "license" to "MIT",
      "name" to "projector-launcher",
      "description" to "Desktop launcher for Projector, written in Kotlin, Electron and Node",
      "version" to project.version.toString()
    )

    project.file("$distDir/package.json").writeText(packageJson.toJsonString())
  }
}

val initDistEnvironment by tasks.creating(Exec::class) {
  group = "dist"
  dependsOn(generateDistPackageJson)
  workingDir(project.file(distDir))
  commandLine(npmCommand + listOf("install"))
}

val electronOutDir = "build/electronOut"
val appName = "projector"

val packagerCommand = npmCommand + listOf(
  "run",
  "electron-packager",
  "--",
  "--executable-name=projector",
  "--out=${project.file(electronOutDir).absolutePath}",
  "--overwrite",
  "--icon=${project.file("src/main/resources/assets/img/electron-icon.ico").absolutePath}",
  ".",
  appName
)

fun getPackageTaskName(platform: String, arch: String) = "package${platform.capitalize()}${arch.capitalize()}"
fun getPackageZipTaskName(platform: String, arch: String) = "packageZip${platform.capitalize()}${arch.capitalize()}"

fun Task.createPackageTask(platform: String, arch: String, configuration: Exec.() -> Unit = {}): Task {

  val packageTaskName = getPackageTaskName(platform, arch)

  return task<Exec>(packageTaskName) {
    group = checkNotNull(this@createPackageTask.group) { "Grouping task of $packageTaskName group name is not defined" }
    dependsOn(initDistEnvironment)
    workingDir(project.file(distDir))
    commandLine(packagerCommand + listOf("--platform=$platform", "--arch=$arch"))
    configuration()
    this@createPackageTask.dependsOn(this)
  }
}

fun Task.createPackageZipTask(platform: String, arch: String, configuration: Zip.() -> Unit = {}): Task {

  val packageZipTaskName = getPackageZipTaskName(platform, arch)
  val packageTaskName = getPackageTaskName(platform, arch)

  return task<Zip>(packageZipTaskName) {
    group = checkNotNull(this@createPackageZipTask.group) { "Grouping task of $packageZipTaskName group name is not defined" }
    dependsOn(packageTaskName)
    val targetName = "$appName-$platform-$arch"
    from(project.file(electronOutDir)) {
      include("$targetName/**")
    }
    archiveFileName.set("$targetName.zip")
    destinationDirectory.set(project.file(electronOutDir))
    configuration()
    this@createPackageZipTask.dependsOn(this)
  }
}

val platformArchPairs = listOf(
  "darwin" to "x64",
  "darwin" to "arm64",
  "linux"  to "x64",
  "win32"  to "x64",
)

tasks.create("dist") {
  group = "dist"
  platformArchPairs.forEach { (platform, arch) -> createPackageTask(platform, arch) }
}

tasks.create("distZip") {
  group = "dist"
  platformArchPairs.forEach { (platform, arch) -> createPackageZipTask(platform, arch) }
}

tasks.create<Exec>("electronProductionRun") {
  dependsOn(initDistEnvironment)
  workingDir(project.file(distDir))
  commandLine(npmCommand + listOf("run", "electron", "--", "."))
}
