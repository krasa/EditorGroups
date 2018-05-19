package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.support.IndexCache;

public class ReindexThisFileAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		VirtualFile data = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		if (data != null) {
			IndexCache.getInstance(e.getProject()).reindex(data);
		}
	}
}
