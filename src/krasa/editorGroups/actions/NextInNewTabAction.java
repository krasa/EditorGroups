package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.EditorGroupPanel;

public class NextInNewTabAction extends DumbAwareAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {   
		Editor data = anActionEvent.getData(CommonDataKeys.EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data	.getUserData(EditorGroupPanel.EDITOR_GROUPS_PANEL);
			if (panel != null) {
				panel.next(true, false);
			}
		}

	}
}
