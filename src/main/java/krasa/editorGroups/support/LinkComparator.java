package krasa.editorGroups.support;

import com.intellij.ide.util.treeView.FileNameComparator;
import krasa.editorGroups.model.Link;

import java.util.Comparator;

public class LinkComparator implements Comparator<Link> {
  public static final LinkComparator INSTANCE = new LinkComparator();

  protected LinkComparator() {
  }

  @Override
  public int compare(Link link1, Link link2) {
    String s1 = link1.getPath();
    String s2 = link2.getPath();

    return FileNameComparator.INSTANCE.compare(s1, s2);
  }
}
