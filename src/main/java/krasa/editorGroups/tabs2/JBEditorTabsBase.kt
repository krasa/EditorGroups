package krasa.editorGroups.tabs2

import java.awt.Color
import java.util.function.Supplier

/**
 * @author yole
 */
interface JBEditorTabsBase : JBTabs {
  @Deprecated("Used only by the old tabs implementation")
  fun setEmptySpaceColorCallback(callback: Supplier<out Color>)
}