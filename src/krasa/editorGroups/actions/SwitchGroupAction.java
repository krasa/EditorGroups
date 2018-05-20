package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FolderGroup;
import krasa.editorGroups.support.Utils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;

public class SwitchGroupAction extends AnAction implements DumbAware, CustomComponentAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		final DefaultActionGroup actionGroup = new DefaultActionGroup();

		Editor data = e.getData(CommonDataKeys.EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				EditorGroup displayedGroup = panel.getDisplayedGroup();
				VirtualFile file = panel.getFile();

				Collection<EditorGroup> groups = EditorGroupManager.getInstance(e.getProject()).getGroups(file);
				for (EditorGroup group : groups) {
					boolean isSelected = displayedGroup.equals(group);
					if (group instanceof FolderGroup) {
						continue;
					}

					String ownerPath = group.getOwnerPath();
					String name = Utils.toPresentableName(ownerPath);

					String title = group.getPresentableTitle(e.getProject(), name);
					DumbAwareAction action = new DumbAwareAction(title, "Owner:" + ownerPath, isSelected ? PlatformIcons.CHECK_ICON_SELECTED : null) {
						@Override
						public void actionPerformed(AnActionEvent e) {
							panel.refresh(true, group);
						}
					};
					actionGroup.add(action);
				}
			}

		}

		InputEvent inputEvent = e.getInputEvent();
		int x = 0;
		int y = 0;
		if (inputEvent instanceof MouseEvent) {
			x = ((MouseEvent) inputEvent).getX();
			y = ((MouseEvent) inputEvent).getY();
		}

		ActionManager.getInstance().createActionPopupMenu("", actionGroup).getComponent().show(inputEvent.getComponent(), x, y);
	}

	@Override
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton refresh = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		presentation.setIcon(AllIcons.Actions.Module);
		return refresh;
	}
}
