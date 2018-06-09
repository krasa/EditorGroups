package krasa.editorGroups.support;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

public class Notifications {
	public static final NotificationGroup NOTIFICATION = new NotificationGroup("Editor Groups", NotificationDisplayType.BALLOON, true);

	public static void notifyMissingFile(EditorGroup group, String path) {
		String content = "Path='" + path + "'; Owner='" + group.getId() + "'";
		Notification notification = NOTIFICATION.createNotification("File does not exist", content, NotificationType.WARNING,
			null);
		show(notification);
	}


	public static void notifyBugs() {
		String content = "Settings | ... | Editor Tabs | 'Open declaration source in the same tab' is enabled.<br/> It may cause problems when switching too fast.<br/><a href=\"#\">Click here to disable it<a/>.";

		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", content, NotificationType.WARNING, new NotificationListener.Adapter() {
			@Override
			protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
				UISettings.getInstance().setReuseNotModifiedTabs(false);
				notification.expire();
			}
		});
		show(notification);
	}

	public static void duplicateId(EditorGroupIndexValue lastGroup, VirtualFile file, Project project) {
		String content = "Duplicate Group ID '" + lastGroup.getId() + "' in <a href=\"#\">" + file.getName() + "<a/>";
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", content, NotificationType.WARNING, new NotificationListener.Adapter() {
			@Override
			protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
				OpenFileAction.openFile(file, project);
				notification.expire();
			}
		});
		show(notification);
	}


	private static void show(Notification notification) {
		ApplicationManager.getApplication().invokeLater(() -> {
			com.intellij.notification.Notifications.Bus.notify(notification);
		});
	}

	public static void duplicateId(String id, List<EditorGroupIndexValue> values) {
		StringBuilder sb = new StringBuilder("Duplicate Group ID '" + id + "' in: [");
		for (int i = 0; i < values.size(); i++) {
			EditorGroupIndexValue value = values.get(i);
			String ownerPath = value.getOwnerPath();
			sb.append(ownerPath);
			if (i != values.size()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", sb.toString(), NotificationType.WARNING, null);
		show(notification);
	}

	public static void notify2(String content) {
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", content, NotificationType.WARNING, null);
		show(notification);
	}
}
