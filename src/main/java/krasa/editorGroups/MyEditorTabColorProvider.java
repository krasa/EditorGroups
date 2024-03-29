package krasa.editorGroups;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class MyEditorTabColorProvider implements EditorTabColorProvider {

//	@Nullable
//	@Override
//	public Color getProjectViewColor(@NotNull Project project, @NotNull VirtualFile file) {
//		Color color = null;
//		if (!file.isDirectory() && file.isInLocalFileSystem()) {
//			EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//			if (userData != null) {
//				color = userData.getBgColor();
//			}
//			if (color == null) {
//				color = EditorGroupManager.getInstance(project).getColor(file);
//			}
//		}
//		return color;
//	}

	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
//		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(file);

		return getBgColor(project, null, file);
	}

//	/**
//	 * since 2018.2
//	 * to 2021.3
//	 */
//	@Nullable
//	@Override
//	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable EditorWindow editorWindow) {
//		if (editorWindow != null) {
//			for (EditorWithProviderComposite editor : editorWindow.getEditors()) {
//				if (editor.getFile().equals(file)) {
//					Pair<FileEditor, FileEditorProvider> pair = editor.getSelectedEditorWithProvider();
//					FileEditor first = pair.first;
//					return getBgColor(project, first, file);
//				}
//			}
//		}
//		EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//		if (userData != null) {
//			return userData.getBgColor();
//		}
//		return null;
//	}

	/**
	 * https://youtrack.jetbrains.com/issue/IDEA-193430
	 */
//	@Nullable
//	@Override
//	public Color getEditorTabForegroundColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable EditorWindow editorWindow) {
//		if (editorWindow != null) {
//			for (EditorWithProviderComposite editor : editorWindow.getEditors()) {
//				if (editor.getFile().equals(file)) {
//					Pair<FileEditor, FileEditorProvider> pair = editor.getSelectedEditorWithProvider();
//					FileEditor first = pair.first;
//					return getFgColor(project, first, file);
//				}
//			}
//		}
//		EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//		if (userData != null) {
//			return userData.getFgColor();
//		}
//		return null;
//	}
	private Color getFgColor(Project project, FileEditor textEditor, VirtualFile file) {
		Color fgColor = null;

		EditorGroup group = null;
		if (textEditor != null) {
			group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}
		if (group != null) {
			fgColor = group.getFgColor();
		}

		return fgColor;
	}


	@Nullable
	private Color getBgColor(Project project, FileEditor textEditor, VirtualFile file) {
		Color bgColor = null;

		EditorGroup group = null;
		if (file != null) {
			group = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}
		if (group != null) {
			bgColor = group.getBgColor();
		}

		return bgColor;
	}

}
