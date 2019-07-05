package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class FolderGroup extends AutoGroup {

	private final String folderPath;

	public FolderGroup(String folderPath, List<Link> links) {
		super(links);
		this.folderPath = FileUtil.toSystemIndependentName(folderPath);
	}

	@Override
	public boolean isValid() {
		return valid && ("DIRECTORY_INSTANCE".equals(folderPath) || new File(folderPath).isDirectory());
	}

	@Override
	public Icon icon() {
		return MyIcons.folder;
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
