package krasa.editorGroups.support;

import com.intellij.ide.ui.UISettings;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class Notifications {
	public static final NotificationGroup NOTIFICATION = new NotificationGroup("Editor Groups", NotificationDisplayType.BALLOON, true);

	public static void notifyMissingFile(EditorGroup group, String path) {
		Notification notification = NOTIFICATION.createNotification("File does not exist", "Path='" + path + "'; Owner='" + group.getId() + "'", NotificationType.WARNING,
			null);
		ApplicationManager.getApplication().invokeLater(() -> {
			com.intellij.notification.Notifications.Bus.notify(notification);
		});
	}

	public static void notifyBugs() {
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", "Settings | ... | Editor Tabs | 'Open declaration source in the same tab' is enabled.<br/> It may cause problems when switching too fast.<br/><a href=\"#\">Click here to disable it<a/>.", NotificationType.WARNING, new NotificationListener.Adapter() {
			@Override
			protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
				UISettings.getInstance().setReuseNotModifiedTabs(false);
				notification.expire();
			}
		});
		ApplicationManager.getApplication().invokeLater(() -> {
			com.intellij.notification.Notifications.Bus.notify(notification);
		});
	}
}
