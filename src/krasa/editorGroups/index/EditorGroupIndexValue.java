package krasa.editorGroups.index;

import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditorGroupIndexValue implements EditorGroup {

	private String ownerPath;
	private String title;
	private List<String> relatedPaths = new ArrayList<>();

	private transient List<String> links;

	public EditorGroupIndexValue setTitle(String title) {
		this.title = title;
		return this;
	}

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
	public boolean valid() {
		return title != null || relatedPaths.size() > 1;
	}

	@Override
	public int size() {
		return relatedPaths.size();
	}

	@Override
	public VirtualFile getOwnerVirtualFile() {
		if (ownerPath == null) {
			return null;
		}
		return Utils.getFileByPath(ownerPath);
	}

	@Override
	public boolean contains(String canonicalPath) {
		return relatedPaths.contains(canonicalPath);
	}

	@Override
	public List<String> getLinks() {
		if (links == null) {
			ArrayList<String> objects = new ArrayList<>(relatedPaths.size() + 1);
			objects.add(ownerPath);
			objects.addAll(relatedPaths);
			links = objects.stream().distinct().collect(Collectors.toList());
		}

		return links;
	}

	public EditorGroupIndexValue addRelated(String value) {
		relatedPaths.add(value);
		return this;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EditorGroupIndexValue that = (EditorGroupIndexValue) o;

		if (ownerPath != null ? !ownerPath.equals(that.ownerPath) : that.ownerPath != null) return false;
		if (!title.equals(that.title)) return false;
		return relatedPaths != null ? relatedPaths.equals(that.relatedPaths) : that.relatedPaths == null;
	}

	@Override
	public int hashCode() {
		int result = ownerPath != null ? ownerPath.hashCode() : 0;
		result = 31 * result + title.hashCode();
		result = 31 * result + (relatedPaths != null ? relatedPaths.hashCode() : 0);
		return result;
	}

	public boolean isOwner(String canonicalPath) {
		return ownerPath.equals(canonicalPath);
	}

	@Override
	public String toString() {
		return "EditorGroupIndexValue{" +
			"title='" + title + '\'' +
			'}';
	}
}
