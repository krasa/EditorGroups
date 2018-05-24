package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@State(name = "EditorGroups", storages = {@Storage(value = "EditorGroups.xml")})
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent, PersistentStateComponent<ProjectComponent.State> {
	private static final Logger log = LoggerFactory.getLogger(ProjectComponent.class);

	private final Project project;

	public ProjectComponent(Project project) {
		this.project = project;
	}

	@Override
	public void projectOpened() {
		EditorGroupManager.getInstance(project).initCache();

		project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			/**on EDT*/
			@Override
			public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
				long start = System.currentTimeMillis();
				final FileEditor[] fileEditors = manager.getAllEditors(file);
				for (final FileEditor fileEditor : fileEditors) {
					if (fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
						continue;
					}

					EditorGroup switchingGroup = EditorGroupManager.getInstance(project).getSwitchingGroup();
					EditorGroupPanel panel = new EditorGroupPanel(fileEditor, project, switchingGroup, file);

					manager.addTopComponent(fileEditor, panel.getRoot());
					log.debug("EditorGroupPanel created in " + (System.currentTimeMillis() - start));
				}
			}

			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
				FileEditor fileEditor = event.getNewEditor();
				if (fileEditor != null) {
					EditorGroupPanel panel = fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
					if (panel != null) {    //UI form editor is not disposed, so the panel might exist and it has no focus listener... 
						EditorGroup switchingGroup = EditorGroupManager.getInstance(project).getSwitchingGroup();
						panel.refresh(false, switchingGroup);
					}
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
			}
		});

	}

	@Nullable
	@Override
	public State getState() {
		long start = System.currentTimeMillis();
		State state = IndexCache.getInstance(project).getState();
		log.debug("ProjectComponent.getState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
		return state;
	}

	@Override
	public void loadState(@NotNull State state) {
		long start = System.currentTimeMillis();
		IndexCache.getInstance(project).loadState(state);
		log.debug("ProjectComponent.loadState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
	}

	public static class State {
		@XCollection(propertyElementName = "lastGroup", elementTypes = StringPair.class)
		public List<StringPair> lastGroup = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			State state = (State) o;

			return lastGroup != null ? lastGroup.equals(state.lastGroup) : state.lastGroup == null;
		}

		@Override
		public int hashCode() {
			return lastGroup != null ? lastGroup.hashCode() : 0;
		}
	}


	@Tag("p")
	public static class StringPair {
		@Attribute("k")
		public String key;
		@Attribute("v")
		public String value;

		public StringPair() {
		}

		public StringPair(String key, String value) {
			this.key = key;
			this.value = value;
		}


	}


}
