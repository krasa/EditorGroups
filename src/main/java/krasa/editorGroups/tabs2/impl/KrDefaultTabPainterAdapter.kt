package krasa.editorGroups.tabs2.impl

import krasa.editorGroups.tabs2.KrTabPainter
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle

class KrDefaultTabPainterAdapter(private val painter: KrTabPainter) : KrTabPainterAdapter {
  override val tabPainter: KrTabPainter
    get() = painter

  override fun paintBackground(label: KrTabLabel, g: Graphics, tabs: KrTabsImpl) {
    val info = label.info
    val isSelected = info == tabs.selectedInfo

    val rect = Rectangle(0, 0, label.width, label.height)

    val g2d = g as Graphics2D
    if (isSelected && tabs.getVisibleInfos().size > 1) {
      painter
        .paintSelectedTab(tabs.position, g2d, rect, tabs.borderThickness, info.tabColor, tabs.isActiveTabs(info),
          tabs.isHoveredTab(label))
    } else {
      painter.paintTab(tabs.position, g2d, rect, tabs.borderThickness, info.tabColor, tabs.isActiveTabs(info), tabs.isHoveredTab(label) && tabs.getVisibleInfos().size > 1)
    }
  }
}