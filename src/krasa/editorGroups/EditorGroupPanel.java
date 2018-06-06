package krasa.editorGroups;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
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
import com.intellij.ui.components.JBPanel;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBUI;
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
import java.awt.*;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;
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
	private volatile int myScrollOffset;
	private int currentIndex = -1;
	@NotNull
	private volatile EditorGroup displayedGroup = EditorGroup.EMPTY;
	private volatile EditorGroup toBeRendered;
	private VirtualFile fileFromTextEditor;
	private krasa.editorGroups.tabs.impl.JBEditorTabs tabs;
	private FileEditorManagerImpl fileEditorManager;
	public EditorGroupManager groupManager;
	private ActionToolbar toolbar;

	public EditorGroupPanel(@NotNull FileEditor fileEditor, @NotNull Project project, @Nullable EditorGroup editorGroup, VirtualFile file, int myScrollOffset) {
		super(new BorderLayout());
		this.fileEditor = fileEditor;
		this.project = project;
		this.file = file;
		this.myScrollOffset = myScrollOffset;
		toBeRendered = editorGroup;
		groupManager = EditorGroupManager.getInstance(this.project);
		fileEditorManager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		System.out.println("EditorGroupPanel " + "textEditor = [" + fileEditor + "], project = [" + project + "], userData = [" + editorGroup + "], file = [" + file + "]");
		fileEditor.putUserData(EDITOR_PANEL, this);

		if (fileEditor instanceof TextEditorImpl) {
			Editor editor = ((TextEditorImpl) fileEditor).getEditor();
			if (editor instanceof EditorImpl) {
				EditorImpl editorImpl = (EditorImpl) editor;
				editorImpl.addFocusListener(new FocusChangeListener() {
					@Override
					public void focusGained(Editor editor) {
						EditorGroupPanel.this.focusGained();
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

				if (info instanceof MyGroupTabInfo) {
					refresh(false, ((MyGroupTabInfo) info).editorGroup);
				} else {
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
				}
				return ActionCallback.DONE;
			}
		});
		JComponent component = tabs.getComponent();
		add(component, BorderLayout.CENTER);


		addMouseListener(getPopupHandler());
		tabs.addMouseListener(getPopupHandler());


		if (editorGroup == null) {
			refresh(false, null);
		} else {
// TODO minimize flicker  - DOES NOT WORK
//			render();

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					render();
				}
			});
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


	private void addButtons() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Refresh"));
//		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Previous"));
//		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.Next"));
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.SwitchGroup"));


		DefaultActionGroup action = new DefaultActionGroup();
		actionGroup.add(action);

		toolbar = ActionManager.getInstance().createActionToolbar("krasa.editorGroups.EditorGroupPanel", actionGroup, true);
		toolbar.setTargetComponent(this);
		JComponent component = toolbar.getComponent();
		component.addMouseListener(getPopupHandler());
		component.setBorder(JBUI.Borders.empty());
		add(component, BorderLayout.WEST);
	}

	private void reloadTabs() {
		tabs.removeAllTabs();
		createLinks();
		if (displayedGroup instanceof GroupsHolder) {
			createGroupLinks(((GroupsHolder) displayedGroup).getGroups());
		}


		tabs.doLayout();
		tabs.scroll(myScrollOffset);
	}

	private void createLinks() {
		List<String> paths = displayedGroup.getLinks(project);

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);

			MyTabInfo tab = new MyTabInfo(path);

			tabs.addTab(tab);

			if (Utils.isTheSameFile(path, fileFromTextEditor)) {
				tabs.setMySelectedInfo(tab);
				currentIndex = i1;
			}
		}
	}

	private void createGroupLinks(Collection<EditorGroup> groups) {
		if (tabs.getTabCount() == 0) {
			tabs.addTab(new MyTabInfo(file.getCanonicalPath()));
		}
		for (EditorGroup editorGroup : groups) {
			tabs.addTab(new MyGroupTabInfo(editorGroup));
		}
	}

	class MyTabInfo extends TabInfo {
		String path;

		public MyTabInfo(String path) {
			this.path = path;
			String name = Utils.toPresentableName(path);
			setText(name);
			setTooltipText(path);

			if (!new File(path).exists()) {
				setEnabled(false);
			}
		}
	}

	class MyGroupTabInfo extends TabInfo {
		EditorGroup editorGroup;

		public MyGroupTabInfo(EditorGroup editorGroup) {
			this.editorGroup = editorGroup;
			String title = editorGroup.getTitle();
			if (title.isEmpty()) {
				title = Utils.toPresentableName(editorGroup.getOwnerPath());
			}
			if (ApplicationConfiguration.state().showSize) {
				title += ":" + editorGroup.size(project);
			}
			setText("[ " + title + " ]");
			setToolTipText(editorGroup.getPresentableTitle(project, "Owner: " + editorGroup.getOwnerPath(), true));

		}
	}

	public void previous(boolean newTab, boolean newWindow) {
		if (currentIndex == -1) { //group was not refreshed
			return;
		}
		if (displayedGroup.isInvalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}

		int iterations = 0;
		List<String> paths = displayedGroup.getLinks(project);
		VirtualFile fileByPath = null;

		if (!ApplicationConfiguration.state().isContinuousScrolling() && currentIndex == 0) {
			return;
		}
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
		if (currentIndex == -1) { //group was not refreshed
			return;
		}
		if (displayedGroup.isInvalid()) {
			return;
		}
		if (!isVisible()) {
			return;
		}
		VirtualFile fileByPath = null;
		int iterations = 0;
		List<String> paths = displayedGroup.getLinks(project);

		if (!ApplicationConfiguration.state().isContinuousScrolling() && currentIndex == paths.size() - 1) {
			return;
		}
		while (fileByPath == null && iterations < paths.size()) {
			iterations++;

			String s = paths.get((currentIndex + iterations) % paths.size());

			fileByPath = Utils.getVirtualFileByAbsolutePath(s);
		}

		openFile(fileByPath, newTab, newWindow);
	}

	private void openFile(VirtualFile fileToOpen, boolean newTab, boolean newWindow) {
		if (fileToOpen == null) {
			System.err.println("openFile fail - file is null");
			return;
		}

		if (fileToOpen.equals(file) && !newWindow) {
			System.err.println("openFile fail - same file");
			return;
		}


		if (groupManager.switching()) {
			System.out.println("openFile fail - switching");
			return;
		}
		if (toBeRendered != null) {
			System.out.println("openFile fail - toBeRendered != null");
			return;
		}
		groupManager.open(fileToOpen, displayedGroup, newWindow, newTab, file, tabs.getMyScrollOffset());

	}


	@Override
	public double getWeight() {
		return Integer.MIN_VALUE;
	}


	private void focusGained() {
		//important when switching to a file that has an exsting editor

		EditorGroup switchingGroup = groupManager.getSwitchingGroup(file);
		System.out.println("focusGained " + file + " " + switchingGroup);
		if (switchingGroup != null && switchingGroup.isValid() && displayedGroup != switchingGroup) {
			refresh(false, switchingGroup);
		} else {
			refresh(false, null);
		}
		groupManager.switching(false);
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
		if (!refresh && newGroup == null) { //unnecessary or initial refresh
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
		System.out.println(">refresh3 " + request);

		EditorGroup requestedGroup = request.requestedGroup;
		boolean refresh = request.refresh;

		try {
			EditorGroup group = ApplicationManager.getApplication().runReadAction(new Computable<EditorGroup>() {
				@Override
				public EditorGroup compute() {
					EditorGroup lastGroup = toBeRendered == null ? displayedGroup : toBeRendered;
					return groupManager.getGroup(project, fileEditor, lastGroup, requestedGroup, refresh, file);
				}
			});
			if (!refresh && (group == displayedGroup || group == toBeRendered || group.isSame(project, displayedGroup))) {
				groupManager.switching(false); //need for UI forms - when switching to open editors , focus listener does not do that
				System.out.println("no change, skipping refresh, toBeRendered=" + toBeRendered);
				return;
			}
			toBeRendered = group;
			if (refresh) {
				myScrollOffset = tabs.getMyScrollOffset();   //this will have edge cases
			}
			
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					render();
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

	private void render() {
		EditorGroup rendering = toBeRendered;
		if (rendering == null) {
			return;
		}
		displayedGroup = rendering;
		toBeRendered = null;

		long start = System.currentTimeMillis();

		reloadTabs();

		if (ApplicationConfiguration.state().hideEmpty) {
			boolean hide = (rendering instanceof AutoGroup && ((AutoGroup) rendering).isEmpty());
			setVisible(!hide);
		} else {
			setVisible(true);
		}

		fileEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
		file.putUserData(EDITOR_GROUP, displayedGroup); // for project view colors
		fileEditorManager.updateFilePresentation(file);
		toolbar.updateActionsImmediately();

		revalidate();
		repaint();
		failed = 0;
		groupManager.switching(false);
		System.out.println("<refreshOnEDT " + (System.currentTimeMillis() - start) + "ms " + fileEditor.getName() + " " + displayedGroup);
	}


	@Override
	public void dispose() {
	}

	public void onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		if (atomicReference.get() == null && displayedGroup.isOwner(ownerPath) && !displayedGroup.equals(group)) {
			System.out.println("onIndexingDone " + "ownerPath = [" + ownerPath + "], group = [" + group + "]");
			//concurrency is a bitch, do not alter data
//			displayedGroup.invalid();                    0o
			refresh(false, null);
		}
	}

	@NotNull
	public EditorGroup getDisplayedGroup() {
		return displayedGroup;
	}

	public EditorGroup getToBeRendered() {
		return toBeRendered;
	}

	public VirtualFile getFile() {
		return file;
	}
}
