package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.IndexCache;

public class ReindexAction extends AnAction {
	private static final Logger LOG = Logger.getInstance(ReindexAction.class);


	@Override
	public void actionPerformed(AnActionEvent e) {
		if (LOG.isDebugEnabled()) LOG.debug("INDEXING START " + System.currentTimeMillis());
		IndexCache.getInstance(e.getProject()).clear();
		FileBasedIndex.getInstance().requestRebuild(EditorGroupIndex.NAME);
//		FileBasedIndex.getInstance().requestRebuild(FilenameWithoutExtensionIndex.NAME);
	}
}
