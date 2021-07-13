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
package org.jetbrains.projector.agent.init

public object IjArgs {
  public const val IS_AGENT: String = "isAgent"
  public const val IJ_CL_PROVIDER_CLASS: String = "ijClProviderClass"
  public const val IJ_CL_PROVIDER_METHOD: String = "ijClProviderMethod"
  public const val MD_PANEL_MAKER_CLASS: String = "mdPanelMakerClass"
  public const val MD_PANEL_MAKER_METHOD: String = "mdPanelMakerMethod"
}

public fun Map<String, Any>.toIjArgs(): String {
  return entries.joinToString(separator = ";") { "${it.key}=${it.value}" }
}

public fun String.toArgsMap(): Map<String, String> {

  return split(";").associate {
    val (key, value) = it.split("=")
    key to value
  }
}
