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
package org.jetbrains.projector.intTest.headless

import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.confirm
import com.codeborne.selenide.WebDriverRunner
import org.jetbrains.projector.intTest.ConnectionUtil
import org.openqa.selenium.JavascriptExecutor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotSecureAlertTest {

  private companion object {

    private fun openClientAndActivatePage(clientUrl: String) {
      Selenide.open(clientUrl)

      // don't click because it will disable the initial pop up:
      //Selenide.element("body").click(5, 5)  // enable onbeforeunload listener, can't click without arguments because of an exception
    }

    private const val port = 8888
    private const val host = "0.0.0.0"  // 0.0.0.0 is considered as insecure
  }

  @Test
  fun shouldWarnOnNotSecureConnection() {
    val server = ConnectionUtil.hostFiles(port, host)
    server.start()
    openClientAndActivatePage("http://$host:$port/index.html")

    assertTrue(isAlertPresent())
    confirm()
    assertFalse(
      (WebDriverRunner.getWebDriver() as JavascriptExecutor).executeScript("return window.isSecureContext;") as Boolean,
      "The context should be insecure in this test",
    )
    server.stop(500, 1000)
  }

  @Test
  fun shouldNotWarnOnSecureConnection() {
    openClientAndActivatePage(
      ConnectionUtil.clientUrl)  // local files are meant as secure: https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts#When_is_a_context_considered_secure

    assertFalse(isAlertPresent())
    assertTrue(
      (WebDriverRunner.getWebDriver() as JavascriptExecutor).executeScript("return window.isSecureContext;") as Boolean,
      "The context should be secure in this test",
    )
  }
}
