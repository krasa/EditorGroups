package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.PlatformIcons;
import krasa.editorGroups.EditorGroupManager;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static krasa.editorGroups.actions.PopupMenu.popupInvoked;

public class SwitchGroupAction extends QuickSwitchSchemeAction implements DumbAware, CustomComponentAction {

	private static final Logger LOG = Logger.getInstance(SwitchGroupAction.class);

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
		FileEditor data = dataContext.getData(PlatformDataKeys.FILE_EDITOR);
		EditorGroupPanel panel = null;
		if (data != null) {
			panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				fillCurrentFileGroups(project, defaultActionGroup, panel);
			}
		}
		fillOtherGroup(defaultActionGroup, panel, project);

		defaultActionGroup.add(new Separator());
		defaultActionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.OpenConfiguration"));
	}

	private void fillCurrentFileGroups(Project project, @NotNull DefaultActionGroup defaultActionGroup, EditorGroupPanel panel) {
		EditorGroup displayedGroup = panel.getDisplayedGroup();
		VirtualFile file = panel.getFile();
		EditorGroupManager manager = EditorGroupManager.getInstance(project);
		Collection<EditorGroup> groups = manager.getGroups(file);

		Handler refresh = refreshHandler(panel);

		defaultActionGroup.add(createAction(displayedGroup, new SameNameGroup(file.getNameWithoutExtension(), Collections.emptyList()), project, refresh, null));
		defaultActionGroup.add(createAction(displayedGroup, new FolderGroup(file.getParent().getCanonicalPath(), Collections.emptyList()), project, refresh, null));


		defaultActionGroup.add(new Separator("Groups for the current file"));
		for (EditorGroup g : groups) {
			defaultActionGroup.add(createAction(displayedGroup, g, project, refresh, null));
		}
		defaultActionGroup.add(new Separator("Other groups"));
	}

	private void fillOtherGroup(DefaultActionGroup defaultActionGroup, @Nullable EditorGroupPanel panel, Project project) {
		EditorGroupManager manager = EditorGroupManager.getInstance(project);
		EditorGroup displayedGroup = EditorGroup.EMPTY;
		Collection<EditorGroup> groups = Collections.emptyList();
		VirtualFile currentFile = null;

		if (panel != null) {
			displayedGroup = panel.getDisplayedGroup();
			currentFile = panel.getFile();
			groups = manager.getGroups(currentFile);
		}


		try {
			List<EditorGroupIndexValue> allGroups = manager.getAllGroups();
			for (EditorGroupIndexValue g : allGroups) {
//				if (currentFile != null && g.getId().equals(currentFile.getCanonicalPath())) {
//					continue;
//				}
				if (!groups.contains(g)) {
					defaultActionGroup.add(createAction(displayedGroup, g, project, otherGroupHandler(project), null));
				}
			}

		} catch (ProcessCanceledException | IndexNotReadyException e) {
			AnAction action = new AnAction("Indexing...") {
				@Override
				public void actionPerformed(AnActionEvent anActionEvent) {

				}
			};
			action.getTemplatePresentation().setEnabled(false);
			defaultActionGroup.add(action);
		}


		Collection<FavoritesGroup> favoritesGroups = manager.cache.getFavoritesGroups();
		if (!favoritesGroups.isEmpty()) {
			Separator favourites = new Separator("Favourites");
			defaultActionGroup.add(favourites);
			for (FavoritesGroup favoritesGroup : favoritesGroups) {
//				if (displayedGroup instanceof FavoritesGroup && displayedGroup.getTitle().equals(favoritesGroup.getTitle())) {
//					continue;
//				}
				defaultActionGroup.add(createAction(displayedGroup, favoritesGroup, project, otherGroupHandler(project), null));
			}
		}

	}

	@NotNull
	private Handler refreshHandler(EditorGroupPanel panel) {
		return new Handler() {
			@Override
			void run(EditorGroup groupLink) {
				panel.refresh(false, groupLink);
			}
		};
	}

	@NotNull
	private Handler otherGroupHandler(@Nullable Project project) {
		return new Handler() {
			@Override
			void run(EditorGroup editorGroup) {
				VirtualFile file = editorGroup.getFirstExistingFile(project);
				if (file != null) {
					EditorGroupManager.getInstance(project).open(file, editorGroup, false, true, null, 0);
				} else {
					String ownerPath = editorGroup.getOwnerPath();
					VirtualFile virtualFileByAbsolutePath = Utils.getVirtualFileByAbsolutePath(ownerPath);
					if (virtualFileByAbsolutePath != null) {
						EditorGroupManager.getInstance(project).open(virtualFileByAbsolutePath, editorGroup, false, true, null, 0);
					} else {
						if (LOG.isDebugEnabled())
							LOG.debug("opening failed, no file and not even owner exist " + editorGroup);
					}

					if (LOG.isDebugEnabled()) LOG.debug("opening failed, no file exists " + editorGroup);
				}
			}
		};
	}

	@NotNull
	private DumbAwareAction createAction(EditorGroup displayedGroup, EditorGroup groupLink, Project project, final Handler actionHandler, final Icon icon) {
		boolean isSelected = displayedGroup.equals(groupLink);
		String title = groupLink.switchTitle(project);
		String description = groupLink.getSwitchDescription();

		return new DumbAwareAction(title, description, isSelected ? PlatformIcons.CHECK_ICON_SELECTED : icon) {
			@Override
			public void actionPerformed(AnActionEvent e1) {
				actionHandler.run(groupLink);
			}
		};
	}

	abstract class Handler {
		abstract void run(EditorGroup groupLink);
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
				if (displayedGroup instanceof FolderGroup) {
					presentation.setIcon(AllIcons.Nodes.Folder);
				} else if (displayedGroup instanceof SameNameGroup) {
					presentation.setIcon(AllIcons.Actions.Copy);
				} else if (displayedGroup instanceof FavoritesGroup) {
					presentation.setIcon(AllIcons.Toolwindows.ToolWindowFavorites);
				} else {
					presentation.setIcon(AllIcons.Actions.GroupByModule);
				}
			}
		}

	}
}
