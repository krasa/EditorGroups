// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs;

public interface TabsListener {
	default void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
	}

	default void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
	}

	default void tabRemoved(TabInfo tabToRemove) {
	}

	default void tabsMoved() {
	}

	/**
	 * Use {@link krasa.editorGroups.tabs.TabsListener} directly
	 */
	@Deprecated
	class Adapter implements krasa.editorGroups.tabs.TabsListener {
	}
}
