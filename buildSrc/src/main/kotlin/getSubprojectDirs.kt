import org.gradle.api.Project

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

public fun Project.getSubprojectDirs(dirType: String): String {
  val srcMainDirs: MutableSet<String> = HashSet()
  var matchResult: MatchResult?
  projectDir.walk()
    .filter { it.isDirectory }
    .forEach {
      when (dirType) {
        "sources" -> {
          matchResult = "src/(common|js|jvm)?[mM]ain".toRegex().find(it.absolutePath)
          matchResult?.value?.split("/")?.last()?.let { dir -> srcMainDirs.add("src/$dir") }
        }
        "test" -> {
          matchResult = "src/(common|int|jvm)?[tT]est".toRegex().find(it.absolutePath)
          matchResult?.value?.split("/")?.last()?.let { dir -> srcMainDirs.add("src/$dir") }
        }
      }
    }

  return srcMainDirs.joinToString(",")
}
