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
@file:UseProjectorLoader

package org.jetbrains.projector.common.intellij

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import org.jetbrains.projector.util.loading.UseProjectorLoader

fun buildAtLeast(version: String) = ApplicationInfo.getInstance().build.buildAtLeast(version)

@Suppress("unused")
fun buildLowerThan(version: String) = ApplicationInfo.getInstance().build.buildLowerThan(version)

@Suppress("unused")
fun buildInRange(start: String, endExclusive: String) = ApplicationInfo.getInstance().build.buildInRange(start, endExclusive)

fun BuildNumber.buildAtLeast(version: String): Boolean {
  val versionToCheck = BuildNumber.fromString(version) ?: throw IllegalArgumentException("Invalid version string $version")

  return this >= versionToCheck
}

fun BuildNumber.buildLowerThan(version: String): Boolean {
  val versionToCheck = BuildNumber.fromString(version) ?: throw IllegalArgumentException("Invalid version string $version")

  return this < versionToCheck
}

fun BuildNumber.buildInRange(start: String, endExclusive: String): Boolean {
  val startVersion = BuildNumber.fromString(start) ?: throw IllegalArgumentException("Invalid version string $start")
  val endExclusiveVersion = BuildNumber.fromString(endExclusive) ?: throw IllegalArgumentException("Invalid version string $endExclusive")

  return startVersion <= this && this < endExclusiveVersion
}
