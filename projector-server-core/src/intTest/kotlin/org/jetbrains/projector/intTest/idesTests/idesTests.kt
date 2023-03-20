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
package org.jetbrains.projector.intTest.idesTests

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selectors.byXpath
import com.codeborne.selenide.Selenide.*
import com.codeborne.selenide.WebDriverRunner.getWebDriver
import org.junit.jupiter.api.Test
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import kotlin.test.Ignore

class IdesTest
{
  private fun pressTab(times : Int, element: WebElement) {
    for (i in 1..times) {
      element.sendKeys(Keys.TAB)
      Thread.sleep(80)
    }
  }

  private fun accaptAgreementsAndSetSettings(ide : String) {
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
    elementBody.sendKeys(Keys.SPACE)      // Accapt Agreements
    if (ide != "pycharmcom2020.3.3") {
      if (!(ide == "clion2020.3.2" || ide == "datagrip2020.3.2" || ide == "rider")) {
        `$`(byXpath("/html/body/canvas[7]")).should(Condition.appear)
        `$`(byXpath("/html/body/canvas[7]")).shouldBe(Condition.visible)
        Thread.sleep(500)
        action.moveToElement(elementBody, -300, 640).click().build().perform()      // set default theme
        `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
        `$`(byXpath("/html/body/canvas[5]")).should(Condition.appear)
      }
      else if (ide == "rider")
      {
        `$`(byXpath("/html/body/canvas[8]")).should(Condition.appear)
        `$`(byXpath("/html/body/canvas[8]")).shouldBe(Condition.visible)

        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)
        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)
        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)
        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)
        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)
        action.moveToElement(elementBody, 270, 640).click().build().perform()
        Thread.sleep(200)

        `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
        `$`(byXpath("/html/body/canvas[5]")).should(Condition.appear)
      }
      else
      {
        `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
        `$`(byXpath("/html/body/canvas[5]")).should(Condition.appear)
      }

      if (ide != "pycharmcom2020.2") {
        Thread.sleep(100)
        action.moveToElement(elementBody, -250, 10).click().build().perform()   // click free trial
        Thread.sleep(80)
        pressTab(3, elementBody)
        if (ide != "datagrip2020.3.2")
          pressTab(1, elementBody)
        elementBody.sendKeys(Keys.SPACE)

        if (ide == "clion2020.2" || ide == "goland2020.2.2") {
          `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
          `$`(byXpath("/html/body/canvas[5]")).should(Condition.appear)
          Thread.sleep(600)
          pressTab(7, elementBody)
          elementBody.sendKeys(Keys.SPACE)      //accept settings and run ide
          Thread.sleep(80)
          `$`(byXpath("/html/body/canvas[2]")).shouldBe(Condition.visible)
        }
        if (ide == "rider")
        {
          pressTab(3, elementBody)
          elementBody.sendKeys(Keys.SPACE)      //accept settings and run ide
          Thread.sleep(80)
          `$`(byXpath("/html/body/canvas[2]")).shouldBe(Condition.visible)
        }
      }
    }
    Thread.sleep(300)
    close()
  }

  private fun accaptAgreementsAndSetSettingsForDataGrip(ide : String) {
    open("http://localhost:8887/projector/")

    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))
    if (ide == "datagrip2020.2") {
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
      elementBody.sendKeys(Keys.SPACE)      // Accapt Agreements
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.visible)
      action.moveToElement(`$`(byXpath("/html/body/canvas[5]")), 0, 30).click().build().perform()  // click free trial
      Thread.sleep(80)
      pressTab(3, elementBody)
      Thread.sleep(80)
      elementBody.sendKeys(Keys.SPACE)
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.appear)
      action.moveToElement(elementBody, -500, 670).click().build().perform()      // set default theme
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
    }

    if (ide == "datagrip2020.3.2") {
      Thread.sleep(1500)
      action.moveToElement(`$`(byXpath("/html/body/canvas[4]")), -530, 315).click().build().perform()      // set default theme
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
    }
    if (ide == "datagrip2019.3.4") {
      Thread.sleep(2000)
      action.moveToElement(`$`(byXpath("/html/body/canvas[5]")), -330, 315).click().build().perform()      // set default theme
      `$`(byXpath("/html/body/canvas[5]")).should(Condition.disappear)
    }

    `$`(byXpath("/html/body/canvas[8]")).waitUntil(Condition.appear, 10000)
    action.moveToElement(`$`(byXpath("/html/body/canvas[8]")), 280, 1).click().build().perform()
    Thread.sleep(200)
    action.moveToElement(`$`(byXpath("/html/body/canvas[4]")), -643, -315).click().build().perform()
    Thread.sleep(200)
    action.moveToElement(elementBody, -650, 40).click().build().perform()
    Thread.sleep(500)

    action.sendKeys("/home/projector-user/DemoProject/README.md").build().perform()
    Thread.sleep(400)
    elementBody.sendKeys(Keys.ENTER)
    Thread.sleep(1300)

    close()
  }

  private fun accaptAgreementsAndSetSettings1(ide : String) {
    open("http://localhost:8887/projector/")
    Thread.sleep(3000)

    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))
    action.moveToElement(elementBody, 0, 0).click().build().perform()
    action.sendKeys(Keys.TAB).build().perform()
    Thread.sleep(80)
    action.sendKeys(Keys.SPACE).build().perform()
    Thread.sleep(80)
    action.sendKeys(Keys.TAB).build().perform()
    Thread.sleep(80)
    action.sendKeys(Keys.SPACE).build().perform()
    Thread.sleep(500)
    action.sendKeys(Keys.TAB).build().perform()
    Thread.sleep(80)
    action.sendKeys(Keys.SPACE).build().perform()       // Accapt Agreements
    Thread.sleep(80)
    if (ide != "datagrip2019.3.4") {
      Thread.sleep(2000)
      action.moveToElement(elementBody, -300, 640).click().build().perform()      // set default theme
      Thread.sleep(1200)
    }
    val elementCanvas: WebElement = `$`(byXpath("/html/body/canvas[8]"))
    action.moveToElement(elementCanvas, 0, 30).click().build().perform()   // click free trial
    Thread.sleep(400)

    if (ide == "datagrip2019.3.4") {
      pressTab(3, elementBody)
      action.sendKeys(Keys.SPACE).build().perform()
      Thread.sleep(80)
    }
    pressTab(4, elementBody)
    action.sendKeys(Keys.SPACE).build().perform()
    Thread.sleep(80)
    pressTab(4, elementBody)
    Thread.sleep(80)
    action.sendKeys(Keys.SPACE).build().perform()
    Thread.sleep(500)
    if (ide != "datagrip2019.3.4") {
      Thread.sleep(3500)
      pressTab(7, elementBody)
      action.sendKeys(Keys.SPACE).build().perform()       //accept settings and run ide
      Thread.sleep(1000)
    }


    close()
  }

  private fun openReadmeForCLionLight(ide: String) {
    open("http://localhost:8887/projector/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))
    `$`(byXpath("/html/body/canvas[4]")).should(Condition.appear)
    `$`(byXpath("/html/body/canvas[4]")).should(Condition.visible)

    if (ide == "goland2020.2.2") {
      action.moveToElement(elementBody, -600, 10).click().build().perform()
      pressTab(2, elementBody)
      elementBody.sendKeys(Keys.SPACE)
    }
    else if (ide == "phpstorm2020.2") {
      action.moveToElement(elementBody, -100, 260).click().build().perform()
    }
    else if (ide == "pycharmcom2020.2") {
      action.moveToElement(elementBody, -60, 260).click().build().perform()
    }
    else {
      action.moveToElement(elementBody, -60, 290).click().build().perform()
    }
    `$`(byXpath("/html/body/canvas[8]")).should(Condition.appear)
    Thread.sleep(400)
    action.sendKeys("/home/projector-user/DemoProject/README.md").build().perform()
    Thread.sleep(400)
    elementBody.sendKeys(Keys.ENTER)
    `$`(byXpath("/html/body/iframe")).waitUntil(Condition.appear, 10000)
    Thread.sleep(100)
  }

  private fun openReadmeForCLion03(ide: String) {
    open("http://localhost:8887/projector/")

    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))
    `$`(byXpath("/html/body/canvas[3]")).should(Condition.appear)
    Thread.sleep(1000)
    action.sendKeys(Keys.TAB).build().perform()
    Thread.sleep(200)
    action.sendKeys(Keys.SPACE).build().perform()
    Thread.sleep(1000)
    action.sendKeys(Keys.BACK_SPACE).build().perform()
    Thread.sleep(1000)
    action.sendKeys("/home/projector-user/DemoProject/README.md").build().perform()
    Thread.sleep(400)
    elementBody.sendKeys(Keys.ENTER)
    `$`(byXpath("/html/body/iframe")).waitUntil(Condition.appear, 10000)
  }

  fun openReadmeForIdea() {
    open("http://localhost:8887/projector/")

    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("//body"))

    `$`(byXpath("/html/body/canvas[8]")).waitUntil(Condition.appear, 10000)
    action.moveToElement(`$`(byXpath("/html/body/canvas[8]")), 280, 1).click().build().perform()
    Thread.sleep(200)
    action.moveToElement(elementBody, -650, 240).doubleClick().build().perform()
    `$`(byXpath("/html/body/iframe")).waitUntil(Condition.appear, 10000)

    close()
  }

  @Test
  fun cLionTest1() //CLion 2020.2
  {
    //todo: automate opening the desired ide
    //Runtime.getRuntime().exec("src/openIde.sh").waitFor(100, TimeUnit.SECONDS)

    accaptAgreementsAndSetSettings("clion2020.2")
    openReadmeForCLionLight("clion2020.2")
  }

  // ide freezes
  @Ignore
  @Test
  fun cLionTest2()    //CLion 2019.3.5
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()

    accaptAgreementsAndSetSettings1("clion2019.3.5")
    openReadmeForCLionLight("clion2019.3.5")
  }

  @Test
  fun cLionTest3()    //CLion 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun goLand2()       // GoLand 2020.2.2
  {
    accaptAgreementsAndSetSettings("goland2020.2.2")
    openReadmeForCLionLight("goland2020.2.2")
  }

  // ide freezes
  @Ignore
  @Test
  fun goLand19()      // GoLand 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()
    accaptAgreementsAndSetSettings1("goland2019.3.4")
    //openReadmeForCLion03("goland2019.3.4")   //todo: it doesn't work, if the problem with ide freezing will be solved, then you will need to write a function that opens readme.md
  }

  @Test
  fun goLand3()       //Goland 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("goland2020.3.2")
  }

  @Test
  fun dataGrip2()     //DataGrip 2020.2
  {
    accaptAgreementsAndSetSettingsForDataGrip("datagrip2020.2")
  }

  @Test
  fun dataGrip19()        //DataGrip 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()
    accaptAgreementsAndSetSettings1("datagrip2019.3.4")
    accaptAgreementsAndSetSettingsForDataGrip("datagrip2019.3.4")
  }

  @Test
  fun dataGrip3()         //DataGrip 2020.3.2
  {
    accaptAgreementsAndSetSettings("datagrip2020.3.2")
    accaptAgreementsAndSetSettingsForDataGrip("datagrip2020.3.2")
  }

  //ide freezes
  @Ignore
  @Test
  fun phpStorm19()        //PhpStorm 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()

    accaptAgreementsAndSetSettings1("clion2019.3.5")
    openReadmeForCLionLight("phpstorm2020.2")
  }

  @Test
  fun phpStorm2()     //PhpStorm 2020.2
  {
    accaptAgreementsAndSetSettings("clion2020.2")
    openReadmeForCLionLight("phpstorm2020.2")
  }

  @Test
  fun phpStorm3()     //PhpStorm 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  //ide freezes
  @Ignore
  @Test
  fun pyCharmCommunity19()      //PyCharm Community 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()
    //accaptAgreementsAndSetSettings1("clion2019.3.5") // todo: it doesn't work, if the problem with ide freezing will be solved, then you will need to write a function that opens readme.md
    //openReadmeForCLionLight("phpstorm2020.2")
  }

  @Test
  fun pyCharmCommunity2()     //PyCharm Community 2020.2
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.2")
    openReadmeForCLionLight("pycharmcom2020.2")
  }

  @Test
  fun pyCharmCommunity3()     //PyCharm Community 2020.3.3
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.3.3")
    openReadmeForCLion03("clion2020.3.2")
  }

  //ide freezes
  @Ignore
  @Test
  fun pyCharmProfessional19()      //PyCharm Professional 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()

    accaptAgreementsAndSetSettings1("clion2019.3.5")
    openReadmeForCLionLight("pycharmcom2020.2")
  }

  @Test
  fun pyCharmProfessional2()     //PyCharm Professional 2020.2
  {
    accaptAgreementsAndSetSettings("clion2020.2")
    openReadmeForCLionLight("pycharmcom2020.2")
  }

  @Test
  fun pyCharmProfessional3()     //PyCharm Professional 2020.3.3
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun pyCharmProfessional5()     //PyCharm Professional 2020.3.3
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun rubyMine()      //RubyMine 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun webStorm()      //WebStorm 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun webStorm3()   //WebStorm 2020.3.3
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun rider()     //Rider 2020.3.3
  {
    accaptAgreementsAndSetSettings("rider")
    openReadmeForCLion03("clion2020.3.2")
  }

  // failing to download from https://download.jetbrains.com/python/jetbrains-data-spell-212.443.21.tar.gz
  @Ignore
  @Test
  fun dataSpell()   //DataSpell EAP 2021.1
  {

  }

  //no readme.md file
  @Ignore
  @Test
  fun mps()       //MPS 2020.3
  {
    accaptAgreementsAndSetSettings("clion2020.3.2") //todo change this to close "import" menu
    openReadmeForCLion03("clion2020.3.2")
  }

  //ide freezes
  @Ignore
  @Test
  fun ideaCommunity19()       //IntelliJ IDEA Community 2019.3.4
  {
    //accaptAgreementsAndSetSettings("pycharmcom2020.3.3")// todo write "accaptAgreementsAndSetSettings" fun
    Thread.sleep(4000)
    openReadmeForIdea()
  }

  @Test
  fun ideaCommunity1()        //IntelliJ IDEA Community 2020.1.1
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.3.3")
    Thread.sleep(4000)
    openReadmeForIdea()
  }

  @Test
  fun ideaCommunity2()        //IntelliJ IDEA Community 2020.2
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.3.3")
    Thread.sleep(4000)
    openReadmeForIdea()
  }

  @Test
  fun ideaCommunity3()        //IntelliJ IDEA Community 2020.3.2
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.3.3")
    Thread.sleep(4000)
    openReadmeForIdea()
  }

  @Test
  fun ideaCommunity4()        //IntelliJ IDEA Community 2020.3.4
  {
    accaptAgreementsAndSetSettings("pycharmcom2020.3.3")
    Thread.sleep(4000)
    openReadmeForIdea()
  }

  //ide freezes
  @Ignore
  @Test
  fun ideaUltimate19()       //IntelliJ IDEA Ultimate 2019.3.4
  {
    open("http://localhost:8887/")
    val action = Actions(getWebDriver())
    val elementBody: WebElement = `$`(byXpath("/html/body/canvas[8]"))

    action.moveToElement(elementBody, 205, 10).click().build().perform() //close import
    close()

    accaptAgreementsAndSetSettings1("clion2019.3.5")
    openReadmeForCLionLight("clion2019.3.5")
  }

  @Test
  fun ideaUltimate2()        //IntelliJ IDEA Ultimate 2020.2
  {
    accaptAgreementsAndSetSettings("clion2020.2")
    openReadmeForCLionLight("pycharmcom2020.2")
  }

  @Test
  fun ideaUltimate3()        //IntelliJ IDEA Ultimate 2020.3.2
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }

  @Test
  fun ideaUltimate4()        //IntelliJ IDEA Ultimate 2020.3.4
  {
    accaptAgreementsAndSetSettings("clion2020.3.2")
    openReadmeForCLion03("clion2020.3.2")
  }
}
