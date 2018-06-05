package krasa.editorGroups;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(
	name = "EditorGroups",
	storages = {
		@Storage(value = "EditorGroups.xml")
	}
)
public class ApplicationConfiguration implements PersistentStateComponent<ApplicationConfiguration.State> {
	private State state = new State();

	public static ApplicationConfiguration getInstance() {
		return ServiceManager.getService(ApplicationConfiguration.class);
	}

	public static State state() {
		return getInstance().getState();
	}

	@NotNull
	@Override
	public State getState() {
		return state;
	}

	@Override
	public void loadState(@NotNull State state) {
		this.state = state;
	}


	public static class State {
		public boolean autoFolders = true;
		public boolean autoSameName = true;
		public boolean forceSwitch = true;
		public boolean hideEmpty = true;
		public boolean showSize = false;
		private boolean continuousScrolling;


		public boolean isAutoFolders() {
			return autoFolders;
		}

		public void setAutoFolders(boolean autoFolders) {
			this.autoFolders = autoFolders;
		}

		public boolean isAutoSameName() {
			return autoSameName;
		}

		public void setAutoSameName(boolean autoSameName) {
			this.autoSameName = autoSameName;
		}

		public boolean isForceSwitch() {
			return forceSwitch;
		}

		public void setForceSwitch(boolean forceSwitch) {
			this.forceSwitch = forceSwitch;
		}

		public boolean isHideEmpty() {
			return hideEmpty;
		}

		public void setHideEmpty(boolean hideEmpty) {
			this.hideEmpty = hideEmpty;
		}

		public boolean isShowSize() {
			return showSize;
		}

		public void setShowSize(boolean showSize) {
			this.showSize = showSize;
		}


		public boolean isContinuousScrolling() {
			return continuousScrolling;
		}

		public void setContinuousScrolling(final boolean continuousScrolling) {
			this.continuousScrolling = continuousScrolling;
		}
	}
}
