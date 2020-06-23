package com.intellij.projector.client.common.misc

import kotlin.browser.window

actual object TimeStamp {

  actual val current: Double
    get() = window.performance.now()
}