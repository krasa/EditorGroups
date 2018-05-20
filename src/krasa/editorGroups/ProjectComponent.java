package krasa.editorGroups;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.support.HackedJBScrollPane;
import krasa.editorGroups.support.IndexCache;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent {
	private final Project project;

	public ProjectComponent(Project project) {
		this.project = project;
	}

	@Override
	public void projectOpened() {
		initCache();

		project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			/**on EDT*/
			@Override
			public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
				System.out.println("fileOpened " + file);
//				EditorGroup userData = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
//				if (userData != null) {
//					EditorGroupManager.getInstance(project).setCurrentGroup(userData);
//				}
//				EditorGroupManager.getInstance(project).reparse(file);
				final FileEditor[] fileEditors = manager.getAllEditors(file);
				for (final FileEditor fileEditor : fileEditors) {
					if (fileEditor instanceof TextEditorImpl) {
						Editor editor = ((TextEditorImpl) fileEditor).getEditor();
						if (editor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
							continue;
						}

						FileEditor textEditor = FileEditorManagerImpl.getInstanceEx(project).getSelectedEditor(file);
						EditorGroup userData = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);

						EditorGroupPanel panel = new EditorGroupPanel((TextEditorImpl) fileEditor, project, userData, file);
						JScrollPane scrollPane = new HackedJBScrollPane(panel);


						panel.setScrollPane((JBScrollPane) scrollPane);
						manager.addTopComponent(fileEditor, scrollPane);
					}
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
			}
		});

	}

	private void initCache() {

		DumbService.getInstance(project).runWhenSmart(new Runnable() {
			@Override
			public void run() {
				long start = System.currentTimeMillis();
				FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
				IndexCache instance = IndexCache.getInstance(project);
				Processor<String> processor = new Processor<String>() {
					@Override
					public boolean process(String s) {
						List<EditorGroupIndexValue> values = fileBasedIndex.getValues(EditorGroupIndex.NAME, s, GlobalSearchScope.allScope(project));
						for (EditorGroupIndexValue value : values) {
							instance.initGroup(value);
						}
						return true;
					}
				};
				fileBasedIndex.processAllKeys(EditorGroupIndex.NAME, processor, project);
				System.err.println("initCache " + (System.currentTimeMillis() - start));
			}
		});
	}

}
