/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
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
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

plugins {
  kotlin("multiplatform") apply false
  `maven-publish`
}

val kotlinVersion: String by project
val targetJvm: String by project
val publishingVersion: String by project

subprojects {
  group = "org.jetbrains.projector"
  version = publishingVersion

  repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = targetJvm
  }

  tasks.withType<KotlinCompile<*>> {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
      //allWarningsAsErrors = true  // todo: resolve "different Kotlin JARs in runtime classpath" and "bundled Kotlin runtime"
    }
  }

  tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
      jvmTarget = targetJvm
    }
  }
}

plugins.withType(NodeJsRootPlugin::class.java) {
  the<NodeJsRootExtension>().nodeVersion = "16.14.2"
}

if (System.getenv("CHROME_BIN") == null) {
  gradle.taskGraph.beforeTask {
    if (name in setOf("jsTest", "jsBrowserTest", "browserTest")) {
      actions.clear()
      System.err.println("Skipping task as no CHROME_BIN env is set")
    }
  }
}
