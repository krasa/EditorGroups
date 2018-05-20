package krasa.editorGroups;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FolderGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EditorGroupTabTitleProvider implements EditorTabTitleProvider {

	/**
	 * TODO bad EP, no way to distinguishe between windows
	 */
	@Nullable
	@Override
	public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile virtualFile) {
		String presentableNameForUI = getPresentableNameForUI(project, virtualFile);

		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(virtualFile);

		return getTitle(project, textEditor, presentableNameForUI);
	}

	@NotNull
	public static String getPresentableNameForUI(@NotNull Project project, @NotNull VirtualFile file) {
		List<EditorTabTitleProvider> providers = DumbService.getInstance(project).filterByDumbAwareness(
			Extensions.getExtensions(EditorTabTitleProvider.EP_NAME));
		for (EditorTabTitleProvider provider : providers) {
			if (provider instanceof EditorGroupTabTitleProvider) {
				continue;
			}
			String result = provider.getEditorTabTitle(project, file);
			if (result != null) {
				return result;
			}
		}

		return file.getPresentableName();
	}


	/*not yet existing api*/
	@Nullable
	@Override
	public String getEditorTabTitle(Project project, VirtualFile virtualFile, FileEditor textEditor) {
		System.out.println("getEditorTabTitle project = [" + project + "], virtualFile = [" + virtualFile + "], textEditor = [" + textEditor + "]");
		String presentableNameForUI = getPresentableNameForUI(project, virtualFile);

		return getTitle(project, textEditor, presentableNameForUI);
	}

	private String getTitle(Project project, FileEditor textEditor, String presentableNameForUI) {
		EditorGroup group = null;
		if (textEditor != null) {
			group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}

		if (group != null && group.isValid() && !(group instanceof FolderGroup)) {
			presentableNameForUI = group.getPresentableTitle(project, presentableNameForUI);
		}
		return presentableNameForUI;
	}

}
