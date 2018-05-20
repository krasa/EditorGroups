package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FolderGroup implements EditorGroup {

	private final String currentFilePath;
	private final List<String> links;
	private boolean valid;
	private final Collection<EditorGroup> groups;

	public FolderGroup(String currentFilePath, List<String> links, Collection<EditorGroup> groups) {
		this.currentFilePath = currentFilePath;
		this.links = links;
		valid = !links.isEmpty();
		this.groups = groups;
	}

	public Collection<EditorGroup> getGroups() {
		return groups;
	}

	@Override
	public String getOwnerPath() {
		return currentFilePath;
	}

	@Override
	public List<String> getRelatedPaths() {
		return Collections.emptyList();
	}

	@Override
	public String getTitle() {
		return "DIRECTORY";
	}

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
}
