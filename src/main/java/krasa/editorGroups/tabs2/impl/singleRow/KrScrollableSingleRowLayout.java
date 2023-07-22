// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.singleRow;

import com.intellij.ui.ExperimentalUI;
import com.intellij.util.ui.JBUI;
import krasa.editorGroups.tabs2.KrTabInfo;
import krasa.editorGroups.tabs2.impl.KrTabLabel;
import krasa.editorGroups.tabs2.impl.KrTabLayout;
import krasa.editorGroups.tabs2.impl.KrTabsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author yole
 */
public class KrScrollableSingleRowLayout extends KrSingleRowLayout {
  public static final int DEADZONE_FOR_DECLARE_TAB_HIDDEN = 10;
  private int myScrollOffset = 0;
  private final boolean myWithScrollBar;

  public KrScrollableSingleRowLayout(final KrTabsImpl tabs) {
    this(tabs, false);
  }

  public KrScrollableSingleRowLayout(final KrTabsImpl tabs, boolean isWithScrollBar) {
    super(tabs);
    myWithScrollBar = isWithScrollBar;
  }

  @Override
  public int getScrollOffset() {
    return myScrollOffset;
  }

  @Override
  public void scroll(int units) {
    myScrollOffset += units;
    clampScrollOffsetToBounds(lastSingleRowLayout);
  }

  @Override
  protected boolean checkLayoutLabels(KrSingleRowPassInfo data) {
    return true;
  }

  private void clampScrollOffsetToBounds(@Nullable KrSingleRowPassInfo data) {
    if (data == null) {
      return;
    }
    if (data.requiredLength < data.toFitLength) {
      myScrollOffset = 0;
    } else {
      int max = data.requiredLength - data.toFitLength + getMoreRectAxisSize();
      Insets actionInsets = myTabs.getActionsInsets();
      max += myTabs.isHorizontalTabs() ? actionInsets.left + actionInsets.right
        : actionInsets.top + actionInsets.bottom;
      if (!ExperimentalUI.isNewUI() && getStrategy() instanceof KrSingleRowLayoutStrategy.Vertical) {
        max += data.entryPointAxisSize;
      }
      myScrollOffset = Math.max(0, Math.min(myScrollOffset, max));
    }
  }

  private void doScrollToSelectedTab(KrSingleRowPassInfo passInfo) {
    if (myTabs.isMouseInsideTabsArea()) {
      return;
    }
    int offset = -myScrollOffset;
    for (KrTabInfo info : passInfo.myVisibleInfos) {
      final int length = getRequiredLength(info);
      if (info == myTabs.getSelectedInfo()) {
        if (offset < 0) {
          scroll(offset);
        } else {
          int maxLength = passInfo.toFitLength - getMoreRectAxisSize();
          Insets actionInsets = myTabs.getActionsInsets();
          if (myTabs.getEntryPointPreferredSize().width == 0) {
            maxLength -= myTabs.isHorizontalTabs() ? actionInsets.left + actionInsets.right
              : actionInsets.top + actionInsets.bottom;
          }
          if (!ExperimentalUI.isNewUI() && getStrategy() instanceof KrSingleRowLayoutStrategy.Vertical) {
            maxLength -= passInfo.entryPointAxisSize;
          }
          if (offset + length > maxLength) {
            // a left side should always be visible
            if (length < maxLength) {
              scroll(offset + length - maxLength);
            } else {
              scroll(offset);
            }
          }
        }
        break;
      }
      offset += length;
    }
  }

  @Override
  protected void recomputeToLayout(KrSingleRowPassInfo data) {
    calculateRequiredLength(data);
    clampScrollOffsetToBounds(data);
    doScrollToSelectedTab(data);
    clampScrollOffsetToBounds(data);
  }

  @Override
  protected void layoutMoreButton(KrSingleRowPassInfo data) {
    if (data.requiredLength > data.toFitLength) {
      data.moreRect = getStrategy().getMoreRect(data);
    }
  }

  @Override
  protected boolean applyTabLayout(KrSingleRowPassInfo data, KrTabLabel label, int length) {
    if (data.requiredLength > data.toFitLength && !(label.isPinned() && KrTabLayout.showPinnedTabsSeparately())) {
      length = getStrategy().getLengthIncrement(label.getPreferredSize());
      int moreRectSize = getMoreRectAxisSize();
      if (data.entryPointAxisSize == 0) {
        Insets insets = myTabs.getActionsInsets();
        moreRectSize += insets.left + insets.right;
      }
      if (data.position + length > data.toFitLength - moreRectSize) {
        if (getStrategy().drawPartialOverflowTabs()) {
          int clippedLength = ExperimentalUI.isNewUI() && myTabs.getTabsPosition().isSide()
            ? length : data.toFitLength - data.position - moreRectSize;
          final Rectangle rec = getStrategy().getLayoutRect(data, data.position, clippedLength);
          myTabs.layout(label, rec);
        }
        label.setAlignmentToCenter(false);
        return false;
      }
    }
    return super.applyTabLayout(data, label, length);
  }

  @Override
  public boolean isTabHidden(@NotNull KrTabInfo info) {
    KrTabLabel label = myTabs.getInfoToLabel().get(info);
    Rectangle bounds = label.getBounds();
    int deadzone = JBUI.scale(DEADZONE_FOR_DECLARE_TAB_HIDDEN);
    return getStrategy().getMinPosition(bounds) < -deadzone
      || bounds.width < label.getPreferredSize().width - deadzone
      || bounds.height < label.getPreferredSize().height - deadzone;
  }

  @Nullable
  @Override
  protected KrTabLabel findLastVisibleLabel(KrSingleRowPassInfo data) {
    int i = data.toLayout.size() - 1;
    while (i >= 0) {
      KrTabInfo info = data.toLayout.get(i);
      KrTabLabel label = myTabs.getInfoToLabel().get(info);
      if (!label.getBounds().isEmpty()) {
        return label;
      }
      i--;
    }
    return null;
  }

  private int getMoreRectAxisSize() {
    if (ExperimentalUI.isNewUI() && myTabs.getPosition().isSide()) {
      return 0;
    }
    return getStrategy().getMoreRectAxisSize();
  }

  @Override
  public boolean isScrollable() {
    return true;
  }

  @Override
  public boolean isWithScrollBar() {
    return myWithScrollBar;
  }
}
