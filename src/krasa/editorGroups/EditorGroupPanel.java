package krasa.editorGroups;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Weighted;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBDimension;
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
	private EditorGroup myGroup = EditorGroup.EMPTY;
	private VirtualFile fileFromTextEditor;
	boolean reload = false;

	public EditorGroupPanel(@NotNull TextEditorImpl textEditor, @NotNull Project project, @Nullable EditorGroup userData, VirtualFile file) {
		super(new HorizontalLayout(0));
		textEditor.getEditor().putUserData(EDITOR_GROUPS_PANEL, this);
		System.out.println("new EditorGroupPanel");
		if (userData != null) {
			myGroup = userData;
		}
		this.textEditor = textEditor;
		this.project = project;
		this.file = file;
		if (textEditor.getEditor() instanceof EditorImpl) {
			EditorImpl editor = (EditorImpl) textEditor.getEditor();
			editor.addFocusListener(new FocusChangeListener() {
				@Override
				public void focusGained(Editor editor) {
					EditorGroup userData1 = textEditor.getUserData(EditorGroupPanel.EDITOR_GROUP);
					System.out.println("focusGained " + userData1);
					if (userData1 != null && userData1.valid() && myGroup != userData1) {
						reload = true;
						myGroup = userData1;
						refresh(false);
					}
				}

				@Override
				public void focusLost(Editor editor) {

				}
			});
		}
		fileFromTextEditor = Utils.getFileFromTextEditor(project, textEditor);

		EditorGroup group = EditorGroupManager.getInstance(project).getGroup(textEditor, myGroup, false);

		init(group);
	}

	private void init(EditorGroup group) {
		this.myGroup = group;
		textEditor.putUserData(EDITOR_GROUP, myGroup); // for titles
		setVisible(!group.getPaths().isEmpty());

		add(refreshButton());
		add(previousButton());
		add(nextButton());

		createLinks();
	}

	@NotNull
	private JButton refreshButton() {
		JButton refresh = new JButton();
		refresh.setToolTipText(getLabel("krasa.editorGroups.Refresh", "Refresh"));
		refresh.addActionListener(e -> refresh(true));
		refresh.setIcon(AllIcons.Actions.Refresh);
		refresh.setPreferredSize(new JBDimension(AllIcons.Actions.Refresh.getIconWidth() + 10, refresh.getHeight()));
		return refresh;
	}

	private Component previousButton() {
		JButton refresh = new JButton();
		refresh.setToolTipText(getLabel("krasa.editorGroups.Previous", "Previous File"));
		refresh.addActionListener(e -> previous(BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK), BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK)));
		refresh.setIcon(AllIcons.Actions.Back);
		refresh.setPreferredSize(new JBDimension(AllIcons.Actions.Refresh.getIconWidth() + 10, refresh.getHeight()));
		return refresh;
	}

	private Component nextButton() {
		JButton refresh = new JButton();
		refresh.setToolTipText(getLabel("krasa.editorGroups.Next", "Next File"));
		refresh.addActionListener(e -> next(BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK), BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK)));
		refresh.setIcon(AllIcons.Actions.Forward);
		refresh.setPreferredSize(new JBDimension(AllIcons.Actions.Refresh.getIconWidth() + 10, refresh.getHeight()));
		return refresh;
	}

	public void previous(boolean newTab, boolean newWindow) {
		if (myGroup.invalid()) {
			return;
		}
		int iterations = 0;
		List<String> paths = myGroup.getPaths();
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
		if (myGroup.invalid()) {
			return;
		}
		VirtualFile fileByPath = null;
		int iterations = 0;
		List<String> paths = myGroup.getPaths();

		while (fileByPath == null && iterations < paths.size()) {
			iterations++;

			String s = paths.get((currentIndex + iterations) % paths.size());

			fileByPath = Utils.getFileByPath(s);
		}

		openFile(fileByPath, newTab, newWindow);
	}

	private void openFile(VirtualFile fileToOpen, boolean newTab, boolean newWindow) {
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
				fileEditor.putUserData(EDITOR_GROUP, myGroup);
			}
		} else {
			FileEditor[] fileEditors = manager.openFile(fileToOpen, true);
			for (FileEditor fileEditor : fileEditors) {
				fileEditor.putUserData(EDITOR_GROUP, myGroup);
			}

			//not sure, but it seems to mess order of tabs less if we do it after opening a new tab
			if (!newTab) {
				manager.closeFile(selectedFile, currentWindow, false);
			}

		} 
		
	}

	@NotNull
	private static String getLabel(String actionId, final String prefix) {
		Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
		if (shortcuts.length > 0) {
			Shortcut shortcut = shortcuts[0];
			if (shortcut.isKeyboard()) {
				KeyboardShortcut key = (KeyboardShortcut) shortcut;
				String s = KeymapUtil.getShortcutsText(new Shortcut[]{key});
				if (s != null) {
					return prefix + " (" + s.toUpperCase() + ")";
				}
			}
		}
		return prefix;
	}

	private void createLinks() {
		List<String> paths = myGroup.getPaths();
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

	public void refresh(boolean reparse) {
		EditorGroup group = EditorGroupManager.getInstance(project).getGroup(textEditor, myGroup, reparse);
		if (group == myGroup && !reload) {
			return;
		}
		textEditor.putUserData(EDITOR_GROUP, myGroup); // for titles
		removeAll();
		init(group);
		revalidate();
		repaint();
		reload = false;
	}

	@Override
	public void dispose() {
	}
}
