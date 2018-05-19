package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import krasa.editorGroups.support.IndexCache;

public class ReindexAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		IndexCache.getInstance(e.getProject()).reindex();
	}
}
