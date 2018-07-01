package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import krasa.editorGroups.EditorGroupPanel;

public class NextInNewWindowAction extends EditorGroupsAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		EditorGroupPanel panel = getEditorGroupPanel(anActionEvent);
		if (panel != null) {
			panel.next(true, true, false);
		}

	}
}
