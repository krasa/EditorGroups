package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FolderGroup implements EditorGroup {

	private final String folderPath;
	private final List<String> links;
	private boolean valid;
	private final Collection<EditorGroup> groups;

	public FolderGroup(String folderPath, List<String> links, Collection<EditorGroup> groups) {
		this.folderPath = folderPath;
		this.links = links;
		valid = !links.isEmpty();
		this.groups = groups;
	}

	public Collection<EditorGroup> getGroups() {
		return groups;
	}

	@Override
	public String getOwnerPath() {
		return folderPath;
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
		return valid && new File(folderPath).isDirectory();
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
