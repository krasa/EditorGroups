package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.PanelRefresher;
import org.jetbrains.annotations.NotNull;

public class TogglePanelVisibilityAction extends DumbAwareAction {
	@Override
	public void actionPerformed(AnActionEvent e) {
		ApplicationConfiguration state = ApplicationConfiguration.state();
		state.setShowPanel(state.isShowPanel());
		PanelRefresher.getInstance(getEventProject(e)).refresh();
	}


	@Override
	public void update(@NotNull AnActionEvent e) {
		super.update(e);
		ApplicationConfiguration state = ApplicationConfiguration.state();
		if (state.isShowPanel()) {
			e.getPresentation().setText("Hide Panel");
		} else {
			e.getPresentation().setText("Show Panel");
		}
	}
}
