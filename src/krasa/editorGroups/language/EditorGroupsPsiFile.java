package krasa.editorGroups.language;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class EditorGroupsPsiFile extends PsiFileBase {

	public EditorGroupsPsiFile(FileViewProvider viewProvider) {
		super(viewProvider, EditorGroupsLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public FileType getFileType() {
		return EditorGroupsFileType.EDITOR_GROUPS_FILE_TYPE;
	}


	@Override
	public String toString() {
		return "EditorGroupsPsiFile{" +
			"myOriginalFile=" + myOriginalFile +
			'}';
	}
}
