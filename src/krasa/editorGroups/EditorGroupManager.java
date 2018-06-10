package krasa.editorGroups;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EditorGroupManager {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(EditorGroupManager.class);

	private final Project project;
	//	@NotNull
//	private EditorGroup currentGroup = EditorGroup.EMPTY;
	public IndexCache cache;

	private ApplicationConfigurationComponent config = ApplicationConfigurationComponent.getInstance();
	private PanelRefresher panelRefresher;
	/**
	 * protection for too fast switching - without getting triggering focuslistener - resulting in switching with a wrong group
	 */
	private volatile boolean switching;
	private volatile EditorGroup switchingGroup;
	private volatile VirtualFile switchingFile;
	public int myScrollOffset = 0;
	private IdeFocusManager ideFocusManager;
	private boolean warningShown;
	private ExternalGroupProvider externalGroupProvider;
	private AutoGroupProvider autogroupProvider;
	private Key<?> initial_editor_index;

	public EditorGroupManager(Project project, PanelRefresher panelRefresher, IdeFocusManager ideFocusManager, ExternalGroupProvider externalGroupProvider, AutoGroupProvider autogroupProvider, IndexCache cache) {
		this.cache = cache;
		this.panelRefresher = panelRefresher;
		this.ideFocusManager = ideFocusManager;
		this.project = project;
		this.externalGroupProvider = externalGroupProvider;
		this.autogroupProvider = autogroupProvider;

		//TODO 
		try {
			//noinspection deprecation
			initial_editor_index = Key.findKeyByName("initial editor index");
		} catch (Exception e) {
			LOG.error(e);
		}
	}


	public static EditorGroupManager getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, EditorGroupManager.class);
	}

	@NotNull
	EditorGroup getGroup(Project project, FileEditor fileEditor, @NotNull EditorGroup displayedGroup, @Nullable EditorGroup requestedGroup, boolean refresh, @NotNull VirtualFile currentFile) {

		if (LOG.isDebugEnabled())
			LOG.debug(">getGroup project = [" + project + "], fileEditor = [" + fileEditor + "], displayedGroup = [" + displayedGroup + "], requestedGroup = [" + requestedGroup + "], force = [" + refresh + "]");

		long start = System.currentTimeMillis();

		EditorGroup result = EditorGroup.EMPTY;
		if (requestedGroup == null) {
			requestedGroup = displayedGroup;
		}


		String currentFilePath = currentFile.getCanonicalPath();


		boolean force = refresh && ApplicationConfiguration.state().isForceSwitch();
		if (force) {
			if (result.isInvalid()) {
				result = cache.getOwningOrSingleGroup(currentFilePath);
			}
			if (result.isInvalid()) {
				result = cache.getLastEditorGroup(currentFilePath, false, true);
			}
		}

		if (result.isInvalid()) {
			cache.validate(requestedGroup);
			if (requestedGroup.isValid()
				&& (requestedGroup instanceof AutoGroup || requestedGroup.containsLink(project, currentFilePath) || requestedGroup.isOwner(currentFilePath))) {
				result = requestedGroup;
			}
		}

		if (!force) {
			if (result.isInvalid()) {
				result = cache.getOwningOrSingleGroup(currentFilePath);
			}

			if (result.isInvalid()) {
				result = cache.getLastEditorGroup(currentFilePath, true, true);
			}
		}

		if (result.isInvalid()) {
			if (config.getState().isAutoSameName()) {
				result = AutoGroup.SAME_NAME_INSTANCE;
			} else if (config.getState().isAutoFolders()) {
				result = AutoGroup.DIRECTORY_INSTANCE;
			}
		}

		if (refresh || (result instanceof AutoGroup && result.size(project) == 0)) {
			//refresh
			if (result == requestedGroup && result instanceof EditorGroupIndexValue) { // force loads new one from index
				cache.initGroup((EditorGroupIndexValue) result);
			} else if (result instanceof SameNameGroup) {
				result = autogroupProvider.getSameNameGroup(currentFile);
			} else if (result instanceof FolderGroup) {
				result = autogroupProvider.getFolderGroup(currentFile);
			} else if (result instanceof FavoritesGroup) {
				result = externalGroupProvider.getFavoritesGroup(result.getTitle());
			}


			if (result instanceof SameNameGroup && result.size(project) <= 1 && !(requestedGroup instanceof SameNameGroup)) {
				EditorGroup slaveGroup = cache.getSlaveGroup(currentFilePath);
				if (slaveGroup.isValid()) {
					result = slaveGroup;
				} else if (config.getState().isAutoFolders()
					&& !AutoGroup.SAME_FILE_NAME.equals(cache.getLast(currentFilePath))) {
					result = autogroupProvider.getFolderGroup(currentFile);
				}
			}
		}

//		if (result instanceof AutoGroup) {
//			result = cache.updateGroups((AutoGroup) result, currentFilePath);
//		}


		if (LOG.isDebugEnabled())
			LOG.debug("< getGroup " + (System.currentTimeMillis() - start) + "ms, file=" + currentFile.getName() + " title='" + result.getTitle() + "'");
		cache.setLast(currentFilePath, result);
		return result;
	}

	public void switching(boolean switching, @NotNull EditorGroup group, @NotNull VirtualFile fileToOpen, int myScrollOffset) {
		if (LOG.isDebugEnabled())
			LOG.debug("switching " + "switching = [" + switching + "], group = [" + group + "], fileToOpen = [" + fileToOpen + "], myScrollOffset = [" + myScrollOffset + "]");
		this.myScrollOffset = myScrollOffset;
		switchingFile = fileToOpen;
		this.switching = switching;
		switchingGroup = group;
	}

	public void switching(boolean b) {
		ideFocusManager.doWhenFocusSettlesDown(() -> {
			if (LOG.isDebugEnabled()) LOG.debug("switching " + " [" + b + "]");
			switching = false;
		});
	}

	public EditorGroup getSwitchingGroup(@NotNull VirtualFile file) {
		if (file.equals(switchingFile)) {
			EditorGroup switchingGroup = this.switchingGroup;
			this.switchingGroup = null;
			return switchingGroup;
		}
		if (LOG.isDebugEnabled())
			LOG.debug("getSwitchingGroup returning null for " + "file = [" + file + "], switchingFile=" + switchingFile);
		return null;
	}

	public boolean switching() {
		return switching;
	}

	public Collection<EditorGroup> getGroups(VirtualFile file) {
		return cache.getGroups(file.getCanonicalPath());
	}

	//TODO cache it?
	public List<EditorGroupIndexValue> getAllGroups() {
		long start = System.currentTimeMillis();
		List<EditorGroupIndexValue> allGroups = cache.getAllGroups();
		Collections.sort(allGroups, new Comparator<EditorGroupIndexValue>() {
			@Override
			public int compare(EditorGroupIndexValue o1, EditorGroupIndexValue o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
		if (LOG.isDebugEnabled()) LOG.debug("getAllGroups " + (System.currentTimeMillis() - start));
		return allGroups;
	}

	public void initCache() {
		panelRefresher.initCache();
	}

	public Color getColor(VirtualFile file) {
		String canonicalPath = file.getCanonicalPath();
		EditorGroup group = cache.getEditorGroupForColor(canonicalPath);
		if (group != null) {
			return group.getBgColor();
		}
		return null;
	}

	public Color getFgColor(VirtualFile file) {
		String canonicalPath = file.getCanonicalPath();
		EditorGroup group = cache.getEditorGroupForColor(canonicalPath);
		if (group != null) {
			return group.getFgColor();
		}
		return null;
	}

	public void open(VirtualFile fileToOpen, EditorGroup group, boolean newWindow, boolean newTab, @Nullable VirtualFile currentFile, int myScrollOffset) {
		if (LOG.isDebugEnabled())
			LOG.debug("open fileToOpen = [" + fileToOpen + "], currentFile = [" + currentFile + "], group = [" + group + "], newWindow = [" + newWindow + "], newTab = [" + newTab + "], myScrollOffset = [" + myScrollOffset + "]");

		switching(true, group, fileToOpen, myScrollOffset);

		if (!warningShown && UISettings.getInstance().getReuseNotModifiedTabs()) {
			Notifications.notifyBugs();
			warningShown = true;
		}

		CommandProcessor.getInstance().executeCommand(project, () -> {
			final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

			//must find window before opening the file!
			VirtualFile selectedFile = null;
			EditorWindow currentWindow = manager.getCurrentWindow();
			if (currentWindow != null) {
				selectedFile = currentWindow.getSelectedFile();
			}

			if (fileToOpen.equals(selectedFile)) {
				LOG.error("fileToOpen.equals(selectedFile) [fileToOpen=" + fileToOpen + ", selectedFile=" + selectedFile + ", currentFile=" + currentFile + "]");
				switching(false);
				return;
			}
			fileToOpen.putUserData(EditorGroupPanel.EDITOR_GROUP, group); // for project view colors

			if (initial_editor_index != null) {
				fileToOpen.putUserData(initial_editor_index, null);
			}

			if (newWindow) {
				if (LOG.isDebugEnabled()) LOG.debug("openFileInNewWindow fileToOpen = " + fileToOpen);
				manager.openFileInNewWindow(fileToOpen);
			} else {
				boolean reuseNotModifiedTabs = UISettings.getInstance().getReuseNotModifiedTabs();
//				boolean fileWasAlreadyOpen = currentWindow.isFileOpen(fileToOpen);

				try {
					if (newTab) {
						UISettings.getInstance().setReuseNotModifiedTabs(false);
					}

					if (LOG.isDebugEnabled()) LOG.debug("openFile " + fileToOpen);
					FileEditor[] fileEditors = manager.openFile(fileToOpen, true);
					if (fileEditors.length == 0) {  //directory or some fail
						switching(false);
						return;
					}
					for (FileEditor fileEditor : fileEditors) {
						if (LOG.isDebugEnabled()) LOG.debug("opened fileEditor = " + fileEditor);
					}


					if (reuseNotModifiedTabs  //it is bugged, do no close files - bad workaround -> when switching to an already opened file, the previous tab would not close   
//						&& !fileWasAlreadyOpen  //this mostly works, but not always - sometimes current file gets closed and editor loses focus
					) {
						return;
					}
					//not sure, but it seems to mess order of tabs less if we do it after opening a new tab
					if (selectedFile != null && !newTab) {
						if (LOG.isDebugEnabled()) LOG.debug("closeFile " + selectedFile);
						manager.closeFile(selectedFile, currentWindow, false);
					}
				} finally {
					UISettings.getInstance().setReuseNotModifiedTabs(reuseNotModifiedTabs);
				}


			}
		}, null, null);
	}


}
