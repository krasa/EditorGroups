package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;

public class ReindexThisFileAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		VirtualFile data = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		if (data != null) {
			FileBasedIndex.getInstance().requestReindex(data);
		}
	}
}
