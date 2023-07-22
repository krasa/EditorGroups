package krasa.editorGroups.tabs2.impl.multiRow

import krasa.editorGroups.tabs2.KrTabInfo
import java.awt.Rectangle


abstract class KrTabsRow(val infos: List<KrTabInfo>, val withTitle: Boolean, val withEntryPointToolbar: Boolean) {
  fun layoutRow(data: KrMultiRowPassInfo, y: Int) {
    val tabsRange = layoutTitleAndEntryPoint(data, y)
    layoutTabs(data, tabsRange.first, y, tabsRange.last - tabsRange.first)
  }

  protected abstract fun layoutTabs(data: KrMultiRowPassInfo, x: Int, y: Int, maxLength: Int)

  private fun layoutTitleAndEntryPoint(data: KrMultiRowPassInfo, y: Int): IntRange {
    val tabs = data.tabs
    if (withTitle) {
      data.titleRect = Rectangle(data.toFitRec.x, y, tabs.titleWrapper.preferredSize.width, data.rowHeight)
    }
    if (withEntryPointToolbar) {
      val entryPointWidth = tabs.entryPointPreferredSize.width
      data.entryPointRect = Rectangle(data.toFitRec.x + data.toFitRec.width - entryPointWidth - tabs.getActionsInsets().right,
        y, entryPointWidth, data.rowHeight)
    }
    val leftmostX = data.toFitRec.x + data.titleRect.width
    val rightmostX = if (withEntryPointToolbar) data.entryPointRect.x - tabs.getActionsInsets().left else data.toFitRec.x + data.toFitRec.width
    return leftmostX..rightmostX
  }

  override fun toString(): String {
    return "${javaClass.simpleName}: $infos"
  }
}