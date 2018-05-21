package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collection;
import java.util.List;

public class SameNameGroup extends AutoGroup {

	private final String fileNameWithoutExtension;

	public SameNameGroup(String fileNameWithoutExtension, List<String> links, Collection<EditorGroup> groups) {
		super(groups, links);
		this.fileNameWithoutExtension = fileNameWithoutExtension;
	}

	@Override
	public String getOwnerPath() {
		return fileNameWithoutExtension;
	}

	@Override
	public String getTitle() {
		return "FILE_NAME";
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		return "By file name";
	}

}
