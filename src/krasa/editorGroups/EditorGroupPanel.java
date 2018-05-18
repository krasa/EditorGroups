package krasa.editorGroups;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.MyFileManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Weighted;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.support.Utils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class EditorGroupPanel extends JBPanel implements Weighted, Disposable {
	private static final Logger LOG = Logger.getInstance(EditorGroupPanel.class);


	public static final Key<EditorGroupPanel> EDITOR_PANEL = Key.create("EDITOR_GROUPS_PANEL");
	public static final Key<EditorGroup> EDITOR_GROUP = Key.create("EDITOR_GROUP");
	public static final Key<Object> EDITOR_GROUP_PARSED = Key.create("EDITOR_GROUP_PARSED");

	@NotNull
	private final FileEditor textEditor;
	@NotNull
	private Project project;
	private final VirtualFile file;
	private int currentIndex;
	@NotNull
	private EditorGroup displayedGroup = EditorGroup.EMPTY;
	private VirtualFile fileFromTextEditor;
	boolean reload = true;

	public EditorGroupPanel(@NotNull TextEditorImpl textEditor, @NotNull Project project, @Nullable EditorGroup userData, VirtualFile file) {
		super(new HorizontalLayout(0));
		System.out.println("EditorGroupPanel " + "textEditor = [" + textEditor + "], project = [" + project + "], userData = [" + userData + "], file = [" + file + "]");


		Editor editor = textEditor.getEditor();
		editor.putUserData(EDITOR_PANEL, this);
		if (userData != null) {
			displayedGroup = userData;
		}
		this.textEditor = textEditor;
		this.project = project;
		this.file = file;
		if (editor instanceof EditorImpl) {
			EditorImpl editorImpl = (EditorImpl) editor;
			editorImpl.addFocusListener(new FocusChangeListener() {
				@Override
				public void focusGained(Editor editor) {
					EditorGroupPanel.this.focusGained(editor);
				}

				@Override
				public void focusLost(Editor editor) {

				}
			});
		}
		fileFromTextEditor = Utils.getFileFromTextEditor(project, textEditor);
		addButtons();

		refresh(false, null);
		EditorGroupManager.getInstance(project).switching(false);
	}


	private void init(EditorGroup group) {
		this.displayedGroup = group;
		textEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
		setVisible(!group.getRelatedPaths().isEmpty());

		addButtons();

		createLinks();
	}

	private void addButtons() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Refresh"));
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Previous"));
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Next"));

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("krasa.editorGroups.EditorGroupPanel", actionGroup, true);
		toolbar.setTargetComponent(this);
		add(toolbar.getComponent());
	}

	private void createLinks() {
		List<String> paths = displayedGroup.getLinks();

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);

			JButton button = new JButton(Utils.toPresentableName(path));
			// BROKEN in IJ 2018
			// button.setBorder(null);
			// button.setContentAreaFilled(false);
			// button.setOpaque(false);
			// button.setBorderPainted(false);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openFile(Utils.getFileByPath(path), BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK), BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK));
				}
			});
			if (Utils.isTheSameFile(path, fileFromTextEditor)) {
				Font font = button.getFont();
				button.setFont(font.deriveFont(Font.BOLD));
				if (UIUtil.isUnderDarcula()) {
					button.setForeground(Color.WHITE);
				} else {
					button.setForeground(Color.BLACK);
				}
				currentIndex = i1;
			}

			if (!new File(path).exists()) {
				button.setEnabled(false);
			}
			add(button);
		}
	}

	public void previous(boolean newTab, boolean newWindow) {
		if (displayedGroup.invalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}
		int iterations = 0;
		List<String> paths = displayedGroup.getLinks();
		VirtualFile fileByPath = null;

		while (fileByPath == null && iterations < paths.size()) {
			iterations++;

			int i = currentIndex - iterations;
			if (i < 0) {
				i = paths.size() - Math.abs(i);
			}
			String s = paths.get(i);

			fileByPath = Utils.getVirtualFileByAbsolutePath(s);
		}
		openFile(fileByPath, newTab, newWindow);

	}

	public void next(boolean newTab, boolean newWindow) {
		if (displayedGroup.invalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}
		VirtualFile fileByPath = null;
		int iterations = 0;
		List<String> paths = displayedGroup.getLinks();

		while (fileByPath == null && iterations < paths.size()) {
			iterations++;

			String s = paths.get((currentIndex + iterations) % paths.size());

			fileByPath = Utils.getVirtualFileByAbsolutePath(s);
		}

		openFile(fileByPath, newTab, newWindow);
	}

	private void openFile(VirtualFile fileToOpen, boolean newTab, boolean newWindow) {
		if (EditorGroupManager.getInstance(project).switching()) {
			System.out.println("openFile fail - switching");
			return;
		}

		if (fileToOpen == null || fileToOpen.equals(file)) {
			return;
		}

		CommandProcessor.getInstance().executeCommand(project, () -> {
			open(newTab, fileToOpen, newWindow);
		}, null, null);

	}

	private void open(boolean newTab, VirtualFile fileToOpen, boolean newWindow) {
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

		EditorWindow currentWindow = manager.getCurrentWindow();
		VirtualFile selectedFile = currentWindow.getSelectedFile();
		System.out.println("open " + "newTab = [" + newTab + "], fileToOpen = [" + fileToOpen + "], newWindow = [" + newWindow + "]");

		//not closing existing tab beforehand seems to have either no effect, or it is better, dunno
//		manager.closeFile(fileToOpen, false, false);

		if (newWindow) {
			Pair<FileEditor[], FileEditorProvider[]> pair = manager.openFileInNewWindow(fileToOpen);
			for (FileEditor fileEditor : pair.first) {
				fileEditor.putUserData(EDITOR_GROUP, displayedGroup);
			}
		} else {

			EditorGroupManager.getInstance(project).switching(true);
			FileEditor[] fileEditors = manager.openFile(fileToOpen, true);
			for (FileEditor fileEditor : fileEditors) {
				fileEditor.putUserData(EDITOR_GROUP, displayedGroup);
			}

			//not sure, but it seems to mess order of tabs less if we do it after opening a new tab
			if (!newTab) {
				manager.closeFile(selectedFile, currentWindow, false);
			}

		}

	}


	@Override
	public double getWeight() {
		return -666;
	}


	private void focusGained(@NotNull Editor editor) {
		//important when switching to a file that has an exsting editor
		EditorGroup userData1 = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
		System.out.println("focusGained " + file + " " + userData1);
		if (userData1 != null && userData1.valid() && displayedGroup != userData1) {
			reload = true;
			refresh(false, userData1);
		} else {
			refresh(false, null);
		}
		EditorGroupManager.getInstance(project).switching(false);
	}

	static class RefreshRequest {
		final boolean force;
		final EditorGroup newGroup;

		public RefreshRequest(boolean force, EditorGroup newGroup) {
			this.force = force;
			this.newGroup = newGroup;
		}

		public String toString() {
			return new ToStringBuilder(this)
				.append("force", force)
				.append("newGroup", newGroup)
				.toString();
		}
	}

	AtomicReference<RefreshRequest> atomicReference = new AtomicReference<>();

	/**
	 * call from any thread
	 */
	public void refresh(boolean force, EditorGroup newGroup) {
		if (!force && newGroup == null) { //unnecessary refresh
			atomicReference.compareAndSet(null, new RefreshRequest(force, newGroup));
		} else {
			atomicReference.set(new RefreshRequest(force, newGroup));
		}

		com.intellij.openapi.application.Application application = ApplicationManager.getApplication();
		boolean dispatchThread = application.isDispatchThread();
		if (!dispatchThread) {
			application.invokeLater(new Runnable() {
				@Override
				public void run() {
					refreshSmart();
				}
			});
		} else {
			refreshSmart();
		}
	}

	private void refreshSmart() {
		DumbService.getInstance(project).runWhenSmart(new Runnable() {
			@Override
			public void run() {
				long start = System.currentTimeMillis();
				RefreshRequest request = atomicReference.getAndSet(null);
				if (request == null) {
					System.out.println("nothing to refresh");
					return;
				}
				System.out.println("refresh " + request);

				EditorGroup newGroup = request.newGroup;
				boolean force = request.force;

				try {

					EditorGroup lastGroup = newGroup == null ? displayedGroup : newGroup;
					EditorGroup group = EditorGroupManager.getInstance(project).getGroup(project, textEditor, lastGroup, force);
					if (group == displayedGroup && !reload && !force) {
						return;
					}
					removeAll();
					init(group);
					revalidate();
					repaint();
					reload = false;
					MyFileManager.updateTitle(EditorGroupPanel.this.project, file);

				} catch (Exception e) {
					LOG.error(e);
					e.printStackTrace();
				} finally {
					System.out.println("refreshDone in " + (System.currentTimeMillis() - start) + "ms");
				}

			}
		});
	}

	@Override
	public void dispose() {
	}

	public void onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		if (atomicReference.get() == null && displayedGroup.isOwner(ownerPath) && !displayedGroup.equals(group)) {
			System.out.println("onIndexingDone " + "ownerPath = [" + ownerPath + "], group = [" + group + "]");
			//concurrency is a bitch, do not alter data
//			displayedGroup.invalid();
			refresh(false, null);
		}
	}
}
