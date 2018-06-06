package krasa.editorGroups;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.TreeItem;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.index.MyFileNameIndexService;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.FileResolver;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IndexCache {
	private static final Logger LOG = Logger.getInstance(IndexCache.class);
	public static final int LIMIT_SAME_NAME = 100;

	public static IndexCache getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, IndexCache.class);
	}

	@NotNull
	private Project project;
	private FileResolver fileResolver;
	private Map<String, EditorGroups> groupsByLinks = new ConcurrentHashMap<>();

	private FavoritesManager favoritesManager;
	private ProjectFileIndex fileIndex;

	public IndexCache(@NotNull Project project) {
		this.project = project;
		favoritesManager = FavoritesManager.getInstance(project);
		fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
		fileResolver = new FileResolver();
	}

	public EditorGroup getByOwner(@NotNull String canonicalPath) {
		EditorGroup result = getGroupFromIndexById(canonicalPath + ";0");
		//init
		result.getLinks(project);

		return result;
	}

	public EditorGroup getById(@NotNull String id) {
		EditorGroup result = getGroupFromIndexById(id);
		//init
		result.getLinks(project);

		return result;
	}

	public void clear() {
		groupsByLinks.clear();
	}

	public boolean validate(EditorGroup group) {
		if (group.isInvalid()) {
			return false;
		}
		if (group instanceof EditorGroupIndexValue) {
			String id = group.getId();
			try {
				EditorGroup groupFromIndex = getGroupFromIndexById(id);
				if (!groupFromIndex.equals(group)) {
					group.invalidate();
					return false;
				}
			} catch (ProcessCanceledException e) {
			}

		}
		return group.isValid();
	}

	@NotNull
	private EditorGroup getGroupFromIndexById(String id) {
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, id, GlobalSearchScope.projectScope(project));
		if (values.size() > 1) {
			LOG.error(String.valueOf(values));
		}
		return values.size() == 0 ? EditorGroup.EMPTY : values.get(0);
	}


	private void addToCache(List<String> links, EditorGroupIndexValue group) {
		for (String link : links) {
			EditorGroups editorGroups = groupsByLinks.get(link);
			if (editorGroups == null) {
				EditorGroups value = new EditorGroups();
				value.add(group);
				groupsByLinks.put(link, value);
			} else {
				editorGroups.add(group);
			}
		}
	}

	public EditorGroupIndexValue onIndexingDone(EditorGroupIndexValue group) {
		//return cached group
		EditorGroups editorGroups = groupsByLinks.get(group.getOwnerPath());
		if (editorGroups != null) {
			EditorGroup editorGroup = editorGroups.getById(group.getId());
			if (group.equals(editorGroup)) {
				return (EditorGroupIndexValue) editorGroup;
			}
		}


		initGroup(group);
		return group;
	}

	public void initGroup(@NotNull EditorGroupIndexValue group) {
		LOG.debug("initGroup = [" + group + "]");
		List<String> links = fileResolver.resolveLinks(project, group.getOwnerPath(), group.getRelatedPaths());
		if (links.size() > 100) {
			LOG.warn("Too many links (" + links.size() + ") for group: " + group + ",\nResolved links:" + links);
			links = new ArrayList<>(links.subList(0, 100));
		}
		group.setLinks(links);

		addToCache(links, group);
	}


	public EditorGroup getLastEditorGroup(String currentFilePath, boolean includeAutogroups, boolean includeFavorites) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups groups = groupsByLinks.get(currentFilePath);
		ApplicationConfiguration.State state = ApplicationConfiguration.state();

		if (groups != null) {
			String last = groups.getLast();
			LOG.debug("last = " + last);
			if (last != null) {
				if (includeAutogroups && state.autoSameName && AutoGroup.SAME_FILE_NAME.equals(last)) {
					result = AutoGroup.SAME_NAME_INSTANCE;
				} else if (includeAutogroups && state.autoFolders && AutoGroup.DIRECTORY.equals(last)) {
					result = AutoGroup.DIRECTORY_INSTANCE;
				} else if (includeFavorites && last.startsWith(FavoritesGroup.OWNER_PREFIX)) {
					EditorGroup favoritesGroup = getFavoritesGroup(last.substring(FavoritesGroup.OWNER_PREFIX.length()));
					if (favoritesGroup.containsLink(project, currentFilePath)) {
						result = favoritesGroup;
					}
				} else {
					EditorGroup lastGroup = getById(last);
					if (lastGroup.containsLink(project, currentFilePath)) {
						result = lastGroup;
					}
				}
			}

			if (result.isInvalid()) {
				result = getSlaveGroup(currentFilePath);
			}
		}
		return result;
	}

	public EditorGroup getSlaveGroup(String currentFilePath) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups groups = groupsByLinks.get(currentFilePath);

		if (groups != null) {
			groups.validate(this);

			Collection<EditorGroup> all = groups.getAll();
			for (EditorGroup editorGroup : all) {
				if (result.isValid() && editorGroup.isValid()) {
					result = groups;
					break;
				} else if (editorGroup.isValid()) {
					result = editorGroup;
				}
			}
		}
		return result;
	}

	/**
	 * called very often!
	 */
	public EditorGroup getEditorGroupForColor(String currentFilePath) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups groups = groupsByLinks.get(currentFilePath);

		if (groups != null) {
			String last = groups.getLast();
			if (last != null) {
				EditorGroups editorGroups = groupsByLinks.get(last);
				if (editorGroups != null) {
					EditorGroup lastGroup = editorGroups.getById(last);
					if (lastGroup.isValid() && lastGroup.containsLink(project, currentFilePath)) {
						result = lastGroup;
					}
				}
			}

			if (result.isInvalid()) {
				result = groups.ownerOrLast(currentFilePath);
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
			editorGroups = new EditorGroups();
			editorGroups.add(result);
			groupsByLinks.put(currentFile, editorGroups);
		}
		editorGroups.setLast(result.getId());
	}

	public EditorGroup getFolderGroup(VirtualFile file) {
		if (!file.isInLocalFileSystem()) {
			return EditorGroup.EMPTY;
		}

		VirtualFile parent = file.getParent();
		String folder = parent.getCanonicalPath();
		List<String> links = fileResolver.resolveLinks(project, folder, Collections.singletonList("./"));
		return new FolderGroup(folder, links);
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

		Collection<VirtualFile> virtualFilesByName = MyFileNameIndexService.getVirtualFilesByName(project, nameWithoutExtension, true, GlobalSearchScope.projectScope(project));


		boolean containsCurrent = virtualFilesByName.contains(currentFile);
		int size = virtualFilesByName.size();
		List<String> paths = new ArrayList<>(Math.max(containsCurrent ? size : size + 1, LIMIT_SAME_NAME));

		if (!containsCurrent) {
			paths.add(currentFile.getCanonicalPath());
		}
		for (VirtualFile file : virtualFilesByName) {
			if (ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
				continue;
			}
			if (file.isDirectory()) {
				continue;
			}
			if (paths.size() == LIMIT_SAME_NAME) {
				LOG.warn("#getSameNameGroup: too many results for " + nameWithoutExtension + " =" + size);
				break;
			}
			paths.add(file.getCanonicalPath());
		}

		Collections.sort(paths);

		long t0 = System.currentTimeMillis() - start;
		if (t0 > 100) {
			LOG.warn("getSameNameGroup took " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());
		}
		LOG.debug("getSameNameGroup " + t0 + "ms for '" + nameWithoutExtension + "', results: " + paths.size());

		return new SameNameGroup(nameWithoutExtension, paths);
	}

	public List<EditorGroupIndexValue> getAllGroups() {
		FileBasedIndex instance = FileBasedIndex.getInstance();
		Collection<String> allKeys = instance.getAllKeys(EditorGroupIndex.NAME, project);
		GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

		List<EditorGroupIndexValue> all = new ArrayList<>(allKeys.size());

		for (String allKey : allKeys) {
			instance.processValues(EditorGroupIndex.NAME, allKey, null, new FileBasedIndex.ValueProcessor<EditorGroupIndexValue>() {
				@Override
				public boolean process(@NotNull VirtualFile file, EditorGroupIndexValue value) {
					all.add(value);
					return true;
				}
			}, scope);
		}
		return all;
	}

	public String getLast(String currentFilePath) {
		EditorGroups groups = groupsByLinks.get(currentFilePath);

		if (groups != null) {
			return groups.getLast();

		}
		return null;
	}

	public void loadState(ProjectComponent.State state) {
		for (ProjectComponent.StringPair stringStringPair : state.lastGroup) {
			EditorGroups editorGroups = new EditorGroups();
			groupsByLinks.put(stringStringPair.key, editorGroups);
			editorGroups.setLast(stringStringPair.value);
		}
	}

	public ProjectComponent.State getState() {
		ProjectComponent.State state = new ProjectComponent.State();
		Set<Map.Entry<String, EditorGroups>> entries = groupsByLinks.entrySet();
		boolean autoSameName = ApplicationConfiguration.state().autoSameName;
		boolean autoFolders = ApplicationConfiguration.state().autoFolders;

		for (Map.Entry<String, EditorGroups> entry : entries) {
			String last = entry.getValue().getLast();
			if (last == null) {
				continue;
			} else if (autoSameName && AutoGroup.SAME_FILE_NAME.equals(last)) {
				continue;
			} else if (autoFolders && AutoGroup.DIRECTORY.equals(last)) {
				continue;
			}
			if (state.lastGroup.size() > 1000) {  //TODO config
				break;
			}
			state.lastGroup.add(new ProjectComponent.StringPair(entry.getKey(), last));
		}
		return state;
	}

	public EditorGroup updateGroups(AutoGroup result, String currentFilePath) {
		return result.setGroups(getGroups(currentFilePath));
	}

	public EditorGroup getFavoritesGroup(String title) {
		List<TreeItem<Pair<AbstractUrl, String>>> favoritesListRootUrls = favoritesManager.getFavoritesListRootUrls(title);
		if (favoritesListRootUrls.isEmpty()) {
			return EditorGroup.EMPTY;
		}

		return new FavoritesGroup(title, favoritesListRootUrls, project, fileIndex);
	}

	public Collection<FavoritesGroup> getFavoritesGroups() {
		List<String> availableFavoritesListNames = favoritesManager.getAvailableFavoritesListNames();

		ArrayList<FavoritesGroup> favoritesGroups = new ArrayList<>();
		for (String name : availableFavoritesListNames) {
			List<TreeItem<Pair<AbstractUrl, String>>> favoritesListRootUrls = favoritesManager.getFavoritesListRootUrls(name);
			if (favoritesListRootUrls.isEmpty()) {
				continue;

			}
			FavoritesGroup e = new FavoritesGroup(name, favoritesListRootUrls, project, fileIndex);
			if (e.size(project) > 0) {
				favoritesGroups.add(e);
			}
		}

		return favoritesGroups;
	}

	public void removeGroup(String ownerPath) {
		EditorGroup group = null;
		for (Map.Entry<String, EditorGroups> entry : groupsByLinks.entrySet()) {
			EditorGroups value = entry.getValue();
			for (EditorGroup editorGroup : value.getAll()) {
				group = editorGroup;
				if (group.isOwner(ownerPath)) {
					LOG.debug("removeFromIndex invalidating" + "" + group);
					group.invalidate();
				}
			}
		}

		if (group != null) {
			List<String> links = group.getLinks(project);
			for (String link : links) {
				EditorGroups editorGroups = groupsByLinks.get(link);
				if (editorGroups != null) {
					editorGroups.remove(group);
				}
			}
		}

		PanelRefresher.getInstance(project).refresh(ownerPath);
	}


}
