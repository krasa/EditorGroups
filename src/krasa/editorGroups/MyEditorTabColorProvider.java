package krasa.editorGroups;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class MyEditorTabColorProvider implements EditorTabColorProvider {

	@Nullable
	@Override
	public Color getProjectViewColor(@NotNull Project project, @NotNull VirtualFile file) {
		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(file);
		EditorGroup group = null;
		if (textEditor != null) {
			group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}
		Color tabColor;
		if (group != null && group.isValid()) {
			tabColor = group.getTabColor();
		} else {
			tabColor = EditorGroupManager.getInstance(project).getColor(file);
		}
		if (tabColor != null) {
			return tabColor;
		}

		List<EditorTabColorProvider> providers = DumbService.getInstance(project).filterByDumbAwareness(
			Extensions.getExtensions(EditorTabColorProvider.EP_NAME));
		for (EditorTabColorProvider provider : providers) {
			if (provider instanceof MyEditorTabColorProvider) {
				continue;
			}
			Color result = provider.getProjectViewColor(project, file);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
		FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(file);

		return getColor(project, file, textEditor);
	}

	@Nullable
	@Override
	public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file, @Nullable FileEditor editor) {
		return getColor(project, file, editor);
	}

	@Nullable
	private Color getColor(@NotNull Project project, @NotNull VirtualFile file, FileEditor textEditor) {
		EditorGroup group = null;
		if (textEditor != null) {
			group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		}

		if (group != null && group.isValid()) {
			Color tabColor = group.getTabColor();
			if (tabColor != null) {
				return tabColor;
			}
		}
		List<EditorTabColorProvider> providers = DumbService.getInstance(project).filterByDumbAwareness(
			Extensions.getExtensions(EditorTabColorProvider.EP_NAME));
		for (EditorTabColorProvider provider : providers) {
			if (provider instanceof MyEditorTabColorProvider) {
				continue;
			}
			Color result = provider.getEditorTabColor(project, file, textEditor);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
