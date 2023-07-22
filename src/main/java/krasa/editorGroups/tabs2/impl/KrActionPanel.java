// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl;

import com.intellij.diagnostic.LoadingState;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.ExperimentalUI;
import com.intellij.ui.InplaceButton;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.tabs2.KrTabInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class KrActionPanel extends NonOpaquePanel {
  private final List<KrActionButton> myButtons = new ArrayList<>();
  private final KrTabsImpl myTabs;
  private final KrTabInfo myInfo;

  private boolean myAutoHide;
  private boolean myActionsIsVisible = false;
  private boolean myMarkModified = false;

  public KrActionPanel(KrTabsImpl tabs, KrTabInfo tabInfo, Consumer<? super MouseEvent> pass, Consumer<? super Boolean> hover) {
    myTabs = tabs;
    myInfo = tabInfo;
    ActionGroup group = tabInfo.getTabLabelActions() != null ? tabInfo.getTabLabelActions() : new DefaultActionGroup();
    AnAction[] children = group.getChildren(null);
    if (LoadingState.CONFIGURATION_STORE_INITIALIZED.isOccurred() && !UISettings.getInstance().getCloseTabButtonOnTheRight()) {
      List<AnAction> list = Arrays.asList(children);
      Collections.reverse(list);
      children = list.toArray(AnAction[]::new);
    }

    setFocusable(false);

    final NonOpaquePanel wrapper = new NonOpaquePanel(new BorderLayout());
    wrapper.setFocusable(false);
    NonOpaquePanel inner = new NonOpaquePanel();
    inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
    wrapper.add(inner, BorderLayout.CENTER);
    for (AnAction each : children) {
      KrActionButton eachButton = new KrActionButton(tabInfo, each, tabInfo.getTabActionPlace(), pass, hover, tabs.getTabActionsMouseDeadZone$EditorGroups()) {
        @Override
        public void repaintComponent(final Component c) {
          KrTabLabel tabLabel = (KrTabLabel) SwingUtilities.getAncestorOfClass(KrTabLabel.class, c);
          if (tabLabel != null) {
            Point point = SwingUtilities.convertPoint(c, new Point(0, 0), tabLabel);
            Dimension d = c.getSize();
            tabLabel.repaint(point.x, point.y, d.width, d.height);
          } else {
            super.repaintComponent(c);
          }
        }
      };

      myButtons.add(eachButton);
      InplaceButton component = eachButton.getComponent();
      component.setFocusable(false);
      inner.add(component);
    }

    add(wrapper);

    UIUtil.uiTraverser(wrapper).forEach(c -> c.setFocusable(false));
  }

  @Override
  public void paint(Graphics g) {
    KrTabLabel label = myTabs.getInfoToLabel().get(myInfo);
    boolean isHovered = label != null && label.isHovered();
    boolean isSelected = myTabs.getSelectedInfo() == myInfo;
    if (ExperimentalUI.isNewUI()
      && myTabs instanceof KrEditorTabs
      && !isSelected
      && !isHovered
      && !myMarkModified
      && !myInfo.isPinned()) {
      return;
    }
    super.paint(g);
  }

  public boolean update() {
    if (getRootPane() == null) return false;
    boolean changed = false;
    boolean anyVisible = false;
    boolean anyModified = false;
    for (KrActionButton each : myButtons) {
      changed |= each.update();
      each.setMouseDeadZone(myTabs.getTabActionsMouseDeadZone$EditorGroups());
      anyVisible |= each.getComponent().isVisible();

      Boolean markModified = each.getPrevPresentation().getClientProperty(KrEditorTabs.MARK_MODIFIED_KEY);
      if (markModified != null) {
        anyModified |= markModified;
      }
    }

    myActionsIsVisible = anyVisible;
    myMarkModified = anyModified;

    return changed;
  }

  public boolean isAutoHide() {
    return myAutoHide;
  }

  public void setAutoHide(final boolean autoHide) {
    myAutoHide = autoHide;
    for (KrActionButton each : myButtons) {
      each.setAutoHide(myAutoHide);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return myActionsIsVisible ? super.getPreferredSize() : new Dimension(0, 0);
  }

  public void toggleShowActions(final boolean show) {
    for (KrActionButton each : myButtons) {
      each.toggleShowActions(show);
    }
  }
}