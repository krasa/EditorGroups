package krasa.editorGroups.gui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import krasa.editorGroups.ApplicationConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class SettingsForm {

	private static final Logger LOG = Logger.getInstance(SettingsForm.class);


	private JPanel root;
	private JCheckBox byName;
	private JCheckBox byFolder;
	private JCheckBox autoSwitch;
	private JCheckBox hideEmpty;
	private JCheckBox showSize;
	private JCheckBox continuousScrolling;
	private JCheckBox initializeSynchronously;
	private JCheckBox indexOnlyEditorGroupsFileCheckBox;
	private JCheckBox excludeEGroups;
	private JPanel tabColors;
	private JCheckBox rememberLastGroup;
	/**
	 * Group 'Switch Editor Group' action's list by type and the current file
	 */
	private JCheckBox groupSwitchGroupAction;
	private JPanel modelsPanel;
	private JCheckBox selectRegexGroup;
	private JTextField groupSizeLimit;
	private JTextField tabSizeLimit;
	private JCheckBox showPanel;
	private TabsColors tabsColors;

	private RegexTable regexTable;

	public JPanel getRoot() {
		return root;
	}

	public SettingsForm() {
		regexTable = new RegexTable();
		modelsPanel.add(
			ToolbarDecorator.createDecorator(regexTable)
				.setAddAction(new AnActionButtonRunnable() {
					@Override
					public void run(AnActionButton button) {
						regexTable.addAlias();
					}
				}).setRemoveAction(new AnActionButtonRunnable() {
				@Override
				public void run(AnActionButton button) {
					regexTable.removeSelectedAliases();
				}
			}).setEditAction(new AnActionButtonRunnable() {
				@Override
				public void run(AnActionButton button) {
					regexTable.editAlias();
				}
			}).setMoveUpAction(new AnActionButtonRunnable() {
				@Override
				public void run(AnActionButton anActionButton) {
					regexTable.moveUp();
				}
			}).setMoveDownAction(new AnActionButtonRunnable() {
				@Override
				public void run(AnActionButton anActionButton) {
					regexTable.moveDown();
				}
			}).createPanel(), BorderLayout.CENTER);

		new DoubleClickListener() {
			@Override
			protected boolean onDoubleClick(MouseEvent e) {
				return regexTable.editAlias();
			}
		}.installOn(regexTable);


	}

	public boolean isSettingsModified(ApplicationConfiguration data) {
		if (tabsColors.isModified(data, data.getTabs())) return true;
		if (regexTable.isModified(data)) return true;
		return isModified(data);
	}

	public void importFrom(ApplicationConfiguration data) {
		setData(data);
		tabsColors.setData(data, data.getTabs());
		regexTable.reset(data);
	}

	public void apply() {
		if (LOG.isDebugEnabled()) LOG.debug("apply " + "");
		ApplicationConfiguration data = ApplicationConfiguration.state();

		getData(data);
		regexTable.commit(data);
		tabsColors.getData(data, data.getTabs());
	}


	private void createUIComponents() {
		tabsColors = new TabsColors();
		tabColors = tabsColors.getRoot();
	}

	public void setData(ApplicationConfiguration data) {
		initializeSynchronously.setSelected(data.isInitializeSynchronously());
		indexOnlyEditorGroupsFileCheckBox.setSelected(data.isIndexOnlyEditorGroupsFiles());
		groupSizeLimit.setText(data.getGroupSizeLimit());
		tabSizeLimit.setText(data.getTabSizeLimit());
		byName.setSelected(data.isAutoSameName());
		autoSwitch.setSelected(data.isForceSwitch());
		byFolder.setSelected(data.isAutoFolders());
		selectRegexGroup.setSelected(data.isSelectRegexGroup());
		rememberLastGroup.setSelected(data.isRememberLastGroup());
		hideEmpty.setSelected(data.isHideEmpty());
		showSize.setSelected(data.isShowSize());
		continuousScrolling.setSelected(data.isContinuousScrolling());
		groupSwitchGroupAction.setSelected(data.isGroupSwitchGroupAction());
		excludeEGroups.setSelected(data.isExcludeEditorGroupsFiles());
		showPanel.setSelected(data.isShowPanel());
	}

	public void getData(ApplicationConfiguration data) {
		data.setInitializeSynchronously(initializeSynchronously.isSelected());
		data.setIndexOnlyEditorGroupsFiles(indexOnlyEditorGroupsFileCheckBox.isSelected());
		data.setGroupSizeLimit(groupSizeLimit.getText());
		data.setTabSizeLimit(tabSizeLimit.getText());
		data.setAutoSameName(byName.isSelected());
		data.setForceSwitch(autoSwitch.isSelected());
		data.setAutoFolders(byFolder.isSelected());
		data.setSelectRegexGroup(selectRegexGroup.isSelected());
		data.setRememberLastGroup(rememberLastGroup.isSelected());
		data.setHideEmpty(hideEmpty.isSelected());
		data.setShowSize(showSize.isSelected());
		data.setContinuousScrolling(continuousScrolling.isSelected());
		data.setGroupSwitchGroupAction(groupSwitchGroupAction.isSelected());
		data.setExcludeEditorGroupsFiles(excludeEGroups.isSelected());
		data.setShowPanel(showPanel.isSelected());
	}

	public boolean isModified(ApplicationConfiguration data) {
		if (initializeSynchronously.isSelected() != data.isInitializeSynchronously()) return true;
		if (indexOnlyEditorGroupsFileCheckBox.isSelected() != data.isIndexOnlyEditorGroupsFiles()) return true;
		if (groupSizeLimit.getText() != null ? !groupSizeLimit.getText().equals(data.getGroupSizeLimit()) : data.getGroupSizeLimit() != null)
			return true;
		if (tabSizeLimit.getText() != null ? !tabSizeLimit.getText().equals(data.getTabSizeLimit()) : data.getTabSizeLimit() != null)
			return true;
		if (byName.isSelected() != data.isAutoSameName()) return true;
		if (autoSwitch.isSelected() != data.isForceSwitch()) return true;
		if (byFolder.isSelected() != data.isAutoFolders()) return true;
		if (selectRegexGroup.isSelected() != data.isSelectRegexGroup()) return true;
		if (rememberLastGroup.isSelected() != data.isRememberLastGroup()) return true;
		if (hideEmpty.isSelected() != data.isHideEmpty()) return true;
		if (showSize.isSelected() != data.isShowSize()) return true;
		if (continuousScrolling.isSelected() != data.isContinuousScrolling()) return true;
		if (groupSwitchGroupAction.isSelected() != data.isGroupSwitchGroupAction()) return true;
		if (excludeEGroups.isSelected() != data.isExcludeEditorGroupsFiles()) return true;
		if (showPanel.isSelected() != data.isShowPanel()) return true;
		return false;
	}
}
