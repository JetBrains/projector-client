/*
 * MIT License
 *
 * Copyright (c) 2019-2024 JetBrains s.r.o.
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
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

public fun Project.publishToSpace(fromComponent: String) {
  configure<PublishingExtension> {
    if (fromComponent == "java") {
      publications {
        create<MavenPublication>("maven") {
          pom {
            url.set("https://github.com/JetBrains/projector-client")
            licenses {
              license {
                name.set("MIT Licence")
                url.set("https://github.com/JetBrains/projector-client/blob/master/LICENSE.txt")
              }
            }
          }
          from(project.components[fromComponent])
        }
      }
    }
    repositories {
      maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        credentials(PasswordCredentials::class)
      }
    }
  }
}
