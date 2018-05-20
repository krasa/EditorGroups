package krasa.editorGroups.support;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import gnu.trove.THashMap;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.model.EditorGroups;
import krasa.editorGroups.model.FolderGroup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * TODO synchronization
 */
public class IndexCache {
	private static final Logger LOG = Logger.getInstance(IndexCache.class);

	public static IndexCache getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, IndexCache.class);
	}

	FileResolver fileResolver;
	THashMap<String, EditorGroups> groupsByLinks = new THashMap<>();

	@NotNull
	private final Project project;

	public IndexCache(@NotNull Project project) {
		this.project = project;
		fileResolver = new FileResolver();
	}

	public EditorGroup getByOwner(String canonicalPath) {
		System.out.println("> getByOwner " + canonicalPath);

		EditorGroup result = EditorGroup.EMPTY;
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, canonicalPath, GlobalSearchScope.projectScope(project));
		for (EditorGroupIndexValue value : values) {
			if (value.isOwner(canonicalPath)) {
				result = value;
				break;
			}
		}
		//init
		result.getLinks(project);

		System.out.println("< getByOwner " + canonicalPath + " result=" + result);
		return result;
	}

	public void reindex(VirtualFile currentFile) {
		FileBasedIndex.getInstance().requestReindex(currentFile);
	}

	public void reindex() {
		groupsByLinks.clear();
		FileBasedIndex.getInstance().requestRebuild(EditorGroupIndex.NAME);
	}

	public boolean validate(EditorGroup lastGroup) {
		if (lastGroup.isInvalid()) {
			return false;
		}
		if (lastGroup instanceof FolderGroup) {
			return lastGroup.isValid();
		}

		String ownerPath = lastGroup.getOwnerPath();
		List<EditorGroupIndexValue> values = null;
		try {
			values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, ownerPath, GlobalSearchScope.projectScope(project));
		} catch (ProcessCanceledException e) {
			return true;
		}

		Optional<EditorGroupIndexValue> first = values.stream().filter(Predicate.isEqual(lastGroup)).findFirst();
		EditorGroupIndexValue editorGroupIndexValue = first.orElse(null);
		if (!lastGroup.equals(editorGroupIndexValue)) {
			lastGroup.invalidate();
			return false;
		}
		return true;
	}


	public void initCache(List<EditorGroupIndexValue> values) {
		for (EditorGroupIndexValue value : values) {
			initGroup(value);
		}
	}

	public void initGroup(@NotNull EditorGroupIndexValue group) {
		System.out.println("initGroup = [" + group + "]");
		List<String> links = fileResolver.resolveLinks(project, group.getOwnerPath(), group.getRelatedPaths());
		if (links.size() > 20) {
			LOG.warn("Too many links (" + links.size() + ") for group: " + group + ",\nResolved links:" + links);
			links = new ArrayList<>(links.subList(0, 20));
		}
		group.setLinks(links);

		addToCache(links, group);
	}

	private void addToCache(List<String> links, EditorGroupIndexValue group) {
		for (String link : links) {
			EditorGroups editorGroups = groupsByLinks.get(link);
			if (editorGroups == null) {
				groupsByLinks.put(link, new EditorGroups(group));
			} else {
				editorGroups.add(group);
			}
		}
	}

	public EditorGroup getEditorGroupAsSlave(String currentFilePath) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups s = groupsByLinks.get(currentFilePath);
		if (s != null && s.getLast() != null) {
			result = getByOwner(s.getLast());
		}

		if (result.isInvalid()) {
			EditorGroups editorGroups = groupsByLinks.get(currentFilePath);
			if (editorGroups != null) {
				editorGroups.validate(this);

				int size = editorGroups.size(project);

				if (size == 1) {
					result = editorGroups.first();
				} else if (size > 1) {
					result = editorGroups;
				}
			}
		}
		return result;
	}


	public void setLast(String currentFile, EditorGroup result) {
		if (!result.isValid()) {
			return;
		}

		EditorGroups editorGroups = groupsByLinks.get(currentFile);
		if (editorGroups == null) {
			editorGroups = new EditorGroups(result);
			groupsByLinks.put(currentFile, editorGroups);
		}
		editorGroups.setLast(result.getOwnerPath());
	}

	public EditorGroup getFolderGroup(VirtualFile file) {
		VirtualFile parent = file.getParent();
		String folder = parent.getCanonicalPath();
		List<String> links = fileResolver.resolveLinks(project, folder, Collections.singletonList("./"));
		return new FolderGroup(folder, links);
	}
}
