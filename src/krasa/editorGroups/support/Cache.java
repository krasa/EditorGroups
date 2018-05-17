package krasa.editorGroups.support;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;

import java.util.List;

public interface Cache {
	EditorGroup getByOwner(Project project, String canonicalPath);

	List<EditorGroup> findGroupsAsSlave(Project project, String currentFilePath);

	void reindex(Project project, VirtualFile currentFile);
}
