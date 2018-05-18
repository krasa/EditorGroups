package krasa.editorGroups.support;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class Utils {

	@Nullable
	public static VirtualFile getFileFromTextEditor(Project project, FileEditor textEditor) {
		return FileEditorManagerEx.getInstanceEx(project).getFile(textEditor);
	}

	@Nullable
	public static VirtualFile getFileByUrl(String url) {
		return VirtualFileManagerEx.getInstance().findFileByUrl(url);
	}

	public static String getFileContent(String ownerPath) {
		VirtualFile fileByPath = getFileByPath(ownerPath);
		if (fileByPath == null) {
			return null;
		}
		return getFileContent(fileByPath);
	}

	@Nullable
	public static String getFileContent(VirtualFile url) {
		Document document = FileDocumentManager.getInstance().getDocument(url);
		if (document != null) {
			return document.getText();
		}
		return null;
	}

	@Nullable
	public static VirtualFile getVirtualFileByAbsolutePath(@NotNull String s) {
		VirtualFile fileByPath = null;
		if (new File(s).exists()) {
			fileByPath = getFileByPath(s);
		}
		return fileByPath;
	}
	
	
	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path) {
		return getFileByPath(path, (VirtualFile) null);
	}

	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path, @Nullable VirtualFile currentFile) {
		VirtualFile file = null;
		if (FileUtil.isUnixAbsolutePath(path) || FileUtil.isWindowsAbsolutePath(path)) {
			file = VirtualFileManagerEx.getInstance().findFileByUrl("file://" + path);
		} else if (path.startsWith("file://")) {
			file = VirtualFileManagerEx.getInstance().findFileByUrl(path);
		} else if (currentFile != null) {
			VirtualFile parent = currentFile.getParent();
			if (parent != null) {
				file = parent.findFileByRelativePath(path);
			}
		}

		return file;
	}

	@Nullable
	public static VirtualFile getFileByPath(@NotNull String path, @NotNull EditorGroup group) {
		String ownerPath = group.getOwnerPath();
		VirtualFile virtualFile = getFileByPath(ownerPath);

		return getFileByPath(path, virtualFile);
	}

	public static boolean isTheSameFile(@NotNull String path,@NotNull VirtualFile file) {
		if (file != null) {
			return path.equals(file.getCanonicalPath());
		}
		return false;
	}

	@NotNull
	public static String toPresentableName(String path) {
		String name = path;
		int i = StringUtil.lastIndexOfAny(path, "\\/");
		if (i > 0) {
			name = path.substring(i + 1);
		}
		return name;
	}
}
