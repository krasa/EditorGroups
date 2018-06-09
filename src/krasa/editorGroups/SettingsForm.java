package krasa.editorGroups;

import com.intellij.openapi.diagnostic.Logger;
import krasa.editorGroups.support.CheckBoxWithColorChooser;

import javax.swing.*;
import java.util.Objects;

public class SettingsForm {

	private static final Logger LOG = Logger.getInstance(SettingsForm.class);


	private JPanel root;
	private JCheckBox byName;
	private JCheckBox byFolder;
	private JCheckBox autoSwitch;
	private JCheckBox hideEmpty;
	private JCheckBox showSize;
	private JCheckBox continuousScrolling;
	private JCheckBox latencyOverFlicker;
	private JCheckBox indexOnlyEditorGroupsFileCheckBox;
	private JCheckBox excludeEGroups;
	private CheckBoxWithColorChooser tabBgColor;
	private CheckBoxWithColorChooser tabFgColor;


	public JPanel getRoot() {
		return root;
	}

	public boolean isSettingsModified(ApplicationConfiguration.State data) {
		if (tabBgColor.isSelected() != data.isTabBgColorEnabled()) return true;
		if (!Objects.equals(tabBgColor.getColor(), data.getTabBgColorAsAWT())) return true;

		if (tabFgColor.isSelected() != data.isTabFgColorEnabled()) return true;
		if (!Objects.equals(tabFgColor.getColor(), data.getTabFgColorAsAWT())) return true;
		return isModified(data);
	}

	public void importFrom(ApplicationConfiguration.State state) {
		setData(state);
		tabBgColor.setColor(state.getTabBgColorAsAWT());
		tabBgColor.setSelected(state.isTabBgColorEnabled());

		tabFgColor.setColor(state.getTabFgColorAsAWT());
		tabFgColor.setSelected(state.isTabFgColorEnabled());
	}

	public void apply() {
		if (LOG.isDebugEnabled()) LOG.debug("apply " + "");
		ApplicationConfiguration.State state = ApplicationConfiguration.state();
		getData(state);
		state.setTabBgColor(tabBgColor.getColor());
		state.setTabBgColorEnabled(tabBgColor.isSelected());

		state.setTabFgColor(tabFgColor.getColor());
		state.setTabFgColorEnabled(tabFgColor.isSelected());
	}


	private void createUIComponents() {
		tabFgColor = new CheckBoxWithColorChooser("Custom selected tab foreground color ");
		tabBgColor = new CheckBoxWithColorChooser("Custom selected tab background color ");
	}

	public void setData(ApplicationConfiguration.State data) {
		byName.setSelected(data.isAutoSameName());
		showSize.setSelected(data.isShowSize());
		hideEmpty.setSelected(data.isHideEmpty());
		autoSwitch.setSelected(data.isForceSwitch());
		byFolder.setSelected(data.isAutoFolders());
		continuousScrolling.setSelected(data.isContinuousScrolling());
		latencyOverFlicker.setSelected(data.isPreferLatencyOverFlicker());
		indexOnlyEditorGroupsFileCheckBox.setSelected(data.isIndexOnlyEditorGroupsFiles());
		excludeEGroups.setSelected(data.isExcludeEditorGroupsFiles());
	}

	public void getData(ApplicationConfiguration.State data) {
		data.setAutoSameName(byName.isSelected());
		data.setShowSize(showSize.isSelected());
		data.setHideEmpty(hideEmpty.isSelected());
		data.setForceSwitch(autoSwitch.isSelected());
		data.setAutoFolders(byFolder.isSelected());
		data.setContinuousScrolling(continuousScrolling.isSelected());
		data.setPreferLatencyOverFlicker(latencyOverFlicker.isSelected());
		data.setIndexOnlyEditorGroupsFiles(indexOnlyEditorGroupsFileCheckBox.isSelected());
		data.setExcludeEditorGroupsFiles(excludeEGroups.isSelected());
	}

	public boolean isModified(ApplicationConfiguration.State data) {
		if (byName.isSelected() != data.isAutoSameName()) return true;
		if (showSize.isSelected() != data.isShowSize()) return true;
		if (hideEmpty.isSelected() != data.isHideEmpty()) return true;
		if (autoSwitch.isSelected() != data.isForceSwitch()) return true;
		if (byFolder.isSelected() != data.isAutoFolders()) return true;
		if (continuousScrolling.isSelected() != data.isContinuousScrolling()) return true;
		if (latencyOverFlicker.isSelected() != data.isPreferLatencyOverFlicker()) return true;
		if (indexOnlyEditorGroupsFileCheckBox.isSelected() != data.isIndexOnlyEditorGroupsFiles()) return true;
		if (excludeEGroups.isSelected() != data.isExcludeEditorGroupsFiles()) return true;
		return false;
	}

}
