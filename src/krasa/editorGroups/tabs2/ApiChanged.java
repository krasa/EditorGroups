package krasa.editorGroups.tabs2;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class ApiChanged {
	private static final Logger LOG = Logger.getInstance(ApiChanged.class);
	static boolean reported = false;

	public static void report(@NotNull Throwable e) {
		if (!reported) {
			LOG.error("API CHANGED, PLEASE REPORT THIS", e);
			reported = true;
		}
	}
}
