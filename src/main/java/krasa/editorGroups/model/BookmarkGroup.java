package krasa.editorGroups.model;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookmarkGroup extends EditorGroup {
  public static final String ID = "BOOKMARKS";
  private final List<Link> links;
  private final String name = "Bookmarks";

	public BookmarkGroup(List<Bookmark> validBookmarks) {
		links = new ArrayList<>(validBookmarks.size());
		for (Bookmark validBookmark : validBookmarks) {
			VirtualFile file = validBookmark.getFile();
			Icon icon = validBookmark.getIcon();
			int line = validBookmark.getLine();
			links.add(new VirtualFileLink(file, icon, line));
		}
	}


  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getTitle() {
    return name;
  }


  @Override
  public String switchTitle(Project project) {
    return getTitle();
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public Icon icon() {
    return AllIcons.Actions.Checked_selected;
  }

  @Override
  public void invalidate() {

  }

  @Override
  public int size(Project project) {
    return links.size();
  }

  @Override
  public List<Link> getLinks(Project project) {
    return links;
  }

  @Override
  public boolean isOwner(String ownerPath) {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof BookmarkGroup) && Objects.equals(((BookmarkGroup) obj).getId(), this.getId());
  }

  @Override
  public String toString() {
    return "BookmarksGroup{" +
      "links=" + links + '}';
  }
}
