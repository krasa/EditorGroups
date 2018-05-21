package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.PlatformIcons;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.AutoGroup;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FolderGroup;
import krasa.editorGroups.model.SameNameGroup;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;

import static krasa.editorGroups.actions.PopupMenu.popupInvoked;

public class SwitchGroupAction extends QuickSwitchSchemeAction implements DumbAware, CustomComponentAction {

	protected void showPopup(AnActionEvent e, ListPopup popup) {
		Project project = e.getProject();
		if (project != null) {
			InputEvent inputEvent = e.getInputEvent();
			if (inputEvent instanceof MouseEvent) {
				popup.showUnderneathOf(inputEvent.getComponent());
			} else {
				popup.showCenteredInCurrentWindow(project);
			}
		} else {
			popup.showInBestPositionFor(e.getDataContext());
		}
	}

	@Override
	protected void fillActions(Project project, @NotNull DefaultActionGroup defaultActionGroup, @NotNull DataContext dataContext) {
		Editor data = dataContext.getData(CommonDataKeys.EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				fillGroup(defaultActionGroup, panel, project);
			}
		}
	}

	private void fillGroup(DefaultActionGroup actionGroup, EditorGroupPanel panel, Project project) {
		EditorGroup displayedGroup = panel.getDisplayedGroup();
		VirtualFile file = panel.getFile();
		Collection<EditorGroup> groups = EditorGroupManager.getInstance(project).getGroups(file);

		actionGroup.add(createAction(panel, displayedGroup, new SameNameGroup(file.getNameWithoutExtension(), Collections.emptyList(), Collections.emptyList()), project));
		actionGroup.add(createAction(panel, displayedGroup, new FolderGroup(file.getParent().getCanonicalPath(), Collections.emptyList(), Collections.emptyList()), project));
		for (EditorGroup group : groups) {
			if (group instanceof AutoGroup) {
				continue;
			}
			actionGroup.add(createAction(panel, displayedGroup, group, project));
		}
	}

	@NotNull
	private DumbAwareAction createAction(EditorGroupPanel panel, EditorGroup displayedGroup, EditorGroup groupLink, Project project) {
		boolean isSelected = displayedGroup.equals(groupLink);
		String description = null;
		String title;


		String ownerPath = groupLink.getOwnerPath();
		String name = Utils.toPresentableName(ownerPath);

		title = groupLink.getPresentableTitle(project, name);
		description = "Owner:" + ownerPath;


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
		presentation.setIcon(AllIcons.Actions.GroupByModule);
		button.addMouseListener(new PopupHandler() {
			public void invokePopup(Component comp, int x, int y) {
				popupInvoked(comp, x, y);
			}
		});
		return button;
	}

	@Override
	public void update(@NotNull AnActionEvent e) {
		super.update(e);
		Presentation presentation = e.getPresentation();
		Editor data = e.getData(CommonDataKeys.EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				EditorGroup displayedGroup = panel.getDisplayedGroup();
				if (displayedGroup instanceof FolderGroup) {
					presentation.setIcon(AllIcons.Nodes.Folder);
				} else if (displayedGroup instanceof SameNameGroup) {
					presentation.setIcon(AllIcons.Actions.Copy);
				} else {
					presentation.setIcon(AllIcons.Actions.GroupByModule);
				}
			}
		}

	}
}
