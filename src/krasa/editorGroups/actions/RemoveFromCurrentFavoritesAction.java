package krasa.editorGroups.actions;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.TreeItem;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.Splitters;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FavoritesGroup;
import krasa.editorGroups.support.Notifications;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

public class RemoveFromCurrentFavoritesAction extends EditorGroupsAction {

	public static final String ID = "krasa.editorGroups.actions.RemoveFromCurrentFavorites";
	private static final Logger LOG = Logger.getInstance(RemoveFromCurrentFavoritesAction.class);

	@Override
	public void actionPerformed(AnActionEvent e) {
		FavoritesGroup favoritesGroup = getFavoritesGroup(e);
		if (favoritesGroup != null) {
			String name = favoritesGroup.getName();
			VirtualFile[] selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
			if (selectedFiles == null || selectedFiles.length == 0) {
				return;
			}
			Set<VirtualFile> selected = new HashSet<>(Arrays.asList(selectedFiles));


			FavoritesManager favoritesManager = FavoritesManager.getInstance(e.getProject());
			List<TreeItem<Pair<AbstractUrl, String>>> urls = favoritesManager.getFavoritesListRootUrls(name);
			ArrayList<TreeItem<Pair<AbstractUrl, String>>> forRemoval = new ArrayList<>();
			filter(forRemoval, urls, e.getProject(), selected);

			if (forRemoval.isEmpty()) {
				fail(name, selected);
			} else {
				boolean b = urls.removeAll(forRemoval);
				if (!b) {
					fail(name, selected);
				}


				EditorGroupPanel editorGroupPanel = getEditorGroupPanel(e);
				if (selected.contains(editorGroupPanel.getFile())) {
					boolean next = editorGroupPanel.next(false, false, Splitters.NONE);
					if (!next) {
						editorGroupPanel.previous(false, false, Splitters.NONE);
					}
				}
				
				try {
					Method rootChanged = ReflectionUtil.getDeclaredMethod(FavoritesManager.class, "rootsChanged");
					if (rootChanged != null) { //TODO 2018
						rootChanged.invoke(favoritesManager);
					}
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
			}
		}

	}

	private void fail(String name, Set<VirtualFile> selected) {
		Notifications.warning("Unable to remove, probably the whole folder is favorited. File:" + selected.toString() + "', from '" + name, null);
	}


	private List<TreeItem<Pair<AbstractUrl, String>>> filter(List<TreeItem<Pair<AbstractUrl, String>>> items, List<TreeItem<Pair<AbstractUrl, String>>> validBookmark, Project project, Set<VirtualFile> selected) {
		for (TreeItem<Pair<AbstractUrl, String>> pairTreeItem : validBookmark) {
			Pair<AbstractUrl, String> data = pairTreeItem.getData();
			AbstractUrl first = data.first;
			Object[] path = first.createPath(project);
			if (path == null || path.length < 1 || path[0] == null) {
				continue;
			}
			Object element = path[0];
			if (element instanceof SmartPsiElementPointer) {
				add(items, pairTreeItem, ((SmartPsiElementPointer) element).getElement(), selected);
			}

			if (element instanceof PsiElement) {
				add(items, pairTreeItem, (PsiElement) element, selected);
			}


			filter(items, pairTreeItem.getChildren(), project, selected);
		}
		return items;
	}

	private void add(List<TreeItem<Pair<AbstractUrl, String>>> items, TreeItem<Pair<AbstractUrl, String>> pairTreeItem, PsiElement element1, Set<VirtualFile> selected) {
		final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element1);
		if (virtualFile == null) return;

		if (selected.contains(virtualFile)) {
			items.add(pairTreeItem);
		}
	}


	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();

		FavoritesGroup favoritesGroup = getFavoritesGroup(e);
		presentation.setVisible(favoritesGroup != null);
		if (favoritesGroup != null) {
			presentation.setText("Remove from Favorites - " + favoritesGroup.getName());
		}
		if (favoritesGroup != null) {
			presentation.setEnabled(isEnabled(e, favoritesGroup));
		}
	}

	@Nullable
	private FavoritesGroup getFavoritesGroup(AnActionEvent e) {
		EditorGroupPanel editorGroupPanel = null;
		FavoritesGroup data = EditorGroupPanel.FAVORITE_GROUP.getData(e.getDataContext());
		if (data != null) {
			return data;
		}
		FileEditorManagerEx instance = (FileEditorManagerEx) FileEditorManager.getInstance(getEventProject(e));
		EditorWindow currentWindow = instance.getCurrentWindow();
		if (currentWindow != null) {
			EditorWithProviderComposite editor = currentWindow.getSelectedEditor(true);
			if (editor != null) {
				FileEditor selectedEditor = editor.getSelectedEditorWithProvider().first;
				editorGroupPanel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
			}

		}

		FavoritesGroup favoritesGroup = null;
		if (editorGroupPanel != null) {
			EditorGroup displayedGroup = editorGroupPanel.getDisplayedGroup();
			if (displayedGroup instanceof FavoritesGroup) {
				favoritesGroup = (FavoritesGroup) displayedGroup;
			}
		}
		return favoritesGroup;
	}

	private boolean isEnabled(AnActionEvent e, FavoritesGroup favoritesGroup) {
		Project project = e.getProject();
		boolean enabled = true;
		DataContext dataContext = e.getDataContext();
		VirtualFile[] data1 = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
		if (data1 != null) {
			boolean contains = false;
			for (VirtualFile virtualFile : data1) {
				contains = favoritesGroup.containsLink(project, virtualFile);
				if (contains) {
					break;
				}
			}
			if (contains) {
				enabled = true;
			}
		}
		return enabled;
	}
}
