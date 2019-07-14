package krasa.editorGroups.model;

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class FolderGroup extends AutoGroup {

	public static final MockVirtualFile DIRECTORY_INSTANCE = new MockVirtualFile("DIRECTORY_INSTANCE");
	public static final FolderGroup INSTANCE = new FolderGroup(DIRECTORY_INSTANCE, Collections.emptyList());
	private final VirtualFile folder;

	public FolderGroup(VirtualFile folder, List<Link> links) {
		super(links);
		this.folder = folder;
	}

	@Override
	public boolean isValid() {
		return valid && (DIRECTORY_INSTANCE.equals(folder) || folder.isDirectory());
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
