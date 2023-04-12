package krasa.editorGroups;

import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;

public class SwitchRequest {
	@NotNull
	public EditorGroup group;
	@NotNull
	public VirtualFile fileToOpen;
	public int myScrollOffset;
	public int width;
	private Integer line;

	public SwitchRequest(@NotNull EditorGroup group, @NotNull VirtualFile fileToOpen) {
		this.group = group;
		this.fileToOpen = fileToOpen;
	}

	public SwitchRequest(@NotNull EditorGroup group, @NotNull VirtualFile fileToOpen, int myScrollOffset, int width, Integer line) {
		this.group = group;
		this.fileToOpen = fileToOpen;
		this.myScrollOffset = myScrollOffset;
		this.width = width;
		this.line = line;
	}

	public EditorGroup getGroup() {
		return group;
	}

	public VirtualFile getFileToOpen() {
		return fileToOpen;
	}

	public int getMyScrollOffset() {
		return myScrollOffset;
	}

	public int getWidth() {
		return width;
	}

	public Integer getLine() {
		return line;
	}

	@Override
	public String toString() {
		return "SwitchRequest{" +
			", group=" + group +
			", fileToOpen=" + fileToOpen +
			", myScrollOffset=" + myScrollOffset +
			", width=" + width +
			", line=" + line +
			'}';
	}
}
