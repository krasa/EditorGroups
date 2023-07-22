// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl

import com.intellij.openapi.rd.fill2DRect
import com.intellij.openapi.rd.paint2DLine
import com.intellij.ui.paint.LinePainter2D
import com.jetbrains.rd.swing.fillRect
import krasa.editorGroups.tabs2.ApiChanged
import krasa.editorGroups.tabs2.JBTabPainter
import krasa.editorGroups.tabs2.JBTabsPosition
import krasa.editorGroups.tabs2.impl.themes.DefaultTabTheme
import krasa.editorGroups.tabs2.impl.themes.TabTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

open class JBDefaultTabPainter(val theme: TabTheme = DefaultTabTheme()) : JBTabPainter {

  override fun getTabTheme(): TabTheme = theme

  override fun getBackgroundColor(): Color = theme.background ?: theme.borderColor

  override fun fillBackground(g: Graphics2D, rect: Rectangle) {
    try {
      theme.background?.let {
        g.fill2DRect(rect, it)
      }
    } catch (e: Throwable) {
      ApiChanged.report(e)
    }
  }

  override fun paintTab(position: JBTabsPosition,
                        g: Graphics2D,
                        bounds: Rectangle,
                        borderThickness: Int,
                        tabColor: Color?,
                        hovered: Boolean) {
    try {
      tabColor?.let {
        g.fill2DRect(bounds, it)
      }

      if (hovered) {
        g.fillRect(bounds, theme.hoverBackground)
        return
      }
    } catch (e: Throwable) {
      ApiChanged.report(e)
    }
  }

  override fun paintSelectedTab(position: JBTabsPosition,
                                g: Graphics2D,
                                rect: Rectangle,
                                borderThickness: Int,
                                tabColor: Color?,
                                active: Boolean,
                                hovered: Boolean,
                                singleTab: Boolean) {
    try {
      val color = (tabColor
        ?: if (active) theme.underlinedTabBackground else theme.underlinedTabInactiveBackground)
        ?: theme.background
      val drawUnderline = !singleTab || theme.underlineSingleTab
      if (!drawUnderline) {
        // We have to replace 'thick' underline marker with thin 'border' line
        when (position) {
          JBTabsPosition.top -> rect.height -= borderThickness
          JBTabsPosition.bottom -> rect.y += borderThickness
          JBTabsPosition.left -> rect.width -= borderThickness
          JBTabsPosition.right -> {
            rect.x += borderThickness
          }
        }

      }
      color?.let {
        g.fill2DRect(rect, it)
      }

      if (hovered) {
        (if (active) theme.hoverBackground else theme.hoverInactiveBackground)?.let {
          g.fill2DRect(rect, it)
        }
      }
      if (drawUnderline) {
        val underline = underlineRectangle(position, rect, theme.underlineHeight)
        g.fill2DRect(underline, when {
          active && (!singleTab || theme.underlineSingleTab) -> theme.underlineColor
          else -> theme.inactiveUnderlineColor
        })
      }
    } catch (e: Throwable) {
      ApiChanged.report(e)
    }
  }

  override fun paintBorderLine(g: Graphics2D, thickness: Int, from: Point, to: Point) {
    try {
      g.paint2DLine(from, to, LinePainter2D.StrokeType.INSIDE, thickness.toDouble(), theme.borderColor)
    } catch (e: Throwable) {
      ApiChanged.report(e)
    }
  }

  protected open fun underlineRectangle(position: JBTabsPosition,
                                        rect: Rectangle,
                                        thickness: Int): Rectangle {
    return Rectangle(rect.x, rect.y + rect.height - thickness, rect.width, thickness)
  }
}
