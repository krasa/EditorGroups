package krasa.editorGroups.model;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collections;
import java.util.List;

public interface EditorGroup {
	public static EditorGroup EMPTY = new EditorGroupImpl(Collections.emptyList(), "NOT_EXISTS", null);

	String getOwnerPath();

	List<String> getRelatedPaths();

	String getTitle();

	boolean valid();

	int size();

	VirtualFile getOwnerVirtualFile();

	boolean contains(String canonicalPath);

	default boolean invalid() {
		return !valid();
	}

	List<String> getLinks();

}
