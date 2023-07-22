package krasa.editorGroups.support;

import com.intellij.ide.util.treeView.FileNameComparator;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Comparator;

public final class VirtualFileComparator implements Comparator<VirtualFile> {
  private static final VirtualFileComparator ourInstance = new VirtualFileComparator();

  public static VirtualFileComparator getInstance() {
    return ourInstance;
  }

  @Override
  public int compare(final VirtualFile o1, final VirtualFile o2) {
    return FileNameComparator.INSTANCE.compare(o1.getPath(), o2.getPath());
  }
}
