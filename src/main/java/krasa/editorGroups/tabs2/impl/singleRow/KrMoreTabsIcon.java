// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package krasa.editorGroups.tabs2.impl.singleRow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author pegov
 */
public abstract class KrMoreTabsIcon {
  private final Icon icon = AllIcons.General.MoreTabs;
  private int myCounter;

  public void paintIcon(final Component c, Graphics graphics) {
    if (myCounter <= 0)
      return;
    final Rectangle moreRect = getIconRec();

    if (moreRect == null) return;

    int iconY = getIconY(moreRect);
    int iconX = getIconX(moreRect);
    graphics.setFont(UIUtil.getLabelFont().deriveFont((float) Math.min(8, UIUtil.getButtonFont().getSize())));
    int width = graphics.getFontMetrics().stringWidth(String.valueOf(myCounter));
    iconX -= width / 2 + 1;

    icon.paintIcon(c, graphics, iconX, iconY);
    Graphics g = graphics.create();
    try {
      UISettings.setupAntialiasing(g);
      UIUtil.drawStringWithHighlighting(g, String.valueOf(myCounter),
        iconX + getIconWidth() + 2,
        iconY + getIconHeight() - 5,
        JBColor.BLACK,
        ColorUtil.withPreAlpha(JBColor.WHITE, .9));
    } finally {
      g.dispose();
    }
  }

  public int getIconWidth() {
    return icon.getIconWidth();
  }

  public int getIconHeight() {
    return icon.getIconHeight();
  }

  protected int getIconX(final Rectangle iconRec) {
    return iconRec.x + iconRec.width / 2 - (getIconWidth()) / 2;
  }

  protected int getIconY(final Rectangle iconRec) {
    return iconRec.y + iconRec.height / 2 - getIconHeight() / 2;
  }

  @Nullable
  protected abstract Rectangle getIconRec();

  public void updateCounter(int counter) {
    myCounter = counter;
  }
}
