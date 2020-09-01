/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
import java.net.URL
import java.util.zip.ZipFile

plugins {
  kotlin("jvm")
  `maven-publish`
  idea
}

kotlin {
  explicitApi()
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

val coroutinesVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val selenideVersion: String by project

val downloadIntTestFont = task("downloadIntTestFont") {
  doLast {
    val defaultDownloadLink = "https://assets.ubuntu.com/v1/0cef8205-ubuntu-font-family-0.83.zip"
    val fontPath = "src/intTest/resources/fonts/intTestFont.ttf"
    val destFile = project.file(fontPath)

    if (destFile.exists()) {
      println("Int test font already exists, skipping.")
    }
    else {
      println("Downloading int test font...")

      project.file(fontPath.substringBeforeLast('/')).mkdirs()

      val tempFile = File.createTempFile("defaultIntTestFonts", "zip")
      URL(defaultDownloadLink).openStream().transferTo(tempFile.outputStream())

      ZipFile(tempFile).let {
        it.getInputStream(it.getEntry("ubuntu-font-family-0.83/Ubuntu-R.ttf")).transferTo(destFile.outputStream())
      }

      tempFile.delete()

      println("Download complete")
    }
  }
}

val intTestSourceSetName = "intTest"

sourceSets {
  create(intTestSourceSetName) {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

val intTestImplementation: Configuration by configurations.getting {
  extendsFrom(configurations.implementation.get())
}

configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val integrationTest = task<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"

  testClassesDirs = sourceSets[intTestSourceSetName].output.classesDirs
  classpath = sourceSets[intTestSourceSetName].runtimeClasspath

  systemProperties = System.getProperties().map { (k, v) -> k.toString() to v }.toMap()

  shouldRunAfter("test")
  dependsOn(":projector-client-web:browserProductionWebpack", downloadIntTestFont)
}

// todo: understand why it doesn't work on CI (https://github.com/JetBrains/projector-client/runs/1045863376)
//tasks.check { dependsOn(integrationTest) }

dependencies {
  implementation(project(":projector-common"))

  // todo: remove these dependencies: they should be exported from projector-common but now it seems not working
  testImplementation(kotlin("test", kotlinVersion))
  testImplementation(kotlin("test-junit", kotlinVersion))

  intTestImplementation("com.codeborne:selenide:$selenideVersion")
  intTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  intTestImplementation("io.ktor:ktor-server-core:$ktorVersion")
  intTestImplementation("io.ktor:ktor-server-netty:$ktorVersion")
  intTestImplementation("io.ktor:ktor-websockets:$ktorVersion")

  // todo: remove these dependencies: they should be exported from projector-common but now it seems not working
  intTestImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  intTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
