package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class HidePanelGroup extends EditorGroup {
  private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(HidePanelGroup.class);
  public static final String ID = "HidePanelGroup";

  @NotNull
  @Override
  public String getId() {
    return ID;
  }


  @Override
  public String switchTitle(Project project) {
    return getTitle();
  }

  @Override
  public String getTitle() {
    return "Hide panel";
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public Icon icon() {
    return null;
  }

  @Override
  public void invalidate() {

  }

  @Override
  public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
    return presentableNameForUI;
  }

  @Override
  public int size(Project project) {
    return 0;
  }

  @Override
  public List<Link> getLinks(Project project) {
    return Collections.emptyList();
  }

  @Override
  public boolean isOwner(String ownerPath) {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) || obj instanceof HidePanelGroup;
  }
}
