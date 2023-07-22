package krasa.editorGroups.tabs2.impl.multiRow

import krasa.editorGroups.tabs2.KrTabInfo
import krasa.editorGroups.tabs2.KrTabsPosition
import krasa.editorGroups.tabs2.impl.KrLayoutPassInfo
import krasa.editorGroups.tabs2.impl.KrTabLabel
import krasa.editorGroups.tabs2.impl.KrTabsImpl
import krasa.editorGroups.tabs2.impl.table.KrTableLayout
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.abs


abstract class KrMultiRowLayout(protected val tabs: KrTabsImpl,
                                protected val showPinnedTabsSeparately: Boolean) : KrTableLayout(tabs) {
  protected var prevLayoutPassInfo: KrMultiRowPassInfo? = null

  override fun layoutTable(visibleInfos: List<KrTabInfo>): KrLayoutPassInfo {
    tabs.resetLayout(true)

    val insets = tabs.layoutInsets
    val toFitRec = Rectangle(insets.left, insets.top,
      tabs.width - insets.left - insets.right,
      tabs.height - insets.top - insets.bottom)
    val data = KrMultiRowPassInfo(tabs, visibleInfos, toFitRec, scrollOffset)
    prevLayoutPassInfo = data

    if (!tabs.isHideTabs && !visibleInfos.isEmpty() && !data.toFitRec.isEmpty) {
      val rows = splitToRows(data)
      data.rows.addAll(rows)
      layoutRows(data)

      val topRowInd = if (tabs.position == KrTabsPosition.top) 0 else rows.size - 1
      data.tabsRectangle = Rectangle(toFitRec.x, getRowY(data, topRowInd), toFitRec.width, data.rowCount * data.rowHeight)
    }

    tabs.titleWrapper.bounds = data.titleRect
    tabs.moreToolbar!!.component.bounds = data.moreRect
    tabs.entryPointToolbar?.component?.bounds = data.entryPointRect

    tabs.selectedInfo?.let { layoutTabComponent(data, it) }
    return data
  }

  protected abstract fun splitToRows(data: KrMultiRowPassInfo): List<KrTabsRow>

  private fun layoutRows(data: KrMultiRowPassInfo) {
    for ((index, row) in data.rows.withIndex()) {
      val y = getRowY(data, index)
      row.layoutRow(data, y)
    }
  }

  private fun getRowY(data: KrMultiRowPassInfo, row: Int): Int {
    return when (tabs.position) {
      KrTabsPosition.top -> {
        data.toFitRec.y + row * data.rowHeight
      }

      KrTabsPosition.bottom -> {
        data.toFitRec.y + data.toFitRec.height - (row + 1) * data.rowHeight
      }

      else -> error("MultiRowLayout is not supported for vertical placements")
    }
  }

  private fun layoutTabComponent(data: KrMultiRowPassInfo, info: KrTabInfo) {
    val toolbar = tabs.infoToToolbar.get(info)

    val componentY = when (tabs.position) {
      KrTabsPosition.top -> data.rowCount * data.rowHeight
      KrTabsPosition.bottom -> 0
      else -> error("MultiRowLayout is not supported for vertical placements")
    }
    val componentHeight = when (tabs.position) {
      KrTabsPosition.top -> tabs.height  // it will be adjusted inside KrTabsImpl.layoutComp
      KrTabsPosition.bottom -> tabs.height - data.rowCount * data.rowHeight
      else -> error("MultiRowLayout is not supported for vertical placements")
    }

    if (!tabs.horizontalSide && toolbar != null && !toolbar.isEmpty) {
      val toolbarWidth = toolbar.preferredSize.width
      val vSeparatorWidth = if (toolbarWidth > 0) tabs.separatorWidth else 0
      if (tabs.isSideComponentBefore) {
        val compRect = tabs.layoutComp(Rectangle(toolbarWidth + vSeparatorWidth, componentY, tabs.width, componentHeight),
          info.component, 0, 0)
        tabs.layout(toolbar, compRect.x - toolbarWidth - vSeparatorWidth, compRect.y, toolbarWidth, compRect.height)
      } else {
        val width = tabs.width - toolbarWidth - vSeparatorWidth
        val compRect = tabs.layoutComp(Rectangle(0, componentY, width, componentHeight),
          info.component, 0, 0)
        tabs.layout(toolbar, compRect.x + compRect.width + vSeparatorWidth, compRect.y, toolbarWidth, compRect.height)
      }
    } else tabs.layoutComp(Rectangle(0, componentY, tabs.width, componentHeight),
      info.component, 0, 0)
  }

  protected fun splitToPinnedUnpinned(infosToSplit: List<KrTabInfo>): Pair<List<KrTabInfo>, List<KrTabInfo>> {
    val infos = infosToSplit.toList()
    val lastPinnedInd = infos.indexOfLast { it.isPinned }
    if (lastPinnedInd == -1) {
      return emptyList<KrTabInfo>() to infos
    }
    val pinnedRowEndInd = if (infos.getOrNull(lastPinnedInd + 1)?.let { tabs.isDropTarget(it) } == true) {
      lastPinnedInd + 1  // if next is dnd placeholder, put it to the pinned row
    } else lastPinnedInd
    val pinned = infos.subList(0, pinnedRowEndInd + 1)
    val unpinned = infos.subList(pinnedRowEndInd + 1, infos.size)
    return pinned to unpinned
  }

  override fun getScrollOffset(): Int {
    return 0
  }

  override fun scroll(units: Int) = Unit

  override fun isWithScrollBar(): Boolean {
    return false
  }

  override fun isDragOut(tabLabel: KrTabLabel, deltaX: Int, deltaY: Int): Boolean {
    val data = prevLayoutPassInfo
    if (data == null) {
      return super.isDragOut(tabLabel, deltaX, deltaY)
    }
    return abs(deltaY) > data.tabsRectangle.height * getDragOutMultiplier()
  }

  override fun getDropIndexFor(point: Point): Int {
    val data = prevLayoutPassInfo
    if (data == null) return -1
    var result = -1

    val lastInRow = data.rows.mapNotNull { it.infos.lastOrNull() }

    var component = tabs.getComponentAt(point)
    if (component is KrTabsImpl) {
      for (i in 0 until data.myVisibleInfos.size - 1) {
        val firstInfo = data.myVisibleInfos[i]
        val secondInfo = data.myVisibleInfos[i + 1]
        val first = tabs.infoToLabel.get(firstInfo)!!
        val second = tabs.infoToLabel.get(secondInfo)!!
        val firstBounds = first.bounds
        val secondBounds = second.bounds
        val between = firstBounds.maxX < point.x
          && secondBounds.getX() > point.x
          && firstBounds.y < point.y
          && secondBounds.maxY > point.y
        if (between) {
          component = first
          break
        }
        if (lastInRow.contains(firstInfo)
          && firstBounds.y <= point.y
          && firstBounds.maxY >= point.y
          && firstBounds.maxX <= point.x) {
          component = second
          break
        }
      }
    }

    if (component is KrTabLabel) {
      val info = component.info
      val index = data.myVisibleInfos.indexOf(info)
      if (!tabs.isDropTarget(info)) {
        val dropTargetBefore = data.myVisibleInfos.subList(0, index + 1).any { tabs.isDropTarget(it) }
        result = index - if (dropTargetBefore) 1 else 0
      } else if (index < data.myVisibleInfos.size) {
        result = index
      }
    }
    return result
  }
}