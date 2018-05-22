package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.EditorGroupPanel;

public class NextInNewWindowAction extends DumbAwareAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		FileEditor data = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				panel.next(true, true);
			}
		}

	}
}
