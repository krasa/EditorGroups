package krasa.editorGroups.tabs2.my;

import com.intellij.openapi.diagnostic.Logger;
import krasa.editorGroups.tabs2.impl.JBDefaultTabPainter;

public class EditorGroupsJBDefaultTabPainter extends JBDefaultTabPainter {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(EditorGroupsJBDefaultTabPainter.class);

	public EditorGroupsJBDefaultTabPainter() {
		super(new EditorGroupsTabTheme());
	}
}
