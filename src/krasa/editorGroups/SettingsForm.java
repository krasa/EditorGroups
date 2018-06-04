package krasa.editorGroups;

import javax.swing.*;

public class SettingsForm {
	private JPanel root;
	private JCheckBox byName;
	private JCheckBox byFolder;
	private JCheckBox autoSwitch;
	private JCheckBox hideEmpty;
	private JCheckBox showSize;


	public JPanel getRoot() {
		return root;
	}

	public boolean isSettingsModified(ApplicationConfiguration.State state) {
		return isModified(state);
	}

	public void importFrom(ApplicationConfiguration.State state) {
		setData(state);
	}

	public void apply() {
		System.out.println("apply " + "");
		ApplicationConfiguration.State state = ApplicationConfiguration.state();
		getData(state);
	}


	/**
	 * GENERATED
	 */
	public void setData(ApplicationConfiguration.State data) {
		byName.setSelected(data.isAutoSameName());
		showSize.setSelected(data.isShowSize());
		hideEmpty.setSelected(data.isHideEmpty());
		autoSwitch.setSelected(data.isForceSwitch());
		byFolder.setSelected(data.isAutoFolders());
	}

	/**
	 * GENERATED
	 */
	public void getData(ApplicationConfiguration.State data) {
		data.setAutoSameName(byName.isSelected());
		data.setShowSize(showSize.isSelected());
		data.setHideEmpty(hideEmpty.isSelected());
		data.setForceSwitch(autoSwitch.isSelected());
		data.setAutoFolders(byFolder.isSelected());
	}

	/**
	 * GENERATED
	 */
	public boolean isModified(ApplicationConfiguration.State data) {
		if (byName.isSelected() != data.isAutoSameName()) return true;
		if (showSize.isSelected() != data.isShowSize()) return true;
		if (hideEmpty.isSelected() != data.isHideEmpty()) return true;
		if (autoSwitch.isSelected() != data.isForceSwitch()) return true;
		if (byFolder.isSelected() != data.isAutoFolders()) return true;
		return false;
	}

	private void createUIComponents() {
	}

}
