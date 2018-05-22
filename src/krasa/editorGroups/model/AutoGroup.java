package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtilRt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AutoGroup implements EditorGroup, GroupsHolder {

	public static final String SAME_FILE_NAME = "FILE_NAME";
	public static final String DIRECTORY = "DIRECTORY";

	public static final FolderGroup DIRECTORY_INSTANCE = new FolderGroup("DIRECTORY_INSTANCE", Collections.emptyList(), Collections.emptyList());
	public static final SameNameGroup SAME_NAME_INSTANCE = new SameNameGroup("SAME_NAME_INSTANCE", ContainerUtilRt.emptyList(), Collections.emptyList());


	protected final List<String> links;
	protected Collection<EditorGroup> groups = Collections.emptyList();
	protected volatile boolean valid = true;

	public AutoGroup(Collection<EditorGroup> groups, List<String> links) {
		this.groups = groups;
		this.links = links;
	}

	@Override
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
