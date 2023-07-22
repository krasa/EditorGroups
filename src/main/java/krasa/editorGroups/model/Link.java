package krasa.editorGroups.model;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.LinkComparator;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class Link {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(Link.class);

  @Nullable
  protected Icon icon;
  @Nullable
  protected Integer line;
  @NotNull
  private final Project project;

  public Link(@NotNull Project project) {
    this.project = project;
  }

  public Link(@Nullable Icon icon, @Nullable Integer line, Project project) {
    this(project);
    this.icon = icon;
    this.line = line;
  }

  public static List<Link> from(Collection<String> links, Project project) {
    ArrayList<Link> links1 = new ArrayList<>();
    for (String link : links) {
      links1.add(new PathLink(link, project));
    }
    links1.sort(LinkComparator.INSTANCE);
    return links1;
  }


  public static List<Link> fromVirtualFiles(Collection<VirtualFile> links, Project project) {
    ArrayList<Link> links1 = new ArrayList<>();
    for (VirtualFile link : links) {
      links1.add(new VirtualFileLink(link, project));
    }
    links1.sort(LinkComparator.INSTANCE);
    return links1;
  }

  public static Link from(VirtualFile file, Project project) {
    return new VirtualFileLink(file, project);
  }

  @NotNull
  public abstract String getPath();

  public boolean exists() {
    return new File(getPath()).exists();
  }

  public Icon getFileIcon() {
    return icon != null ? icon : Utils.getFileIcon(getPath(), project);
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    return Utils.getVirtualFileByAbsolutePath(getPath());
  }

  @NotNull
  public String getName() {
    UISettings uiSettings = UISettings.getInstanceOrNull();
    VirtualFile virtualFile = getVirtualFile();
    if (virtualFile == null) {
      LOG.warn("VirtualFile is null for " + getPath());
      return getPath();
    }
    if (uiSettings != null && uiSettings.getHideKnownExtensionInTabs()) {
      return virtualFile.getNameWithoutExtension();
    } else {
      return virtualFile.getName();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Link link)) return false;

    if (!Objects.equals(icon, link.icon)) return false;
    return Objects.equals(line, link.line);
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
