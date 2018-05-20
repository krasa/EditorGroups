package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.PopupHandler;
import krasa.editorGroups.EditorGroupPanel;

import javax.swing.*;
import java.awt.*;

public class RefreshAction extends DumbAwareAction implements CustomComponentAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		Editor data = anActionEvent.getData(CommonDataKeys.EDITOR);
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
				popupInvoked(comp, x, y);
			}
		});
		presentation.setIcon(AllIcons.Actions.Refresh);

		return refresh;
	}

	public static void popupInvoked(Component component, int x, int y) {
		DefaultActionGroup group = new DefaultActionGroup();
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Next"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Previous"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Refresh"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ReindexThisFile"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Reindex"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleFolderEditorGroups"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleForce"));
		ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
		menu.getComponent().show(component, x, y);
	}
}
