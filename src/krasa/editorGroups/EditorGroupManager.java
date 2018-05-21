
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
import krasa.editorGroups.model.SameNameGroup;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * @idea.title CORE
 * @idea.related ./*
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
	private EditorGroup switchingGroup;

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
	 * @param displayedGroup
	 * @param force          if true - return this file's owned group instead of the last one, or anything closest
	 */
	@NotNull
	EditorGroup getGroup(Project project, FileEditor fileEditor, @NotNull EditorGroup displayedGroup, @Nullable EditorGroup requestedGroup, boolean force) {
		if (DumbService.isDumb(project)) {
			throw new RuntimeException("check for dumb");
		}
		System.out.println(">getGroup project = [" + project + "], fileEditor = [" + fileEditor + "], displayedGroup = [" + displayedGroup + "], requestedGroup = [" + requestedGroup + "], force = [" + force + "]");

		long start = System.currentTimeMillis();

		EditorGroup result = EditorGroup.EMPTY;
		if (requestedGroup == null) {
			requestedGroup = displayedGroup;
		}
		force = force && ApplicationConfiguration.state().forceSwitch;

		VirtualFile currentFile = Utils.getFileFromTextEditor(this.project, fileEditor);
		if (currentFile == null) {
			System.out.println("< getGroup - currentFile is null for " + fileEditor);
			return result;
		}

		String currentFilePath = currentFile.getCanonicalPath();


		if (force) {
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}
			if (result.isInvalid()) {
				result = cache.getEditorGroupAsSlave(currentFilePath);
			}
		}

		if (result.isInvalid()) {
			cache.validate(requestedGroup);
			if (requestedGroup.isValid()) {
				result = requestedGroup;
			}
		}

		if (!force) {
			if (requestedGroup instanceof SameNameGroup) {
				result = cache.getSameNameGroup(currentFile);
			} else if (requestedGroup instanceof FolderGroup) {
				result = cache.getFolderGroup(currentFile);
			}
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}

			if (result.isInvalid()) {
				result = cache.getEditorGroupAsSlave(currentFilePath);
			}
		}
		if (result.isInvalid()) {
			if (applicationConfiguration.getState().autoSameName || requestedGroup instanceof SameNameGroup) {
				result = cache.getSameNameGroup(currentFile);
			} else if (applicationConfiguration.getState().autoFolders || requestedGroup instanceof FolderGroup) {
				result = cache.getFolderGroup(currentFile);
			}
		}

		//TODO initalize autogroups on background

		System.out.println("< getGroup " + (System.currentTimeMillis() - start) + "ms, file=" + currentFile.getName() + " title='" + result.getTitle() + "'");
		cache.setLast(currentFilePath, result);
		return result;
	}

	public void switching(boolean b, EditorGroup group) {
		switching = b;
		switchingGroup = group;
	}

	public EditorGroup getSwitchingGroup() {
		return switchingGroup;
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

	public List<EditorGroupIndexValue> getAllGroups() {
		long start = System.currentTimeMillis();
		List<EditorGroupIndexValue> allGroups = cache.getAllGroups();
		Collections.sort(allGroups, new Comparator<EditorGroupIndexValue>() {
			@Override
			public int compare(EditorGroupIndexValue o1, EditorGroupIndexValue o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
		System.err.println("getAllGroups " + (System.currentTimeMillis() - start));
		return allGroups;
	}
}
