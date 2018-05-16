package krasa.editorGroups;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent {
	private final Project project;

	public ProjectComponent(Project project) {

		this.project = project;
	}

	@Override
	public void projectOpened() {
		project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			/**on EDT*/
			@Override
			public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
				System.out.println("fileOpened " + file);
//				EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//				if (userData != null) {
//					EditorGroupManager.getInstance(project).setCurrentGroup(userData);
//				}
				EditorGroupManager.getInstance(project).reparse(file);
				final FileEditor[] fileEditors = manager.getAllEditors(file);
				for (final FileEditor fileEditor : fileEditors) {
					if (fileEditor instanceof TextEditorImpl) {
						Editor editor = ((TextEditorImpl) fileEditor).getEditor();
						if (editor.getUserData(EditorGroupPanel.EDITOR_GROUPS_PANEL) != null) {
							continue;
						}

						EditorGroupPanel panel = new EditorGroupPanel((TextEditorImpl) fileEditor, project, file.getUserData(EditorGroupPanel.EDITOR_GROUP), file);
						manager.addTopComponent(fileEditor, panel);
					}
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
				file.putUserData(EditorGroupPanel.EDITOR_GROUP, null);
			}
		});

	}
}
