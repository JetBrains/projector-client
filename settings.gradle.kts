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
pluginManagement {
  val gradleMkdocsPluginVersion: String by settings
  val kotlinVersion: String by settings
  val symZipVersion: String by settings

  buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
      classpath("org.paleozogt:symzip-plugin:$symZipVersion")
    }
  }

  plugins {
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.paleozogt.symzip") version symZipVersion apply false
    id("ru.vyarus.mkdocs") version gradleMkdocsPluginVersion apply false
  }
}

rootProject.name = "projector-client"

include("docSrc")
include("projector-agent-common")
include("projector-agent-ij-injector")
include("projector-common")
include("projector-client-common")
include("projector-client-web")
include("projector-ij-common")
include("projector-launcher")
include("projector-server-core")
include("projector-util-agent")
include("projector-util-loading")
include("projector-util-logging")
