package krasa.editorGroups.actions;

import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class PopupMenu {
	public static void popupInvoked(Component component, int x, int y) {
		DefaultActionGroup group = getDefaultActionGroup();
		ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
		menu.getComponent().show(component, x, y);
	}

	@NotNull
	public static DefaultActionGroup getDefaultActionGroup() {
		DefaultActionGroup group = new DefaultActionGroup();
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Refresh"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Next"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Previous"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ReindexThisFile"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.Reindex"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleAutoSameNameGroups"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleFolderEditorGroups"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleForce"));
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleHideEmpty"));
//		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.ToggleShowSize"));
		group.add(new Separator());
		group.add(ActionManager.getInstance().getAction("krasa.editorGroups.OpenConfiguration"));
		return group;
	}
}
