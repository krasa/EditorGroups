package krasa.editorGroups.model;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.AlphaComparator;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Link {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(Link.class);

	@Nullable
	protected Icon icon;
	@Nullable
	protected Integer line;

	public Link() {
	}

	public Link(@Nullable Icon icon, @Nullable Integer line) {
		this.icon = icon;
		this.line = line;
	}

	public static List<Link> from(Collection<String> links) {
		ArrayList<Link> links1 = new ArrayList<>();
		for (String link : links) {
			links1.add(new PathLink(link));
		}
		links1.sort(AlphaComparator.INSTANCE);
		return links1;
	}


	public static List<Link> fromVirtualFiles(Collection<VirtualFile> links) {
		ArrayList<Link> links1 = new ArrayList<>();
		for (VirtualFile link : links) {
			links1.add(new VirtualFileLink(link));
		}
		links1.sort(AlphaComparator.INSTANCE);
		return links1;
	}

	public static Link from(VirtualFile file) {
		return new VirtualFileLink(file);
	}

	@NotNull
	public abstract String getPath();

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
		UISettings uiSettings = UISettings.getInstanceOrNull();
		if (uiSettings != null && uiSettings.getHideKnownExtensionInTabs()) {
			return getVirtualFile().getNameWithoutExtension();
		} else {
			return getVirtualFile().getName();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Link)) return false;

		Link link = (Link) o;

		if (icon != null ? !icon.equals(link.icon) : link.icon != null) return false;
		return line != null ? line.equals(link.line) : link.line == null;
	}

	@Override
	public int hashCode() {
		int result = icon != null ? icon.hashCode() : 0;
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

	public boolean fileEquals(VirtualFile currentFile) {
		return getPath().equals(currentFile.getPath());
	}
}
