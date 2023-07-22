package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAwareAction;
import krasa.editorGroups.EditorGroupPanel;

public abstract class EditorGroupsAction extends DumbAwareAction {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(EditorGroupsAction.class);

  protected EditorGroupPanel getEditorGroupPanel(AnActionEvent anActionEvent) {
    EditorGroupPanel panel = null;
    FileEditor data = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
    if (data != null) {
      panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
    }
    return panel;
  }
}
