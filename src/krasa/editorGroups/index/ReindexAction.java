package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.support.IndexCache;

public class ReindexAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		FileBasedIndex.getInstance().requestRebuild(EditorGroupIndex.NAME);
		IndexCache.getInstance(e.getProject()).clear();
	}
}
