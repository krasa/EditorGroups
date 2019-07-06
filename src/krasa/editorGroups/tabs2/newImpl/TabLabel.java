// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.newImpl;

import krasa.editorGroups.tabs2.TabInfo;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2019.3")
public class TabLabel extends krasa.editorGroups.tabs2.impl.TabLabel {
	public TabLabel(JBTabsImpl tabs, TabInfo info) {
		super(tabs, info);
	}
}
