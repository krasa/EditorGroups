package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "EditorGroups", storages = {@Storage(value = "EditorGroups.xml")})
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent, PersistentStateComponent<ProjectComponent.State> {
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
				final FileEditor[] fileEditors = manager.getAllEditors(file);
				for (final FileEditor fileEditor : fileEditors) {
					if (fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
						continue;
					}

					EditorGroup switchingGroup = EditorGroupManager.getInstance(project).getSwitchingGroup();
					EditorGroupPanel panel = new EditorGroupPanel(fileEditor, project, switchingGroup, file);

					manager.addTopComponent(fileEditor, panel.getRoot());
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
			}
		});

	}

	private void initCache() {
		//TODO run it on background?
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
				System.out.println("initCache " + (System.currentTimeMillis() - start));
			}
		});
	}

	@Nullable
	@Override
	public State getState() {
		long start = System.currentTimeMillis();
		State state = IndexCache.getInstance(project).getState();
		System.err.println("ProjectComponent.getState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
		return state;
	}

	@Override
	public void loadState(@NotNull State state) {
		long start = System.currentTimeMillis();
		IndexCache.getInstance(project).loadState(state);
		System.err.println("ProjectComponent.loadState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
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
