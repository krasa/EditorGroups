package krasa.editorGroups;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
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
				color = userData.getColor();
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

	//non existing API
	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable FileEditor editor) {
		return getColor(project, editor, file);
	}

	@Nullable
	private Color getColor(Project project, FileEditor textEditor, VirtualFile file) {
		Color tabColor = null;

		if (textEditor != null) {
			EditorGroup group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);

			if (group != null) {
				tabColor = group.getColor();
			}
		} else {
			tabColor = EditorGroupManager.getInstance(project).getColor(file);
		} 

		return tabColor;
	}
}
