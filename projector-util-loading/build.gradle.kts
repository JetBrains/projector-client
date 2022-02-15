/*
 * MIT License
 *
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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
import com.intellij.openapi.util.BuildNumber

plugins {
  kotlin("jvm")
  `maven-publish`
  jacoco
}

setupJacoco()

kotlin {
  explicitApi()
}

publishToSpace("java")

val coroutinesVersion: String by project
val kotestVersion: String by project
val intellijPlatformVersion: String by project

val intelliJVersionRemovedSuffix = intellijPlatformVersion.takeWhile { it.isDigit() || it == '.' } // in case of EAP
val intellijPlatformBuildNumber = BuildNumber.fromString(intelliJVersionRemovedSuffix)!!

dependencies {
  api(project(":projector-util-logging"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

  compileOnly("com.jetbrains.intellij.platform:bootstrap:$intellijPlatformVersion")

  if (intellijPlatformBuildNumber >= BuildNumber.fromString("213.6461.77")!!) {
    compileOnly("com.jetbrains.intellij.platform:util-base:$intellijPlatformVersion")
  } else {
    compileOnly("com.jetbrains.intellij.platform:util-diagnostic:$intellijPlatformVersion")
  }

  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}
