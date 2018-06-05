package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;

public class FolderGroup extends AutoGroup {

	private final String folderPath;

	public FolderGroup(String folderPath, List<String> links) {
		super(links);
		this.folderPath = folderPath;
	}

	@Override
	public boolean isValid() {
		return valid && new File(folderPath).isDirectory();
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		return "Current folder";
	}

	@Override
	public String getOwnerPath() {
		return DIRECTORY;
	}

	@Override
	public String getTitle() {
		return DIRECTORY;
	}

}
