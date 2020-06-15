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
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  `maven-publish`
}

val kotlinVersion: String by project
val serializationVersion: String by project

kotlin {
  js {
    browser()
  }
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(kotlin("stdlib-common", kotlinVersion))
        api(kotlin("reflect", kotlinVersion))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-common:$serializationVersion")
      }
    }

    val commonTest by getting {
      dependencies {
        api(kotlin("test-common", kotlinVersion))
        api(kotlin("test-annotations-common", kotlinVersion))
      }
    }

    val jsMain by getting {
      dependencies {
        api(kotlin("stdlib-js", kotlinVersion))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-js:$serializationVersion")
      }
    }

    val jsTest by getting {
      dependencies {
        api(kotlin("test-js", kotlinVersion))
      }
    }

    val jvmMain by getting {
      dependencies {
        api(kotlin("stdlib-jdk8", kotlinVersion))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
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
