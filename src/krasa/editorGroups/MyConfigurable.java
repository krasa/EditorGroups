package krasa.editorGroups;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MyConfigurable implements Configurable {
	private SettingsForm form;


	@Nls
	@Override
	public String getDisplayName() {
		return "Editor Groups";
	}


	@Override
	@Nullable
	@NonNls
	public String getHelpTopic() {
		return null;
	}

	@Override
	public JComponent createComponent() {
		if (form == null) {
			form = new SettingsForm();
		}
		return form.getRoot();
	}


	@Override
	public boolean isModified() {
		return form != null && form.isSettingsModified(ApplicationConfiguration.state());
	}

	@Override
	public void apply() throws ConfigurationException {
		if (form != null) {
			form.apply();
			EditorGroupsTabsPainterPatcherComponent.onColorsChanged(ApplicationConfiguration.state());
		}
	}


	@Override
	public void reset() {
		if (form != null) {
			form.importFrom(ApplicationConfiguration.state());
		}
	}

	@Override
	public void disposeUIResources() {
		form = null;
	}


}
