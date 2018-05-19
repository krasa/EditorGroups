package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

// @idea.title EditorGroup
// @idea.related EditorGroupIndexValue.java
public interface EditorGroup {
	public static EditorGroup EMPTY = new EditorGroupIndexValue("NOT_EXISTS", "NOT_EXISTS", false).setLinks(Collections.emptyList());

	String getOwnerPath();

	List<String> getRelatedPaths();

	String getTitle();

	boolean isValid();

	EditorGroupIndexValue invalidate();

	int size(Project project);

	default boolean isInvalid() {
		return !isValid();
	}

	List<String> getLinks(Project project);

	boolean isOwner(String ownerPath);
}
