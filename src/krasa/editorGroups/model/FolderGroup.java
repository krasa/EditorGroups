package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class FolderGroup extends AutoGroup {

	private final String folderPath;

	public FolderGroup(String folderPath, List<String> links, Collection<EditorGroup> groups) {
		super(groups, links);
		this.folderPath = folderPath;
	}

	@Override
	public boolean isValid() {
		return valid && new File(folderPath).isDirectory();
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI) {
		return "Current folder";
	}

	@Override
	public String getOwnerPath() {
		return folderPath;
	}

	@Override
	public String getTitle() {
		return "DIRECTORY";
	}

}
