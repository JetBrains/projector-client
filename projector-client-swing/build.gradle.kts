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

plugins {
  kotlin("jvm")
  jacoco
}

jacoco {
  toolVersion = "0.8.7"
}

tasks.withType<JacocoReport> {
  reports {
    xml.isEnabled = true
    xml.destination = file(layout.buildDirectory.dir("../../JacocoReports/jacocoReportClientSwing.xml"))
    csv.required.set(false)
    html.outputLocation.set(layout.buildDirectory.dir("jacocoHtmlProjectorClientSwing"))
  }
}


val coroutinesVersion: String by project
val serializationVersion: String by project
val javaWebSocketVersion: String by project

dependencies {
  implementation(project(":projector-common"))
  implementation(project(":projector-client-common"))
  implementation(project(":projector-util-logging"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
  implementation("org.java-websocket:Java-WebSocket:$javaWebSocketVersion")

  testImplementation(kotlin("test"))
}

kotlin {

}
tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}
