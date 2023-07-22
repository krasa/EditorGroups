package krasa.editorGroups.tabs2.impl.multiRow

import krasa.editorGroups.tabs2.KrTabInfo

class KrSimpleTabsRow(infos: List<KrTabInfo>,
                      withTitle: Boolean,
                      withEntryPointToolbar: Boolean
) : KrTabsRow(infos, withTitle, withEntryPointToolbar) {
  override fun layoutTabs(data: KrMultiRowPassInfo, x: Int, y: Int, maxLength: Int) {
    val tabs = data.tabs
    var curX = x
    for (info in infos) {
      val len = data.lengths[info]!!
      val label = tabs.infoToLabel[info]!!
      tabs.layout(label, curX, y, len, data.rowHeight)
      curX += len + tabs.tabHGap
    }
  }
}