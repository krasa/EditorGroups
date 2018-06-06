package krasa.editorGroups.support;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import krasa.editorGroups.model.EditorGroup;

public class Notifications {
	public static final NotificationGroup NOTIFICATION = new NotificationGroup("Editor Groups", NotificationDisplayType.BALLOON, true);

	public static void notifyMissingFile(EditorGroup group, String path) {
		Notification notification = NOTIFICATION.createNotification("File does not exist", "Path='" + path + "'; Owner='" + group.getId() + "'", NotificationType.WARNING,
				null);
		ApplicationManager.getApplication().invokeLater(() -> {
			com.intellij.notification.Notifications.Bus.notify(notification);
		});
	}
}
