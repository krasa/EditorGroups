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
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.actions.PopupMenu;
import krasa.editorGroups.model.AutoGroup;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupIndexValue;
import krasa.editorGroups.model.GroupsHolder;
import krasa.editorGroups.support.Utils;
import krasa.editorGroups.tabs.JBTabs;
import krasa.editorGroups.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class EditorGroupPanel extends JBPanel implements Weighted, Disposable {
	private static final Logger LOG = Logger.getInstance(EditorGroupPanel.class);


	public static final Key<EditorGroupPanel> EDITOR_PANEL = Key.create("EDITOR_GROUPS_PANEL");
	public static final Key<EditorGroup> EDITOR_GROUP = Key.create("EDITOR_GROUP");

	@NotNull
	private final FileEditor fileEditor;
	@NotNull
	private Project project;
	private final VirtualFile file;
	private int currentIndex;
	@NotNull
	private volatile EditorGroup displayedGroup = EditorGroup.EMPTY;
	private VirtualFile fileFromTextEditor;
	boolean reload = true;
	private JBPanel groupsPanel = new JBPanel();
	private krasa.editorGroups.tabs.impl.JBEditorTabs tabs;
	private FileEditorManagerImpl fileEditorManager;

	public EditorGroupPanel(@NotNull FileEditor fileEditor, @NotNull Project project, @Nullable EditorGroup userData, VirtualFile file) {
		super(new BorderLayout());
		fileEditorManager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		System.out.println("EditorGroupPanel " + "textEditor = [" + fileEditor + "], project = [" + project + "], userData = [" + userData + "], file = [" + file + "]");
//		scrollPane = new HackedJBScrollPane(this);
//
//		scrollPane.setBorder(JBUI.Borders.empty()); // set empty border, because setting null doesn't always take effect
//		scrollPane.setViewportBorder(JBUI.Borders.empty());
//		scrollPane.createHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
//
//			@Override
//			public void adjustmentValueChanged(AdjustmentEvent e) {
//				adjustScrollPane();
//			}
//		});


		fileEditor.putUserData(EDITOR_PANEL, this);
		if (userData != null) {
			displayedGroup = userData;
		}
		this.fileEditor = fileEditor;
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

//		groupsPanel.setLayout(new HorizontalLayout(0));
		tabs = new krasa.editorGroups.tabs.impl.JBEditorTabs(project, ActionManager.getInstance(), IdeFocusManager.findInstance(), fileEditor);
		tabs.setSelectionChangeHandler(new JBTabs.SelectionChangeHandler() {
			@NotNull
			@Override
			public ActionCallback execute(TabInfo info, boolean requestFocus, Integer modifiers, ActiveRunnable doChangeSelection) {
				if (modifiers == null) {
					return ActionCallback.DONE;
				}
				MyTabInfo myTabInfo = (MyTabInfo) info;
				VirtualFile fileByPath = Utils.getFileByPath(myTabInfo.path);
				if (fileByPath == null) {
					setEnabled(false);
					return null;
				}

				if (modifiers == null) {
					modifiers = 0;
				}

				boolean ctrl = BitUtil.isSet(modifiers, InputEvent.CTRL_MASK);
				boolean alt = BitUtil.isSet(modifiers, InputEvent.ALT_MASK);
				boolean shift = BitUtil.isSet(modifiers, InputEvent.SHIFT_MASK);

				openFile(fileByPath, ctrl || alt, shift);
				return ActionCallback.DONE;
			}
		});
		JComponent component = tabs.getComponent();
		add(component, BorderLayout.CENTER);
		groupsPanel.withPreferredHeight(20);
		add(groupsPanel, BorderLayout.EAST);
		refresh(false, null);


		addMouseListener(getPopupHandler());
		tabs.addMouseListener(getPopupHandler());
	}

	private static class MyPlaceholder extends JPanel {
		MyPlaceholder(String evaluation_in_process) {
			super(new BorderLayout());
			add(new JBLabel(evaluation_in_process, SwingConstants.CENTER), BorderLayout.CENTER);
		}

		void setContent(@NotNull JComponent view, String placement) {
			Arrays.stream(getComponents()).forEach(this::remove);
			add(view, placement);
			revalidate();
			repaint();
		}
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
		tabs.removeAllTabs();
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
					refresh(false, editorGroup);
				}
			});  
			button.setFont(newFont);
			button.setPreferredSize(new Dimension(button.getPreferredSize().width, button.getPreferredSize().height - 10));
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
		add(component, BorderLayout.WEST);
	}

	private void createLinks() {
		List<String> paths = displayedGroup.getLinks(project);

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);

			String name = Utils.toPresentableName(path);
			MyTabInfo tab = new MyTabInfo();
			tab.path = path;
			TabInfo tabInfo = tab.setText(name).setTooltipText(path);


			if (!new File(path).exists()) {
				tab.setEnabled(false);
			}

			tabs.addTab(tabInfo);

			if (Utils.isTheSameFile(path, fileFromTextEditor)) {
				tabs.setMySelectedInfo(tabInfo);
				currentIndex = i1;
			}
		}
	}

	class MyTabInfo extends TabInfo {
		String path;
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
			FileEditor[] fileEditors = manager.openFile(fileToOpen, true);
			if (fileEditors.length == 0) {  //directory or some fail
				EditorGroupManager.getInstance(project).switching(false, null);
				return;
			}

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
			refresh(false, switchingGroup);
		} else {
//			refresh(false, null);
		}
		EditorGroupManager.getInstance(project).switching(false, null);
	}


	public JComponent getRoot() {
		return this;
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
	public void refresh(boolean refresh, EditorGroup newGroup) {
		if (!refresh && newGroup == null) { //unnecessary refresh
			atomicReference.compareAndSet(null, new RefreshRequest(refresh, newGroup));
		} else {
			atomicReference.set(new RefreshRequest(refresh, newGroup));
		}
		refresh2();

	}

	private void refresh2() {
		PanelRefresher.getInstance(project).refreshOnBackground(new Runnable() {
			@Override
			public void run() {
				DumbService.getInstance(project).waitForSmartMode();
				refresh3();
			}
		});
	}

	private volatile int failed = 0;

	private void refresh3() {
		if (SwingUtilities.isEventDispatchThread()) {
			LOG.error("do not execute it on EDT");
		}

		long start = System.currentTimeMillis();
		RefreshRequest request = atomicReference.getAndSet(null);
		if (request == null) {
			System.out.println("nothing to refresh " + fileEditor.getName());
			return;
		}
		System.out.println(">refreshSmart " + request);

		EditorGroup requestedGroup = request.requestedGroup;
		boolean refresh = request.refresh;

		try {
			EditorGroup group = ApplicationManager.getApplication().runReadAction(new Computable<EditorGroup>() {
				@Override
				public EditorGroup compute() {
					return EditorGroupManager.getInstance(project).getGroup(project, fileEditor, displayedGroup, requestedGroup, refresh);
				}
			});
			if (group == displayedGroup
				&& !reload
				&& !refresh
				&& !(group instanceof AutoGroup) //need to refresh group links
			) {
				return;
			}
			displayedGroup = group;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					
					long start = System.currentTimeMillis();
					fileEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
					file.putUserData(EDITOR_GROUP, displayedGroup); // for project view colors

					int groupsCount = 0;
					if (group instanceof GroupsHolder) {
						groupsCount = reloadGroupLinks(((GroupsHolder) group).getGroups());
					} else {
						groupsPanel.setVisible(false);
					}

					reloadLinks(group);

					if (ApplicationConfiguration.state().hideEmpty) {
						setVisible(group.getLinks(project).size() > 1 || groupsCount > 0);
					} else {
						setVisible(true);
					}

					fileEditorManager.updateFilePresentation(file);

					revalidate();
					repaint();
					reload = false;
					failed = 0;
					EditorGroupManager.getInstance(project).switching(false, null);
					System.err.println("<refreshOnEDT " + (System.currentTimeMillis() - start) + "ms " + fileEditor.getName() + " " + displayedGroup);
				}
			});
			atomicReference.compareAndSet(request, null);
			System.out.println("<refreshSmart in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
		} catch (ProcessCanceledException | IndexNotReadyException e) {
			if (++failed > 5) {
				LOG.error(e);
				return;
			}
			System.out.println("refresh failed in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
			if (atomicReference.compareAndSet(null, request)) {
				refresh2();
			}
		} catch (Exception e) {
			LOG.error(e);
			e.printStackTrace();
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
			refresh(false, null);
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
