// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl

import com.intellij.openapi.rd.fill2DRect
import com.jetbrains.rd.swing.fillRect
import krasa.editorGroups.tabs2.ApiChanged
import krasa.editorGroups.tabs2.JBTabsPosition
import krasa.editorGroups.tabs2.impl.themes.EditorTabTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

class JBEditorTabPainter : JBDefaultTabPainter(EditorTabTheme()) {
    override fun paintTab(position: JBTabsPosition, g: Graphics2D, rect: Rectangle, borderThickness: Int, tabColor: Color?, hovered: Boolean) {
        try {
            when (position) {
                JBTabsPosition.top -> rect.height -= borderThickness
                JBTabsPosition.bottom -> rect.y += borderThickness
                JBTabsPosition.left -> rect.width -= borderThickness
                JBTabsPosition.right -> {
                    rect.x += borderThickness
                }
            }

            tabColor?.let {
                g.fill2DRect(rect, it)

                if (theme is EditorTabTheme)
                    theme.inactiveColoredFileBackground?.let { inactive ->
                        g.fill2DRect(rect, inactive)
                    }
            }

            if (hovered) {
                g.fillRect(rect, theme.hoverBackground)
            }
        } catch (e: Throwable) {
            ApiChanged.report(e)
        }
    }

    override fun underlineRectangle(position: JBTabsPosition, rect: Rectangle, thickness: Int): Rectangle {
        return when (position) {
            JBTabsPosition.bottom -> Rectangle(rect.x, rect.y, rect.width, thickness)
            JBTabsPosition.left -> Rectangle(rect.x + rect.width - thickness, rect.y, thickness, rect.height)
            JBTabsPosition.right -> Rectangle(rect.x, rect.y, thickness, rect.height)
            else -> super.underlineRectangle(position, rect, thickness)
        }
    }
}