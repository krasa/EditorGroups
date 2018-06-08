package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import krasa.editorGroups.IndexCache;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EditorGroups extends EditorGroup implements GroupsHolder {

	private Map<String, EditorGroup> map = new ConcurrentHashMap<>();
	private String last;

	public EditorGroups() {
	}


	public void add(EditorGroup editorGroup) {
		if (editorGroup instanceof AutoGroup) {
			return;
		}
		if (editorGroup instanceof FavoritesGroup) {
			return;
		}
		map.put(editorGroup.getId(), editorGroup);
	}

	public Collection<EditorGroup> getAll() {
		return map.values();
	}

	public void remove(EditorGroup editorGroup) {
		map.remove(editorGroup.getId());
	}

	public Map<String, EditorGroup> getMap() {
		return map;
	}

	@NotNull
	@Override
	public String getId() {
		return "EDITOR_GROUPS";
	}


	@Override
	public String getTitle() {
		return "";
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
		return map.size();
	}

	@Override
	public List<String> getLinks(Project project) {
		return Collections.emptyList();
	}

	@Override
	public boolean isOwner(String ownerPath) {
		return false;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public String getLast() {
		return last;
	}

	public void validate(IndexCache indexCache) {
		Iterator<EditorGroup> iterator = map.values().iterator();
		while (iterator.hasNext()) {
			EditorGroup next = iterator.next();
			indexCache.validate(next);
		}
		//IndexCache.validate accesses index which can triggers indexing which updates this map, 
		//removing it in one cycle would remove a key with new validvalue
		iterator = map.values().iterator();
		while (iterator.hasNext()) {
			EditorGroup next = iterator.next();
			if (next.isInvalid()) {
				iterator.remove();
			}
		}
	}

	public EditorGroup first() {
		Iterator<EditorGroup> iterator = map.values().iterator();
		while (iterator.hasNext()) {
			return iterator.next();
		}
		return EMPTY;
	}

	@NotNull
	public EditorGroup getById(@NotNull String id) {
		EditorGroup editorGroup = map.get(id);
		if (editorGroup == null) {
			editorGroup = EMPTY;
		}
		return editorGroup;
	}

	@Override
	public Collection<EditorGroup> getGroups() {
		return map.values();
	}

	public EditorGroup ownerOrLast(String currentFilePath) {
		Iterator<EditorGroup> iterator = map.values().iterator();
		EditorGroup group = EMPTY;
		while (iterator.hasNext()) {
			group = iterator.next();
			if (group.isOwner(currentFilePath)) {
				break;
			}
		}
		return group;
	}

}
