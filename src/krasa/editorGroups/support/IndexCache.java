package krasa.editorGroups.support;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.index.EditorGroupIndexValue;
import krasa.editorGroups.model.EditorGroup;

import java.util.List;

public class IndexCache implements Cache {


	@Override
	public EditorGroup getByOwner(Project project, String canonicalPath) {
		System.out.println("> getByOwner " + canonicalPath);

		EditorGroup result = EditorGroup.EMPTY;
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, canonicalPath, GlobalSearchScope.projectScope(project));
		if (values.isEmpty()) {
			System.out.println();
		}
		for (EditorGroupIndexValue value : values) {
			if (value.isOwner(canonicalPath)) {
				result = value;
				break;
			}
		}

		System.out.println("< getByOwner " + canonicalPath + " result=" + result);
		return result;
	}

	@Override
	public List<EditorGroup> findGroupsAsSlave(Project project, String currentFilePath) {
		List values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, currentFilePath, GlobalSearchScope.projectScope(project));
		return values;
	}

	@Override
	public void reindex(Project project, VirtualFile currentFile) {
		FileBasedIndex.getInstance().requestReindex(currentFile);
	}
}
