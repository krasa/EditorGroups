package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.AlphaComparator;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Link {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(Link.class);
	@NotNull
	private String path;
	@Nullable
	private Icon icon;
	@Nullable
	private Integer line;

	public Link(@NotNull String path) {
		this.path = FileUtil.toSystemIndependentName(path);
	}

	public Link(@NotNull String path, @Nullable Icon icon, @Nullable Integer line) {
		this.path = FileUtil.toSystemIndependentName(path);
		this.icon = icon;
		this.line = line;
	}

	public static List<Link> from(Collection<String> links) {
		ArrayList<Link> links1 = new ArrayList<>();
		for (String link : links) {
			links1.add(new Link(link));
		}
		Collections.sort(links1, AlphaComparator.INSTANCE);
		return links1;
	}

	public boolean isTheSameFile(@NotNull VirtualFile file) {
		return getPath().equals(file.getPath());
	}

	@NotNull
	public String getPath() {
		return path;
	}

	public boolean exists() {
		return new File(getPath()).exists();
	}

	public Icon getFileIcon() {
		return icon != null ? icon : Utils.getFileIcon(getPath());
	}

	public VirtualFile getVirtualFile() {
		return Utils.getVirtualFileByAbsolutePath(getPath());
	}


	@NotNull
	public String getName() {
		return Utils.toPresentableName(getPath());
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Link link = (Link) o;

		if (!path.equals(link.path)) return false;
		return line != null ? line.equals(link.line) : link.line == null;
	}

	@Override
	public int hashCode() {
		int result = path.hashCode();
		result = 31 * result + (line != null ? line.hashCode() : 0);
		return result;
	}

	@Nullable
	public Icon getIcon() {
		return icon;
	}

	@Nullable
	public Integer getLine() {
		return line;
	}

	@Override
	public String toString() {
		return "Link{" +
			"line=" + line +
			", path='" + path + '\'' +
			", icon=" + icon +
			'}';
	}
}
