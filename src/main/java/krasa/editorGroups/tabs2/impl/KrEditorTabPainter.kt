@file:Suppress("UnstableApiUsage")

package krasa.editorGroups.tabs2.impl

import com.intellij.ui.ExperimentalUI
import krasa.editorGroups.tabs2.KrTabsPosition
import krasa.editorGroups.tabs2.impl.themes.KrEditorTabTheme
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

class KrEditorTabPainter : KrDefaultTabPainter(KrEditorTabTheme()) {
  fun paintLeftGap(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int) {
    val maxY = rect.y + rect.height - borderThickness

    paintBorderLine(g, borderThickness, Point(rect.x, rect.y), Point(rect.x, maxY))
  }

  fun paintRightGap(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int) {
    val maxX = rect.x + rect.width - borderThickness
    val maxY = rect.y + rect.height - borderThickness

    paintBorderLine(g, borderThickness, Point(maxX, rect.y), Point(maxX, maxY))
  }

  fun paintTopGap(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int) {
    val maxX = rect.x + rect.width

    paintBorderLine(g, borderThickness, Point(rect.x, rect.y), Point(maxX, rect.y))
  }

  fun paintBottomGap(position: KrTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int) {
    val maxX = rect.x + rect.width - borderThickness
    val maxY = rect.y + rect.height - borderThickness

    paintBorderLine(g, borderThickness, Point(rect.x, maxY), Point(maxX, maxY))
  }

  override fun underlineRectangle(position: KrTabsPosition, rect: Rectangle, thickness: Int): Rectangle {
    return when (position) {
      KrTabsPosition.bottom -> Rectangle(rect.x, rect.y, rect.width, thickness)
      KrTabsPosition.left -> {
        if (ExperimentalUI.isNewUI()) {
          Rectangle(rect.x, rect.y, thickness, rect.height)
        } else Rectangle(rect.x + rect.width - thickness, rect.y, thickness, rect.height)
      }

      KrTabsPosition.right -> Rectangle(rect.x, rect.y, thickness, rect.height)
      else -> super.underlineRectangle(position, rect, thickness)
    }
  }
}