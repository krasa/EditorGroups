package krasa.editorGroups.tabs2

import com.intellij.util.ui.JBUI
import krasa.editorGroups.tabs2.impl.JBTabsImpl
import java.awt.Component
import java.awt.Insets
import javax.swing.border.Border

abstract class JBTabsBorder(val tabs: JBTabsImpl) : Border {
  var thickness: Int = JBUI.scale(1)

  override fun getBorderInsets(c: Component?): Insets = JBUI.emptyInsets()

  override fun isBorderOpaque(): Boolean {
    return true
  }

  open val effectiveBorder: Insets
    get() = JBUI.emptyInsets()

}