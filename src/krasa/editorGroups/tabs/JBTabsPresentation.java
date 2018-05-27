/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package krasa.editorGroups.tabs;

import com.intellij.util.ui.TimedDeadzone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface JBTabsPresentation {

	boolean isHideTabs();

	void setHideTabs(boolean hideTabs);

	krasa.editorGroups.tabs.JBTabsPresentation setPaintBorder(int top, int left, int right, int bottom);

	krasa.editorGroups.tabs.JBTabsPresentation setTabSidePaintBorder(int size);

	krasa.editorGroups.tabs.JBTabsPresentation setPaintFocus(boolean paintFocus);

	krasa.editorGroups.tabs.JBTabsPresentation setAlwaysPaintSelectedTab(final boolean paintSelected);

	krasa.editorGroups.tabs.JBTabsPresentation setStealthTabMode(boolean stealthTabMode);

	krasa.editorGroups.tabs.JBTabsPresentation setSideComponentVertical(boolean vertical);

	krasa.editorGroups.tabs.JBTabsPresentation setSideComponentOnTabs(boolean onTabs);

	krasa.editorGroups.tabs.JBTabsPresentation setSideComponentBefore(boolean before);

	krasa.editorGroups.tabs.JBTabsPresentation setSingleRow(boolean singleRow);

	boolean isSingleRow();

	krasa.editorGroups.tabs.JBTabsPresentation setUiDecorator(@Nullable UiDecorator decorator);

	krasa.editorGroups.tabs.JBTabsPresentation setRequestFocusOnLastFocusedComponent(boolean request);

	void setPaintBlocked(boolean blocked, final boolean takeSnapshot);

	krasa.editorGroups.tabs.JBTabsPresentation setInnerInsets(Insets innerInsets);

	krasa.editorGroups.tabs.JBTabsPresentation setGhostsAlwaysVisible(boolean visible);

	krasa.editorGroups.tabs.JBTabsPresentation setFocusCycle(final boolean root);

	@NotNull
	krasa.editorGroups.tabs.JBTabsPresentation setToDrawBorderIfTabsHidden(boolean draw);

	@NotNull
	JBTabs getJBTabs();

	@NotNull
	krasa.editorGroups.tabs.JBTabsPresentation setActiveTabFillIn(@Nullable Color color);

	@NotNull
	krasa.editorGroups.tabs.JBTabsPresentation setTabLabelActionsAutoHide(boolean autoHide);

	@NotNull
	krasa.editorGroups.tabs.JBTabsPresentation setTabLabelActionsMouseDeadzone(TimedDeadzone.Length length);

	@NotNull
	krasa.editorGroups.tabs.JBTabsPresentation setTabsPosition(JBTabsPosition position);

	JBTabsPosition getTabsPosition();

	krasa.editorGroups.tabs.JBTabsPresentation setTabDraggingEnabled(boolean enabled);
}
