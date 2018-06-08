package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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

	@NotNull
	@Override
	public String getId() {
		return DIRECTORY;
	}

	@Override
	public String getTitle() {
		return DIRECTORY;
	}


	@Override
	public String toString() {
		return "FolderGroup{" +
			"links=" + links.size() +
			'}';
	}
}
