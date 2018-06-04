package krasa.editorGroups;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.AutoGroup;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EditorGroupTabTitleProvider implements EditorTabTitleProvider {

	@Nullable
	@Override
	public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile virtualFile) {
		String presentableNameForUI = getPresentableNameForUI(project, virtualFile, null);

		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(virtualFile);

		return getTitle(project, textEditor, presentableNameForUI);
	}

	/**
	 * since 2018.2
	 */
	@Nullable
	@Override
	public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file, @Nullable EditorWindow editorWindow) {
		String presentableNameForUI = getPresentableNameForUI(project, file, editorWindow);

		if (editorWindow != null) {
			for (EditorWithProviderComposite editor : editorWindow.getEditors()) {
				if (editor.getFile().equals(file)) {
					Pair<FileEditor, FileEditorProvider> pair = editor.getSelectedEditorWithProvider();
					FileEditor first = pair.first;
					return getTitle(project, first, presentableNameForUI);
				}
			}
		}

		return presentableNameForUI;
	}

	@NotNull
	public static String getPresentableNameForUI(@NotNull Project project, @NotNull VirtualFile file, EditorWindow editorWindow) {
		List<EditorTabTitleProvider> providers = DumbService.getInstance(project).filterByDumbAwareness(
			Extensions.getExtensions(EditorTabTitleProvider.EP_NAME));
		for (EditorTabTitleProvider provider : providers) {
			if (provider instanceof EditorGroupTabTitleProvider) {
				continue;
			}
			String result = provider.getEditorTabTitle(project, file, editorWindow);
			if (result != null) {
				return result;
			}
		}

		return file.getPresentableName();
	}

	private String getTitle(Project project, FileEditor textEditor, String presentableNameForUI) {
		EditorGroup group = null;
		if (textEditor != null) {
			group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}

		if (group != null && group.isValid() && !(group instanceof AutoGroup)) {
			presentableNameForUI = group.getPresentableTitle(project, presentableNameForUI, ApplicationConfiguration.state().showSize);
		}
		return presentableNameForUI;
	}

}
