package krasa.editorGroups.support;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import krasa.editorGroups.EditorGroupPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class MyEditorNotificationsProvider implements EditorNotificationProvider, DumbAware {

  private static final Key<EditorGroupPanel> KEY = Key.create("EditorGroups");

  /**
   * on background, called many times
   */
  public EditorGroupPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    return null;
  }

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    return null;
  }
}