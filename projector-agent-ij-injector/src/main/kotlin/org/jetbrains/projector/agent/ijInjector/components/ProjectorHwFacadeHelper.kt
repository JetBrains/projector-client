/*
 * Copyright (c) 2019-2021, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
package org.jetbrains.projector.agent.ijInjector.components

import com.intellij.jdkEx.JdkEx
import com.intellij.ui.jcef.HwFacadeHelper
import org.jetbrains.projector.util.logging.Logger
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.VolatileImage
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Suppress("UnstableApiUsage")
public class ProjectorHwFacadeHelper(private val myTarget: JComponent, ) : HwFacadeHelper(myTarget) {

  private var myHwFacade: BalloonWindow? = null

  private var myOwnerListener: ComponentAdapter? = null
  private var myTargetListener: ComponentAdapter? = null
  private var myBackBuffer: VolatileImage? = null

  //init {
  //  logger.debug { "${hashCode()} init" }
  //}

  override fun addNotify() {
    if (myTarget.isVisible) {
      logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} addNotify" }
      onShowing()
    }
  }

  override fun show() {
    //if (AWTAccessor.getComponentAccessor().getPeer<ComponentPeer?>(myTarget) != null) {
    //  myHwFacade?.apply { isVisible = true } ?: onShowing()
    //}
    myHwFacade?.apply { isVisible = true } ?: onShowing()
  }

  override fun removeNotify() {
    if (isActive()) {
      logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} removeNotify" }
      myHwFacade!!.dispose()
      myHwFacade = null
      myBackBuffer = null
      myTarget.removeComponentListener(myTargetListener)
      val owner = SwingUtilities.getWindowAncestor(myTarget)
      owner?.removeComponentListener(myOwnerListener)
    }
  }

  override fun hide() {
    if (isActive()) {
      logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} Hide" }
      myHwFacade!!.isVisible = false
    }
  }

  override fun paint(g: Graphics, targetPaint: Consumer<in Graphics>) {


    val size = myTarget.size
    if (myBackBuffer == null || myBackBuffer!!.width != size.width || myBackBuffer!!.height != size.height) {
      logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} New BackBuffer" }
      myBackBuffer = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.createCompatibleVolatileImage(
        size.width, size.height, Transparency.TRANSLUCENT)
    }
    val bbGraphics = myBackBuffer!!.graphics as Graphics2D
    bbGraphics.background = TRANSPARENT_COLOR
    bbGraphics.clearRect(0, 0, size.width, size.height)
    targetPaint.accept(bbGraphics)
    myHwFacade?.apply{
      logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} repaint" }
      repaint()
    }

    //targetPaint.accept(g)
    //
    //if (isActive()) {
    //  val size = myTarget.size
    //  if (myBackBuffer == null || myBackBuffer!!.width != size.width || myBackBuffer!!.height != size.height) {
    //    myBackBuffer = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.createCompatibleVolatileImage(
    //      size.width, size.height, Transparency.TRANSLUCENT)
    //  }
    //  val bbGraphics = myBackBuffer!!.graphics as Graphics2D
    //  bbGraphics.background = TRANSPARENT_COLOR
    //  bbGraphics.clearRect(0, 0, size.width, size.height)
    //  targetPaint.accept(bbGraphics)
    //  myHwFacade!!.repaint()
    //}
    //else {
    //  targetPaint.accept(g)
    //}
  }

  private fun isActive(): Boolean {
    return myHwFacade != null
  }

  private fun onShowing() {
    assert(myHwFacade == null)
    assert(myTarget.isVisible)

    myTarget.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        myHwFacade?.apply {
          logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} Resized" }
          size = myTarget.size
        } ?: activate()
      }

      override fun componentMoved(e: ComponentEvent) {
        myHwFacade?.apply {
          if (isVisible) {
            logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} Moved" }
            location = myTarget.location
          }
        } ?: activate()
      }
    }.also { myTargetListener = it })
    activate()
  }

  private fun activate() {
    if (!myTarget.isShowing) return

    logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} Activating" }

    val owner = SwingUtilities.getWindowAncestor(myTarget)
    owner.addComponentListener(object : ComponentAdapter() {
      override fun componentMoved(e: ComponentEvent) {
        if (myTarget.isVisible) {
          logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} componentMoved" }
          myHwFacade!!.location = myTarget.locationOnScreen
        }
      }
    }.also { myOwnerListener = it })

    val facade = BalloonWindow(owner)
    myHwFacade = facade


    val paintedPanel = object : JPanel() {
      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (myBackBuffer != null) {

          if (this@ProjectorHwFacadeHelper !in painted) {
            logger.debug(Exception()) { "${this@ProjectorHwFacadeHelper.hashCode()} paintComponent" }
            painted += this@ProjectorHwFacadeHelper
          }

          g.drawImage(myBackBuffer, 0, 0, null)
        } else {
          logger.debug { "${this@ProjectorHwFacadeHelper.hashCode()} Cannot paint" }
        }
      }

      init {
        background = TRANSPARENT_COLOR
      }
    }

    facade.add(paintedPanel)
    JdkEx.setIgnoreMouseEvents(facade, true)
    facade.bounds = Rectangle(myTarget.locationOnScreen, myTarget.size)
    facade.focusableWindowState = false
    facade.background = TRANSPARENT_COLOR
    facade.isVisible = true

    paintedPanel.repaint()
  }

  override fun hashCode(): Int {
    return System.identityHashCode(this)
  }

  private companion object {
    val logger = Logger<ProjectorHwFacadeHelper>()

    val painted = mutableSetOf<ProjectorHwFacadeHelper>()
  }
}
