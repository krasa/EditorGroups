package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.List;

public class SameNameGroup extends AutoGroup {

	private final String fileNameWithoutExtension;

	public SameNameGroup(String fileNameWithoutExtension, List<String> links) {
		super(links);
		this.fileNameWithoutExtension = fileNameWithoutExtension;
	}

	@Override
	public String getOwnerPath() {
		return SAME_FILE_NAME;
	}

	@Override
	public String getTitle() {
		return SAME_FILE_NAME;
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		return "By same file name";
	}

	@Override
	public String toString() {
		return "SameNameGroup{" +
			"'" + fileNameWithoutExtension + '\'' +
			'}';
	}
}