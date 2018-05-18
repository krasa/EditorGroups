package krasa.editorGroups.model;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

// @idea.title EditorGroup
// @idea.related EditorGroupIndexValue.java
public interface EditorGroup {
	public static EditorGroup EMPTY = new EditorGroupIndexValue("NOT_EXISTS", null, false);

	String getOwnerPath();

	List<String> getRelatedPaths();

	String getTitle();

	boolean valid();

	EditorGroupIndexValue invalidate();

	int size();

	VirtualFile getOwnerVirtualFile();

	boolean contains(String canonicalPath);

	default boolean invalid() {
		return !valid();
	}

	List<String> getLinks();

}
