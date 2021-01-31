package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class FolderGroup extends AutoGroup {

	public static final LightVirtualFile DIRECTORY_INSTANCE = new LightVirtualFile("DIRECTORY_INSTANCE");
	public static final FolderGroup INSTANCE = new FolderGroup(DIRECTORY_INSTANCE, Collections.emptyList());
	private final VirtualFile folder;

	public FolderGroup(@Nullable VirtualFile folder, List<Link> links) {
		super(links);
		this.folder = folder;
	}

	@Override
	public boolean isValid() {
		return folder != null && valid && (DIRECTORY_INSTANCE.equals(folder) || folder.isDirectory());
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
				", stub='" + isStub() + '\'' +
				'}';
	}
}
