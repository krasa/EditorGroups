package krasa.editorGroups.tabs2.impl

import krasa.editorGroups.tabs2.KrTabsBorder
import java.awt.*

open class KrDefaultTabsBorder(tabs: KrTabsImpl) : KrTabsBorder(tabs) {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    if (tabs.isEmptyVisible) return
    g as Graphics2D

    val rect = Rectangle(x, y, width, height)
    val firstLabel = tabs.infoToLabel[tabs.getVisibleInfos()[0]] ?: return
    val maxY = firstLabel.bounds.maxY.toInt() - thickness
    tabs.tabPainter.paintBorderLine(g, thickness, Point(rect.x, maxY), Point(rect.maxX.toInt(), maxY))
  }
}