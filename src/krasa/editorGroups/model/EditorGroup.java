package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

// @idea.title EditorGroup
// @idea.related EditorGroupIndexValue.java
public abstract class EditorGroup {
	public static final EditorGroup EMPTY = new EditorGroupIndexValue("NOT_EXISTS", "NOT_EXISTS", false).setLinks(Collections.emptyList());

	@Nullable
	public abstract String getId();

	public abstract String getTitle();

	public abstract boolean isValid();

	public abstract void invalidate();

	public abstract int size(Project project);

	public boolean isInvalid() {
		return !isValid();
	}

	public abstract List<String> getLinks(Project project);

	public abstract boolean isOwner(String ownerPath);

	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		//			LOG.debug("getEditorTabTitle "+textEditor.getName() + ": "+group.getTitle());

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

	public String getPresentableDescription() {
		if (this instanceof AutoGroup) {
			return null;
		}
		return "Owner:" + getOwnerPath();
	}


	public VirtualFile getOwnerFile() {
		return Utils.getFileByPath(getOwnerPath());
	}

	public Color getColor() {
		return null;
	}

	public boolean containsLink(Project project, String currentFilePath) {
		return getLinks(project).contains(currentFilePath);
	}

	public boolean isSame(Project project, EditorGroup group) {
		if (group == null) {
			return false;
		}
		if (this == group) {
			return true;
		}
		if (!group.getClass().equals(this.getClass())) {
			return false;
		}
		if (!this.getLinks(project).equals(group.getLinks(project))) {
			return false;
		}
		return true;
	}

	public String getOwnerPath() {
		return getId();
	}
}
