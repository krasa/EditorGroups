package krasa.editorGroups.tabs2.impl

import krasa.editorGroups.tabs2.KrTabPainter
import krasa.editorGroups.tabs2.impl.themes.KrTabTheme
import java.awt.Graphics


interface KrTabPainterAdapter {
  fun paintBackground(label: KrTabLabel, g: Graphics, tabs: KrTabsImpl)
  val tabPainter: KrTabPainter
  fun getTabTheme(): KrTabTheme = tabPainter.getTabTheme()
}
