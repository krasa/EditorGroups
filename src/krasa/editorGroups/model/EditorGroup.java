package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

// @idea.title EditorGroup
// @idea.related EditorGroupIndexValue.java
public interface EditorGroup {
	public static EditorGroup EMPTY = new EditorGroupIndexValue("NOT_EXISTS", "NOT_EXISTS", false).setLinks(Collections.emptyList());

	String getOwnerPath();

	List<String> getRelatedPaths();

	String getTitle();

	boolean isValid();

	void invalidate();

	int size(Project project);

	default boolean isInvalid() {
		return !isValid();
	}

	List<String> getLinks(Project project);

	boolean isOwner(String ownerPath);

	default String getPresentableTitle(Project project, String presentableNameForUI) {
		//			System.out.println("getEditorTabTitle "+textEditor.getName() + ": "+group.getTitle());
		int size = size(project);
		if (size > 1 && isNotEmpty(getTitle())) {
			presentableNameForUI = "[" + size + " " + getTitle() + "] " + presentableNameForUI;
		} else if (size > 1) {
			presentableNameForUI = "[" + size + "] " + presentableNameForUI;
		}
		return presentableNameForUI;
	}
}
