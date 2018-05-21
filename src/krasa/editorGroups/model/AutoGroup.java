package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AutoGroup implements EditorGroup {
	protected final List<String> links;
	protected Collection<EditorGroup> groups = Collections.emptyList();
	protected volatile boolean valid;

	public AutoGroup(Collection<EditorGroup> groups, List<String> links) {
		this.groups = groups;
		this.links = links;
		valid = !links.isEmpty();
	}

	public Collection<EditorGroup> getGroups() {
		return groups;
	}

	@Override
	public abstract String getOwnerPath();


	@Override
	public List<String> getRelatedPaths() {
		return Collections.emptyList();
	}

	@Override
	public abstract String getTitle();

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void invalidate() {
		valid = false;

	}

	@Override
	public int size(Project project) {
		return links.size();
	}

	@Override
	public List<String> getLinks(Project project) {
		return links;
	}

	@Override
	public boolean isOwner(String ownerPath) {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return obj.getClass().equals(this.getClass());
	}
}
