package krasa.editorGroups.gui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ProjectUtils {
  private static final Logger LOG = Logger.getInstance(ProjectUtils.class);

  private static List<Project> getOpenProjects() {
    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    if (projects.length == 0) {
      return Collections.emptyList();
    }
    final List<Project> projectList = new SmartList<>();
    for (Project project : projects) {
      if (isValidProject(project)) {
        projectList.add(project);
      }
    }
    return projectList;
  }

  private static boolean isValidProject(@Nullable Project project) {
    return project != null && !project.isDisposed() && !project.isDefault() && project.isInitialized();
  }

  @Nullable
  public static Project getCurrentContextProject() {
    final List<Project> openProjects = getOpenProjects();
    if (openProjects.isEmpty()) {
      return null;
    }
    if (openProjects.size() == 1) {
      return openProjects.get(0);
    }

    Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (window == null) {
      return null;
    }

    Component comp = window;
    while (true) {
      final Container _parent = comp.getParent();
      if (_parent == null) {
        break;
      }
      comp = _parent;
    }

    Project project = null;
    if (comp instanceof IdeFrame) {
      project = ((IdeFrame) comp).getProject();
    }
    if (project == null) {
      project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(comp));
    }

    return isValidProject(project) ? project : null;
  }
}
