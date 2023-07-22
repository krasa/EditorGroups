// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.table;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import krasa.editorGroups.tabs2.KrTabInfo;
import krasa.editorGroups.tabs2.KrTabsUtil;
import krasa.editorGroups.tabs2.impl.KrLayoutPassInfo;
import krasa.editorGroups.tabs2.impl.KrTabLabel;
import krasa.editorGroups.tabs2.impl.KrTabLayout;
import krasa.editorGroups.tabs2.impl.KrTabsImpl;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("removal")
public class KrTableLayout extends KrTabLayout {
  private int myScrollOffset = 0;

  final KrTabsImpl myTabs;

  public KrTablePassInfo lastTableLayout;

  private final boolean myWithScrollBar;

  public KrTableLayout(final KrTabsImpl tabs) {
    this(tabs, false);
  }

  public KrTableLayout(final KrTabsImpl tabs, boolean isWithScrollBar) {
    myTabs = tabs;
    myWithScrollBar = isWithScrollBar;
  }

  private KrTablePassInfo computeLayoutTable(List<KrTabInfo> visibleInfos) {
    final KrTablePassInfo data = new KrTablePassInfo(this, visibleInfos);
    if (myTabs.isHideTabs()) {
      return data;
    }
    doScrollToSelectedTab(lastTableLayout);

    boolean singleRow = myTabs.isSingleRow();
    boolean showPinnedTabsSeparately = showPinnedTabsSeparately();
    boolean scrollable = UISettings.getInstance().getHideTabsIfNeeded() && singleRow;
    int titleWidth = myTabs.getTitleWrapper().getPreferredSize().width;

    data.titleRect.setBounds(data.toFitRec.x, data.toFitRec.y, titleWidth, myTabs.getHeaderFitSize().height);
    data.entryPointRect.setBounds(data.toFitRec.x + data.toFitRec.width - myTabs.getEntryPointPreferredSize().width - myTabs.getActionsInsets().right,
      data.toFitRec.y,
      myTabs.getEntryPointPreferredSize().width,
      myTabs.getHeaderFitSize().height);
    data.moreRect.setBounds(data.toFitRec.x + data.toFitRec.width - myTabs.getEntryPointPreferredSize().width - myTabs.getActionsInsets().right,
      data.toFitRec.y, 0, myTabs.getHeaderFitSize().height);
    calculateLengths(data);

    int eachX = data.titleRect.x + data.titleRect.width;
    Insets insets = myTabs.getLayoutInsets();
    int eachY = insets.top;
    int requiredRowsPinned = 0;
    int requiredRowsUnpinned = 0;

    int maxX = data.moreRect.x - (singleRow ? myTabs.getActionsInsets().left : 0);
    if (!singleRow && showPinnedTabsSeparately && ContainerUtil.all(visibleInfos, info -> !info.isPinned())) {
      maxX += myTabs.getEntryPointPreferredSize().width;
    }

    int hGap = myTabs.getTabHGap();
    int entryPointMargin = scrollable ? 0 : myTabs.getEntryPointPreferredSize().width;
    for (KrTabInfo eachInfo : data.myVisibleInfos) {
      KrTabLabel eachLabel = myTabs.getTabLabel(eachInfo);
      boolean pinned = eachLabel.isPinned();
      int width = data.lengths.get(eachInfo);
      if (!pinned || !showPinnedTabsSeparately) {
        data.requiredLength += width;
      }
      if (pinned && showPinnedTabsSeparately) {
        if (requiredRowsPinned == 0) {
          requiredRowsPinned = 1;
        }
        myTabs.layout(eachLabel, eachX, eachY, width, myTabs.getHeaderFitSize().height);
        data.bounds.put(eachInfo, eachLabel.getBounds());
      } else {
        if ((!scrollable && eachX + width + hGap > maxX - entryPointMargin && !singleRow) || (showPinnedTabsSeparately && eachLabel.isNextToLastPinned())) {
          requiredRowsUnpinned++;
          eachY += myTabs.getHeaderFitSize().height;
          eachX = data.toFitRec.x;
        } else if (requiredRowsUnpinned == 0) {
          requiredRowsUnpinned = 1;
        }
        if (scrollable) {
          if (eachX - getScrollOffset() + width + hGap > maxX - entryPointMargin) {
            width = Math.max(0, maxX - eachX + getScrollOffset());
            data.invisible.add(eachInfo);
          }
        }

        myTabs.layout(eachLabel, eachX - getScrollOffset(), eachY, width == 1 ? 0 : width, myTabs.getHeaderFitSize().height);
        Rectangle rectangle = new Rectangle(myTabs.getHeaderFitSize());
        data.bounds.put(eachInfo, eachLabel.getBounds());
        int intersection = eachLabel.getBounds().intersection(rectangle).width;
        if (scrollable && intersection < eachLabel.getBounds().width) {
          data.invisible.add(eachInfo);
        }
      }
      eachX += width + hGap;
      if (requiredRowsPinned + requiredRowsUnpinned > 1) {
        entryPointMargin = singleRow ? 0 : -data.moreRect.width;
      }
    }
    if (requiredRowsPinned > 0 && requiredRowsUnpinned > 0)
      data.moreRect.y += myTabs.getHeaderFitSize().height /*+ myTabs.getSeparatorWidth()*/;

    if (data.invisible.isEmpty()) {
      data.moreRect.setBounds(0, 0, 0, 0);
    }

    eachY = -1;
    KrTableRow eachTableRow = new KrTableRow(data);

    for (KrTabInfo eachInfo : data.myVisibleInfos) {
      final KrTabLabel eachLabel = myTabs.getTabLabel(eachInfo);
      if (eachY == -1 || eachY != eachLabel.getY()) {
        if (eachY != -1) {
          eachTableRow = new KrTableRow(data);
        }
        eachY = eachLabel.getY();
        data.table.add(eachTableRow);
      }
      eachTableRow.add(eachInfo, eachLabel.getWidth());
    }

    doScrollToSelectedTab(data);
    clampScrollOffsetToBounds(data);
    return data;
  }

  private void calculateLengths(KrTablePassInfo data) {
    boolean compressible = isCompressible();
    boolean showPinnedTabsSeparately = showPinnedTabsSeparately();

    int standardLengthToFit = data.moreRect.x - (data.titleRect.x + data.titleRect.width) - myTabs.getActionsInsets().left;
    if (compressible || showPinnedTabsSeparately) {
      if (showPinnedTabsSeparately) {
        List<KrTabInfo> pinned = ContainerUtil.filter(data.myVisibleInfos, info -> info.isPinned());
        calculateCompressibleLengths(pinned, data, standardLengthToFit);
        List<KrTabInfo> unpinned = ContainerUtil.filter(data.myVisibleInfos, info -> !info.isPinned());
        if (compressible) {
          Insets insets = myTabs.getActionsInsets();
          calculateCompressibleLengths(unpinned, data, pinned.isEmpty()
            ? standardLengthToFit
            : standardLengthToFit + data.titleRect.width + myTabs.getEntryPointPreferredSize().width + insets.left + insets.right);
        } else {
          calculateRawLengths(unpinned, data);
          if (getTotalLength(unpinned, data) > standardLengthToFit) {
            int moreWidth = getMoreRectAxisSize();
            int entryPointsWidth = pinned.isEmpty() ? myTabs.getEntryPointPreferredSize().width : 0;
            data.moreRect.setBounds(data.toFitRec.x + data.toFitRec.width - moreWidth - entryPointsWidth - myTabs.getActionsInsets().right,
              myTabs.getLayoutInsets().top, moreWidth, myTabs.getHeaderFitSize().height);
            calculateRawLengths(unpinned, data);
          }
        }
      } else {
        calculateCompressibleLengths(data.myVisibleInfos, data, standardLengthToFit);
      }
    } else {//both scrollable and multi-row
      calculateRawLengths(data.myVisibleInfos, data);
      if (getTotalLength(data.myVisibleInfos, data) > standardLengthToFit) {
        int moreWidth = getMoreRectAxisSize();
        data.moreRect.setBounds(data.toFitRec.x + data.toFitRec.width - moreWidth, data.toFitRec.y, moreWidth, myTabs.getHeaderFitSize().height);
        calculateRawLengths(data.myVisibleInfos, data);
      }
    }
  }

  private int getMoreRectAxisSize() {
    return myTabs.isSingleRow() ? myTabs.getMoreToolbarPreferredSize().width : 0;
  }

  private static int getTotalLength(@NotNull List<KrTabInfo> list, @NotNull KrTablePassInfo data) {
    int total = 0;
    for (KrTabInfo info : list) {
      total += data.lengths.get(info);
    }
    return total;
  }

  private boolean isCompressible() {
    return myTabs.isSingleRow() && !UISettings.getInstance().getHideTabsIfNeeded() && myTabs.supportsCompression();
  }

  private void calculateCompressibleLengths(List<KrTabInfo> list, KrTablePassInfo data, int toFitLength) {
    if (list.isEmpty()) return;
    int spentLength = 0;
    int lengthEstimation = 0;

    for (KrTabInfo tabInfo : list) {
      lengthEstimation += Math.max(getMinTabWidth(), myTabs.getInfoToLabel().get(tabInfo).getPreferredSize().width);
    }

    final int extraWidth = toFitLength - lengthEstimation;

    for (Iterator<KrTabInfo> iterator = list.iterator(); iterator.hasNext(); ) {
      KrTabInfo tabInfo = iterator.next();
      final KrTabLabel label = myTabs.getInfoToLabel().get(tabInfo);

      int length;
      int lengthIncrement = label.getPreferredSize().width;
      if (!iterator.hasNext()) {
        length = Math.min(toFitLength - spentLength, lengthIncrement);
      } else if (extraWidth <= 0) {//need compress
        length = (int) (lengthIncrement * (float) toFitLength / lengthEstimation);
      } else {
        length = lengthIncrement;
      }
      if (tabInfo.isPinned()) {
        length = Math.min(getMaxPinnedTabWidth(), length);
      }
      length = Math.max(getMinTabWidth(), length);
      data.lengths.put(tabInfo, length);
      spentLength += length + myTabs.getTabHGap();
    }
  }

  private void calculateRawLengths(List<KrTabInfo> list, KrTablePassInfo data) {
    for (KrTabInfo info : list) {
      KrTabLabel eachLabel = myTabs.getTabLabel(info);
      Dimension size =
        eachLabel.isPinned() && showPinnedTabsSeparately() ? eachLabel.getNotStrictPreferredSize() : eachLabel.getPreferredSize();
      data.lengths.put(info, Math.max(getMinTabWidth(), size.width + myTabs.getTabHGap()));
    }
  }

  public KrLayoutPassInfo layoutTable(List<KrTabInfo> visibleInfos) {
    myTabs.resetLayout(true);
    Rectangle unitedTabArea = null;
    KrTablePassInfo data = computeLayoutTable(visibleInfos);

    Rectangle rect = new Rectangle(data.moreRect);
    rect.y += myTabs.getBorderThickness();
    myTabs.getMoreToolbar().getComponent().setBounds(rect);

    ActionToolbar entryPointToolbar = myTabs.getEntryPointToolbar();
    if (entryPointToolbar != null) {
      entryPointToolbar.getComponent().setBounds(data.entryPointRect);
    }
    myTabs.getTitleWrapper().setBounds(data.titleRect);

    Insets insets = myTabs.getLayoutInsets();
    int eachY = insets.top;
    for (KrTabInfo info : visibleInfos) {
      Rectangle bounds = data.bounds.get(info);
      if (unitedTabArea == null) {
        unitedTabArea = bounds;
      } else {
        unitedTabArea = unitedTabArea.union(bounds);
      }
    }

    if (myTabs.getSelectedInfo() != null) {
      final KrTabsImpl.Toolbar selectedToolbar = myTabs.getInfoToToolbar().get(myTabs.getSelectedInfo());

      final int componentY = (unitedTabArea != null ? unitedTabArea.y + unitedTabArea.height : eachY) + (myTabs.isEditorTabs() ? 0 : 2) -
        myTabs.getLayoutInsets().top;
      if (!myTabs.getHorizontalSide() && selectedToolbar != null && !selectedToolbar.isEmpty()) {
        final int toolbarWidth = selectedToolbar.getPreferredSize().width;
        final int vSeparatorWidth = toolbarWidth > 0 ? myTabs.separatorWidth : 0;
        if (myTabs.isSideComponentBefore()) {
          Rectangle compRect =
            myTabs.layoutComp(toolbarWidth + vSeparatorWidth, componentY, myTabs.getSelectedInfo().getComponent(), 0, 0);
          myTabs.layout(selectedToolbar, compRect.x - toolbarWidth - vSeparatorWidth, compRect.y, toolbarWidth, compRect.height);
        } else {
          final int width = myTabs.getWidth() - toolbarWidth - vSeparatorWidth;
          Rectangle compRect = myTabs.layoutComp(new Rectangle(0, componentY, width, myTabs.getHeight()),
            myTabs.getSelectedInfo().getComponent(), 0, 0);
          myTabs.layout(selectedToolbar, compRect.x + compRect.width + vSeparatorWidth, compRect.y, toolbarWidth, compRect.height);
        }
      } else {
        myTabs.layoutComp(0, componentY, myTabs.getSelectedInfo().getComponent(), 0, 0);
      }
    }
    if (unitedTabArea != null) {
      data.tabRectangle.setBounds(unitedTabArea);
    }
    lastTableLayout = data;
    return data;
  }

  @Override
  public boolean isTabHidden(@NotNull KrTabInfo info) {
    KrTabLabel label = myTabs.getInfoToLabel().get(info);
    Rectangle bounds = label.getBounds();
    int deadzone = JBUI.scale(DEADZONE_FOR_DECLARE_TAB_HIDDEN);
    return bounds.x < -deadzone || bounds.width < label.getPreferredSize().width - deadzone;
  }

  @Override
  public boolean isDragOut(@NotNull KrTabLabel tabLabel, int deltaX, int deltaY) {
    if (lastTableLayout == null) {
      return super.isDragOut(tabLabel, deltaX, deltaY);
    }

    Rectangle area = new Rectangle(lastTableLayout.toFitRec.width, tabLabel.getBounds().height);
    for (int i = 0; i < lastTableLayout.myVisibleInfos.size(); i++) {
      area = area.union(myTabs.getInfoToLabel().get(lastTableLayout.myVisibleInfos.get(i)).getBounds());
    }
    return Math.abs(deltaY) > area.height * getDragOutMultiplier();
  }

  @Override
  public int getDropIndexFor(Point point) {
    if (lastTableLayout == null) return -1;
    int result = -1;

    Component c = myTabs.getComponentAt(point);
    Set<KrTabInfo> lastInRow = new HashSet<>();
    for (int i = 0; i < lastTableLayout.table.size(); i++) {
      List<KrTabInfo> columns = lastTableLayout.table.get(i).myColumns;
      if (!columns.isEmpty()) {
        lastInRow.add(columns.get(columns.size() - 1));
      }
    }

    if (c instanceof KrTabsImpl) {
      for (int i = 0; i < lastTableLayout.myVisibleInfos.size() - 1; i++) {
        KrTabInfo firstInfo = lastTableLayout.myVisibleInfos.get(i);
        KrTabInfo secondInfo = lastTableLayout.myVisibleInfos.get(i + 1);
        KrTabLabel first = myTabs.getInfoToLabel().get(firstInfo);
        KrTabLabel second = myTabs.getInfoToLabel().get(secondInfo);

        Rectangle firstBounds = first.getBounds();
        Rectangle secondBounds = second.getBounds();

        final boolean between = firstBounds.getMaxX() < point.x
          && secondBounds.getX() > point.x
          && firstBounds.y < point.y
          && secondBounds.getMaxY() > point.y;

        if (between) {
          c = first;
          break;
        }
        if (lastInRow.contains(firstInfo)
          && firstBounds.y <= point.y
          && firstBounds.getMaxY() >= point.y
          && firstBounds.getMaxX() <= point.x) {
          c = second;
          break;
        }
      }
    }

    if (c instanceof KrTabLabel) {
      KrTabInfo info = ((KrTabLabel) c).getInfo();
      int index = lastTableLayout.myVisibleInfos.indexOf(info);
      boolean isDropTarget = myTabs.isDropTarget(info);
      if (!isDropTarget) {
        for (int i = 0; i <= index; i++) {
          if (myTabs.isDropTarget(lastTableLayout.myVisibleInfos.get(i))) {
            index -= 1;
            break;
          }
        }
        result = index;
      } else if (index < lastTableLayout.myVisibleInfos.size()) {
        result = index;
      }
    }
    return result;
  }

  @Override
  @MagicConstant(intValues = {
    SwingConstants.CENTER,
    SwingConstants.TOP,
    SwingConstants.LEFT,
    SwingConstants.BOTTOM,
    SwingConstants.RIGHT,
    -1
  })
  public int getDropSideFor(@NotNull Point point) {
    return KrTabsUtil.getDropSideFor(point, myTabs);
  }

  @Override
  public int getScrollOffset() {
    return myScrollOffset;
  }

  @Override
  public void scroll(int units) {
    if (!myTabs.isSingleRow()) {
      myScrollOffset = 0;
      return;
    }
    myScrollOffset += units;

    clampScrollOffsetToBounds(lastTableLayout);
  }

  private void clampScrollOffsetToBounds(@Nullable KrTablePassInfo data) {
    if (data == null) {
      return;
    }
    if (data.requiredLength < data.toFitRec.width) {
      myScrollOffset = 0;
    } else {
      int entryPointsWidth = data.moreRect.y == data.entryPointRect.y ? data.entryPointRect.width + 1 : 0;
      myScrollOffset = Math.max(0, Math.min(myScrollOffset,
        data.requiredLength - data.toFitRec.width + data.moreRect.width + entryPointsWidth /*+ (1 + myTabs.getIndexOf(myTabs.getSelectedInfo())) * myTabs.getBorderThickness()*/ + data.titleRect.width));
    }
  }

  @Override
  public boolean isWithScrollBar() {
    return myWithScrollBar;
  }

  public int getScrollUnitIncrement() {
    return 10;
  }

  private void doScrollToSelectedTab(KrTablePassInfo data) {
    if (myTabs.isMouseInsideTabsArea()
      || data == null
      || data.lengths.isEmpty()
      || myTabs.isHideTabs()
      || !showPinnedTabsSeparately()) {
      return;
    }

    int offset = -myScrollOffset;
    for (KrTabInfo info : data.myVisibleInfos) {
      if (info.isPinned()) continue;
      final int length = data.lengths.get(info);
      if (info == myTabs.getSelectedInfo()) {
        if (offset < 0) {
          scroll(offset);
        } else {
          final int maxLength = data.moreRect.x;
          if (offset + length > maxLength) {
            // left side should be always visible
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
}
