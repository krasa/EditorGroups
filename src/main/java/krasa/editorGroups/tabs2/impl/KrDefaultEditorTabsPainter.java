// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class KrDefaultEditorTabsPainter extends KrEditorTabsPainter {

  public KrDefaultEditorTabsPainter(KrEditorTabs tabs) {
    super(tabs);
  }

  @Override
  public void doPaintInactive(Graphics2D g2d,
                              Rectangle effectiveBounds,
                              int x,
                              int y,
                              int w,
                              int h,
                              Color tabColor,
                              int row,
                              int column,
                              boolean vertical) {
    g2d.setColor(tabColor != null ? tabColor : getDefaultTabColor());
    g2d.fillRect(x, y, w, h);
    g2d.setColor(getInactiveMaskColor());
    g2d.fillRect(x, y, w, h);
  }

  @Override
  public void doPaintBackground(Graphics2D g, Rectangle clip, boolean vertical, Rectangle rectangle) {
    g.setColor(getBackgroundColor());
    g.fill(clip);
  }

  @Override
  public Color getBackgroundColor() {
    return BORDER_COLOR;
  }

  protected Color getDefaultTabColor() {
    if (myDefaultTabColor != null) {
      return myDefaultTabColor;
    }
    return DEFAULT_TAB_COLOR;
  }

  protected Color getInactiveMaskColor() {
    return INACTIVE_MASK_COLOR;
  }
}
