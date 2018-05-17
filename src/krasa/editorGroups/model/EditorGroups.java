package krasa.editorGroups.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorGroups {
	private Map<String, EditorGroup> map = new LinkedHashMap<>();
	private EditorGroup lastOne = EditorGroup.EMPTY;

	public EditorGroups(EditorGroup editorGroup) {
		put(editorGroup);
	}

	public void put(EditorGroup editorGroup) {
		map.put(editorGroup.getOwnerPath(), editorGroup);
		lastOne = editorGroup;
	}

	public EditorGroup getLastOne() {
		return lastOne;
	}

	public void evict(EditorGroup ownerGroup) {
		map.remove(ownerGroup.getOwnerPath());
		if (lastOne == ownerGroup) {
			lastOne = EditorGroup.EMPTY;
			for (Map.Entry<String, EditorGroup> stringEditorGroupEntry : map.entrySet()) {
				lastOne = stringEditorGroupEntry.getValue();
				break;
			}
		}
	}

	public String toString() {
		return new ToStringBuilder(this).append("map", map).append("lastOne", lastOne).toString();
	}

	public List<EditorGroup> getAll() {
		return new ArrayList<>(map.values());
	}
}
