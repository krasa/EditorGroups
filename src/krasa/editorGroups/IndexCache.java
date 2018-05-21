package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.FilteringProcessor;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.FileResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class IndexCache {
	private static final Logger LOG = Logger.getInstance(IndexCache.class);

	public static IndexCache getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, IndexCache.class);
	}

	@NotNull
	private Project project;
	private FileResolver fileResolver;
	private Map<String, EditorGroups> groupsByLinks = new ConcurrentHashMap<>();


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

	public void clear() {
		groupsByLinks.clear();
	}

	public boolean validate(EditorGroup group) {
		if (group.isInvalid()) {
			return false;
		}
		if (group instanceof AutoGroup) {
			return group.isValid();
		}

		String ownerPath = group.getOwnerPath();
		List<EditorGroupIndexValue> groups = null;
		try {
			groups = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, ownerPath, GlobalSearchScope.projectScope(project));
		} catch (ProcessCanceledException e) {
			return true;
		}

		Optional<EditorGroupIndexValue> first = groups.stream().filter(Predicate.isEqual(group)).findFirst();
		EditorGroupIndexValue editorGroupIndexValue = first.orElse(null);
		if (!group.equals(editorGroupIndexValue)) {
			group.invalidate();
			return false;
		}
		return true;
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

	public EditorGroupIndexValue onIndexingDone(EditorGroupIndexValue group) {
		//return cached group
		EditorGroups editorGroups = groupsByLinks.get(group.getOwnerPath());
		if (editorGroups != null) {
			EditorGroup editorGroup = editorGroups.getByOwner(group.getOwnerPath());
			if (group.equals(editorGroup)) {
				return (EditorGroupIndexValue) editorGroup;
			}
		}


		initGroup(group);
		return group;
	}

	public void initGroup(@NotNull EditorGroupIndexValue group) {
		System.out.println("initGroup = [" + group + "]");
		List<String> links = fileResolver.resolveLinks(project, group.getOwnerPath(), group.getRelatedPaths());
		if (links.size() > 100) {
			LOG.warn("Too many links (" + links.size() + ") for group: " + group + ",\nResolved links:" + links);
			links = new ArrayList<>(links.subList(0, 100));
		}
		group.setLinks(links);

		addToCache(links, group);
	}


	public EditorGroup getEditorGroupAsSlave(String currentFilePath) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups groups = groupsByLinks.get(currentFilePath);

		if (groups != null && groups.getLast() != null) {
			result = getByOwner(groups.getLast());
		}

		if (result.isInvalid()) {
			if (groups != null) {
				groups.validate(this);

				int size = groups.size(project);

				if (size == 1) {
					result = groups.first();
				} else if (size > 1) {
					result = groups;
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
		if (!file.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}
		
		VirtualFile parent = file.getParent();
		String folder = parent.getCanonicalPath();
		List<String> links = fileResolver.resolveLinks(project, folder, Collections.singletonList("./"));
		return new FolderGroup(folder, links, getGroups(file.getCanonicalPath()));
	}


	public Collection<EditorGroup> getGroups(String canonicalPath) {
		Collection<EditorGroup> result = Collections.emptyList();
		EditorGroups editorGroups = groupsByLinks.get(canonicalPath);
		if (editorGroups != null) {
			editorGroups.validate(this);
			result = editorGroups.getAll();
		}
		return result;
	}

	public EditorGroup getSameNameGroup(VirtualFile currentFile) {
		if (!currentFile.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}
		String nameWithoutExtension = currentFile.getNameWithoutExtension();
		long start = System.currentTimeMillis();

		CommonProcessors.CollectProcessor<String> matchingNamesProc = new CommonProcessors.CollectProcessor<>();
		FilenameIndex.processAllFileNames(new FilteringProcessor<>(s -> s.equals(nameWithoutExtension) || s.startsWith(nameWithoutExtension + "."), matchingNamesProc), GlobalSearchScope.projectScope(project), null);
		Collection<String> matchingNames = matchingNamesProc.getResults();

		CommonProcessors.CollectProcessor<PsiFileSystemItem> processor = new CommonProcessors.CollectProcessor<>();
		for (String matchingName : matchingNames) {
			FilenameIndex.processFilesByName(
				matchingName, false, false, processor, GlobalSearchScope.projectScope(project), project, null);

		}

		List<String> paths = new ArrayList<>();
		Collection<PsiFileSystemItem> results = processor.getResults();
		for (PsiFileSystemItem result : results) {
			if (ProjectCoreUtil.isProjectOrWorkspaceFile(result.getVirtualFile())) {
				continue;
			}
			paths.add(result.getVirtualFile().getCanonicalPath());
		}
		Collections.sort(paths);

		long t0 = System.currentTimeMillis() - start;
		if (t0 > 100) {
			LOG.warn("getSameNameGroup took " + t0 + "ms");
		}
		System.err.println("getSameNameGroup " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());

		return new SameNameGroup(nameWithoutExtension, paths, getGroups(currentFile.getCanonicalPath()));
	}
	
}
