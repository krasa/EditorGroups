
package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.support.Cache;
import krasa.editorGroups.support.IndexCache;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/*
 * @idea.title CORE
 * @idea.related EditorGroupPanel.java
 * @idea.related ProjectComponent.java
 * @idea.related EditorGroupTabTitleProvider.java
 */
public class EditorGroupManager {


	private final Project project;
	//	@NotNull
//	private EditorGroup currentGroup = EditorGroup.EMPTY;
	Cache cache = new IndexCache();

	/**
	 * protection for too fast switching - without getting triggering focuslistener - resulting in switching with a wrong group
	 */
	private boolean switching;

	public EditorGroupManager(Project project) {

		this.project = project;

		project.getMessageBus().connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
			@Override
			public void enteredDumbMode() {
			}

			@Override
			public void exitDumbMode() {
				onSmartMode();
			}
		});
	}


	public static EditorGroupManager getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, EditorGroupManager.class);
	}


	@NotNull
	EditorGroup getGroup(Project project, FileEditor fileEditor, @NotNull EditorGroup lastGroup, boolean force) {
		if (DumbService.isDumb(project)) {
			throw new RuntimeException("check for dumb");
		}

		EditorGroup result = EditorGroup.EMPTY;
		System.out.println("getGroup: " + fileEditor + " lastGroup:" + lastGroup.getTitle() + " reparse:" + force);


		VirtualFile currentFile = Utils.getFileFromTextEditor(this.project, fileEditor);
		if (currentFile == null) {
			System.out.println("< getGroup - currentFile is null for " + fileEditor);
			return EditorGroup.EMPTY;
		}
		String currentFilePath = currentFile.getCanonicalPath();
		if (force) {
			if (result.invalid()) {
				result = cache.getByOwner(project, currentFilePath);
			}
		}

		if (result.invalid()) {
			cache.validate(project, lastGroup);
			if (lastGroup.valid()) {
				result = lastGroup;
			}
		}

		if (result.invalid()) {
			result = cache.getByOwner(project, currentFilePath);
		}


		if (result.invalid()) {
			List<EditorGroup> groupsAsSlave = cache.findGroupsAsSlave(project, currentFilePath);
			//TODO union?
			for (EditorGroup editorGroup : groupsAsSlave) {
				result = editorGroup;
				break;
			}
		}


		if (result.invalid()) {
			System.out.println("no group found");
		}
		System.out.println("< getGroup " + fileEditor.getName() + " " + result.getTitle());
		return result;
	}

	public void switching(boolean b) {
		switching = b;
	}

	public boolean switching() {
		return switching;
	}

	public void onIndexingDone(String ownerPath, EditorGroupIndexValue group) {
		if (DumbService.isDumb(project)) { //optimization
			return;
		}

		long start = System.currentTimeMillis();
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		for (FileEditor selectedEditor : manager.getAllEditors()) {
			if (selectedEditor instanceof TextEditor) {
				Editor editor = ((TextEditor) selectedEditor).getEditor();
				EditorGroupPanel panel = editor.getUserData(EditorGroupPanel.EDITOR_PANEL);
				if (panel != null) {
					panel.onIndexingDone(ownerPath, group);
				}
			}
		}

		System.out.println("onIndexingDone " + (System.currentTimeMillis() - start) + "ms " + Thread.currentThread().getName());
	}

	/*hopefully it wont cause lags*/
	private void onSmartMode() {
		long start = System.currentTimeMillis();
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		for (FileEditor selectedEditor : manager.getAllEditors()) {
			if (selectedEditor instanceof TextEditor) {
				Editor editor = ((TextEditor) selectedEditor).getEditor();
				EditorGroupPanel panel = editor.getUserData(EditorGroupPanel.EDITOR_PANEL);
				if (panel != null) {
					panel.refresh(false, null);
				}
			}
		}

		System.out.println("onSmartMode " + (System.currentTimeMillis() - start) + "ms " + Thread.currentThread().getName());
	}

}
