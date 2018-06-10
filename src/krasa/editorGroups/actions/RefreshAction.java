package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.PopupHandler;
import krasa.editorGroups.EditorGroupPanel;

import javax.swing.*;
import java.awt.*;

public class RefreshAction extends DumbAwareAction implements CustomComponentAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		Document doc = getDocument(anActionEvent);
		if (doc != null) {
			FileDocumentManager.getInstance().saveDocument(doc);
		}
		
		FileEditor data = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				panel.refresh(true, null);
			}
		}
	}

	@Override
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton refresh = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		refresh.addMouseListener(new PopupHandler() {
			public void invokePopup(Component comp, int x, int y) {
				PopupMenu.popupInvoked(comp, x, y);
			}
		});
		presentation.setIcon(AllIcons.Actions.Refresh);

		return refresh;
	}

	private static Document getDocument(AnActionEvent e) {
		Editor editor = e.getData(CommonDataKeys.EDITOR);
		return editor != null ? editor.getDocument() : null;
	}
}
