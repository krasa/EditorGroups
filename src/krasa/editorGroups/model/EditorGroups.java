package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import krasa.editorGroups.IndexCache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EditorGroups implements EditorGroup, GroupsHolder {

	private Map<String, EditorGroup> map = new ConcurrentHashMap<>();
	private String last;

	public EditorGroups(EditorGroup editorGroup) {
		if (editorGroup instanceof AutoGroup) {
			return;
		}
		add(editorGroup);
	}

	public void add(EditorGroup editorGroup) {
		map.put(editorGroup.getOwnerPath(), editorGroup);
	}

	public Collection<EditorGroup> getAll() {
		return map.values();
	}

	public void remove(EditorGroup editorGroup) {
		map.remove(editorGroup.getOwnerPath());
	}

	@Override
	public String getOwnerPath() {
		return "EDITOR_GROUPS";
	}

	@Override
	public List<String> getRelatedPaths() {
		return Collections.emptyList();
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
			boolean valid = indexCache.validate(next);
			if (!valid) {
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

	public EditorGroup getByOwner(String ownerPath) {
		EditorGroup editorGroup = map.get(ownerPath);
		if (editorGroup == null) {
			editorGroup = EMPTY;
		}
		return editorGroup;
	}

	@Override
	public Collection<EditorGroup> getGroups() {
		return map.values();
	}
}
