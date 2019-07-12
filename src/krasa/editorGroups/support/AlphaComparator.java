package krasa.editorGroups.support;

import com.intellij.ide.util.treeView.FileNameComparator;
import krasa.editorGroups.model.Link;

import java.util.Comparator;

public class AlphaComparator implements Comparator<Link> {
	public static final AlphaComparator INSTANCE = new AlphaComparator();

	protected AlphaComparator() {
	}

	@Override
	public int compare(Link link1, Link link2) {
		String s1 = link1.getPath();
		String s2 = link2.getPath();
		if (s1 == null) return s2 == null ? 0 : -1;
		if (s2 == null) return +1;

		return FileNameComparator.INSTANCE.compare(s1, s2);
	}
}
