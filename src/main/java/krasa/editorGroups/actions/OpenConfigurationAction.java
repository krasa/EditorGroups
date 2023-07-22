package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.MyConfigurable;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class OpenConfigurationAction extends DumbAwareAction implements CustomComponentAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    MyConfigurable instance = new MyConfigurable();
    ShowSettingsUtil.getInstance().editConfigurable(e.getProject(), "EditorGroupsSettings", instance, true);
  }

  @Override
  public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation) {
    ActionButton refresh = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    presentation.setIcon(MyIcons.settings);
    return refresh;
  }

}
