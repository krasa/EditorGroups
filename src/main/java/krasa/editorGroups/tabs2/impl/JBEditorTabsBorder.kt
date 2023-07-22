package krasa.editorGroups.tabs2.impl

import com.intellij.util.ui.JBUI
import krasa.editorGroups.tabs2.JBTabsBorder
import krasa.editorGroups.tabs2.JBTabsPosition
import java.awt.*

class JBEditorTabsBorder(tabs: JBTabsImpl) : JBTabsBorder(tabs) {

  override val effectiveBorder: Insets
    get() = JBUI.insetsTop(thickness)

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    g as Graphics2D

    tabs.tabPainter.paintBorderLine(g, thickness, Point(x, y), Point(x + width, y))
    if (tabs.isEmptyVisible) return

    val firstLabel = tabs.myInfo2Label.get(tabs.lastLayoutPass.getTabAt(0, 0)) ?: return

    val startY = firstLabel.y - if (tabs.position == JBTabsPosition.bottom) 0 else thickness


    when (tabs.position) {
      JBTabsPosition.top -> {
        for (eachRow in 1..tabs.lastLayoutPass.rowCount) {
          val yl = (eachRow * tabs.myHeaderFitSize.height) + startY
          tabs.tabPainter.paintBorderLine(g, thickness, Point(x, yl), Point(x + width, yl))
        }
      }

      JBTabsPosition.bottom -> {
        tabs.tabPainter.paintBorderLine(g, thickness, Point(x, startY), Point(x + width, startY))
        tabs.tabPainter.paintBorderLine(g, thickness, Point(x, y), Point(x + width, y))
      }

      JBTabsPosition.right -> {
        val lx = firstLabel.x
        tabs.tabPainter.paintBorderLine(g, thickness, Point(lx, y), Point(lx, y + height))
      }

      else -> {
        val bounds = firstLabel.bounds
        val i = bounds.x + bounds.width - thickness
        tabs.tabPainter.paintBorderLine(g, thickness, Point(i, y), Point(i, y + height))
      }
    }
  }
}