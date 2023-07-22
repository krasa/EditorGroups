// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl

import krasa.editorGroups.tabs2.KrTabsPosition
import krasa.editorGroups.tabs2.impl.themes.KrToolWindowTabTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

class KrToolWindowTabPainter : KrDefaultTabPainter(KrToolWindowTabTheme()) {
  override fun paintTab(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int, tabColor: Color?, active: Boolean, hovered: Boolean) {
    rect.y += borderThickness
    rect.height -= borderThickness

    if (position == KrTabsPosition.top) {
      rect.height -= borderThickness
    }

    super.paintTab(position, g, rect, borderThickness, tabColor, active, hovered)
  }

  override fun paintSelectedTab(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int, tabColor: Color?, active: Boolean, hovered: Boolean) {
    rect.y += borderThickness
    rect.height -= borderThickness

    super.paintSelectedTab(position, g, rect, borderThickness, tabColor, active, hovered)
  }

}