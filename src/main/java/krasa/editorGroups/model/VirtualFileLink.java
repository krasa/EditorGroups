package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class VirtualFileLink extends Link {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(VirtualFileLink.class);
  @NotNull
  private final VirtualFile virtualFile;

  public VirtualFileLink(@NotNull VirtualFile virtualFile) {
    this.virtualFile = virtualFile;
  }

  public VirtualFileLink(@NotNull VirtualFile file, Icon icon, int line) {
    super(icon, line);
    this.virtualFile = file;
  }

  public boolean isTheSameFile(@NotNull VirtualFile file) {
    return fileEquals(file);
  }

  @NotNull
  @Override
  public String getPath() {
    return virtualFile.getPath();
  }

  @Override
  public boolean exists() {
    return virtualFile.exists();
  }

  @Override
  @NotNull
  public VirtualFile getVirtualFile() {
    return virtualFile;
  }

  @NotNull
  @Override
  public String getName() {
    return virtualFile.getPresentableName();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    VirtualFileLink that = (VirtualFileLink) o;

    return virtualFile.equals(that.virtualFile);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + virtualFile.hashCode();
    return result;
  }

  @Override
  public boolean fileEquals(VirtualFile currentFile) {
    return virtualFile.equals(currentFile);
  }

  @Override
  public String toString() {
    return "VirtualFileLink{" +
      "virtualFile=" + virtualFile +
      ", icon=" + icon +
      ", line=" + line +
      '}';
  }
}
