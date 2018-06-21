package krasa.editorGroups;

import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import org.jetbrains.annotations.NotNull;

public class SwitchRequest {
	public volatile boolean switching = true;
	@NotNull
	public volatile EditorGroup group;
	@NotNull
	public volatile VirtualFile fileToOpen;
	public volatile int myScrollOffset;
	public volatile int width;

	public SwitchRequest(@NotNull EditorGroup group, @NotNull VirtualFile fileToOpen) {
		this.group = group;
		this.fileToOpen = fileToOpen;
	}

	public SwitchRequest(@NotNull EditorGroup group, @NotNull VirtualFile fileToOpen, int myScrollOffset, int width) {
		this.group = group;
		this.fileToOpen = fileToOpen;
		this.myScrollOffset = myScrollOffset;
		this.width = width;
	}

	public boolean isSwitching() {
		return switching;
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

	@Override
	public String toString() {
		return "SwitchRequest{" +
			"switching=" + switching +
			", group=" + group +
			", fileToOpen=" + fileToOpen +
			", myScrollOffset=" + myScrollOffset +
			", width=" + width +
			'}';
	}
}
