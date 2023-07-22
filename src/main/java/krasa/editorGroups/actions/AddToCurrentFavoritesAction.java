package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.EditorGroupPanel;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FavoritesGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AddToCurrentFavoritesAction extends EditorGroupsAction {
  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    FavoritesGroup favoritesGroup = getFavoritesGroup(e);
    if (favoritesGroup != null) {
      new AddToFavoritesAction(favoritesGroup.getName()).actionPerformed(e);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    FavoritesGroup favoritesGroup = getFavoritesGroup(e);
    presentation.setVisible(favoritesGroup != null);
    if (favoritesGroup != null) {
      presentation.setText("Add to Favorites - " + favoritesGroup.getName());
    }
    if (favoritesGroup != null) {
      presentation.setEnabled(isEnabled(e, favoritesGroup));
    }
  }

  @Nullable
  private FavoritesGroup getFavoritesGroup(AnActionEvent e) {
    EditorGroupPanel editorGroupPanel = null;

    Project eventProject = getEventProject(e);
    if (eventProject == null) {
      return null;
    }
    FileEditorManagerEx instance = (FileEditorManagerEx) FileEditorManager.getInstance(eventProject);
    EditorWindow currentWindow = instance.getCurrentWindow();
    if (currentWindow != null) {
      EditorComposite editor = currentWindow.getSelectedComposite(true);
      if (editor != null) {
        FileEditor selectedEditor = Objects.requireNonNull(editor.getSelectedWithProvider()).getFileEditor();
        editorGroupPanel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
      }

    }

    FavoritesGroup favoritesGroup = null;
    if (editorGroupPanel != null) {
      EditorGroup displayedGroup = editorGroupPanel.getDisplayedGroup();
      if (displayedGroup instanceof FavoritesGroup) {
        favoritesGroup = (FavoritesGroup) displayedGroup;
      }
    }
    return favoritesGroup;
  }

  private boolean isEnabled(AnActionEvent e, FavoritesGroup favoritesGroup) {
    Project project = e.getProject();
    boolean enabled = true;
    DataContext dataContext = e.getDataContext();
    VirtualFile[] data1 = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (data1 != null) {
      boolean everyFileIsContained = true;
      for (VirtualFile virtualFile : data1) {
        everyFileIsContained = favoritesGroup.containsLink(project, virtualFile);
        if (!everyFileIsContained) {
          break;
        }
      }
      if (everyFileIsContained) {
        enabled = false;
      }
    }
    return enabled;
  }
}
