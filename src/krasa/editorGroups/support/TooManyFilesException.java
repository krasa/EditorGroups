package krasa.editorGroups.support;

public class TooManyFilesException extends RuntimeException {
	private final int size;

	public TooManyFilesException(int size) {
		this.size = size;
	}

	public void showNotification() {
		Notifications.warning("Found too many matching files, aborting. Size=" + size);
	}

}
