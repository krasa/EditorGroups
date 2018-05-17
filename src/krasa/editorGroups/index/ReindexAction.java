package krasa.editorGroups.index;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.indexing.FileBasedIndex;

public class ReindexAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		System.out.println("requestRebuild");
		FileBasedIndex.getInstance().requestRebuild(EditorGroupIndex.NAME);
	}
}
