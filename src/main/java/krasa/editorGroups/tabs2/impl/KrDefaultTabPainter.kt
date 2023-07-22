package krasa.editorGroups.tabs2.impl

import com.intellij.openapi.rd.fill2DRect
import com.intellij.openapi.rd.fill2DRoundRect
import com.intellij.openapi.rd.paint2DLine
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ColorUtil
import com.intellij.ui.paint.LinePainter2D
import krasa.editorGroups.tabs2.KrTabPainter
import krasa.editorGroups.tabs2.KrTabsPosition
import krasa.editorGroups.tabs2.impl.themes.KrDefaultTabTheme
import krasa.editorGroups.tabs2.impl.themes.KrTabTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

open class KrDefaultTabPainter(private val theme: KrTabTheme = KrDefaultTabTheme()) : KrTabPainter {

  override fun getTabTheme(): KrTabTheme = theme

  override fun getBackgroundColor(): Color = theme.background ?: theme.borderColor

  override fun getCustomBackground(tabColor: Color?, selected: Boolean, active: Boolean, hovered: Boolean): Color? {
    var bg: Color? = null
    if (!selected) {
      if (tabColor != null) {
        bg = theme.inactiveColoredTabBackground?.let { inactive ->
          ColorUtil.alphaBlending(inactive, tabColor)
        } ?: tabColor
      }

      if (hovered) {
        (if (active) theme.hoverBackground else theme.hoverInactiveBackground)?.let { hover ->
          bg = bg?.let { ColorUtil.alphaBlending(hover, it) } ?: hover
        }
      }
    } else {
      bg = (tabColor ?: if (active) theme.underlinedTabBackground else theme.underlinedTabInactiveBackground)
        ?: theme.background
      if (hovered) {
        (if (active) theme.hoverSelectedBackground else theme.hoverSelectedInactiveBackground).let { hover ->
          bg = bg?.let { ColorUtil.alphaBlending(hover, it) } ?: hover
        }
      }
    }
    return bg
  }

  override fun fillBackground(g: Graphics2D, rect: Rectangle) {
    theme.background?.let {
      g.fill2DRect(rect, it)
    }
  }

  override fun paintTab(position: KrTabsPosition,
                        g: Graphics2D,
                        rect: Rectangle,
                        borderThickness: Int,
                        tabColor: Color?,
                        active: Boolean,
                        hovered: Boolean) {
    getCustomBackground(tabColor, selected = false, active, hovered)?.let {
      g.fill2DRect(rect, it)
    }

    paintLeftRightBorders(g, borderThickness, rect, position)
  }

  override fun paintSelectedTab(position: KrTabsPosition,
                                g: Graphics2D,
                                rect: Rectangle,
                                borderThickness: Int,
                                tabColor: Color?,
                                active: Boolean,
                                hovered: Boolean) {
    getCustomBackground(tabColor, selected = true, active, hovered)?.let {
      g.fill2DRect(rect, it)
    }

    //this code smells. Remove when animation is default for all tabs
    if (!KrEditorTabsBorder.hasAnimation() || this !is KrEditorTabPainter) {
      paintUnderline(position, rect, borderThickness, g, active)
    }
    paintLeftRightBorders(g, borderThickness, rect, position)
  }

  private fun paintLeftRightBorders(g: Graphics2D, borderThickness: Int, rect: Rectangle, position: KrTabsPosition) {
    if (!position.isSide && Registry.`is`("ide.new.editor.tabs.vertical.borders")) {
      paintBorderLine(g,
        borderThickness,
        Point(rect.x, rect.y),
        Point(rect.x + rect.width, rect.y))
      paintBorderLine(g,
        borderThickness,
        Point(rect.x, rect.y + rect.height - 1),
        Point(rect.x + rect.width, rect.y + rect.height - 1))
      return
    }
  }

  override fun paintUnderline(position: KrTabsPosition,
                              rect: Rectangle,
                              borderThickness: Int,
                              g: Graphics2D,
                              active: Boolean) {
    val underline = underlineRectangle(position, rect, theme.underlineHeight)
    val arc = theme.underlineArc
    val color = if (active) theme.underlineColor else theme.inactiveUnderlineColor
    if (arc > 0) {
      g.fill2DRoundRect(underline, arc.toDouble(), color)
    } else {
      g.fill2DRect(underline, color)
    }
  }

  override fun paintBorderLine(g: Graphics2D, thickness: Int, from: Point, to: Point) {
    g.paint2DLine(from, to, LinePainter2D.StrokeType.INSIDE, thickness.toDouble(), theme.borderColor)
  }

  protected open fun underlineRectangle(position: KrTabsPosition,
                                        rect: Rectangle,
                                        thickness: Int): Rectangle {
    return Rectangle(rect.x, rect.y + rect.height - thickness, rect.width, thickness)
  }
}
