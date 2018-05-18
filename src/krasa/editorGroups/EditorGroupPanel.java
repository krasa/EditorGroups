package krasa.editorGroups;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
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
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Weighted;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;

public class EditorGroupPanel extends JBPanel implements Weighted, Disposable {
	private static final Logger LOG = Logger.getInstance(EditorGroupPanel.class);


	public static final Key<EditorGroupPanel> EDITOR_GROUPS_PANEL = Key.create("EDITOR_GROUPS_PANEL");
	public static final Key<EditorGroup> EDITOR_GROUP = Key.create("EDITOR_GROUP");
	public static final Key<Object> EDITOR_GROUP_PARSED = Key.create("EDITOR_GROUP_PARSED");

	@NotNull
	private final TextEditorImpl textEditor;
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
		textEditor.getEditor().putUserData(EDITOR_GROUPS_PANEL, this);
		System.out.println("new EditorGroupPanel");
		if (userData != null) {
			displayedGroup = userData;
		}
		this.textEditor = textEditor;
		this.project = project;
		this.file = file;
		if (textEditor.getEditor() instanceof EditorImpl) {
			EditorImpl editor = (EditorImpl) textEditor.getEditor();
			editor.addFocusListener(new FocusChangeListener() {
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

			fileByPath = Utils.getFileByPath(s);
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

			fileByPath = Utils.getFileByPath(s);
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
		System.out.println("open");

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


	private void createLinks() {
		List<String> paths = displayedGroup.getLinks();

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);
			String name = path;
			int i = StringUtil.lastIndexOfAny(path, "\\/");
			if (i > 0) {
				name = path.substring(i + 1);
			}

			JButton button = new JButton(name);
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
				button.setForeground(Color.red);
			}
			add(button);
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
		if (userData1 != null) {
			if (userData1.valid() && displayedGroup != userData1) {
				reload = true;
				refresh(false, userData1);
			} else if (!displayedGroup.valid()) {
				refresh(false, null);
			}
		}
		EditorGroupManager.getInstance(project).switching(false);
	}

	public void refresh(boolean refresh, EditorGroup newGroup) {
		DumbService.getInstance(project).runWhenSmart(new Runnable() {
			@Override
			public void run() {
				System.out.println("refresh");
				try {

					EditorGroup lastGroup = newGroup == null ? displayedGroup : newGroup;
					EditorGroup group = EditorGroupManager.getInstance(project).getGroup(project, textEditor, lastGroup, refresh);
					if (group == displayedGroup && !reload && !refresh) {
						return;
					}
					textEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
					removeAll();
					init(group);
					revalidate();
					repaint();
					reload = false;
				} catch (Exception e) {
					LOG.error(e);
					e.printStackTrace();
				}
			}

		})
		;
	}

	@Override
	public void dispose() {
	}
}
