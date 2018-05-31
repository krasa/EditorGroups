package krasa.editorGroups.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FavoritesGroup implements EditorGroup {
	public static final String OWNER_PREFIX = "Bookmark: ";
	private final List<VirtualFile> files = new ArrayList<>();
	private final String name;

	public FavoritesGroup(String name, List<TreeItem<Pair<AbstractUrl, String>>> validBookmark, Project project, ProjectFileIndex projectFileIndex) {
		this.name = name;


		for (TreeItem<Pair<AbstractUrl, String>> pairTreeItem : validBookmark) {
			Pair<AbstractUrl, String> data = pairTreeItem.getData();
			AbstractUrl first = data.first;
			Object[] path = first.createPath(project);
			if (path == null || path.length < 1 || path[0] == null) {
				continue;
			}
			Object element = path[0];
			if (element instanceof SmartPsiElementPointer) {
				final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(((SmartPsiElementPointer) element).getElement());
				if (virtualFile == null) continue;
				if (virtualFile.isDirectory()) {
					iterateContentUnderDirectory(projectFileIndex, virtualFile);
				} else {
					files.add(virtualFile);
				}
			}

			if (element instanceof PsiElement) {
				final VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement) element);
				if (virtualFile == null) continue;
				if (virtualFile.isDirectory()) {
					iterateContentUnderDirectory(projectFileIndex, virtualFile);
				} else {
					files.add(virtualFile);
				}
			}
		}

	}

	private boolean iterateContentUnderDirectory(ProjectFileIndex projectFileIndex, VirtualFile virtualFile) {
		final ContentIterator contentIterator = fileOrDir -> {
			if (fileOrDir.isDirectory() && !fileOrDir.equals(virtualFile)) {
				iterateContentUnderDirectory(projectFileIndex, fileOrDir);
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

	@Override
	public String getOwnerPath() {
		return OWNER_PREFIX + getTitle();
	}

	@Override
	public String getTitle() {
		return name;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void invalidate() {

	}

	@Override
	public int size(Project project) {
		return files.size();
	}

	@Override
	public List<String> getLinks(Project project) {
		return files.stream().map(VirtualFile::getCanonicalPath).collect(Collectors.toList());
	}

	@Override
	public boolean isOwner(String ownerPath) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof FavoritesGroup) && ((FavoritesGroup) obj).getOwnerPath().equals(this.getOwnerPath());
	}
}
