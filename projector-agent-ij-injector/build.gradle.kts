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
import org.gradle.jvm.tasks.Jar

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
    xml.destination = file(layout.buildDirectory.dir("../../JacocoReports/jacocoReportAgentIjInjector.xml"))
    csv.required.set(false)
    html.outputLocation.set(layout.buildDirectory.dir("jacocoHtmlProjectorClient"))
  }
}

kotlin {
  explicitApi()
}

val javassistVersion: String by project

dependencies {
  implementation(project(":projector-agent-common"))
  implementation(project(":projector-agent-initialization"))
  implementation(project(":projector-util-logging"))
  implementation("org.javassist:javassist:$javassistVersion")
  testImplementation(kotlin("test"))
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

  exclude("META-INF/versions/9/module-info.class")

  from(inline(configurations.runtimeClasspath))
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}
