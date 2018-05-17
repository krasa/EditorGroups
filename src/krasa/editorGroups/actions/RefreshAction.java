package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.EditorGroupPanel;

import javax.swing.*;

public class RefreshAction extends DumbAwareAction implements CustomComponentAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		Editor data = anActionEvent.getData(CommonDataKeys.EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_GROUPS_PANEL);
			if (panel != null) {
				panel.refresh(true);
			}
		}
	}

	@Override
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton refresh = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		presentation.setIcon(AllIcons.Actions.Refresh);
		return refresh;
	}

}
