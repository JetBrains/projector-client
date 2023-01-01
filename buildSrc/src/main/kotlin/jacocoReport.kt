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

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

private fun JacocoReport.setup(project: Project, isKotlinMpModule: Boolean) {
  if (isKotlinMpModule) {
    // todo: use an official way to setup Kotlin/MP projects
    dependsOn("jvmTest")
    val coverageSourceDirs = arrayOf(
      "commonMain/src",
      "jvmMain/src"
    )
    val classFiles = File("${project.buildDir}/classes/kotlin/jvm/")
      .walkBottomUp()
      .toSet()

    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(project.files(coverageSourceDirs))
    additionalSourceDirs.setFrom(project.files(coverageSourceDirs))
    executionData.setFrom(project.files("${project.buildDir}/jacoco/jvmTest.exec"))
  }

  val moduleName = project.name.split("-").joinToString("") { it.capitalize() }

  // fix jacoco error "Can't add different class with same name" due to
  // projector-server-core/build/resources/main/projector-agent/projector-agent-ij-injector.jar
  classDirectories.setFrom(classDirectories.files.filter { !it.endsWith("projector-server-core/build/resources/main") })

  reports {
    xml.required.set(true)
    xml.outputLocation.set(project.rootProject.file("JacocoReports/jacocoReport$moduleName.xml"))
    csv.required.set(false)
    html.outputLocation.set(project.layout.buildDirectory.dir("jacocoHtml$moduleName"))
  }
}

public fun Project.setupJacoco(isKotlinMpModule: Boolean = false) {
  val jacocoVersion: String by this

  configure<JacocoPluginExtension> {
    toolVersion = jacocoVersion
  }

  tasks.withType<JacocoReport> {
    setup(project, isKotlinMpModule)
  }

  if (!isKotlinMpModule) {
    tasks.named<Test>("test") {
      useJUnitPlatform()
      finalizedBy(tasks.named<JacocoReport>("jacocoTestReport"))
    }
  }
}
