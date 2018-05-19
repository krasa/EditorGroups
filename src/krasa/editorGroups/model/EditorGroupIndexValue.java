package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.IndexCache;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EditorGroupIndexValue implements EditorGroup {

	/*definitions*/
	private String ownerPath = "";
	private String title = "";
	private List<String> relatedPaths = new ArrayList<>();

	/*runtime data*/
	private transient List<String> links;
	private transient boolean valid = true;

	public EditorGroupIndexValue() {
	}

	public EditorGroupIndexValue(String ownerPath, String title, boolean valid) {
		this.ownerPath = ownerPath;
		this.title = title;
		this.valid = valid;
	}

	public EditorGroupIndexValue setTitle(String title) {
		this.title = StringUtil.notNullize(title);
		return this;
	}

	@NotNull
	public String getOwnerPath() {
		return ownerPath;
	}

	public void setOwnerPath(String ownerPath) {
		this.ownerPath = ownerPath;
	}

	public List<String> getRelatedPaths() {
		return relatedPaths;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	public EditorGroupIndexValue invalidate() {
		this.valid = false;
		return this;
	}

	@Override
	public int size(Project project) {
		return getLinks(project).size();
	}

	public VirtualFile getOwnerVirtualFile() {
		if (ownerPath == null) {
			return null;
		}
		return Utils.getFileByPath(ownerPath);
	}

	@Override
	@NotNull
	public List<String> getLinks(Project project) {
		if (links == null) {
			IndexCache.getInstance(project).initGroup(this);
		}

		return links;
	}

	public EditorGroupIndexValue addRelated(String value) {
		relatedPaths.add(value);
		return this;
	}

	@Override
	public boolean isOwner(@NotNull String canonicalPath) {
		return ownerPath.equals(canonicalPath);
	}

	public EditorGroupIndexValue setLinks(List<String> links) {
		this.links = links;
		return this;
	}           
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EditorGroupIndexValue that = (EditorGroupIndexValue) o;

		if (!ownerPath.equals(that.ownerPath)) return false;
		if (!title.equals(that.title)) return false;
		return relatedPaths.equals(that.relatedPaths);
	}

	@Override
	public int hashCode() {
		int result = ownerPath.hashCode();
		result = 31 * result + title.hashCode();
		result = 31 * result + relatedPaths.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "EditorGroupIndexValue{" +
			"title='" + title + '\'' +
			"owner='" + ownerPath + '\'' +
			"related='" + relatedPaths + '\'' +
			'}';
	}

}
