package krasa.editorGroups.model;

import gnu.trove.THashMap;

import java.util.Collection;
import java.util.Map;

public class EditorGroups {

	Map<String, EditorGroup> list = new THashMap<>();

	public EditorGroups(EditorGroup editorGroup) {
		add(editorGroup);
	}

	public void add(EditorGroup editorGroup) {
		list.put(editorGroup.getOwnerPath(), editorGroup);
	}

	public Collection<EditorGroup> getAll() {
		return list.values();
	}

	public void remove(EditorGroup editorGroup) {
		list.remove(editorGroup.getOwnerPath());
	}
}
