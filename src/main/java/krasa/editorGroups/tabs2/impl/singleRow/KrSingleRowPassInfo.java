// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.singleRow;

import com.intellij.ui.ExperimentalUI;
import krasa.editorGroups.tabs2.KrTabInfo;
import krasa.editorGroups.tabs2.impl.KrLayoutPassInfo;
import krasa.editorGroups.tabs2.impl.KrTabsImpl;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class KrSingleRowPassInfo extends KrLayoutPassInfo {
  private final KrTabsImpl tabs;
  final Dimension layoutSize;
  final int contentCount;
  int position;
  int requiredLength;
  int toFitLength;
  public final List<KrTabInfo> toLayout;
  public final List<KrTabInfo> toDrop;
  final int entryPointAxisSize;
  final int moreRectAxisSize;

  public WeakReference<JComponent> hToolbar;
  public WeakReference<JComponent> vToolbar;

  public Insets insets;

  public WeakReference<JComponent> component;
  public Rectangle tabRectangle;
  final int scrollOffset;

  public KrSingleRowPassInfo(KrSingleRowLayout layout, List<KrTabInfo> visibleInfos) {
    super(visibleInfos);
    tabs = layout.myTabs;
    layoutSize = tabs.getSize();
    contentCount = tabs.getTabCount();
    toLayout = new ArrayList<>();
    toDrop = new ArrayList<>();
    entryPointAxisSize = layout.getStrategy().getEntryPointAxisSize();
    moreRectAxisSize = layout.getStrategy().getMoreRectAxisSize();
    scrollOffset = layout.getScrollOffset();
  }

  @Override
  public int getRowCount() {
    return 1;
  }

  @Override
  public Rectangle getHeaderRectangle() {
    return (Rectangle) tabRectangle.clone();
  }

  @Override
  public int getRequiredLength() {
    return requiredLength;
  }

  @Override
  public int getScrollExtent() {
    if (tabs.isHorizontalTabs()) {
      return !moreRect.isEmpty() ? moreRect.x - tabs.getActionsInsets().left
        : !entryPointRect.isEmpty() ? entryPointRect.x - tabs.getActionsInsets().left
        : layoutSize.width;
    } else {
      if (ExperimentalUI.isNewUI()) {
        return layoutSize.height;
      }
      return !moreRect.isEmpty() ? moreRect.y - tabs.getActionsInsets().top
        : !entryPointRect.isEmpty() ? entryPointRect.y - tabs.getActionsInsets().top
        : layoutSize.height;
    }
  }
}
