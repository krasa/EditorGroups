package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.BitUtil;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Notifications;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static krasa.editorGroups.actions.PopupMenu.popupInvoked;

public class SwitchFileAction extends QuickSwitchSchemeAction implements DumbAware, CustomComponentAction {

	private static final Logger LOG = Logger.getInstance(SwitchFileAction.class);

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
		try {
			FileEditor data = dataContext.getData(PlatformDataKeys.FILE_EDITOR);
			EditorGroupPanel panel = null;
			if (data != null) {
				panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
				if (panel != null) {
					String currentFile = panel.getFile().getCanonicalPath();
					EditorGroup group = panel.getDisplayedGroup();
					for (String link: group.getLinks(project)) {
						OpenFileAction action = new OpenFileAction(link, project, group, data);
						if (link.equals(currentFile)) {
							action.getTemplatePresentation().setEnabled(false);
							action.getTemplatePresentation().setText(Utils.toPresentableName(link) + " - current", false);
							action.getTemplatePresentation().setIcon(null);
						}
						defaultActionGroup.add(action);
					}
				}
			}
		} catch (IndexNotReadyException e) {
			LOG.error("That should not happen", e);
		}
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
		FileEditor data = e.getData(PlatformDataKeys.FILE_EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				EditorGroup displayedGroup = panel.getDisplayedGroup();
				if (displayedGroup == EditorGroup.EMPTY) {
					EditorGroup toBeRendered = panel.getToBeRendered();
					if (toBeRendered != null) {
						displayedGroup = toBeRendered; //to remove flicker when switching
					}
				}
				presentation.setIcon(displayedGroup.icon());
			}
		}
	}

	private static class OpenFileAction extends DumbAwareAction {
		private final String link;
		private final VirtualFile virtualFileByAbsolutePath;
		private final Project project;
		private final EditorGroup group;
		private final FileEditor data;

		public OpenFileAction(String link, Project project, EditorGroup group, FileEditor data) {
			super(Utils.toPresentableName(link), link, Utils.getFileIcon(link));
			this.link = link;
			VirtualFile virtualFileByAbsolutePath = Utils.getVirtualFileByAbsolutePath(link);
			this.virtualFileByAbsolutePath = virtualFileByAbsolutePath;
			this.project = project;
			this.group = group;
			this.data = data;
			getTemplatePresentation().setEnabled(virtualFileByAbsolutePath.exists());
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			if (virtualFileByAbsolutePath != null) {
				boolean tab = BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK);
				boolean window = BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK);
				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				instance.open(virtualFileByAbsolutePath, window, tab, group, data.getFile());
			} else {
				Notifications.warning("File not found " + link, null);
			}
		}
	}
}
