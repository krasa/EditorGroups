package krasa.editorGroups.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class StubGroup extends EditorGroup {
	public static final String ID = "STUB_GROUP";

	public StubGroup() {
		setStub(true);
	}

	@NotNull
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Icon icon() {
		return AllIcons.Actions.GroupByModule;
	}

	@Override
	public void invalidate() {

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
		return false;
	}
}
