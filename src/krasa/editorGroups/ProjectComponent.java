package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "EditorGroups", storages = {@Storage(value = "EditorGroups.xml")})
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent, PersistentStateComponent<ProjectComponent.State> {
	private static final Logger LOG = Logger.getInstance(ProjectComponent.class);

	private final Project project;

	public ProjectComponent(Project project) {
		this.project = project;
	}

	@Override
	public void projectOpened() {
		EditorGroupManager.getInstance(project).initCache();


		project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {

			//IJ 2018.2
			@Override
			public void fileOpenedSync(@NotNull FileEditorManager manager, @NotNull VirtualFile file, @NotNull Pair<FileEditor[], FileEditorProvider[]> editors) {
				if (LOG.isDebugEnabled()) LOG.debug(">fileOpenedSync [" + file + "]");
				file = Utils.unwrap(file);
				EditorGroupManager instance = EditorGroupManager.getInstance(project);
				SwitchRequest switchRequest = instance.getAndClearSwitchingRequest(file);

				for (FileEditor fileEditor : editors.getFirst()) {
					if (fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL) != null) {
						continue;
					}
					long start = System.currentTimeMillis();

					createPanel(manager, file, switchRequest, fileEditor);


					if (LOG.isDebugEnabled()) {
						if (LOG.isDebugEnabled())
							LOG.debug("<fileOpenedSync EditorGroupPanel created, file=" + file + " in " + (System.currentTimeMillis() - start) + "ms" + ", fileEditor=" + fileEditor);
					}
				}
			}

			private void createPanel(@NotNull FileEditorManager manager, @NotNull VirtualFile file, SwitchRequest switchRequest, FileEditor fileEditor) {
				if (Disposer.isDisposed(fileEditor)) {
					return;
				}
				EditorGroupPanel panel = new EditorGroupPanel(fileEditor, project, switchRequest, file);
				manager.addTopComponent(fileEditor, panel.getRoot());
				panel.postConstruct();
			}

			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("selectionChanged " + event);
				}
				FileEditor fileEditor = event.getNewEditor();
				if (fileEditor != null) {
					EditorGroupPanel panel = fileEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
					if (panel != null) {
						EditorGroupManager instance = EditorGroupManager.getInstance(project);
						SwitchRequest switchRequest = instance.getAndClearSwitchingRequest(panel.getFile());
						if (switchRequest != null) {
							EditorGroup switchingGroup = switchRequest.group;
							int scrollOffset = switchRequest.myScrollOffset;
							panel.refreshOnSelectionChanged(false, switchingGroup, scrollOffset);
						} else {
							panel.refresh(false, null);
						}
					}
				}
			}

			@Override
			public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
				if (LOG.isDebugEnabled()) LOG.debug("fileClosed [" + file + "]");
			}
		});
	}


	@Nullable
	@Override
	public State getState() {
		if (ApplicationConfiguration.state().isRememberLastGroup()) {
			long start = System.currentTimeMillis();
			State state = IndexCache.getInstance(project).getState();
			if (LOG.isDebugEnabled())
				LOG.debug("ProjectComponent.getState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
			return state;
		} else {
			return new State();
		}
	}

	@Override
	public void loadState(@NotNull State state) {
		if (ApplicationConfiguration.state().isRememberLastGroup()) {
			long start = System.currentTimeMillis();
			IndexCache.getInstance(project).loadState(state);
			if (LOG.isDebugEnabled())
				LOG.debug("ProjectComponent.loadState size:" + state.lastGroup.size() + " " + (System.currentTimeMillis() - start) + "ms");
		}
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
