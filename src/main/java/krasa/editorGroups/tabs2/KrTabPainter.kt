package krasa.editorGroups.tabs2

import krasa.editorGroups.tabs2.impl.KrDefaultTabPainter
import krasa.editorGroups.tabs2.impl.KrEditorTabPainter
import krasa.editorGroups.tabs2.impl.KrToolWindowTabPainter
import krasa.editorGroups.tabs2.impl.themes.KrDebuggerTabTheme
import krasa.editorGroups.tabs2.impl.themes.KrTabTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

interface KrTabPainter {
  companion object {
    @JvmStatic
    val DEFAULT: KrTabPainter = KrDefaultTabPainter()

    @JvmStatic
    val EDITOR: KrEditorTabPainter = KrEditorTabPainter()

    @JvmStatic
    val TOOL_WINDOW: KrTabPainter = KrToolWindowTabPainter()

    @JvmStatic
    val DEBUGGER: KrTabPainter = KrDefaultTabPainter(KrDebuggerTabTheme())
  }

  fun getTabTheme(): KrTabTheme

  fun getBackgroundColor(): Color

  /** Color that should be painted on top of [KrTabTheme.background] */
  fun getCustomBackground(tabColor: Color?, selected: Boolean, active: Boolean, hovered: Boolean): Color? {
    return tabColor
  }

  fun paintBorderLine(g: Graphics2D, thickness: Int, from: Point, to: Point)

  fun fillBackground(g: Graphics2D, rect: Rectangle)

  fun paintTab(position: KrTabsPosition,
               g: Graphics2D,
               rect: Rectangle,
               borderThickness: Int,
               tabColor: Color?,
               active: Boolean,
               hovered: Boolean)

  fun paintSelectedTab(position: KrTabsPosition,
                       g: Graphics2D,
                       rect: Rectangle,
                       borderThickness: Int,
                       tabColor: Color?,
                       active: Boolean,
                       hovered: Boolean)

  fun paintUnderline(position: KrTabsPosition,
                     rect: Rectangle,
                     borderThickness: Int,
                     g: Graphics2D,
                     active: Boolean)
}