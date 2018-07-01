package krasa.editorGroups.actions;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.ActionMenuItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.BitUtil;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Notifications;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class SwitchFileAction extends QuickSwitchSchemeAction implements DumbAware {

	private static final Logger LOG = Logger.getInstance(SwitchFileAction.class);

	protected void showPopup(AnActionEvent e, ListPopup popup) {
		Project project = e.getProject();
		if (project != null) {
			InputEvent inputEvent = e.getInputEvent();
			if (inputEvent instanceof MouseEvent) {
				Component component = inputEvent.getComponent();
				if (component instanceof ActionMenuItem) { //from popup menu
					popup.showInBestPositionFor(e.getDataContext());
				} else {
					popup.showUnderneathOf(component);
				}
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
						defaultActionGroup.add(newAction(project, panel, currentFile, link));
					}
				}
			}
		} catch (IndexNotReadyException e) {
			LOG.error("That should not happen", e);
		}
	}

	@NotNull
	private OpenFileAction newAction(Project project, EditorGroupPanel panel, String currentFile, String link) {
		OpenFileAction action = new OpenFileAction(link, project, panel);
		if (link.equals(currentFile)) {
			action.getTemplatePresentation().setEnabled(false);
			action.getTemplatePresentation().setText(Utils.toPresentableName(link) + " - current", false);
			action.getTemplatePresentation().setIcon(null);
		}
		return action;
	}


	private static class OpenFileAction extends DumbAwareAction {
		private final String link;
		private final EditorGroupPanel panel;
		private final VirtualFile virtualFileByAbsolutePath;
		private final Project project;

		public OpenFileAction(String link, Project project, EditorGroupPanel panel) {
			super(Utils.toPresentableName(link), link, Utils.getFileIcon(link));
			this.link = link;
			this.panel = panel;
			VirtualFile virtualFileByAbsolutePath = Utils.getVirtualFileByAbsolutePath(link);
			this.virtualFileByAbsolutePath = virtualFileByAbsolutePath;
			this.project = project;
			getTemplatePresentation().setEnabled(virtualFileByAbsolutePath.exists());
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			if (virtualFileByAbsolutePath != null) {
				boolean tab = BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK);
				boolean window = BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK);
				boolean split = BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK);
				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				instance.open(panel, virtualFileByAbsolutePath, window, tab, split);
			} else {
				Notifications.warning("File not found " + link, null);
			}
		}
	}
}
