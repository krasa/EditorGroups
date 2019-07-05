package krasa.editorGroups.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionMenuItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.BitUtil;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.Splitters;
import krasa.editorGroups.UniqueTabNameBuilder;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.Link;
import krasa.editorGroups.support.Notifications;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class SwitchFileAction extends QuickSwitchSchemeAction implements DumbAware {

	private static final Logger LOG = Logger.getInstance(SwitchFileAction.class);

	protected void showPopup(AnActionEvent e, ListPopup popup) {
		registerActions((ListPopupImpl) popup);
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

	private void registerActions(ListPopupImpl popup) {
		popup.registerAction("newTab", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), customAction(popup));
		popup.registerAction("newWindow", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), customAction(popup));
		popup.registerAction("verticalSplit", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), customAction(popup));
		popup.registerAction("horizontalSplit", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), customAction(popup));
	}

	@NotNull
	private AbstractAction customAction(ListPopupImpl popup) {
		return new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JList list = popup.getList();
				PopupFactoryImpl.ActionItem selectedValue = (PopupFactoryImpl.ActionItem) list.getSelectedValue();
				if (selectedValue != null) {
					AnAction action1 = selectedValue.getAction();
					action1.actionPerformed(new AnActionEvent(null, getDataContext(popup), myActionPlace, getTemplatePresentation(), ActionManager.getInstance(), e.getModifiers()));
					popup.closeOk(null);
				}
			}
		};
	}

	private DataContext getDataContext(ListPopupImpl popup) {
		DataContext dataContext = DataManager.getInstance().getDataContext(popup.getOwner());
		Project project = dataContext.getData(CommonDataKeys.PROJECT);
		if (project == null) {
			throw new IllegalStateException("Project is null for " + popup.getOwner());
		}
		return dataContext;
	}

	@Override
	protected void fillActions(Project project, @NotNull DefaultActionGroup defaultActionGroup, @NotNull DataContext dataContext) {
		try {
			FileEditor data = dataContext.getData(PlatformDataKeys.FILE_EDITOR);
			EditorGroupPanel panel = null;
			if (data != null) {
				panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
				if (panel != null) {
					String currentFile = panel.getFile().getPath();
					EditorGroup group = panel.getDisplayedGroup();

					List<Link> links = group.getLinks(project);
					UniqueTabNameBuilder uniqueTabNameBuilder = new UniqueTabNameBuilder(project);
					Map<Link, String> namesByPath = uniqueTabNameBuilder.getNamesByPath(links, null);

					for (Map.Entry<Link, String> link : namesByPath.entrySet()) {
						Link linkKey = link.getKey();
						defaultActionGroup.add(newAction(project, panel, currentFile, linkKey.getPath(), link.getValue()));
					}
				}
			}
		} catch (IndexNotReadyException e) {
			LOG.error("That should not happen", e);
		}
	}

	@NotNull
	private OpenFileAction newAction(Project project, EditorGroupPanel panel, String currentFile, String link, String text) {
		OpenFileAction action = new OpenFileAction(link, project, panel, text);
		if (link.equals(currentFile)) {
			action.getTemplatePresentation().setEnabled(false);
			action.getTemplatePresentation().setText(text + " - current", false);
			action.getTemplatePresentation().setIcon(null);
		}
		return action;
	}


	private static class OpenFileAction extends DumbAwareAction {
		private final String link;
		private final EditorGroupPanel panel;
		private final VirtualFile virtualFileByAbsolutePath;
		private final Project project;

		public OpenFileAction(String link, Project project, EditorGroupPanel panel, String text) {
			super(text, link, Utils.getFileIcon(link));
			this.link = link;
			this.panel = panel;
			VirtualFile virtualFileByAbsolutePath = Utils.getVirtualFileByAbsolutePath(link);
			this.virtualFileByAbsolutePath = virtualFileByAbsolutePath;
			this.project = project;
			getTemplatePresentation().setEnabled(virtualFileByAbsolutePath != null && virtualFileByAbsolutePath.exists());
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			if (virtualFileByAbsolutePath != null) {
				boolean tab = BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK);
				boolean window = BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK);
				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				instance.open(panel, virtualFileByAbsolutePath, null, window, tab, Splitters.from(e));
			} else {
				Notifications.warning("File not found " + link, null);
			}
		}
	}
}
