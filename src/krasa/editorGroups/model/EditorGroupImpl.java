package krasa.editorGroups.model;

import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.Utils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditorGroupImpl implements EditorGroup {

	private List<String> paths;
	private String ownerPath;
	private String title = null;
	private transient List<String> links;

	public EditorGroupImpl(@NotNull List<String> paths, @NotNull String ownerPath, String title) {
		this.paths = paths;
		this.ownerPath = ownerPath;
		this.title = title;
	}

	public EditorGroupImpl() {
	}

	@Override
	public String getOwnerPath() {
		return ownerPath;
	}

	@Override
	public List<String> getRelatedPaths() {
		return paths.stream().distinct().collect(Collectors.toList());
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public boolean valid() {
		return title != null || paths.size() > 1;//1 for owner
	}

	@Override
	public int size() {
		return paths.size();
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
		return paths.contains(canonicalPath);
	}

	@Override
	public List<String> getLinks() {
		if (links == null) {
			ArrayList<String> objects = new ArrayList<>(paths.size() + 1);
			objects.add(ownerPath);
			objects.addAll(paths);
			links = objects.stream().distinct().collect(Collectors.toList());
		}

		return links;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("paths", paths)
			.append("ownerPath", ownerPath)
			.append("title", title)
			.toString();
	}
}
