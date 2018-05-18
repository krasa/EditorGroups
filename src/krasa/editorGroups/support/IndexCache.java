package krasa.editorGroups.support;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IndexCache implements Cache {


	@Override
	public EditorGroup getByOwner(Project project, String canonicalPath) {
		System.out.println("> getByOwner " + canonicalPath);

		EditorGroup result = EditorGroup.EMPTY;
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, canonicalPath, GlobalSearchScope.projectScope(project));
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
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, currentFilePath, GlobalSearchScope.projectScope(project));
		List<EditorGroup> collect = values.stream().filter(o -> validate(project, o)).collect(Collectors.toList());
		return collect;
	}

	@Override
	public void reindex(Project project, VirtualFile currentFile) {
		FileBasedIndex.getInstance().requestReindex(currentFile);
	}

	@Override
	public boolean validate(Project project, EditorGroup lastGroup) {
		if (lastGroup.invalid()) {
			return false;
		}
		String ownerPath = lastGroup.getOwnerPath();
		List<EditorGroupIndexValue> values = FileBasedIndex.getInstance().getValues(EditorGroupIndex.NAME, ownerPath, GlobalSearchScope.projectScope(project));

		Optional<EditorGroupIndexValue> first = values.stream().filter(Predicate.isEqual(lastGroup)).findFirst();
		EditorGroupIndexValue editorGroupIndexValue = first.orElse(null);
		if (!lastGroup.equals(editorGroupIndexValue)) {
			lastGroup.invalidate();
			return false;
		}
		return true;
	}
}
