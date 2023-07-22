package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.PanelRefresher;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ViewPanelToggleAction extends ToggleAction implements DumbAware {
  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

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
    PanelRefresher.getInstance(Objects.requireNonNull(event.getProject())).refresh();
  }
}
