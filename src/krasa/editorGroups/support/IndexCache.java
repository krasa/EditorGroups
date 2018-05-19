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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class IndexCache {
	private static final Logger LOG = Logger.getInstance(IndexCache.class);

	public static IndexCache getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, IndexCache.class);
	}

	FileResolver fileResolver;
	THashMap<String, EditorGroups> groupsByLinks = new THashMap<>();
	//TODO persist?
	Map<String, String> slaveFileByLastGroupOwner = new THashMap<>();

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
		List<String> links = fileResolver.resolveLinks(project, group);
		if (links.size() > 100) {
			LOG.error("Too many links (" + links.size() + ") for group: " + group + ",\nResolved links:" + links);
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
		String s = slaveFileByLastGroupOwner.get(currentFilePath);
		if (s != null) {
			result = getByOwner(s);
		}

		if (result.isInvalid()) {
			Collection<EditorGroup> groupsAsSlave = findGroupsAsSlave(currentFilePath);
			//TODO union?
			for (EditorGroup editorGroup : groupsAsSlave) {
				if (validate(editorGroup)) {
					result = editorGroup;
					break;
				} else {
					evict(currentFilePath, editorGroup);
				}
			}
		}
		return result;
	}

	private Collection<EditorGroup> findGroupsAsSlave(String currentFilePath) {
		Collection<EditorGroup> result = Collections.emptyList();
		EditorGroups editorGroups = groupsByLinks.get(currentFilePath);
		if (editorGroups != null) {
			result = editorGroups.getAll();
		}
		return result;
	}

	private void evict(String currentFilePath, EditorGroup editorGroup) {
		EditorGroups editorGroups = groupsByLinks.get(currentFilePath);
		if (editorGroups != null) {
			editorGroups.remove(editorGroup);
		}
	}

	public void setLast(String currentFile, EditorGroup result) {
		if (result.isValid()) {
			slaveFileByLastGroupOwner.put(currentFile, result.getOwnerPath());
		}
	}
}
