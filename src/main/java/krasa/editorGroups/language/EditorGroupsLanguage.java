package krasa.editorGroups.language;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorGroupsLanguage extends Language {

	public static final EditorGroupsLanguage INSTANCE = new EditorGroupsLanguage();

	private EditorGroupsLanguage() {
		super("EditorGroups");
	}

	public static boolean isEditorGroupsLanguage(VirtualFile file) {
		if (file == null) {
			return false;
		}
		return getFileTypeLanguage(file.getFileType()) == INSTANCE;
	}

	@Nullable
	public static Language getFileTypeLanguage(@Nullable FileType fileType) {
		return fileType instanceof LanguageFileType ? ((LanguageFileType) fileType).getLanguage() : null;
	}

	public static boolean isEditorGroupsLanguage(@NotNull String ownerPath) {
		FileType sd = FileTypeManager.getInstance().getFileTypeByFileName(ownerPath);
		return getFileTypeLanguage(sd) == INSTANCE;
	}
}