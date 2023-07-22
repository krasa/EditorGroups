package krasa.editorGroups.tabs2.impl.multiRow

import krasa.editorGroups.tabs2.KrTabInfo
import krasa.editorGroups.tabs2.impl.KrTabsImpl


class KrWrapMultiRowLayout(tabs: KrTabsImpl, showPinnedTabsSeparately: Boolean)
  : KrMultiRowLayout(tabs, showPinnedTabsSeparately) {
  override fun splitToRows(data: KrMultiRowPassInfo): List<KrTabsRow> {
    val leftmostX = data.toFitRec.x + tabs.titleWrapper.preferredSize.width
    val entryToolbarWidth = tabs.entryPointToolbar?.component?.let { toolbar ->
      tabs.getActionsInsets().left + toolbar.preferredSize.width + tabs.getActionsInsets().right
    } ?: 0
    val rightmostX = data.toFitRec.x + data.toFitRec.width - entryToolbarWidth
    val firstRowWidth = rightmostX - leftmostX
    val getRowMaxLen: (Int) -> Int = { index -> if (index == 0) firstRowWidth else data.toFitRec.width }

    val infos = data.myVisibleInfos
    val rows = mutableListOf<KrTabsRow>()
    if (showPinnedTabsSeparately) {
      val (pinned, unpinned) = splitToPinnedUnpinned(infos)
      if (pinned.isNotEmpty()) {
        rows.add(KrCompressibleTabsRow(pinned, withTitle = tabs.titleWrapper.preferredSize.width > 0,
          withEntryPointToolbar = tabs.entryPointPreferredSize.width > 0))
      }
      doSplitToRows(data, rows, unpinned, getRowMaxLen)
    } else {
      doSplitToRows(data, rows, infos, getRowMaxLen)
    }

    return rows
  }

  private fun doSplitToRows(data: KrMultiRowPassInfo,
                            rows: MutableList<KrTabsRow>,
                            infosToSplit: List<KrTabInfo>,
                            getRowMaxLen: (index: Int) -> Int) {
    var curRowInfos = mutableListOf<KrTabInfo>()
    var curLen = 0
    for (info in infosToSplit) {
      val len = tabs.infoToLabel[info]!!.preferredSize.width
      data.lengths[info] = len
      if (curLen + len <= getRowMaxLen(rows.size)) {
        curRowInfos.add(info)
      } else {
        rows.add(createRow(curRowInfos, isFirst = rows.size == 0))
        curRowInfos = mutableListOf(info)
        curLen = 0
      }
      curLen += len + tabs.tabHGap
    }
    if (curRowInfos.isNotEmpty()) {
      rows.add(createRow(curRowInfos, isFirst = rows.size == 0))
    }
  }

  private fun createRow(infos: List<KrTabInfo>, isFirst: Boolean): KrTabsRow {
    return KrSimpleTabsRow(infos,
      withTitle = isFirst && tabs.titleWrapper.preferredSize.width > 0,
      withEntryPointToolbar = isFirst && tabs.entryPointPreferredSize.width > 0)
  }
}