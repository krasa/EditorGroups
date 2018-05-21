package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import krasa.editorGroups.ApplicationConfiguration;

public class ToggleAutoFolderGroupsAction extends ToggleAction implements DumbAware {

	@Override
	public boolean isSelected(AnActionEvent e) {
		return ApplicationConfiguration.getInstance().getState().autoFolders;
	}

	@Override
	public void setSelected(AnActionEvent e, boolean state) {
		ApplicationConfiguration.getInstance().getState().autoFolders = state;
	}
}
