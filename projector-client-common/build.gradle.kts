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
plugins {
  kotlin("multiplatform")
  `maven-publish`
}

val kotlinVersion: String by project
val coroutinesVersion: String by project

kotlin {
  js {
    browser()
  }

  jvm {
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(kotlin("reflect", kotlinVersion))
        implementation(project(":projector-common"))
        implementation(project(":projector-util-logging"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
      }
    }

    val jsMain by getting {
    }

    val jvmMain by getting {
    }

    val commonTest by getting {
      dependencies {
        api(kotlin("test-common", kotlinVersion))
        api(kotlin("test-annotations-common", kotlinVersion))
      }
    }

    val jsTest by getting {
      dependencies {
        api(kotlin("test-js", kotlinVersion))
      }
    }

    val jvmTest by getting {
      dependencies {
        api(kotlin("test", kotlinVersion))
        api(kotlin("test-junit", kotlinVersion))
      }
    }
  }
}
