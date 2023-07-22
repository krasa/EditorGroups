package krasa.editorGroups;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class MyEditorTabColorProvider implements EditorTabColorProvider {

  @Nullable
  @Override
  public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
    return getBgColor(project, file);
  }

  private Color getFgColor(Project project, FileEditor textEditor, VirtualFile file) {
    Color fgColor = null;

    EditorGroup group = null;
    if (textEditor != null) {
      group = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
    }
    if (group != null) {
      fgColor = group.getFgColor();
    }

    return fgColor;
  }


  @Nullable
  private Color getBgColor(Project project, VirtualFile file) {
    Color bgColor = null;

    EditorGroup group = null;
    if (file != null) {
      group = file.getUserData(EditorGroupPanel.EDITOR_GROUP);
    }
    if (group != null) {
      bgColor = group.getBgColor();
    }

    return bgColor;
  }

}
