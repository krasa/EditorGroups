package krasa.editorGroups.support;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

public class Notifications {

	private static final Logger LOG = Logger.getInstance(Notifications.class);

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

	public static void indexingWarn(Project project, VirtualFile file, String message) {
		String content = message + " in " + href(file);
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", content, NotificationType.WARNING, new NotificationListener.Adapter() {
			@Override
			protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
				OpenFileAction.openFile(file, project);
				notification.expire();
			}
		});
		show(notification);
	}

	public static String href(VirtualFile file) {
		if (file == null) {
			return null;
		}
		return href(file.getName());
	}

	@NotNull
	public static String href(String name) {
		return "<a href=\"" + name + "\">" + name + "<a/>";
	}


	private static void show(Notification notification) {
		ApplicationManager.getApplication().invokeLater(() -> {
			com.intellij.notification.Notifications.Bus.notify(notification);
		});
	}

	public static void duplicateId(Project project, String id, List<EditorGroupIndexValue> values) {
		StringBuilder sb = new StringBuilder("Duplicate Group ID '" + id + "' in: [");
		for (int i = 0; i < values.size(); i++) {
			EditorGroupIndexValue value = values.get(i);
			String ownerPath = value.getOwnerPath();
			sb.append(href(ownerPath));
			if (i != values.size()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		String content = sb.toString();
		warning(content, new NotificationListener.Adapter() {
			@Override
			protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
				OpenFileAction.openFile(e.getDescription(), project);
			}
		});
	}

	public static void warning(String content, NotificationListener listener) {
		Notification notification = NOTIFICATION.createNotification("EditorGroups plugin", content, NotificationType.WARNING, listener);
		LOG.warn(new RuntimeException(content));
		show(notification);
	}
}
