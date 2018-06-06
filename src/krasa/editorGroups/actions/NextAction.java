package krasa.editorGroups.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.PopupHandler;
import com.intellij.util.BitUtil;
import krasa.editorGroups.EditorGroupPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static krasa.editorGroups.actions.PopupMenu.popupInvoked;

public class NextAction extends DumbAwareAction implements CustomComponentAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		FileEditor data = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
		if (data != null) {
			EditorGroupPanel panel = data.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				InputEvent e = anActionEvent.getInputEvent();

				boolean newTab = BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK) && (e instanceof MouseEvent) && ((MouseEvent) e).getClickCount() > 0;
				panel.next(newTab, BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK));
			}
		}

	}

	@Override
	public JComponent createCustomComponent(Presentation presentation) {
		ActionButton button = new ActionButton(this, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		presentation.setIcon(AllIcons.Actions.Forward);
		button.addMouseListener(new PopupHandler() {
			public void invokePopup(Component comp, int x, int y) {
				popupInvoked(comp, x, y);
			}
		});

		return button;
	}
}
