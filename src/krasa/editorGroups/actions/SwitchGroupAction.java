package krasa.editorGroups.actions;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionMenuItem;
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
import krasa.editorGroups.*;
import krasa.editorGroups.icons.MyIcons;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static krasa.editorGroups.actions.PopupMenu.popupInvoked;

public class SwitchGroupAction extends QuickSwitchSchemeAction implements DumbAware, CustomComponentAction {

	private static final Logger LOG = Logger.getInstance(SwitchGroupAction.class);

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
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton button = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		presentation.setIcon(MyIcons.groupBy);
		button.addMouseListener(new PopupHandler() {
			public void invokePopup(Component comp, int x, int y) {
				popupInvoked(comp, x, y);
			}
		});
		return button;
	}


	@Override
	protected void fillActions(Project project, @NotNull DefaultActionGroup defaultActionGroup, @NotNull DataContext dataContext) {
		try {
			FileEditor data = dataContext.getData(PlatformDataKeys.FILE_EDITOR);
			EditorGroupPanel panel = null;
			EditorGroup displayedGroup = EditorGroup.EMPTY;
			List<EditorGroup> editorGroups = Collections.emptyList();
			DefaultActionGroup tempGroup = new DefaultActionGroup();
			VirtualFile file = null;


			if (data != null) {
				panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
				if (panel != null) {
					file = panel.getFile();
					displayedGroup = panel.getDisplayedGroup();

					defaultActionGroup.add(createAction(displayedGroup, new SameNameGroup(file.getNameWithoutExtension(), Collections.emptyList()), project, refreshHandler(panel)));
					defaultActionGroup.add(createAction(displayedGroup, new FolderGroup(file.getParent().getCanonicalPath(), Collections.emptyList()), project, refreshHandler(panel)));


					editorGroups = fillCurrentFileGroups(project, tempGroup, panel, file);
				}
			}

			addBookmarkGroup(project, defaultActionGroup, panel, displayedGroup, file);
			fillOtherGroup(tempGroup, editorGroups, displayedGroup, project);
			fillFavorites(tempGroup, project, editorGroups, displayedGroup);


			if (ApplicationConfiguration.state().isGroupSwitchGroupAction()) {
				defaultActionGroup.addAll(tempGroup.getChildActionsOrStubs());
			} else {
				AnAction[] childActionsOrStubs = tempGroup.getChildActionsOrStubs();
				List<AnAction> list = Arrays.stream(childActionsOrStubs)
					.filter(anAction -> !(anAction instanceof Separator))
					.sorted(new Comparator<AnAction>() {
						@Override
						public int compare(AnAction o1, AnAction o2) {
							return o1.getTemplatePresentation().getText().compareToIgnoreCase(o2.getTemplatePresentation().getText());
						}
					}).collect(Collectors.toList());
				defaultActionGroup.add(new Separator());
				defaultActionGroup.addAll(list);
			}

			defaultActionGroup.add(new Separator());
			defaultActionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.OpenConfiguration"));
		} catch (IndexNotReadyException e) {
			LOG.error("That should not happen", e);
		}
	}

	private void addBookmarkGroup(Project project, @NotNull DefaultActionGroup defaultActionGroup, EditorGroupPanel panel, EditorGroup displayedGroup, VirtualFile file) {
		BookmarkGroup bookmarkGroup = ExternalGroupProvider.getInstance(project).getBookmarkGroup();
		DumbAwareAction action = createAction(displayedGroup, bookmarkGroup, project, new Handler() {
			@Override
			void run(EditorGroup groupLink) {
				if (panel != null && file != null && bookmarkGroup.containsLink(project, file.getCanonicalPath())) {
					refreshHandler(panel).run(bookmarkGroup);
				} else {
					otherGroupHandler(project).run(bookmarkGroup);
				}
			}
		});
		if (bookmarkGroup.size(project) == 0) {
			action.getTemplatePresentation().setEnabled(false);
			action.getTemplatePresentation().setText(bookmarkGroup.getName() + " - empty");
		}
		defaultActionGroup.add(action);
	}

	private List<EditorGroup> fillCurrentFileGroups(Project project, @NotNull DefaultActionGroup group, EditorGroupPanel panel, VirtualFile file) {
		EditorGroup displayedGroup = panel.getDisplayedGroup();
		EditorGroupManager manager = EditorGroupManager.getInstance(project);
		List<EditorGroup> groups = manager.getGroups(file);

		group.add(new Separator("Groups for the current file"));

		for (EditorGroup g: groups) {
			group.add(createAction(displayedGroup, g, project, refreshHandler(panel)));
		}
		return groups;
	}

	private void fillOtherGroup(DefaultActionGroup group, List<EditorGroup> currentGroups, EditorGroup displayedGroup, Project project) {
		EditorGroupManager manager = EditorGroupManager.getInstance(project);

		group.add(new Separator("Other groups"));

		try {
			List<EditorGroupIndexValue> allGroups = manager.getAllGroups();
			for (EditorGroupIndexValue g: allGroups) {
				if (!((Collection<EditorGroup>) currentGroups).contains(g)) {
					group.add(createAction(displayedGroup, g, project, otherGroupHandler(project)));
				}
			}

		} catch (ProcessCanceledException | IndexNotReadyException e) {
			AnAction action = new AnAction("Indexing...") {
				@Override
				public void actionPerformed(AnActionEvent anActionEvent) {

				}
			};
			action.getTemplatePresentation().setEnabled(false);
			group.add(action);
		}


	}

	private void fillFavorites(DefaultActionGroup defaultActionGroup, Project project, List<EditorGroup> editorGroups, EditorGroup displayedGroup) {
		Collection<FavoritesGroup> favoritesGroups = ExternalGroupProvider.getInstance(project).getFavoritesGroups();

		Set<String> alreadyDisplayedFavourites = new HashSet<>();
		for (EditorGroup group: editorGroups) {
			if (group instanceof FavoritesGroup) {
				alreadyDisplayedFavourites.add(((FavoritesGroup) group).getName());
			}
		}

		if (!favoritesGroups.isEmpty()) {
			Separator favourites = new Separator("Favourites");
			defaultActionGroup.add(favourites);
			for (FavoritesGroup favoritesGroup: favoritesGroups) {
				if (!alreadyDisplayedFavourites.contains(favoritesGroup.getName())) {
					defaultActionGroup.add(createAction(displayedGroup, favoritesGroup, project, otherGroupHandler(project)));
				}
			}
		}
	}

	@NotNull
	private Handler refreshHandler(@NotNull EditorGroupPanel panel) {
		return new Handler() {
			@Override
			void run(EditorGroup editorGroup) {
				LOG.debug("switching group");
				panel.refresh(false, editorGroup);
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
					EditorGroupManager.getInstance(project).open(file, false, true, Splitters.NONE, editorGroup, null);
				} else {
					String ownerPath = editorGroup.getOwnerPath();
					VirtualFile virtualFileByAbsolutePath = Utils.getVirtualFileByAbsolutePath(ownerPath);
					if (virtualFileByAbsolutePath != null) {
						EditorGroupManager.getInstance(project).open(virtualFileByAbsolutePath, false, true, Splitters.NONE, editorGroup, null);
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
	private DumbAwareAction createAction(EditorGroup displayedGroup, EditorGroup groupLink, Project project, final Handler actionHandler) {
		boolean isSelected = displayedGroup.equals(groupLink);
		String title = groupLink.switchTitle(project);
		String description = groupLink.getSwitchDescription();
		if (isSelected) {
			title += " - current";
		}
		DumbAwareAction dumbAwareAction = new DumbAwareAction(title, description, groupLink.icon()) {
			@Override
			public void actionPerformed(AnActionEvent e1) {
				actionHandler.run(groupLink);
			}
		};
		dumbAwareAction.getTemplatePresentation().setEnabled(!isSelected);
		return dumbAwareAction;
	}

	abstract class Handler {
		abstract void run(EditorGroup groupLink);
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
}
