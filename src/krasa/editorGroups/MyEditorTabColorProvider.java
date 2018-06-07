package krasa.editorGroups;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class MyEditorTabColorProvider implements EditorTabColorProvider {

	@Nullable
	@Override
	public Color getProjectViewColor(@NotNull Project project, @NotNull VirtualFile file) {
		Color color = null;
		if (!file.isDirectory() && file.isInLocalFileSystem()) {
			EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
			if (userData != null) {
				color = userData.getBgColor();
			}
			if (color == null) {
				color = EditorGroupManager.getInstance(project).getColor(file);
			}
		}
		return color;
	}

	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(file);

		return getColor(project, textEditor, file);
	}

	/**
	 * since 2018.2
	 */
	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable EditorWindow editorWindow) {
		if (editorWindow != null) {
			for (EditorWithProviderComposite editor : editorWindow.getEditors()) {
				if (editor.getFile().equals(file)) {
					Pair<FileEditor, FileEditorProvider> pair = editor.getSelectedEditorWithProvider();
					FileEditor first = pair.first;
					return getColor(project, first, file);
				}
			}
		}
		return null;
	}

	/**
	 * https://youtrack.jetbrains.com/issue/IDEA-193430
	 */
	@Nullable
	@Override
	public Color getEditorTabForegroundColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable EditorWindow editorWindow) {
		long start = System.nanoTime();
		if (editorWindow != null) {
			for (EditorWithProviderComposite editor : editorWindow.getEditors()) {
				if (editor.getFile().equals(file)) {
					Pair<FileEditor, FileEditorProvider> pair = editor.getSelectedEditorWithProvider();
					FileEditor first = pair.first;
					Color fgColor = getFgColor(project, first, file);
					System.err.println(fgColor + "xx " + (System.nanoTime() - start) + "ms");
					return fgColor;
				}
			}
		}
		System.err.println("xxx " + (System.nanoTime() - start) + "ms");
		return null;
	}

	private Color getFgColor(Project project, FileEditor textEditor, VirtualFile file) {
		Color tabColor = null;

		if (textEditor != null) {
			EditorGroup group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
			if (group != null) {
				tabColor = group.getFgColor();
			}
		} else {
			tabColor = EditorGroupManager.getInstance(project).getFgColor(file);
		}

		return tabColor;
	}


	@Nullable
	private Color getColor(Project project, FileEditor textEditor, VirtualFile file) {
		Color tabColor = null;

		if (textEditor != null) {
			EditorGroup group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);

			if (group != null) {
				tabColor = group.getBgColor();
			}
		} else {
			tabColor = EditorGroupManager.getInstance(project).getColor(file);
		}

		return tabColor;
	}
}
