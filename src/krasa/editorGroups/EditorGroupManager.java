
package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.MyFileManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.model.FolderGroup;
import krasa.editorGroups.support.IndexCache;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/*
 * @idea.title CORE
 * @idea.related ./*
 * @idea.related support/IndexCache.java
 * @idea.related support/FileResolver.java
 */
public class EditorGroupManager {


	private final Project project;
	//	@NotNull
//	private EditorGroup currentGroup = EditorGroup.EMPTY;
	IndexCache cache; 

	/**
	 * protection for too fast switching - without getting triggering focuslistener - resulting in switching with a wrong group
	 */
	private boolean switching;
	private ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.getInstance();

	public EditorGroupManager(Project project) {
		cache = IndexCache.getInstance(project);

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

	/**
	 * @param force if true - return this file's owned group instead of the last one
	 */
	@NotNull
	EditorGroup getGroup(Project project, FileEditor fileEditor, @NotNull EditorGroup lastGroup, boolean force) {
		if (DumbService.isDumb(project)) {
			throw new RuntimeException("check for dumb");
		}
		long start = System.currentTimeMillis();

		EditorGroup result = EditorGroup.EMPTY;
		System.out.println("getGroup: " + fileEditor + " lastGroup:" + lastGroup.getTitle() + " reparse:" + force);


		VirtualFile currentFile = Utils.getFileFromTextEditor(this.project, fileEditor);
		if (currentFile == null) {
			System.out.println("< getGroup - currentFile is null for " + fileEditor);
			return EditorGroup.EMPTY;
		}
		String currentFilePath = currentFile.getCanonicalPath();
		if (force) {
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}
		}

		if (result.isInvalid()) {
			cache.validate(lastGroup);
			if (lastGroup.isValid()) {
				result = lastGroup;
			}
		}

		if (!force) {//already tried
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}
		}


		if (result.isInvalid() || (force && !(result instanceof EditorGroupIndexValue))) {
			result = cache.getEditorGroupAsSlave(currentFilePath);


			if (applicationConfiguration.getState().autoFolders) {
				if (result.isInvalid() || result instanceof FolderGroup) {  //create or refresh
					result = cache.getFolderGroup(currentFile, result);
				}
			} else if (result instanceof FolderGroup) {
				result = EditorGroup.EMPTY;
			}
		}


		System.out.println("< getGroup " + (System.currentTimeMillis() - start) + "ms, file=" + currentFile.getName() + " title='" + result.getTitle() + "'");
		cache.setLast(currentFilePath, result);
		return result;
	}

	public void switching(boolean b) {
		switching = b;
	}

	public boolean switching() {
		return switching;
	}

	public EditorGroupIndexValue onIndexingDone(String ownerPath, EditorGroupIndexValue group) {
		group = cache.onIndexingDone(group);
		if (DumbService.isDumb(project)) { //optimization
			return group;
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
		return group;
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
					MyFileManager.updateTitle(project, selectedEditor.getFile());
					
				}
			}
		}

		System.out.println("onSmartMode " + (System.currentTimeMillis() - start) + "ms " + Thread.currentThread().getName());
	}

	public Collection<EditorGroup> getGroups(VirtualFile file) {
		return cache.getGroups(file.getCanonicalPath());
	}
}
