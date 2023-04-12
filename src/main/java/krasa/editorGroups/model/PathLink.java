package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PathLink extends Link {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(PathLink.class);
	@NotNull
	private String path;

	public PathLink(@NotNull String path) {
		this.path = FileUtil.toSystemIndependentName(path);
	}

	public PathLink(@NotNull String path, @Nullable Icon icon, @Nullable Integer line) {
		this.path = FileUtil.toSystemIndependentName(path);
		this.icon = icon;
		this.line = line;
	}


	@NotNull
	@Override
	public String getPath() {
		return path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		PathLink pathLink = (PathLink) o;

		return path.equals(pathLink.path);
	}

	@Override
	public int hashCode() {
		int result = path.hashCode();
		result = 31 * result + (line != null ? line.hashCode() : 0);
		return result;
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
