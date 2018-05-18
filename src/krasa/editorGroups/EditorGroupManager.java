
package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
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
	}

	public static EditorGroupManager getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, EditorGroupManager.class);
	}


	@NotNull
	EditorGroup getGroup(Project project, TextEditorImpl fileEditor, @NotNull EditorGroup lastGroup, boolean refresh) {
		if (DumbService.isDumb(project)) {
			throw new RuntimeException("check for dumb");
		}

		EditorGroup result = EditorGroup.EMPTY;
		System.out.println("getGroup: " + fileEditor + " lastGroup:" + lastGroup.getTitle() + " reparse:" + refresh);


		VirtualFile currentFile = Utils.getFileFromTextEditor(this.project, fileEditor);
		if (currentFile == null) {
			System.out.println("< getGroup - currentFile is null for " + fileEditor);
			return EditorGroup.EMPTY;
		}
		String currentFilePath = currentFile.getCanonicalPath();
		if (refresh) {

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
}
