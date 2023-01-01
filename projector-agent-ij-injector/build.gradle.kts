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
import org.gradle.jvm.tasks.Jar

plugins {
  kotlin("jvm")
  jacoco
}

setupJacoco()

kotlin {
  explicitApi()
}

val coroutinesVersion: String by project
val javassistVersion: String by project
val intellijPlatformVersion: String by project
val intellijMarkdownPluginVersion: String by project
val intellijJcefVersion: String by project
val kotestVersion: String by project

dependencies {
  implementation(project(":projector-agent-common"))
  implementation(project(":projector-ij-common"))
  implementation(project(":projector-util-loading"))
  implementation(project(":projector-util-logging"))
  implementation("org.javassist:javassist:$javassistVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

  compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijPlatformVersion")
  compileOnly("com.jetbrains.intellij.platform:util-ui:$intellijPlatformVersion")

  compileOnly("com.jetbrains.intellij.markdown:markdown:$intellijMarkdownPluginVersion")
  compileOnly("org.jetbrains.intellij.deps.jcef:jcef:$intellijJcefVersion")

  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

val agentClass = "org.jetbrains.projector.agent.ijInjector.IjInjectorAgent"

tasks.withType<Jar> {
  manifest {
    attributes(
      "Can-Redefine-Classes" to true,
      "Can-Retransform-Classes" to true,
      "Agent-Class" to agentClass
    )
  }

  duplicatesStrategy = DuplicatesStrategy.WARN

  exclude("META-INF/versions/9/module-info.class")

  from(inline(configurations.runtimeClasspath))
}
