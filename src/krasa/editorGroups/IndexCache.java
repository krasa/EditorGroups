package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.FileResolver;
import krasa.editorGroups.support.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IndexCache {
	private static final Logger LOG = Logger.getInstance(IndexCache.class);
	public static final int LINKS_LIMIT = 100;
	public static final int LIMIT_SAME_NAME = LINKS_LIMIT;

	public static IndexCache getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, IndexCache.class);
	}

	@NotNull
	private Project project;
	private Map<String, EditorGroups> groupsByLinks = new ConcurrentHashMap<>();

	private final ExternalGroupProvider externalGroupProvider;

	public IndexCache(@NotNull Project project, ExternalGroupProvider externalGroupProvider) {
		this.project = project;
		this.externalGroupProvider = externalGroupProvider;
	}

	public EditorGroup getOwningOrSingleGroup(@NotNull String canonicalPath) {
		EditorGroup result = EditorGroup.EMPTY;

		EditorGroups editorGroups = groupsByLinks.get(canonicalPath);
		if (editorGroups != null) {
			Collection<EditorGroup> values = editorGroups.getAll();
			if (values.size() == 1) {
				result = (EditorGroup) values.toArray()[0];
			} else {
				for (EditorGroup value : values) {
					if (value.getOwnerPath().equals(canonicalPath)) {
						if (result != EditorGroup.EMPTY) {//more than one group in file
							result = EditorGroup.EMPTY;
							break;
						}
						result = value;
					}
				}
			}
		}
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
			Notifications.duplicateId(id, values);
		}
		return values.size() == 0 ? EditorGroup.EMPTY : values.get(0);
	}


	private void addToCache(List<String> links, EditorGroupIndexValue group) {
		add(group, group.getOwnerPath());

		for (String link : links) {
			add(group, link);
		}
	}


	private void add(@NotNull EditorGroupIndexValue group, @NotNull String link) {
		EditorGroups editorGroups = groupsByLinks.get(link);
		if (editorGroups == null) {
			EditorGroups value = new EditorGroups();
			value.add(group);
			groupsByLinks.put(link, value);
		} else {
			editorGroups.add(group);
		}
	}

	public EditorGroupIndexValue onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		EditorGroups editorGroups = groupsByLinks.get(ownerPath);
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
		if (LOG.isDebugEnabled()) LOG.debug("initGroup = [" + group + "]");
		List<String> links = FileResolver.resolveLinks(group, project);
		if (links.size() > LINKS_LIMIT) {
			LOG.warn("Too many links (" + links.size() + ") for group: " + group + ",\nResolved links:" + links);
			links = new ArrayList<>(links.subList(0, LINKS_LIMIT));
		}
		group.setLinks(links);

		addToCache(links, group);
	}


	public EditorGroup getLastEditorGroup(String currentFilePath, boolean includeAutogroups, boolean includeFavorites) {
		EditorGroup result = EditorGroup.EMPTY;
		EditorGroups groups = groupsByLinks.get(currentFilePath);
		ApplicationConfiguration config = ApplicationConfiguration.state();

		if (groups != null) {
			String last = groups.getLast();
			if (LOG.isDebugEnabled()) LOG.debug("last = " + last);
			if (last != null) {
				if (includeAutogroups && config.isAutoSameName() && AutoGroup.SAME_FILE_NAME.equals(last)) {
					result = AutoGroup.SAME_NAME_INSTANCE;
				} else if (includeAutogroups && config.isAutoFolders() && AutoGroup.DIRECTORY.equals(last)) {
					result = AutoGroup.DIRECTORY_INSTANCE;
				} else if (includeFavorites && last.startsWith(FavoritesGroup.OWNER_PREFIX)) {
					EditorGroup favoritesGroup = externalGroupProvider.getFavoritesGroup(last.substring(FavoritesGroup.OWNER_PREFIX.length()));
					if (favoritesGroup.containsLink(project, currentFilePath)) {
						result = favoritesGroup;
					}
				} else {
					EditorGroup lastGroup = getById(last);
					if (lastGroup.containsLink(project, currentFilePath) || lastGroup.isOwner(currentFilePath)) {
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

	public List<EditorGroup> findGroups(String canonicalPath) {
		List<EditorGroup> result = new ArrayList<>();
		EditorGroups editorGroups = groupsByLinks.get(canonicalPath);
		if (editorGroups != null) {
			editorGroups.validate(this);
			result.addAll(editorGroups.getAll());
		}
		result.addAll(externalGroupProvider.findGroups(canonicalPath));
		return result;
	}

	public EditorGroup getSlaveGroup(String currentFilePath) {
		EditorGroup result = EditorGroup.EMPTY;

		List<EditorGroup> favouriteGroups = findGroups(currentFilePath);
		if (favouriteGroups.size() == 1) {
			result = favouriteGroups.get(0);
		} else if (favouriteGroups.size() > 1) {
			result = new EditorGroups(favouriteGroups);
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
		boolean autoSameName = ApplicationConfiguration.state().isAutoSameName();
		boolean autoFolders = ApplicationConfiguration.state().isAutoFolders();

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


	public void removeGroup(String ownerPath) {
		EditorGroup group = null;
		for (Map.Entry<String, EditorGroups> entry : groupsByLinks.entrySet()) {
			EditorGroups value = entry.getValue();
			for (EditorGroup editorGroup : value.getAll()) {
				group = editorGroup;
				if (group.isOwner(ownerPath)) {
					if (LOG.isDebugEnabled()) LOG.debug("removeFromIndex invalidating" + "" + group);
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


	@NotNull
	public EditorGroup getCached(@NotNull EditorGroup userData) {
		EditorGroups editorGroups = groupsByLinks.get(userData.getOwnerPath());
		if (editorGroups != null) {
			return editorGroups.getById(userData.getId());
		}
		return EditorGroup.EMPTY;
	}
}
