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
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDceDsl

plugins {
  kotlin("js")
}

val coroutinesVersion: String by project
val inlineStylePrefixerVersion: String by project
val kotlinReactVersion: String by project
val kotlinStyledComponentsVersion: String by project
val radiumVersion: String by project
val reactVersion: String by project
val reactLoadingIndicatorVersion: String by project
val serializationVersion: String by project
val styledComponentsVersion: String by project

dependencies {
  implementation(project(":projector-common"))
  implementation(project(":projector-client-common"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

  implementation("org.jetbrains:kotlin-react:$kotlinReactVersion")
  implementation("org.jetbrains:kotlin-react-dom:$kotlinReactVersion")
  implementation(npm("react", reactVersion))
  implementation(npm("react-dom", reactVersion))
  implementation("org.jetbrains:kotlin-styled:$kotlinStyledComponentsVersion")
  implementation(npm("styled-components", styledComponentsVersion))
  implementation(npm("inline-style-prefixer", inlineStylePrefixerVersion))
  implementation(npm("radium", radiumVersion))
  implementation(npm("react-loading-indicator", reactLoadingIndicatorVersion))
}

kotlin {
  js {
    browser {
      @OptIn(ExperimentalDceDsl::class)
      dceTask {
        keep.add("projector-client-projector-client-web.org.jetbrains.projector.client.web.onLoad")
      }
    }
  }
}
