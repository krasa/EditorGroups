package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.Splitters;

public class PreviousInNewTabAction extends EditorGroupsAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		EditorGroupPanel panel = getEditorGroupPanel(anActionEvent);
		if (panel != null) {
			panel.previous(true, false, Splitters.NONE);
		}
	}
}
