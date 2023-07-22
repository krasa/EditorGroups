package krasa.editorGroups.gui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.support.CheckBoxWithColorChooser;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TabsColors {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(TabsColors.class);

  private JPanel root;

  private JCheckBox enabledCheckBox;

  private CheckBoxWithColorChooser mask;
  private JTextField opacity;

  private CheckBoxWithColorChooser darcula_mask;
  private JTextField darcula_opacity;
  private JButton darcula_opacityDefault;
  private JButton opacityDefault;
  private CheckBoxWithColorChooser tabBgColor;
  private CheckBoxWithColorChooser tabFgColor;
  private JPanel classic;
  private JPanel darcula;
  private JPanel ideTabs;

  public JPanel getRoot() {
    return root;
  }

  public TabsColors() {
    darcula.setVisible(UIUtil.isUnderDarcula());
    classic.setVisible(!UIUtil.isUnderDarcula());

    opacityDefault.addActionListener(e -> opacity.setText(String.valueOf(ApplicationConfiguration.Tabs.DEFAULT_OPACITY)));
    darcula_opacityDefault.addActionListener(e -> opacity.setText(String.valueOf(ApplicationConfiguration.Tabs.DEFAULT_DARCULA_OPACITY)));
    ideTabs.setVisible(false);
  }

  private void createUIComponents() {
    tabFgColor = new CheckBoxWithColorChooser("Default selected tab foreground color ", null);
    tabBgColor = new CheckBoxWithColorChooser("Default selected tab background color ", null);

    Dimension colorDimension = new JBDimension(30, 30);
    mask = new CheckBoxWithColorChooser(null, null, ApplicationConfiguration.Tabs.DEFAULT_MASK).setColorDimension(colorDimension);
    CheckBoxWithColorChooser defaultTabColor = new CheckBoxWithColorChooser(null, null, ApplicationConfiguration.Tabs.DEFAULT_TAB_COLOR).setColorDimension(colorDimension);

    darcula_mask = new CheckBoxWithColorChooser(null, null, ApplicationConfiguration.Tabs.DEFAULT_DARCULA_MASK).setColorDimension(colorDimension);
    CheckBoxWithColorChooser darcula_defaultTabColor = new CheckBoxWithColorChooser(null, null, ApplicationConfiguration.Tabs.DEFAULT_DARCULA_TAB_COLOR).setColorDimension(colorDimension);
  }

  public void setData(ApplicationConfiguration applicationConfiguration, ApplicationConfiguration.Tabs data) {
    tabBgColor.setColor(applicationConfiguration.getTabBgColorAsAWT());
    tabBgColor.setSelected(applicationConfiguration.isTabBgColorEnabled());

    tabFgColor.setColor(applicationConfiguration.getTabFgColorAsAWT());
    tabFgColor.setSelected(applicationConfiguration.isTabFgColorEnabled());


    enabledCheckBox.setSelected(data.isPatchPainter());

    mask.setColor(data.getMask());
    opacity.setText(String.valueOf(data.getOpacity()));

    darcula_mask.setColor(data.getDarcula_mask());
    darcula_opacity.setText(String.valueOf(data.getDarcula_opacity()));
  }

  public void getData(ApplicationConfiguration applicationConfiguration, ApplicationConfiguration.Tabs data) {
    applicationConfiguration.setTabBgColorAWT(tabBgColor.getColor());
    applicationConfiguration.setTabBgColorEnabled(tabBgColor.isSelected());

    applicationConfiguration.setTabFgColorAWT(tabFgColor.getColor());
    applicationConfiguration.setTabFgColorEnabled(tabFgColor.isSelected());


    data.setPatchPainter(enabledCheckBox.isSelected());

    data.setMask(mask.getColorAsRGB());
    data.setOpacity(opacity.getText());

    data.setDarcula_mask(darcula_mask.getColorAsRGB());
    data.setDarcula_opacity(darcula_opacity.getText());

    setData(applicationConfiguration, data);
  }


  public boolean isModified(ApplicationConfiguration applicationConfiguration, ApplicationConfiguration.Tabs data) {
    if (tabBgColor.isSelected() != applicationConfiguration.isTabBgColorEnabled()) return true;
    if (!Objects.equals(tabBgColor.getColor(), applicationConfiguration.getTabBgColorAsAWT())) return true;

    if (tabFgColor.isSelected() != applicationConfiguration.isTabFgColorEnabled()) return true;
    if (!Objects.equals(tabFgColor.getColor(), applicationConfiguration.getTabFgColorAsAWT())) return true;


    if (enabledCheckBox.isSelected() != data.isPatchPainter()) return true;


    if (!Objects.equals(mask.getColorAsRGB(), data.getMask())) return true;
    if (!Objects.equals(opacity.getText(), String.valueOf(data.getOpacity()))) return true;

    if (!Objects.equals(darcula_mask.getColorAsRGB(), data.getDarcula_mask())) return true;
    return !Objects.equals(darcula_opacity.getText(), String.valueOf(data.getDarcula_opacity()));
  }
}
