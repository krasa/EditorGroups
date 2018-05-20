package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.PlatformIcons;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FolderGroup;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;

import static krasa.editorGroups.actions.RefreshAction.popupInvoked;

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
				actionGroup.add(createAction(e, panel, displayedGroup, new FolderGroup(file.getParent().getCanonicalPath(), Collections.emptyList(), Collections.emptyList())));
				for (EditorGroup group : groups) {
					if (group instanceof FolderGroup) {
						continue;
					}
					actionGroup.add(createAction(e, panel, displayedGroup, group));
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

	@NotNull
	private DumbAwareAction createAction(AnActionEvent e, EditorGroupPanel panel, EditorGroup displayedGroup, EditorGroup groupLink) {
		boolean isSelected = displayedGroup.equals(groupLink) || (displayedGroup instanceof FolderGroup && groupLink instanceof FolderGroup);
		String description = null;
		String title;


		if (groupLink instanceof FolderGroup) {
			title = "Current folder";
		} else {
			String ownerPath = groupLink.getOwnerPath();
			String name = Utils.toPresentableName(ownerPath);

			title = groupLink.getPresentableTitle(e.getProject(), name);
			description = "Owner:" + ownerPath;
		}


		return new DumbAwareAction(title, description, isSelected ? PlatformIcons.CHECK_ICON_SELECTED : null) {
			@Override
			public void actionPerformed(AnActionEvent e1) {
				panel.refresh(false, groupLink);
			}
		};
	}

	@Override
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton button = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		presentation.setIcon(AllIcons.Actions.Module);
		button.addMouseListener(new PopupHandler() {
			public void invokePopup(Component comp, int x, int y) {
				popupInvoked(comp, x, y);
			}
		});
		return button;
	}

}
