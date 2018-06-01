package krasa.editorGroups;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
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
	public IndexCache cache;

	/**
	 * protection for too fast switching - without getting triggering focuslistener - resulting in switching with a wrong group
	 */
	private boolean switching;
	private ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.getInstance();
	private EditorGroup switchingGroup;
	private PanelRefresher panelRefresher;

	public EditorGroupManager(Project project) {
		cache = IndexCache.getInstance(project);
		panelRefresher = PanelRefresher.getInstance(project);

		this.project = project;

	}


	public static EditorGroupManager getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, EditorGroupManager.class);
	}

	/**
	 * @param displayedGroup
	 */
	@NotNull
	EditorGroup getGroup(Project project, FileEditor fileEditor, @NotNull EditorGroup displayedGroup, @Nullable EditorGroup requestedGroup, boolean refresh) {
		System.out.println(">getGroup project = [" + project + "], fileEditor = [" + fileEditor + "], displayedGroup = [" + displayedGroup + "], requestedGroup = [" + requestedGroup + "], force = [" + refresh + "]");

		long start = System.currentTimeMillis();

		EditorGroup result = EditorGroup.EMPTY;
		if (requestedGroup == null) {
			requestedGroup = displayedGroup;
		}

		VirtualFile currentFile = Utils.getFileFromTextEditor(this.project, fileEditor);
		if (currentFile == null) {
			System.out.println("< getGroup - currentFile is null for " + fileEditor);
			return result;
		}

		String currentFilePath = currentFile.getCanonicalPath();


		boolean force = refresh && ApplicationConfiguration.state().forceSwitch;
		if (force) {
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}
			if (result.isInvalid()) {
				result = cache.getEditorGroupAsSlave(currentFilePath, false, true);
			}
		}

		if (result.isInvalid()) {
			cache.validate(requestedGroup);
			if (requestedGroup.isValid()) {
				result = requestedGroup;
			}
		}

		if (!force) {
			if (result.isInvalid()) {
				result = cache.getByOwner(currentFilePath);
			}

			if (result.isInvalid()) {
				result = cache.getEditorGroupAsSlave(currentFilePath, true, true);
			}
		}

		if (result.isInvalid()) {
			if (applicationConfiguration.getState().autoSameName) {
				result = AutoGroup.SAME_NAME_INSTANCE;
			} else if (applicationConfiguration.getState().autoFolders) {
				result = AutoGroup.DIRECTORY_INSTANCE;
			}
		}

		if (refresh || (result instanceof AutoGroup && result.size(project) == 0)) {
			//refresh
			if (result instanceof SameNameGroup) {
				result = cache.getSameNameGroup(currentFile);
			} else if (result instanceof FolderGroup) {
				result = cache.getFolderGroup(currentFile);
			} else if (result instanceof FavoritesGroup) {
				result = cache.getFavoritesGroup(result.getTitle());
			}

			if (applicationConfiguration.getState().autoFolders
				&& result instanceof SameNameGroup && result.size(project) <= 1
				&& !(requestedGroup instanceof SameNameGroup)
				&& !AutoGroup.SAME_FILE_NAME.equals(cache.getLast(currentFilePath))) {
				result = cache.getFolderGroup(currentFile);
			}
		} else if (result instanceof AutoGroup) {
			result = cache.updateGroups((AutoGroup) result, currentFilePath);
		}


		System.out.println("< getGroup " + (System.currentTimeMillis() - start) + "ms, file=" + currentFile.getName() + " title='" + result.getTitle() + "'");
		cache.setLast(currentFilePath, result);
		return result;
	}

	public void switching(boolean switching, EditorGroup group) {
		System.out.println("switching " + "[" + switching + "], group = [" + group + "]");
		this.switching = switching;
		switchingGroup = group;
	}

	public EditorGroup getSwitchingGroup() {
		return switchingGroup;
	}

	public boolean switching() {
		return switching;
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
		System.out.println("getAllGroups " + (System.currentTimeMillis() - start));
		return allGroups;
	}

	public void initCache() {
		panelRefresher.initCache();
	}

	public Color getColor(VirtualFile file) {
		String canonicalPath = file.getCanonicalPath();
		EditorGroup group = cache.getEditorGroupForColor(canonicalPath);
		if (group != null) {
			return group.getColor();
		}
		return null;
	}

	public void open(VirtualFile fileToOpen, EditorGroup group, boolean newWindow, boolean newTab) {
		CommandProcessor.getInstance().executeCommand(project, () -> {
			final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

			System.out.println("open " + "newTab = [" + newTab + "], fileToOpen = [" + fileToOpen + "], newWindow = [" + newWindow + "]");

			//must find window before opening the file!
			VirtualFile selectedFile = null;
			EditorWindow currentWindow = manager.getCurrentWindow();
			if (currentWindow != null) {
				selectedFile = currentWindow.getSelectedFile();
			}
			
			//not closing existing tab beforehand seems to have either no effect, or it is better, dunno
//		manager.closeFile(fileToOpen, false, false);

			switching(true, group);
			if (newWindow) {
				manager.openFileInNewWindow(fileToOpen);
			} else {
				FileEditor[] fileEditors = manager.openFile(fileToOpen, true);
				if (fileEditors.length == 0) {  //directory or some fail
					switching(false, null);
					return;
				}

				//not sure, but it seems to mess order of tabs less if we do it after opening a new tab
				if (selectedFile != null && !newTab) {
					manager.closeFile(selectedFile, currentWindow, false);
				}

			}
		}, null, null);
	}
}
