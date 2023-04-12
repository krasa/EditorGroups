package krasa.editorGroups.support;

public class TooManyFilesException extends RuntimeException {

	public static final String FOUND_TOO_MANY_MATCHING_FILES_SKIPPING = "Found too many matching files, skipping.";

	public TooManyFilesException() {
	}

	public void showNotification() {
		Notifications.warning(FOUND_TOO_MANY_MATCHING_FILES_SKIPPING);
	}

}
