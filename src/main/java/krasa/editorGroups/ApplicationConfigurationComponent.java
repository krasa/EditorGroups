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
public class ApplicationConfigurationComponent implements PersistentStateComponent<ApplicationConfiguration> {
	private ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();

	public static ApplicationConfigurationComponent getInstance() {
		return ServiceManager.getService(ApplicationConfigurationComponent.class);
	}

	@NotNull
	@Override
	public ApplicationConfiguration getState() {
		return applicationConfiguration;
	}

	@Override
	public void loadState(@NotNull ApplicationConfiguration applicationConfiguration) {
		this.applicationConfiguration = applicationConfiguration;
	}


}
