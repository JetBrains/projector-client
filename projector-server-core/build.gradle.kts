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
import java.net.URL
import java.util.*
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
val dnsjavaVersion: String by project
val javassistVersion: String by project
val javaWebSocketVersion: String by project
val jsoupVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val selenideVersion: String by project
val slf4jVersion: String by project

val devBuilding = rootProject.file("local.properties").let {
  when (it.canRead()) {
    true -> Properties().apply { load(it.inputStream()) }.getProperty("projectorDevBuilding")?.toBoolean() ?: false
    false -> false
  }
}

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
      URL(defaultDownloadLink).openStream().copyTo(tempFile.outputStream())

      ZipFile(tempFile).let {
        it.getInputStream(it.getEntry("ubuntu-font-family-0.83/Ubuntu-R.ttf")).copyTo(destFile.outputStream())
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
  dependsOn(downloadIntTestFont)
  when (devBuilding) {
    true -> dependsOn(":projector-client-web:browserDevelopmentWebpack")
    false -> dependsOn(":projector-client-web:browserProductionWebpack")
  }
}

// todo: understand why it doesn't work on CI (https://github.com/JetBrains/projector-client/runs/1045863376)
//tasks.check { dependsOn(integrationTest) }

dependencies {
  api(project(":projector-common"))
  implementation(project(":projector-agent-initialization"))
  implementation(project(":projector-util-agent"))
  implementation(project(":projector-util-logging"))
  implementation("org.javassist:javassist:$javassistVersion")
  api("org.java-websocket:Java-WebSocket:$javaWebSocketVersion")
  implementation("org.slf4j:slf4j-simple:$slf4jVersion")
  implementation("dnsjava:dnsjava:$dnsjavaVersion")
  implementation("org.jsoup:jsoup:$jsoupVersion")

  testImplementation(kotlin("test", kotlinVersion))

  intTestImplementation("com.codeborne:selenide:$selenideVersion")
  intTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  intTestImplementation("io.ktor:ktor-server-core:$ktorVersion")
  intTestImplementation("io.ktor:ktor-server-netty:$ktorVersion")
  intTestImplementation("io.ktor:ktor-websockets:$ktorVersion")
  intTestImplementation("io.ktor:ktor-client-cio:$ktorVersion")
  intTestImplementation(kotlin("test", kotlinVersion))
  intTestImplementation(kotlin("test-junit", kotlinVersion))
}

val copyProjectorClientWebDistributionToResources = task<Copy>("copyProjectorClientWebDistributionToResources") {
  from("../projector-client-web/build/distributions")
  into("src/main/resources/projector-client-web-distribution")

  when (devBuilding) {
    true -> {
      dependsOn(":projector-client-web:browserDevelopmentWebpack")
    }
    false -> {
      exclude("*.js.map")
      dependsOn(":projector-client-web:browserProductionWebpack")
    }
  }
}

val copyProjectorAgentIjInjectorJarToResources = task<Copy>("copyProjectorAgentIjInjectorJarToResources") {
  include("projector-agent-ij-injector-*.jar")
  from("../projector-agent-ij-injector/build/libs")
  into("src/main/resources/projector-agent/")
  rename { "projector-agent-ij-injector.jar" }
  dependsOn(":projector-agent-ij-injector:jar")
}

tasks.processResources {
  dependsOn(
    copyProjectorClientWebDistributionToResources,
    copyProjectorAgentIjInjectorJarToResources
  )
}

// todo: clean these generated resources on "clean" task
