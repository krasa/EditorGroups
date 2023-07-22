// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.TimedDeadzone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface KrTabsPresentation {
  boolean isHideTabs();

  void setHideTabs(boolean hideTabs);

  KrTabsPresentation setPaintFocus(boolean paintFocus);

  KrTabsPresentation setSideComponentVertical(boolean vertical);

  KrTabsPresentation setSideComponentOnTabs(boolean onTabs);

  KrTabsPresentation setSideComponentBefore(boolean before);

  KrTabsPresentation setSingleRow(boolean singleRow);

  boolean isSingleRow();

  KrTabsPresentation setUiDecorator(@Nullable KrUiDecorator decorator);

  KrTabsPresentation setRequestFocusOnLastFocusedComponent(boolean request);

  void setPaintBlocked(boolean blocked, final boolean takeSnapshot);

  KrTabsPresentation setInnerInsets(Insets innerInsets);

  KrTabsPresentation setFocusCycle(final boolean root);

  @NotNull
  KrTabsPresentation setToDrawBorderIfTabsHidden(boolean draw);

  @NotNull
  KrTabs getJBTabs();

  @NotNull
  KrTabsPresentation setActiveTabFillIn(@Nullable Color color);

  @NotNull
  KrTabsPresentation setTabLabelActionsAutoHide(boolean autoHide);

  @NotNull
  KrTabsPresentation setTabLabelActionsMouseDeadzone(TimedDeadzone.Length length);

  @NotNull
  KrTabsPresentation setTabsPosition(KrTabsPosition position);

  KrTabsPosition getTabsPosition();

  KrTabsPresentation setTabDraggingEnabled(boolean enabled);

  KrTabsPresentation setAlphabeticalMode(boolean alphabeticalMode);

  KrTabsPresentation setSupportsCompression(boolean supportsCompression);

  void setFirstTabOffset(int offset);

  KrTabsPresentation setEmptyText(@Nullable @NlsContexts.StatusText String text);
}
