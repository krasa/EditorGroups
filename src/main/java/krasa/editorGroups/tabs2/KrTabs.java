// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.ActiveRunnable;
import com.intellij.ui.DropAreaAware;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.function.Supplier;

public interface KrTabs extends DropAreaAware {
  @NotNull
  KrTabInfo addTab(KrTabInfo info, int index);

  @NotNull
  KrTabInfo addTab(KrTabInfo info);

  @NotNull
  ActionCallback removeTab(@Nullable KrTabInfo info);

  void removeAllTabs();

  @NotNull
  ActionCallback select(@NotNull KrTabInfo info, boolean requestFocus);

  @Nullable
  KrTabInfo getSelectedInfo();

  @NotNull
  KrTabInfo getTabAt(int tabIndex);

  int getTabCount();

  @NotNull
  KrTabsPresentation getPresentation();

  @Nullable
  DataProvider getDataProvider();

  KrTabs setDataProvider(@NotNull DataProvider dataProvider);

  @NotNull
  List<KrTabInfo> getTabs();

  @Nullable
  KrTabInfo getTargetInfo();

  @NotNull
  KrTabs addTabMouseListener(@NotNull MouseListener listener);

  KrTabs addListener(@NotNull KrTabsListener listener);

  KrTabs addListener(@NotNull KrTabsListener listener, @Nullable Disposable disposable);

  KrTabs setSelectionChangeHandler(KrTabs.SelectionChangeHandler handler);

  @NotNull
  JComponent getComponent();

  @Nullable
  KrTabInfo findInfo(MouseEvent event);

  @Nullable
  KrTabInfo findInfo(Object object);

  @Nullable
  KrTabInfo findInfo(Component component);

  int getIndexOf(@Nullable final KrTabInfo tabInfo);

  void requestFocus();

  void setNavigationActionBinding(String prevActionId, String nextActionId);

  @NotNull
  KrTabs setPopupGroup(@NotNull ActionGroup popupGroup, @NotNull String place, boolean addNavigationGroup);

  @NotNull
  KrTabs setPopupGroup(@NotNull Supplier<? extends ActionGroup> popupGroup, @NotNull String place, boolean addNavigationGroup);

  void resetDropOver(KrTabInfo tabInfo);

  Image startDropOver(KrTabInfo tabInfo, RelativePoint point);

  void processDropOver(KrTabInfo over, RelativePoint point);

  Component getTabLabel(KrTabInfo tabInfo);

  @Override
  @NotNull
  default Rectangle getDropArea() {
    Rectangle r = new Rectangle(getComponent().getBounds());
    if (getTabCount() > 0) {
      @SuppressWarnings("UseDPIAwareInsets")
      Insets insets = JBUI.insets(0);
      Rectangle bounds = getTabLabel(getTabAt(0)).getBounds();
      switch (getPresentation().getTabsPosition()) {
        case top -> insets.top = bounds.height;
        case left -> insets.left = bounds.width;
        case bottom -> insets.bottom = bounds.height;
        case right -> insets.right = bounds.width;
      }
      JBInsets.removeFrom(r, insets);
    }
    return r;
  }


  interface SelectionChangeHandler {
    @NotNull ActionCallback execute(KrTabInfo info, boolean requestFocus, @NotNull ActiveRunnable doChangeSelection);
  }
}
