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
package org.jetbrains.projector.intTest

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selectors.byXpath
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner.getWebDriver
import hooks.GetTextFromClipboard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

class ClipboardTest {

  private val ans = """object KotlinObject {

    @JvmStatic
    fun main(vararg args: String) {
        println("projector: Hello, Kotlin!")
    }
}
"""

  @Test
  fun checkClipboard() {
    //todo before test build and run IntelliJ IDEA Community 2020.3.4
    open("http://localhost:8887/projector/")

    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))
    `$`(byXpath("/html/body/canvas[2]")).should(Condition.appear)
    action.moveToElement(elementBody, 0, 0).click().build().perform()
    elementBody.sendKeys(Keys.TAB)
    Thread.sleep(80)
    elementBody.sendKeys(Keys.SPACE)
    Thread.sleep(80)
    elementBody.sendKeys(Keys.SPACE)
    Thread.sleep(200)
    elementBody.sendKeys(Keys.TAB)
    Thread.sleep(80)
    elementBody.sendKeys(Keys.SPACE)
    Thread.sleep(200)
    `$`(byXpath("/html/body/canvas[8]")).waitUntil(Condition.appear, 20000)
    action.moveToElement(`$`(byXpath("/html/body/canvas[8]")), 280, 1).click().build().perform()
    Thread.sleep(200)
    action.keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND).build().perform()
    Thread.sleep(200)
    action.keyDown(Keys.COMMAND).sendKeys("c").keyUp(Keys.COMMAND).build().perform()
    Thread.sleep(400)
    closeWebDriver()
    assertEquals(GetTextFromClipboard.get(), ans)
  }
}
