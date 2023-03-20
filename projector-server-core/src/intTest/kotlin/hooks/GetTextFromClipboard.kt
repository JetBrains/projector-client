package hooks

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

object GetTextFromClipboard {
  fun get(): String {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val contents = clipboard.getContents(null)
    val hasStringText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)
    if (hasStringText) {
      try {
        return contents!!.getTransferData(DataFlavor.stringFlavor) as String
      } catch (ex: UnsupportedFlavorException) {
        println(ex)
        ex.printStackTrace()
      } catch (ex: IOException) {
        println(ex)
        ex.printStackTrace()
      }
    }
    return ""
  }
}
