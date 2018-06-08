package krasa.editorGroups.language;

import com.intellij.lang.Language;

public class EditorGroupsLanguage extends Language {

	public static final EditorGroupsLanguage INSTANCE = new EditorGroupsLanguage();

	private EditorGroupsLanguage() {
		super("EditorGroups");
	}
}