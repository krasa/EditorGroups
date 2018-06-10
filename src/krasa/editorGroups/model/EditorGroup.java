package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

// @group.title EditorGroup
// @group.related EditorGroupIndexValue.java
public abstract class EditorGroup {
	public static final EditorGroup EMPTY = new EditorGroupIndexValue("NOT_EXISTS", "NOT_EXISTS", false).setLinks(Collections.emptyList());

	@NotNull
	public abstract String getId();

	public String getOwnerPath() {
		return getId();
	}
	
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
		//			if (LOG.isDebugEnabled()) LOG.debug("getEditorTabTitle "+textEditor.getName() + ": "+group.getTitle());

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

	public String getSwitchDescription() {   
		
		if (this instanceof AutoGroup) {
			return null;
		}
		if (!(this instanceof FavoritesGroup)) {
			return "Owner:" + getOwnerPath();
		}
		return null;
	}


	public Color getBgColor() {
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
		if (!this.equals(group)) {
			return false;
		}
		if (!this.getLinks(project).equals(group.getLinks(project))) {
			return false;
		}
		return true;
	}


	public Color getFgColor() {
		return null;
	}

	public VirtualFile getFirstExistingFile(Project project) {
		List<String> links = getLinks(project);
		for (String link : links) {
			VirtualFile fileByPath = Utils.getFileByPath(link);
			if (fileByPath != null && fileByPath.exists() && !fileByPath.isDirectory()) {
				return fileByPath;
			}
		}

		return null;
	}

	@NotNull
	public String tabTitle(Project project) {
		String title = getTitle();
		if (title.isEmpty()) {
			title = Utils.toPresentableName(getOwnerPath());
		}
		if (ApplicationConfiguration.state().isShowSize()) {
			title += ":" + size(project);
		}
		return title;
	}

	public String switchTitle(Project project) {
		String title;
		if (this instanceof FavoritesGroup) {
			title = getTitle();
		} else {
			String ownerPath = getOwnerPath();
			String name = Utils.toPresentableName(ownerPath);
			title = getPresentableTitle(project, name, false);   //never show size - initializes links and lags

		}
		return title;
	}

	public String getTabGroupTooltipText(Project project) {
		return getPresentableTitle(project, "Owner: " + getOwnerPath(), true);
	}
}
