package krasa.editorGroups.support;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import krasa.editorGroups.EditorGroupPanel;
import org.jetbrains.annotations.NotNull;

public class MyEditorNotificationsProvider extends EditorNotifications.Provider<EditorGroupPanel> implements DumbAware {

	private static final Key<EditorGroupPanel> KEY = Key.create("EditorGroups");

	@NotNull
	@Override
	public Key<EditorGroupPanel> getKey() {
		return KEY;
	}

	/**
	 * on background, called many times
	 */
	public EditorGroupPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
//		if (fileEditor instanceof PsiAwareTextEditorImpl) {
//			DataProvider component = (DataProvider) fileEditor.getComponent();
//			PsiFile data = LangDataKeys.PSI_FILE.getData(component);
//			if (data != null && fileEditor instanceof TextEditorImpl) {
//				TextEditorImpl textEditor = (TextEditorImpl) fileEditor;
//				Editor editor = textEditor.getEditor();
//				Project project = editor.getProject();
//				if (project != null) {
//					EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//					return new EditorGroupPanel(textEditor, project, userData, file);
//				}
//			}
//		}
		return null;
	}
}