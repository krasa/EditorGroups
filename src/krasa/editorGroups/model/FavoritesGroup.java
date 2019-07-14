package krasa.editorGroups.model;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.TreeItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FavoritesGroup extends EditorGroup {
	public static final String ID_PREFIX = "Favorites: ";
	private List<VirtualFile> files;
	private final String name;

	public FavoritesGroup(String name, List<TreeItem<Pair<AbstractUrl, String>>> validBookmark, Project project, ProjectFileIndex projectFileIndex) {
		this.name = name;

		files = add(validBookmark, project, projectFileIndex);
	}

	private static List<VirtualFile> add(List<TreeItem<Pair<AbstractUrl, String>>> validBookmark, Project project, ProjectFileIndex projectFileIndex) {
		List<VirtualFile> files = new ArrayList<>();
		//fixes ConcurrentModificationException
		ArrayList<TreeItem<Pair<AbstractUrl, String>>> treeItems = new ArrayList<>(validBookmark);

		for (TreeItem<Pair<AbstractUrl, String>> pairTreeItem : treeItems) {
			Pair<AbstractUrl, String> data = pairTreeItem.getData();
			AbstractUrl first = data.first;
			Object[] path = first.createPath(project);
			if (path == null || path.length < 1 || path[0] == null) {
				continue;
			}
			Object element = path[0];
			if (element instanceof SmartPsiElementPointer) {
				add(projectFileIndex, ((SmartPsiElementPointer) element).getElement(), files);
			}

			if (element instanceof PsiElement) {
				add(projectFileIndex, (PsiElement) element, files);
			}
			add(pairTreeItem.getChildren(), project, projectFileIndex);
		}
		return files;
	}

	private static void add(ProjectFileIndex projectFileIndex, PsiElement element1, List<VirtualFile> files) {
		final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element1);
		if (virtualFile == null) return;
		if (virtualFile.isDirectory()) {
			iterateContentUnderDirectory(projectFileIndex, virtualFile, files);
		} else {
			files.add(virtualFile);
		}
	}

	private static boolean iterateContentUnderDirectory(ProjectFileIndex projectFileIndex, VirtualFile virtualFile, List<VirtualFile> files) {
		final ContentIterator contentIterator = fileOrDir -> {
			if (fileOrDir.isDirectory() && !fileOrDir.equals(virtualFile)) {
				iterateContentUnderDirectory(projectFileIndex, fileOrDir, files);
			} else if (!fileOrDir.isDirectory()) {
				files.add(fileOrDir);
			}
			return true;
		};

		return projectFileIndex.iterateContentUnderDirectory(virtualFile, contentIterator);
	}

	public VirtualFile getOwnerFile() {
		return files.get(0);
	}

	@NotNull
	@Override
	public String getId() {
		return ID_PREFIX + getTitle();
	}

	@Override
	public String getTitle() {
		return name;
	}


	@Override
	public String switchTitle(Project project) {
		return getTitle();
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Icon icon() {
		return AllIcons.Toolwindows.ToolWindowFavorites;
	}

	@Override
	public void invalidate() {

	}

	@Override
	public int size(Project project) {
		return files.size();
	}

	@Override
	public List<Link> getLinks(Project project) {
		ArrayList<Link> paths = new ArrayList<>(files.size());
		for (VirtualFile file : files) {
			paths.add(new VirtualFileLink(file));
		}
		return paths;
	}

	@Override
	public boolean isOwner(String ownerPath) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof FavoritesGroup) && Objects.equals(((FavoritesGroup) obj).getId(), this.getId());
	}

	@Override
	public boolean equalsVisually(Project project, EditorGroup group) {
		return super.equalsVisually(project, group) && files.equals(((FavoritesGroup) group).files);
	}

	@Override
	public String toString() {
		return "FavoritesGroup{" +
			"files=" + files +
			", name='" + name + '\'' +
			'}';
	}
}
