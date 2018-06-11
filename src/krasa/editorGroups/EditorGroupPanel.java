package krasa.editorGroups;

import com.intellij.ide.favoritesTreeView.FavoritesListener;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
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
import com.intellij.openapi.fileTypes.FileTypeManager;
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
import krasa.editorGroups.language.EditorGroupsLanguage;
import krasa.editorGroups.model.*;
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
	public static final int NOT_INITIALIZED = -10000;

	@NotNull
	private final FileEditor fileEditor;
	@NotNull
	private Project project;
	private final VirtualFile file;
	private volatile int myScrollOffset;
	private int currentIndex = NOT_INITIALIZED;
	private volatile EditorGroup displayedGroup;
	private volatile EditorGroup toBeRendered;
	private VirtualFile fileFromTextEditor;
	private krasa.editorGroups.tabs.impl.JBEditorTabs tabs;
	private FileEditorManagerImpl fileEditorManager;
	public EditorGroupManager groupManager;
	private ActionToolbar toolbar;
	private boolean disposed;
	private FavoritesListener favoritesListener;

	public EditorGroupPanel(@NotNull FileEditor fileEditor, @NotNull Project project, @Nullable EditorGroup editorGroup, VirtualFile file, int myScrollOffset) {
		super(new BorderLayout());
		if (LOG.isDebugEnabled())
			LOG.debug("EditorGroupPanel " + "fileEditor = [" + fileEditor + "], project = [" + project + "], editorGroup = [" + editorGroup + "], file = [" + file + "], myScrollOffset = [" + myScrollOffset + "]");
		this.fileEditor = fileEditor;
		Disposer.register(fileEditor, this);
		this.project = project;
		this.file = file;
		this.myScrollOffset = myScrollOffset;
		toBeRendered = editorGroup;
		groupManager = EditorGroupManager.getInstance(this.project);
		fileEditorManager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
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
		tabs = new krasa.editorGroups.tabs.impl.JBEditorTabs(project, ActionManager.getInstance(), IdeFocusManager.findInstance(), fileEditor) {

			@Override
			public boolean hasUnderlineSelection() {
				return true;
			}
		};
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
		setPreferredSize(new Dimension(0, 26));
		tabs.setAlwaysPaintSelectedTab(false);
		JComponent component = tabs.getComponent();
		add(component, BorderLayout.CENTER);

		addMouseListener(getPopupHandler());
		tabs.addMouseListener(getPopupHandler());

		//minimize flicker for the price of latency
		if (editorGroup == null && ApplicationConfiguration.state().isPreferLatencyOverFlicker() && !DumbService.isDumb(project)) {
			long start = System.currentTimeMillis();
			try {
				editorGroup = groupManager.getGroup(project, fileEditor, EditorGroup.EMPTY, editorGroup, false, file);
			} catch (Exception e) {
				LOG.error(e);
			}
			long delta = System.currentTimeMillis() - start;
			if (delta > 500) {
				LOG.warn("lag on editor opening - #getGroup took " + delta + " ms for " + file);
			}
		}


		if (editorGroup == null) {
			setVisible(false);
			refresh(false, null);
		} else {
// TODO minimize flicker  - DOES NOT WORK - tabs do not like being updated while not visible first - it really messes up scrolling
//			render();
			updateVisibility(editorGroup);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						render2();
					} catch (Exception e) {
						displayedGroup = EditorGroup.EMPTY;
						LOG.error(e);
					}
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
		actionGroup.add(ActionManager.getInstance().getAction("krasa.editorGroups.SwitchGroup"));

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

		addCurrentFileTab();

		if (displayedGroup instanceof GroupsHolder) {
			createGroupLinks(((GroupsHolder) displayedGroup).getGroups());
		}

		tabs.doLayout();
		tabs.scroll(myScrollOffset);

		if (tabs.getTabCount() > 0) { //premature optimization
			tabs.validate();
			RepaintManager.currentManager(tabs).paintDirtyRegions(); //less flicker 
		}
	}

	private void createLinks() {
		List<String> paths = displayedGroup.getLinks(project);

		for (int i1 = 0; i1 < paths.size(); i1++) {
			String path = paths.get(i1);

			MyTabInfo tab = new MyTabInfo(path);

			tabs.addTab(tab);
//			if (EditorGroupsLanguage.isEditorGroupsLanguage(path) && StringUtils.isNotEmpty(displayedGroup.getTitle()) && displayedGroup.isOwner(path)) {
//				tab.setText("[" + displayedGroup.getTitle() + "]");
//			}
			if (Utils.isTheSameFile(path, fileFromTextEditor)) {
				tabs.setMySelectedInfo(tab);
				customizeSelectedColor(tab);
				currentIndex = i1;
			}
		}


	}

	private void addCurrentFileTab() {
		if (currentIndex < 0 && (EditorGroupsLanguage.isEditorGroupsLanguage(file))) {
			MyTabInfo info = new MyTabInfo(file.getCanonicalPath());
			customizeSelectedColor(info);
			currentIndex = -1;
			tabs.addTab(info, 0);
			tabs.setMySelectedInfo(info);
		} else if (currentIndex < 0 && displayedGroup != EditorGroup.EMPTY && !(displayedGroup instanceof EditorGroups)) {
			LOG.error("current file is not contained in group " + file + " " + displayedGroup);
		}
	}


	private void createGroupLinks(Collection<EditorGroup> groups) {
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
			setIcon(getFileIcon(path));
			if (!new File(path).exists()) {
				setEnabled(false);
			}
		}
	}


	class MyGroupTabInfo extends TabInfo {
		EditorGroup editorGroup;

		public MyGroupTabInfo(EditorGroup editorGroup) {
			this.editorGroup = editorGroup;
			String title = editorGroup.tabTitle(EditorGroupPanel.this.project);
			setText("[" + title + "]");
			setToolTipText(editorGroup.getTabGroupTooltipText(EditorGroupPanel.this.project));
			setIcon(editorGroup.icon());
		}
	}

	public void previous(boolean newTab, boolean newWindow) {
		if (currentIndex == NOT_INITIALIZED) { //group was not refreshed
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - currentIndex == -1");
			return;
		}
		if (displayedGroup.isInvalid()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - displayedGroup.isInvalid");
			return;
		}
		if (!isVisible()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - !isVisible()");
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
		if (currentIndex == NOT_INITIALIZED) { //group was not refreshed
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - currentIndex == -1");
			return;
		}
		if (displayedGroup.isInvalid()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - displayedGroup.isInvalid");
			return;
		}
		if (!isVisible()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - !isVisible()");
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
		if (disposed) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - already disposed");
			return;
		}

		if (fileToOpen == null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - file is null");
			return;
		}

		if (fileToOpen.equals(file) && !newWindow) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - same file");
			return;
		}


		if (groupManager.switching()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - switching");
			return;
		}
		if (toBeRendered != null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - toBeRendered != null");
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
		if (LOG.isDebugEnabled()) LOG.debug("focusGained " + file + " " + switchingGroup);
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
				if (disposed) {
					return;
				}
				DumbService.getInstance(project).waitForSmartMode();
				refresh3();
			}
		});
	}

	private volatile int failed = 0;

	private void refresh3() {
		if (disposed) {
			return;
		}
		if (SwingUtilities.isEventDispatchThread()) {
			LOG.error("do not execute it on EDT");
		}

		long start = System.currentTimeMillis();
		RefreshRequest request = atomicReference.getAndSet(null);
		if (request == null) {
			if (LOG.isDebugEnabled()) LOG.debug("nothing to refresh " + fileEditor.getName());
			return;
		}
		if (LOG.isDebugEnabled()) LOG.debug(">refresh3 " + request);

		EditorGroup requestedGroup = request.requestedGroup;
		boolean refresh = request.refresh;

		try {
			EditorGroup group = ApplicationManager.getApplication().runReadAction(new Computable<EditorGroup>() {
				@Override
				public EditorGroup compute() {
					EditorGroup lastGroup = toBeRendered == null ? displayedGroup : toBeRendered;
					lastGroup = lastGroup == null ? EditorGroup.EMPTY : lastGroup;
					return groupManager.getGroup(project, fileEditor, lastGroup, requestedGroup, refresh, file);
				}
			});
			if (!refresh && (group == displayedGroup || group == toBeRendered || group.equalsVisually(project, displayedGroup))) {
				if (!(fileEditor instanceof TextEditorImpl)) {
					groupManager.switching(false); //need for UI forms - when switching to open editors , focus listener does not do that
				}
				if (LOG.isDebugEnabled()) LOG.debug("no change, skipping refresh, toBeRendered=" + toBeRendered);
				return;
			}
			toBeRendered = group;
			if (refresh) {
				myScrollOffset = tabs.getMyScrollOffset();   //this will have edge cases
			}

			AtomicReference<Exception> ex = new AtomicReference<>();

			SwingUtilities.invokeLater(() -> {
				if (disposed) {
					return;
				}
				EditorGroup rendering = toBeRendered;
				//tabs do not like being updated while not visible first - it really messes up scrolling
				if (!isVisible() && rendering != null && updateVisibility(rendering)) {
					SwingUtilities.invokeLater(() -> render(ex));
				} else {
					render(ex);
				}
			});
			Exception o = ex.get();
			if (o != null) {
				throw o;
			}

			atomicReference.compareAndSet(request, null);
			if (LOG.isDebugEnabled())
				LOG.debug("<refreshSmart in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
		} catch (ProcessCanceledException | IndexNotReadyException e) {
			if (++failed > 5) {
				LOG.error(e);
				return;
			}
			if (LOG.isDebugEnabled())
				LOG.debug("refresh failed in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
			if (atomicReference.compareAndSet(null, request)) {
				refresh2();
			}
		} catch (Exception e) {
			LOG.error(e);
			e.printStackTrace();
		}
	}

	private void render(AtomicReference<Exception> ex) {
		try {
			render2();
		} catch (Exception e) {
			if (LOG.isDebugEnabled()) LOG.debug(e);
			ex.set(e);
		}
	}

	private void render2() {
		if (disposed) {
			return;
		}
		EditorGroup rendering = toBeRendered;
		if (rendering == null) {
			if (LOG.isDebugEnabled()) LOG.debug("skipping render toBeRendered =" + rendering);
			return;
		}
		displayedGroup = rendering;
		toBeRendered = null;

		long start = System.currentTimeMillis();

		reloadTabs();

		updateVisibility(rendering);
		addFavouritesListener();
		fileEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
		file.putUserData(EDITOR_GROUP, displayedGroup); // for project view colors
		fileEditorManager.updateFilePresentation(file);
		toolbar.updateActionsImmediately();


		failed = 0;

		groupManager.switching(false);
		if (LOG.isDebugEnabled())
			LOG.debug("<refreshOnEDT " + (System.currentTimeMillis() - start) + "ms " + fileEditor.getName() + ", displayedGroup=" + displayedGroup);
	}

	private void addFavouritesListener() {
		if (displayedGroup instanceof FavoritesGroup && favoritesListener == null) {
			favoritesListener = new FavoritesListener() {
				@Override
				public void rootsChanged() {
					refresh(true, displayedGroup);
				}

				@Override
				public void listAdded(String listName) {

				}

				@Override
				public void listRemoved(String listName) {

				}
			};
			FavoritesManager.getInstance(project).addFavoritesListener(favoritesListener, fileEditor);
		}
	}

	private boolean updateVisibility(@NotNull EditorGroup rendering) {
		boolean visible;
		if (ApplicationConfiguration.state().isHideEmpty()) {
			boolean hide = (rendering instanceof AutoGroup && ((AutoGroup) rendering).isEmpty()) || rendering == EditorGroup.EMPTY;
			visible = !hide;
		} else {
			visible = true;
		}
		setVisible(visible);
		return visible;
	}


	@Override
	public void dispose() {
		disposed = true;
	}

	public void onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		if (atomicReference.get() == null && displayedGroup != null && displayedGroup.isOwner(ownerPath) && !displayedGroup.equals(group)) {
			if (LOG.isDebugEnabled())
				LOG.debug("onIndexingDone " + "ownerPath = [" + ownerPath + "], group = [" + group + "]");
			//concurrency is a bitch, do not alter data
//			displayedGroup.invalid();                    0o
			refresh(false, null);
		}
	}

	@NotNull
	public EditorGroup getDisplayedGroup() {
		if (displayedGroup == null) {
			return EditorGroup.EMPTY;
		}
		return displayedGroup;
	}

	public EditorGroup getToBeRendered() {
		return toBeRendered;
	}

	public VirtualFile getFile() {
		return file;
	}


	@Nullable
	private static Icon getFileIcon(String path) {
		return FileTypeManager.getInstance().getFileTypeByFileName(path).getIcon();
	}

	private void customizeSelectedColor(MyTabInfo tab) {
		ApplicationConfiguration config = ApplicationConfiguration.state();
		Color bgColor = displayedGroup.getBgColor();
		if (bgColor != null) {
			tab.setTabColor(bgColor);
		} else if (config.isTabBgColorEnabled()) {
			tab.setTabColor(config.getTabBgColorAsAWT());
		}

		Color fgColor = displayedGroup.getFgColor();
		if (fgColor != null) {
			tab.setDefaultForeground(fgColor);
		} else if (config.isTabFgColorEnabled()) {
			tab.setDefaultForeground(config.getTabFgColorAsAWT());
		}

	}
}
