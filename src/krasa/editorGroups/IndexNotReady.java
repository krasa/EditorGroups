package krasa.editorGroups;

import com.intellij.openapi.diagnostic.Logger;

public class IndexNotReady extends Exception {
	private static final Logger LOG = Logger.getInstance(IndexNotReady.class);

	public IndexNotReady(String s, RuntimeException e) {
		super(s, e);
	}
}
