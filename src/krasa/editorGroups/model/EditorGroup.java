package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isEmpty;
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

	default String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		//			System.out.println("getEditorTabTitle "+textEditor.getName() + ": "+group.getTitle());

		if (showSize) {
			int size = size(project);
			if (isNotEmpty(getTitle())) {
				String title = getTitle() + ":" + size;
				presentableNameForUI = "[" + title + "] " + presentableNameForUI;
			} else {
				presentableNameForUI = "[" + size + "] " + presentableNameForUI;
			}
		} else {
			boolean empty = isEmpty(getTitle());
			if (empty) {
//				presentableNameForUI = "[" + presentableNameForUI + "]";
			} else {
				presentableNameForUI = "[" + getTitle() + "] " + presentableNameForUI;
			}
		}
		return presentableNameForUI;
	}

}
