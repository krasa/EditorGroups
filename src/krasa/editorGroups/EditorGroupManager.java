/*
 * @idea.title CORE
 * @idea.related EditorGroupPanel.java
 * @idea.related ProjectComponent.java
 * @idea.related EditorGroupTabTitleProvider.java
 */
package krasa.editorGroups;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Cache;
import krasa.editorGroups.support.Parser;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

public class EditorGroupManager {


	private final Project project;
	//	@NotNull
//	private EditorGroup currentGroup = EditorGroup.EMPTY;
	Cache cache = new Cache();
	Parser parser = new Parser();

	public EditorGroupManager(Project project) {

		this.project = project;
	}

	public static EditorGroupManager getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, EditorGroupManager.class);
	}

	public void reparse(VirtualFile file) {
		parser.parse(Utils.getFileContent(file), file.getCanonicalPath(), cache);
	}

	@NotNull
	EditorGroup getGroup(TextEditorImpl fileEditor, @NotNull EditorGroup lastGroup, boolean reparse) {
		EditorGroup result = EditorGroup.EMPTY;
		System.out.println("getGroup: " + fileEditor + " lastGroup:" + lastGroup.getTitle() + " reparse:" + reparse);

		VirtualFile currentFile = Utils.getFileFromTextEditor(project, fileEditor);
		String currentFilePath = currentFile.getCanonicalPath();
		if (reparse) {
			result = parse(currentFile);

			if (result.invalid()) {
				if (lastGroup.valid()) {
					result = parse(lastGroup.getOwnerVirtualFile());
				}

			}
//			if (result.invalid()) {
//				if (currentGroup.valid()) {
//					EditorGroup parse = parse(currentGroup.getOwnerVirtualFile());
//					if (result.contains(currentFilePath)) {
//						result = parse;
//					}
//				}
//			}
		}

		if (result.invalid()) {
			if (lastGroup.valid()) {
				result = lastGroup;
			}
		}
		if (result.invalid()) {
			result = cache.getByOwner(currentFilePath);
		}

//		if (result.invalid()) {
//			if (currentGroup.valid()) {
//				if (currentGroup.contains(currentFilePath)) {
//					result = currentGroup;
//				}
//			}
//		}
		if (result.invalid()) {
			EditorGroup slaveGroup = cache.findGroupAsSlave(currentFilePath);
			if (slaveGroup.valid()) {
				result = slaveGroup;
			}
		}


		if (result.invalid()) {
			System.out.println("no group found");
		}
		System.out.println("getGroup returning " + result.getTitle());
		return result;
	}

	@NotNull
	private EditorGroup parse(VirtualFile currentFile) {
		String text = Utils.getFileContent(currentFile);
		return parser.parse(text, currentFile.getCanonicalPath(), cache);
	}


	@NotNull
	public EditorGroup getGroupByOwner(@NotNull VirtualFile virtualFile) {
		EditorGroup ifPresent = cache.getByOwner(virtualFile.getCanonicalPath());
		if (ifPresent == null) {
			return EditorGroup.EMPTY;
		}
		return ifPresent;
	}

//	public void setCurrentGroup(EditorGroup myGroup) {
//		currentGroup = myGroup;
//	}
}
