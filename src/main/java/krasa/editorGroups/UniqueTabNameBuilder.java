package krasa.editorGroups;

import com.intellij.filename.UniqueNameBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.Link;
import krasa.editorGroups.model.VirtualFileLink;

import java.util.*;

public class UniqueTabNameBuilder {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(UniqueTabNameBuilder.class);
  private String root;

  public UniqueTabNameBuilder(Project project) {
    this.root = project.getBasePath();
    this.root = this.root == null ? "" : FileUtil.toSystemIndependentName(this.root);
  }

  public Map<Link, String> getNamesByPath(List<Link> paths, VirtualFile currentFile, Project project) {
    UniqueNameBuilder<Link> uniqueNameBuilder = new UniqueNameBuilder<>(root, "/");
    Map<Link, String> path_name = new HashMap<>();
    Map<String, Link> name_path = new HashMap<>();
    Set<Link> paths_withDuplicateName = new HashSet<>();

    for (Link link : paths) {
      put(path_name, name_path, paths_withDuplicateName, link);
    }

    if (currentFile != null) {
      String currentFilePath = currentFile.getPath();
      boolean containsCurrentFile = path_name.keySet().stream().anyMatch(link1 -> link1.getPath().equals(currentFilePath));
      if (!containsCurrentFile) {
        Link link = new VirtualFileLink(currentFile, project);
        put(path_name, name_path, paths_withDuplicateName, link);
      }
    }

    for (Link link : paths_withDuplicateName) {
      uniqueNameBuilder.addPath(link, link.getPath());
    }

    for (Link link : paths_withDuplicateName) {
      String uniqueName = uniqueNameBuilder.getShortPath(link);
      path_name.put(link, uniqueName);
    }
    return path_name;
  }

  private void put(Map<Link, String> path_name, Map<String, Link> name_path, Set<Link> paths_withDuplicateName, Link link) {
    String name = link.getName();

    Link duplicatePath = name_path.get(name);
    if (duplicatePath != null) {
      paths_withDuplicateName.add(duplicatePath);
      paths_withDuplicateName.add(link);
    }
    path_name.put(link, name);
    name_path.put(name, link);
  }

}
