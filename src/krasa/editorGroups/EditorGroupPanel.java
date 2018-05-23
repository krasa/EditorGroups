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
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.MyFileManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Weighted;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.actions.PopupMenu;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.HackedJBScrollPane;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class EditorGroupPanel extends JBPanel implements Weighted, Disposable {
	private static final Logger LOG = Logger.getInstance(EditorGroupPanel.class);


	public static final Key<EditorGroupPanel> EDITOR_PANEL = Key.create("EDITOR_GROUPS_PANEL");
	public static final Key<EditorGroup> EDITOR_GROUP = Key.create("EDITOR_GROUP");

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
	private JBPanel links = new JBPanel();
	private JBPanel groupsPanel = new JBPanel();
	private JBScrollPane scrollPane;
	private JButton currentButton;

	public EditorGroupPanel(@NotNull FileEditor fileEditor, @NotNull Project project, @Nullable EditorGroup userData, VirtualFile file) {
		super(new HorizontalLayout(0));
		System.out.println("EditorGroupPanel " + "textEditor = [" + fileEditor + "], project = [" + project + "], userData = [" + userData + "], file = [" + file + "]");
		scrollPane = new HackedJBScrollPane(this);

		scrollPane.setBorder(JBUI.Borders.empty()); // set empty border, because setting null doesn't always take effect
		scrollPane.setViewportBorder(JBUI.Borders.empty());
		scrollPane.createHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				adjustScrollPane();
			}
		});


		fileEditor.putUserData(EDITOR_PANEL, this);
		if (userData != null) {
			displayedGroup = userData;
		}
		this.textEditor = fileEditor;
		this.project = project;
		this.file = file;

		if (fileEditor instanceof TextEditorImpl) {
			Editor editor = ((TextEditorImpl) fileEditor).getEditor();
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
		}

		fileFromTextEditor = Utils.getFileFromTextEditor(project, fileEditor);
		addButtons();

		groupsPanel.setLayout(new HorizontalLayout(0));
		links.setLayout(new HorizontalLayout(0));


		add(links);
		add(groupsPanel);
		refresh(false, null, true);

		EditorGroupManager.getInstance(project).switching(false, null);


		addMouseListener(getPopupHandler());
		links.addMouseListener(getPopupHandler());
		groupsPanel.addMouseListener(getPopupHandler());
		scrollPane.addMouseListener(getPopupHandler());
	}

	@NotNull
	private PopupHandler getPopupHandler() {
		return new PopupHandler() {
			@Override
			public void invokePopup(Component comp, int x, int y) {
				PopupMenu.popupInvoked(comp, x, y);
			}
		};
	}


	private void reloadLinks(EditorGroup group) {
		links.removeAll();
		this.displayedGroup = group;
		createLinks();
	}

	private int reloadGroupLinks(Collection<EditorGroup> groups) {
		Font font = getFont();
		Map attributes = font.getAttributes();
//		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		Font newFont = font.deriveFont(attributes);

		this.groupsPanel.removeAll();
		boolean added = false;
		int groupsCount = 0;
		for (EditorGroup editorGroup : groups) {
			added = true;
			String title = editorGroup.getTitle();
			if (title.isEmpty()) {
				title = Utils.toPresentableName(editorGroup.getOwnerPath());
			}

			JButton button = new JButton("[ " + title + " ]");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					refresh(false, editorGroup, false);
				}
			});
			button.setFont(newFont);
			button.setPreferredSize(new Dimension(button.getPreferredSize().width, button.getPreferredSize().height - 5));
			button.addMouseListener(getPopupHandler());
			if (UIUtil.isUnderDarcula()) {
				button.setBorder(new LineBorder(Color.lightGray));
			} else {
				button.setBorder(new LineBorder(Color.BLACK));
			}
			button.setToolTipText(editorGroup.getPresentableTitle(project, "Owner: " + editorGroup.getOwnerPath(), true));
			this.groupsPanel.add(button);
			groupsCount++;
		}
		this.groupsPanel.setVisible(added);
		return groupsCount;
	}

	private void addButtons() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Refresh"));
//		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Previous"));
//		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Next"));
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.SwitchGroup"));


		DefaultActionGroup action = new DefaultActionGroup();
		actionGroup.add(action);

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("krasa.editorGroups.EditorGroupPanel", actionGroup, true);
		toolbar.setTargetComponent(this);
		JComponent component = toolbar.getComponent();
		component.addMouseListener(getPopupHandler());
		component.setBorder(JBUI.Borders.empty());
		add(component);
	}

	private void createLinks() {
		List<String> paths = displayedGroup.getLinks(project);

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);

			JButton button = new JButton(Utils.toPresentableName(path));
			button.setPreferredSize(new Dimension(button.getPreferredSize().width, button.getPreferredSize().height - 5));
			// BROKEN in IJ 2018
			// button.setBorder(null);
			// button.setContentAreaFilled(false);
			// button.setOpaque(false);
			// button.setBorderPainted(false);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					VirtualFile fileByPath = Utils.getFileByPath(path);
					if (fileByPath == null) {
						setEnabled(false);
						return;
					}
					boolean ctrl = BitUtil.isSet(e.getModifiers(), InputEvent.CTRL_MASK);
					boolean alt = BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK);
					boolean shift = BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK);

					openFile(fileByPath, ctrl || alt, shift);
				}
			});
			if (Utils.isTheSameFile(path, fileFromTextEditor)) {
				button.setFont(button.getFont().deriveFont(Font.BOLD));
				if (UIUtil.isUnderDarcula()) {
					button.setForeground(Color.WHITE);
				} else {
					button.setForeground(Color.BLACK);
				}
				currentIndex = i1;
				currentButton = button;
			} else {
				if (displayedGroup instanceof AutoGroup) {
					if (UIUtil.isUnderDarcula()) {
						if (displayedGroup instanceof FolderGroup) {
							button.setForeground(Color.orange);
						}
						button.setFont(button.getFont().deriveFont(Font.ITALIC));
					} else {
//						Color fg = new Color(0, 23, 3, 255);
//						button.setForeground(fg);
						button.setFont(button.getFont().deriveFont(Font.ITALIC));
					}
				}
			}


			button.setToolTipText(path);
			button.addMouseListener(getPopupHandler());

			if (!new File(path).exists()) {
				button.setEnabled(false);
			}
			links.add(button);
		}
	}

	public void previous(boolean newTab, boolean newWindow) {
		if (displayedGroup.isInvalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}
		int iterations = 0;
		List<String> paths = displayedGroup.getLinks(project);
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
		if (displayedGroup.isInvalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}
		VirtualFile fileByPath = null;
		int iterations = 0;
		List<String> paths = displayedGroup.getLinks(project);

		while (fileByPath == null && iterations < paths.size()) {
			iterations++;

			String s = paths.get((currentIndex + iterations) % paths.size());

			fileByPath = Utils.getVirtualFileByAbsolutePath(s);
		}

		openFile(fileByPath, newTab, newWindow);
	}

	private void openFile(VirtualFile fileToOpen, boolean newTab, boolean newWindow) {
		if (fileToOpen == null) {
			return;
		}
	
		if (fileToOpen.equals(file) && !newWindow) {
			return;
		}


		if (EditorGroupManager.getInstance(project).switching()) {
			System.out.println("openFile fail - switching");
			return;
		}

		CommandProcessor.getInstance().executeCommand(project, () -> {
			open(fileToOpen, displayedGroup, newWindow, newTab);
		}, null, null);

	}

	public void open(VirtualFile fileToOpen, EditorGroup group, boolean newWindow, boolean newTab) {
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

		EditorWindow currentWindow = manager.getCurrentWindow();
		VirtualFile selectedFile = currentWindow.getSelectedFile();
		System.out.println("open " + "newTab = [" + newTab + "], fileToOpen = [" + fileToOpen + "], newWindow = [" + newWindow + "]");

		//not closing existing tab beforehand seems to have either no effect, or it is better, dunno
//		manager.closeFile(fileToOpen, false, false);

		EditorGroupManager.getInstance(project).switching(true, group);
		if (newWindow) {
			manager.openFileInNewWindow(fileToOpen);
		} else {
			manager.openFile(fileToOpen, true);

			//not sure, but it seems to mess order of tabs less if we do it after opening a new tab
			if (!newTab) {
				manager.closeFile(selectedFile, currentWindow, false);
			}

		}

	}


	@Override
	public double getWeight() {
		return Integer.MIN_VALUE;
	}


	private void focusGained(@NotNull Editor editor) {
		//important when switching to a file that has an exsting editor

		EditorGroup switchingGroup = EditorGroupManager.getInstance(project).getSwitchingGroup();
		System.out.println("focusGained " + file + " " + switchingGroup);
		if (switchingGroup != null && switchingGroup.isValid() && displayedGroup != switchingGroup) {
			reload = true;
			refresh(false, switchingGroup, false);
		} else {
			refresh(false, null, false);
		}
		EditorGroupManager.getInstance(project).switching(false, null);
	}


	public JComponent getRoot() {
		return scrollPane;
	}


	static class RefreshRequest {
		final boolean refresh;
		final EditorGroup requestedGroup;

		public RefreshRequest(boolean refresh, EditorGroup requestedGroup) {
			this.refresh = refresh;
			this.requestedGroup = requestedGroup;
		}


		public String toString() {
			return "RefreshRequest{" +
				"refresh=" + refresh +
				", requestedGroup=" + requestedGroup +
				'}';
		}
	}

	AtomicReference<RefreshRequest> atomicReference = new AtomicReference<>();

	/**
	 * call from any thread
	 */
	public void refresh(boolean refresh, EditorGroup newGroup, boolean alwaysInvokeLater) {
		if (!refresh && newGroup == null) { //unnecessary refresh
			atomicReference.compareAndSet(null, new RefreshRequest(refresh, newGroup));
		} else {
			atomicReference.set(new RefreshRequest(refresh, newGroup));
		}

		if (alwaysInvokeLater || !SwingUtilities.isEventDispatchThread()) {
			//this one is better than   Application.invokeLater
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					refreshSmart();
				}
			});
		} else {
			refreshSmart();
		}
	}

	private int failed = 0;

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

				EditorGroup requestedGroup = request.requestedGroup;
				boolean refresh = request.refresh;

				try {

					EditorGroup group = EditorGroupManager.getInstance(project).getGroup(project, textEditor, displayedGroup, requestedGroup, refresh);
					if (group == displayedGroup && !reload && !refresh) {
						return;
					}
					textEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles

					int groupsCount = 0;
					if (group instanceof GroupsHolder) {
						groupsCount = reloadGroupLinks(((GroupsHolder) group).getGroups());
					} else {
						groupsPanel.setVisible(false);
					}

					reloadLinks(group);

					if (ApplicationConfiguration.state().hideEmpty) {
						scrollPane.setVisible(group.getLinks(project).size() > 1 || groupsCount > 0);
					} else {
						scrollPane.setVisible(true);
					}


					MyFileManager.updateTitle(EditorGroupPanel.this.project, file);
					scrollPane.revalidate();
					scrollPane.repaint();
					reload = false;
					failed = 0;

				} catch (ProcessCanceledException e) {
					if (++failed > 5) {
						LOG.error(e);
						return;
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							atomicReference.compareAndSet(null, request);
							refreshSmart();
						}
					});
				} catch (Exception e) {
					LOG.error(e);
					e.printStackTrace();
				} finally {
					System.out.println("refreshDone in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
				}

			}
		});
	}


	private void adjustScrollPane() {
		if (scrollPane != null) {

//			scrollPane.getHorizontalScrollBar().setValue(100);
//			
//			System.out.println("adjustScrollPane");
//			Rectangle bounds = scrollPane.getViewport().getViewRect();
//			Dimension size = scrollPane.getViewport().getViewSize();
//			int x = (size.width - bounds.width) / 2;
//			int y = (size.height - bounds.height) / 2;
//			scrollPane.getViewport().setViewPosition(new Point(x, y));
//			ScrollUtil.center(scrollPane, new Rectangle(5,5));
		}
	}

	@Override
	public void dispose() {
	}

	public void onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		if (atomicReference.get() == null && displayedGroup.isOwner(ownerPath) && !displayedGroup.equals(group)) {
			System.out.println("onIndexingDone " + "ownerPath = [" + ownerPath + "], group = [" + group + "]");
			//concurrency is a bitch, do not alter data
//			displayedGroup.invalid();
			refresh(false, null, false);
		}
	}

	@NotNull
	public EditorGroup getDisplayedGroup() {
		return displayedGroup;
	}

	public VirtualFile getFile() {
		return file;
	}
}
