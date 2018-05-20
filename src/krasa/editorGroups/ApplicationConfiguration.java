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
		public boolean forceSwitch = true;
		
	}
}
