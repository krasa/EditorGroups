package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.PanelRefresher;
import org.jetbrains.annotations.NotNull;

public class ViewPanelToggleAction extends ToggleAction implements DumbAware {
	public ViewPanelToggleAction() {
		super("Editor Groups Panel");
	}

	@Override
	public boolean isSelected(@NotNull AnActionEvent event) {
		return ApplicationConfiguration.state().isShowPanel();
	}

	@Override
	public void setSelected(@NotNull AnActionEvent event, boolean state) {
		ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.state();
		applicationConfiguration.setShowPanel(!applicationConfiguration.isShowPanel());
		PanelRefresher.getInstance(event.getProject()).refresh();
	}
}
