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

version = "1.0.0"  // todo: npm doesn't support versions like "1.0-SNAPSHOT"

val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows

val npmCommand = when (isWindows) {
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
        "electron-packager" to electronPackagerVersion
      ),
      "dependencies" to mapOf(
        "electron" to electronVersion
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

val packageDarwinX64 by tasks.creating(Exec::class) {
  group = "dist"
  dependsOn(initDistEnvironment)
  workingDir(project.file(distDir))
  commandLine(packagerCommand + listOf("--platform=darwin", "--arch=x64"))
}

val packageLinuxX64 by tasks.creating(Exec::class) {
  group = "dist"
  dependsOn(initDistEnvironment)
  workingDir(project.file(distDir))
  commandLine(packagerCommand + listOf("--platform=linux", "--arch=x64"))
  doLast {
    if (!isWindows) {
      exec {
        commandLine(
          "chmod",
          "+x",
          project.file("$electronOutDir/$appName-linux-x64/$appName").absolutePath
        )
      }
    }
  }
}

val packageWin32X64 by tasks.creating(Exec::class) {
  group = "dist"
  dependsOn(initDistEnvironment)
  workingDir(project.file(distDir))
  commandLine(packagerCommand + listOf("--platform=win32", "--arch=x64"))
}

tasks.create("dist") {
  group = "dist"
  dependsOn(packageDarwinX64, packageLinuxX64, packageWin32X64)
}

val packageZipDarwinX64 by tasks.creating(Zip::class) {
  group = "dist"
  dependsOn(packageDarwinX64)
  val targetName = "$appName-darwin-x64"
  from(project.file(electronOutDir)) {
    include("$targetName/**")
  }
  archiveFileName.set("$targetName.zip")
  destinationDirectory.set(project.file(electronOutDir))
}

val packageZipLinuxX64 by tasks.creating(Zip::class) {
  group = "dist"
  dependsOn(packageLinuxX64)
  val targetName = "$appName-linux-x64"
  from(project.file(electronOutDir)) {
    include("$targetName/**")
  }
  archiveFileName.set("$targetName.zip")
  destinationDirectory.set(project.file(electronOutDir))
}

val packageZipWin32X64 by tasks.creating(Zip::class) {
  group = "dist"
  dependsOn(packageWin32X64)
  val targetName = "$appName-win32-x64"
  from(project.file(electronOutDir)) {
    include("$targetName/**")
  }
  archiveFileName.set("$targetName.zip")
  destinationDirectory.set(project.file(electronOutDir))
}

tasks.create("distZip") {
  group = "dist"
  dependsOn(packageZipDarwinX64, packageZipLinuxX64, packageZipWin32X64)
}

tasks.create<Exec>("electronProductionRun") {
  dependsOn(initDistEnvironment)
  workingDir(project.file(distDir))
  commandLine(npmCommand + listOf("run", "electron", "--", "."))
}
