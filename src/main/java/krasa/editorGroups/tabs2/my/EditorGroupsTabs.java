package krasa.editorGroups.tabs2.my;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.intellij.util.ui.JBUI.getInt;

public class EditorGroupsTabs {
  @NotNull
  public static Color underlineColor() {
    return JBColor.namedColor("EditorGroupsTabs.underlineColor", new JBColor(0x4083C9, 0x4A88C7));
  }

  public static int underlineHeight() {
    return getInt("EditorGroupsTabs.underlineHeight", JBUIScale.scale(5));
  }

  @NotNull
  public static Color inactiveUnderlineColor() {
    return JBColor.namedColor("EditorGroupsTabs.inactiveUnderlineColor", new JBColor(0x9ca7b8, 0x747a80));
  }

  @NotNull
  public static Color borderColor() {
    return JBColor.namedColor("EditorGroupsTabs.borderColor", UIUtil.CONTRAST_BORDER_COLOR);
  }

  @NotNull
  public static Color background() {
    return JBColor.namedColor("EditorGroupsTabs.background", new JBColor(0xECECEC, 0x3C3F41));
  }

  @NotNull
  public static Color hoverBackground() {
    return JBColor.namedColor("EditorGroupsTabs.hoverBackground",
      new JBColor(ColorUtil.withAlpha(Color.BLACK, .10),
        ColorUtil.withAlpha(Color.BLACK, .35)));
  }

  public static Color underlinedTabBackground() {
    return UIManager.getColor("EditorGroupsTabs.underlinedTabBackground");
  }

  @NotNull
  public static Color underlinedTabForeground() {
    return JBColor.namedColor("EditorGroupsTabs.underlinedTabForeground", UIUtil.getLabelForeground());
  }

  @NotNull
  public static Color hoverColor() {
    return JBColor.namedColor("EditorGroupsTabs.hoverColor",
      new JBColor(0xD9D9D9, 0x2E3133));
  }
}
