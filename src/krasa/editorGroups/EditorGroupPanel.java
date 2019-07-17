package krasa.editorGroups;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.BitUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import krasa.editorGroups.actions.PopupMenu;
import krasa.editorGroups.actions.RemoveFromCurrentFavoritesAction;
import krasa.editorGroups.language.EditorGroupsLanguage;
import krasa.editorGroups.model.*;
import krasa.editorGroups.support.FileResolver;
import krasa.editorGroups.support.Utils;
import krasa.editorGroups.tabs2.JBTabs;
import krasa.editorGroups.tabs2.TabInfo;
import krasa.editorGroups.tabs2.my.MyJBEditorTabs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;


public class EditorGroupPanel extends JBPanel implements Weighted, Disposable {
	public static final DataKey<FavoritesGroup> FAVORITE_GROUP = DataKey.create("krasa.FavoritesGroup");
	private static final Logger LOG = Logger.getInstance(EditorGroupPanel.class);
	private final ExecutorService myTaskExecutor;


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
	private MyJBEditorTabs tabs;
	private FileEditorManagerImpl fileEditorManager;
	public EditorGroupManager groupManager;
	private ActionToolbar toolbar;
	private boolean disposed;
	private volatile boolean brokenScroll;
	private UniqueTabNameBuilder uniqueNameBuilder;
	private Integer line;
	private boolean hideGlobally;
	private DumbService dumbService;

	public EditorGroupPanel(@NotNull FileEditor fileEditor, @NotNull Project project, @Nullable SwitchRequest switchRequest, VirtualFile file) {
		super(new BorderLayout());
		if (LOG.isDebugEnabled())
			LOG.debug("EditorGroupPanel " + "fileEditor = [" + fileEditor + "], project = [" + project + "], switchingRequest = [" + switchRequest + "], file = [" + file + "]");
		this.fileEditor = fileEditor;
		Disposer.register(fileEditor, this);
		this.project = project;
		this.file = file;
		uniqueNameBuilder = new UniqueTabNameBuilder(project);

		this.myScrollOffset = switchRequest == null ? 0 : switchRequest.myScrollOffset;
		toBeRendered = switchRequest == null ? null : switchRequest.group;
		line = switchRequest == null ? null : switchRequest.getLine();

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
		tabs = new MyJBEditorTabs(project, ActionManager.getInstance(), IdeFocusManager.findInstance(), fileEditor, file);
		Getter<ActionGroup> getter = new Getter<ActionGroup>() {
			@Override
			public ActionGroup get() {
				return (ActionGroup) CustomActionsSchema.getInstance().getCorrectedAction("EditorGroupsTabPopupMenu");
			}
		};
		tabs.setDataProvider(new DataProvider() {
			@Nullable
			@Override
			public Object getData(String dataId) {
				if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
					TabInfo targetInfo = tabs.getTargetInfo();
					if (targetInfo instanceof MyTabInfo) {
						Link path = ((MyTabInfo) targetInfo).link;
						return path.getVirtualFile();
					}
				}
				if (FAVORITE_GROUP.is(dataId)) {
					TabInfo targetInfo = tabs.getTargetInfo();
					if (targetInfo instanceof MyGroupTabInfo) {
						EditorGroup group = ((MyGroupTabInfo) targetInfo).editorGroup;
						if (group instanceof FavoritesGroup) {
							return (FavoritesGroup) group;
						}
					}
				}
				return null;
			}

		});
		tabs.addTabMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
					final TabInfo info = tabs.findInfo(e);
					if (info != null) {
						IdeEventQueue.getInstance().blockNextEvents(e);
						tabs.setMyPopupInfo(info);
						try {
							ActionManager.getInstance().getAction(RemoveFromCurrentFavoritesAction.ID).actionPerformed(AnActionEvent.createFromInputEvent(e, ActionPlaces.UNKNOWN, new Presentation(), DataManager.getInstance().getDataContext(tabs)));
						} finally {
							tabs.setMyPopupInfo(null);
						}
					}
				}

			}
		});
		tabs.setPopupGroup(getter, "EditorGroupsTabPopup", false);
		tabs.setSelectionChangeHandler(new JBTabs.SelectionChangeHandler() {

			@NotNull
			@Override
			public ActionCallback execute(TabInfo info, boolean requestFocus, ActiveRunnable doChangeSelection) {
				Integer modifiers = null;
				AWTEvent trueCurrentEvent = IdeEventQueue.getInstance().getTrueCurrentEvent();
				if (trueCurrentEvent instanceof MouseEvent) {
					modifiers = ((MouseEvent) trueCurrentEvent).getModifiersEx();
				} else if (trueCurrentEvent instanceof ActionEvent) {
					modifiers = ((ActionEvent) trueCurrentEvent).getModifiers();
				}


				if (modifiers == null) {
					return ActionCallback.DONE;
				}

				if (info instanceof MyGroupTabInfo) {
					refresh(false, ((MyGroupTabInfo) info).editorGroup);
				} else {
					MyTabInfo myTabInfo = (MyTabInfo) info;
					VirtualFile fileByPath = myTabInfo.link.getVirtualFile();
					if (fileByPath == null) {
						setEnabled(false);
						return ActionCallback.DONE;
					}

					if (modifiers == null) {
						modifiers = 0;
					}
					boolean ctrl = BitUtil.isSet(modifiers, InputEvent.CTRL_DOWN_MASK);
					boolean alt = BitUtil.isSet(modifiers, InputEvent.ALT_DOWN_MASK);
					boolean shift = BitUtil.isSet(modifiers, InputEvent.SHIFT_DOWN_MASK);
					boolean button2 = BitUtil.isSet(modifiers, InputEvent.BUTTON2_DOWN_MASK);

					openFile(myTabInfo.link, ctrl, shift, Splitters.from(alt, shift));
				}
				return ActionCallback.DONE;
			}
		});

		setPreferredSize(new Dimension(0, 26));
		JComponent component = tabs.getComponent();
		add(component, BorderLayout.CENTER);

		addMouseListener(getPopupHandler());
		tabs.addMouseListener(getPopupHandler());


		myTaskExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("krasa.editorGroups.EditorGroupPanel-" + file.getName(), 1);
		dumbService = DumbService.getInstance(this.project);
	}

	public void postConstruct() {
		ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.state();

		EditorGroup editorGroup = toBeRendered;

		//minimize flicker for the price of latency
		boolean preferLatencyOverFlicker = applicationConfiguration.isInitializeSynchronously();
		if (editorGroup == null && preferLatencyOverFlicker && !DumbService.isDumb(project)) {
			long start = System.currentTimeMillis();
			try {
				editorGroup = groupManager.getGroup(project, fileEditor, EditorGroup.EMPTY, editorGroup, false, file, !applicationConfiguration.isShowPanel());
				toBeRendered = editorGroup;
			} catch (IndexNotReady e) {
				LOG.debug(e);
			} catch (Throwable e) {
				LOG.error(e);
			}
			long delta = System.currentTimeMillis() - start;
			if (delta > 200) {
				LOG.warn("lag on editor opening - #getGroup took " + delta + " ms for " + file);
			}
		}

		if (editorGroup == null && !preferLatencyOverFlicker) {
			try {
				long start = System.currentTimeMillis();
				editorGroup = groupManager.getGroup(project, fileEditor, EditorGroup.EMPTY, editorGroup, false, file, true);
				toBeRendered = editorGroup;
				long delta = System.currentTimeMillis() - start;
				if (LOG.isDebugEnabled())
					LOG.debug("#getGroup:stub - on editor opening took " + delta + " ms for " + file + " " + editorGroup);
			} catch (IndexNotReady indexNotReady) {
				LOG.warn("Getting stub group failed" + indexNotReady);
			}
		}

		if (editorGroup == null) {
			setVisible(false);
			refresh(false, null);
		} else {
			boolean visible;
			visible = updateVisibility(editorGroup);
			getLayout().layoutContainer(this.getParent()); //  forgot what this does :( 
			render2(false);

			if (visible && editorGroup.isStub()) {
				refresh(false, null);
			}
		}
	}

	private void renderLater() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					render2(true);
				} catch (Exception e) {
					displayedGroup = EditorGroup.EMPTY;
					LOG.error(e);
				}
			}
		});
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

	private boolean reloadTabs(boolean paintNow) {
		boolean visible;
		try {
			tabs.bulkUpdate = true;

			tabs.removeAllTabs();
			currentIndex = NOT_INITIALIZED;

			List<Link> links = displayedGroup.getLinks(project);
			visible = updateVisibility(displayedGroup);

			Map<Link, String> path_name = uniqueNameBuilder.getNamesByPath(links, file);
			createTabs(links, path_name);

			addCurrentFileTab(path_name);

			if (displayedGroup instanceof GroupsHolder) {
				createGroupLinks(((GroupsHolder) displayedGroup).getGroups());
			}


//			if (tabs.getTabCount() > 0 && paintNow) { //premature optimization
//				tabs.validate();
//				RepaintManager.currentManager(tabs).paintDirtyRegions(); //less flicker 
//			}


		} finally {
			tabs.bulkUpdate = false;

			tabs.doLayout();
			tabs.scroll(myScrollOffset);
		}
		return visible;
	}

	private void createTabs(List<Link> links, Map<Link, String> path_name) {
		int start = 0;
		int end = links.size();
		int tabSizeLimitInt = ApplicationConfiguration.state().getTabSizeLimitInt();

		if (links.size() > tabSizeLimitInt) {
			int currentFilePosition = -1;

			for (int i = 0; i < links.size(); i++) {
				Link link = links.get(i);
				if (link.getVirtualFile().equals(fileFromTextEditor) && (line == null || Objects.equals(link.getLine(), line))) {
					currentFilePosition = i;
					break;
				}
			}
			if (currentFilePosition > -1) {
				start = Math.max(0, currentFilePosition - tabSizeLimitInt / 2);
				end = Math.min(links.size(), start + tabSizeLimitInt);
			}
			LOG.debug("Too many tabs, skipping: " + (links.size() - tabSizeLimitInt));
		}

		int j = 0;
		for (int i1 = start; i1 < end; i1++) {
			Link link = links.get(i1);

			MyTabInfo tab = new MyTabInfo(link, path_name.get(link));

			tabs.addTabSilently(tab, -1);
//			if (EditorGroupsLanguage.isEditorGroupsLanguage(path) && StringUtils.isNotEmpty(displayedGroup.getTitle()) && displayedGroup.isOwner(path)) {
//				tab.setText("[" + displayedGroup.getTitle() + "]");
//			}
			if (Objects.equals(link.getLine(), line) && link.fileEquals(fileFromTextEditor)) {
				tabs.setMySelectedInfo(tab);
				customizeSelectedColor(tab);
				currentIndex = j;
			}
			j++;
		}
		if (currentIndex == NOT_INITIALIZED) {
			selectTabFallback();
		}
	}


	private void addCurrentFileTab(Map<Link, String> path_name) {
		if (currentIndex < 0 && (EditorGroupsLanguage.isEditorGroupsLanguage(file))) {
			Link link = Link.from(file);
			MyTabInfo info = new MyTabInfo(link, path_name.get(link));
			customizeSelectedColor(info);
			currentIndex = 0;
			tabs.addTabSilently(info, 0);
			tabs.setMySelectedInfo(info);
		} else if (currentIndex < 0 && displayedGroup != EditorGroup.EMPTY
			&& !(displayedGroup instanceof EditorGroups)
			&& !(displayedGroup instanceof BookmarkGroup)
			&& !(displayedGroup instanceof EmptyGroup)
		) {

			if (!displayedGroup.isStub() && !FileResolver.excluded(new File(file.getPath()), ApplicationConfiguration.state().isExcludeEditorGroupsFiles())) {
				String message = "current file is not contained in group. file=" + file + ", group=" + displayedGroup + ", links=" + displayedGroup.getLinks(project);
				if (ApplicationManager.getApplication().isInternal()) {
					LOG.error(message);
				} else {
					LOG.warn(message);
				}
			} else {
				LOG.debug("current file is excluded from the group " + file + " " + displayedGroup + " " + displayedGroup.getLinks(project));
			}
		}
	}


	private void createGroupLinks(Collection<EditorGroup> groups) {
		for (EditorGroup editorGroup : groups) {
			tabs.addTab(new MyGroupTabInfo(editorGroup));
		}
	}


	class MyTabInfo extends TabInfo {
		Link link;

		public MyTabInfo(Link link, String name) {
			super(new JLabel(""));
			this.link = link;
			Integer line = link.getLine();
			if (line != null) {
				name += ":" + line;
			}
			setText(name);
			setTooltipText(link.getPath());
			setIcon(link.getFileIcon());
			if (!link.exists()) {
				setEnabled(false);
			}
		}

		public Link getLink() {
			return link;
		}
	}


	class MyGroupTabInfo extends TabInfo {
		EditorGroup editorGroup;

		public MyGroupTabInfo(EditorGroup editorGroup) {
			super(new JLabel(""));
			setVisible(false);
			this.editorGroup = editorGroup;
			String title = editorGroup.tabTitle(EditorGroupPanel.this.project);
			setText("[" + title + "]");
			setToolTipText(editorGroup.getTabGroupTooltipText(EditorGroupPanel.this.project));
			setIcon(editorGroup.icon());
		}
	}

	public boolean previous(boolean newTab, boolean newWindow, Splitters split) {
		if (currentIndex == NOT_INITIALIZED) { //group was not refreshed
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - currentIndex == -1");
			return false;
		}
		if (displayedGroup.isInvalid()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - displayedGroup.isInvalid");
			return false;
		}
		if (!isVisible()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - !isVisible()");
			return false;
		}

		int iterations = 0;
		List<TabInfo> tabs = this.tabs.getTabs();
		Link link = null;

		while (link == null && iterations < tabs.size()) {
			iterations++;

			int index = currentIndex - iterations;

			if (!ApplicationConfiguration.state().isContinuousScrolling() && currentIndex - iterations < 0) {
				return newTab;
			}

			if (index < 0) {
				index = tabs.size() - Math.abs(index);
			}
			link = getLink(tabs, index);
			if (LOG.isDebugEnabled()) {
				LOG.debug("previous: index=" + index + ", link=" + link);
			}
		}
		;

		return openFile(link, newTab, newWindow, split);
	}

	private Link getLink(List<TabInfo> tabs, int index) {
		TabInfo tabInfo = tabs.get(index);
		if (tabInfo instanceof MyTabInfo) {
			return ((MyTabInfo) tabInfo).link;
		}
		return null;
	}

	public boolean next(boolean newTab, boolean newWindow, Splitters split) {
		if (currentIndex == NOT_INITIALIZED) { //group was not refreshed
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - currentIndex == -1");
			return false;
		}
		if (displayedGroup.isInvalid()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - displayedGroup.isInvalid");
			return false;
		}
		if (!isVisible()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - !isVisible()");
			return false;
		}
		int iterations = 0;
		List<TabInfo> tabs = this.tabs.getTabs();
		Link link = null;

		while (link == null && iterations < tabs.size()) {
			iterations++;

			if (!ApplicationConfiguration.state().isContinuousScrolling() && currentIndex + iterations >= tabs.size()) {
				return false;
			}

			int index = (currentIndex + iterations) % tabs.size();
			link = getLink(tabs, index);
			if (LOG.isDebugEnabled()) {
				LOG.debug("next: index=" + index + ", link=" + link);
			}

		}

		return openFile(link, newTab, newWindow, split);
	}

	private boolean openFile(@Nullable Link link, boolean newTab, boolean newWindow, Splitters split) {
		if (disposed) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - already disposed");
			return false;
		}

		if (link == null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - link is null");
			return false;
		}

		if (link.getVirtualFile() == null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - file is null for " + link);
			return false;
		}

		if (file.equals(link.getVirtualFile()) && !newWindow && !split.isSplit() && link.getLine() == null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - same file");
			return false;
		}


		if (groupManager.isSwitching()) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - switching ");
			return false;
		}
		if (toBeRendered != null) {
			if (LOG.isDebugEnabled()) LOG.debug("openFile fail - toBeRendered != null");
			return false;
		}

		EditorGroupManager.Result result = groupManager.open(this, link.getVirtualFile(), link.getLine(), newWindow, newTab, split);


		if (result != null && result.isScrolledOnly()) {
			selectTab(link);
		}
		return true;
	}

	private void selectTabFallback() {
		List<TabInfo> tabs1 = tabs.getTabs();
		for (int i = 0; i < tabs1.size(); i++) {
			TabInfo t = tabs1.get(i);
			if (t instanceof MyTabInfo) {
				MyTabInfo tab = (MyTabInfo) t;
				if (tab.link.fileEquals(fileFromTextEditor)) {
					tabs.setMySelectedInfo(tab);
					customizeSelectedColor(tab);
					currentIndex = i;
				}
			}
		}
	}

	private void selectTab(@NotNull Link link) {
		List<TabInfo> tabs = this.tabs.getTabs();
		for (int i = 0; i < tabs.size(); i++) {
			TabInfo tab = tabs.get(i);
			if (tab instanceof MyTabInfo) {
				Link link1 = ((MyTabInfo) tab).getLink();
				if (link1.equals(link)) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("selectTab selecting " + link);
					}
					this.tabs.setMySelectedInfo(tab);
					this.tabs.repaint();
					currentIndex = i;
					break;
				}
			}
		}
	}

	public MyJBEditorTabs getTabs() {
		return tabs;
	}

	@Override
	public double getWeight() {
		return Integer.MIN_VALUE;
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

	private void focusGained() {
		refresh(false, null);
		groupManager.enableSwitching();
	}

	public void refreshOnSelectionChanged(boolean refresh, EditorGroup switchingGroup, int scrollOffset) {
		if (LOG.isDebugEnabled()) LOG.debug("refreshOnSelectionChanged");
		myScrollOffset = scrollOffset;
		if (switchingGroup == displayedGroup) {
			tabs.scroll(myScrollOffset);
		}
		refresh(refresh, switchingGroup);
		groupManager.enableSwitching();
	}

	private void refresh2() {
		try {
			myTaskExecutor.submit(new Runnable() {
				@Override
				public void run() {
					if (disposed) {
						return;
					}
					boolean selected = isSelected();
					if (LOG.isDebugEnabled()) {
						LOG.debug("refresh2 selected=" + selected + " for " + file.getName());
					}
					if (selected) {
						refresh3();
					}

				}

			});
		} catch (RejectedExecutionException e) {
			LOG.debug(e);
		}
	}

	private void refresh3() {
		long start = System.currentTimeMillis();
		if (disposed) {
			return;
		}
		if (SwingUtilities.isEventDispatchThread()) {
			LOG.error("do not execute it on EDT");
		}

		try {
			Ref<EditorGroup> editorGroupRef = new Ref<>();

			RefreshRequest request = getGroupInReadActionWithRetries(editorGroupRef);
			if (request == null) return;

			EditorGroup group = editorGroupRef.get();

			if (LOG.isDebugEnabled()) {
				LOG.debug("refresh3 before if: brokenScroll =" + brokenScroll + ", request =" + request + ", group =" + group + ", displayedGroup =" + displayedGroup + ", toBeRendered =" + toBeRendered);
			}
			boolean skipRefresh = !brokenScroll && !request.refresh && (group == displayedGroup || group == toBeRendered || group.equalsVisually(project, displayedGroup));
			//noinspection DoubleNegation
			boolean updateVisibility = hideGlobally != !ApplicationConfiguration.state().isShowPanel();
			if (updateVisibility) {
				skipRefresh = false;
			}
			if (skipRefresh) {
				if (!(fileEditor instanceof TextEditorImpl)) {
					groupManager.enableSwitching(); //need for UI forms - when switching to open editors , focus listener does not do that
				} else {
					//switched by bookmark shortcut -> need to select the right tab
					Editor editor = ((TextEditorImpl) fileEditor).getEditor();
					int line = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
					selectTab(new VirtualFileLink(file, null, line));
				}


				if (LOG.isDebugEnabled())
					LOG.debug("no change, skipping refresh, toBeRendered=" + toBeRendered + ". Took " + (System.currentTimeMillis() - start) + "ms ");
				return;
			}
			toBeRendered = group;
			if (request.refresh) {
				myScrollOffset = tabs.getMyScrollOffset();   //this will have edge cases
			}


			SwingUtilities.invokeLater(() -> {
				if (disposed) {
					return;
				}
				EditorGroup rendering = toBeRendered;
				//tabs do not like being updated while not visible first - it really messes up scrolling
				if (!isVisible() && rendering != null && updateVisibility(rendering)) {
					SwingUtilities.invokeLater(() -> render());
				} else {
					render();
				}
			});

			atomicReference.compareAndSet(request, null);
			if (LOG.isDebugEnabled())
				LOG.debug("<refreshSmart in " + (System.currentTimeMillis() - start) + "ms " + file.getName());
		} catch (Throwable e) {
			LOG.error(file.getName(), e);
		}
	}

	@Nullable
	private RefreshRequest getGroupInReadActionWithRetries(Ref<EditorGroup> editorGroupRef) {
		RefreshRequest request = null;
		boolean success = false;
		while (!success) {
			RefreshRequest tempRequest = atomicReference.getAndSet(null);
			if (tempRequest != null) {
				request = tempRequest;
			}

			if (request == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("getGroupInReadActionWithRetries - nothing to refresh " + fileEditor.getName());
				return null;
			}
			if (LOG.isDebugEnabled()) LOG.debug("getGroupInReadActionWithRetries - " + request);

			ProgressIndicatorUtils.yieldToPendingWriteActions();
			EditorGroup lastGroup = getLastGroup();
			if (needSmartMode(request, lastGroup)) {
				LOG.debug("waiting on smart mode");
				dumbService.waitForSmartMode();
			}

			EditorGroup requestedGroup = request.requestedGroup;
			boolean refresh = request.refresh;

			success = ProgressManager.getInstance().runInReadActionWithWriteActionPriority(new Runnable() {
				@Override
				public void run() {
					try {
						EditorGroup group = groupManager.getGroup(project, fileEditor, lastGroup, requestedGroup, refresh, file, !ApplicationConfiguration.state().isShowPanel());
						editorGroupRef.set(group);
					} catch (IndexNotReady e) {
						if (LOG.isDebugEnabled()) LOG.debug("getGroupInReadActionWithRetries - " + e.toString());
						throw new ProcessCanceledException(e);
					}
				}
			}, null);
		}
		return request;
	}

	private boolean needSmartMode(@Nullable RefreshRequest request, EditorGroup lastGroup) {
		EditorGroup requestedGroup = null;
		boolean refresh = false;
		if (request != null) {
			requestedGroup = request.requestedGroup;
			refresh = request.refresh;
		}

		return (requestedGroup != null && EditorGroup.exists(requestedGroup) && requestedGroup.needSmartMode()) || (requestedGroup == null && EditorGroup.exists(lastGroup) && lastGroup.needSmartMode()) || requestedGroup == null;
	}

	private EditorGroup getLastGroup() {
		EditorGroup lastGroup = toBeRendered == null ? displayedGroup : toBeRendered;
		lastGroup = lastGroup == null ? EditorGroup.EMPTY : lastGroup;
		return lastGroup;
	}


	private void render() {
		try {
			render2(true);
		} catch (Exception e) {
			LOG.error(file.getName(), e);
		}
	}

	private void render2(boolean paintNow) {
		if (disposed) {
			return;
		}
		EditorGroup rendering = toBeRendered;
		if (rendering == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("skipping render toBeRendered=" + rendering + " file=" + file.getName());
			return;
		}


		brokenScroll = !isSelected();
		if (brokenScroll && LOG.isDebugEnabled()) {
			LOG.warn("rendering editor that is not selected, scrolling might break: " + file.getName());
		}

		displayedGroup = rendering;
		toBeRendered = null;

		long start = System.currentTimeMillis();

		reloadTabs(paintNow);

		fileEditor.putUserData(EDITOR_GROUP, displayedGroup); // for titles
		file.putUserData(EDITOR_GROUP, displayedGroup); // for project view colors
		fileEditorManager.updateFilePresentation(file);
		toolbar.updateActionsImmediately();

		groupManager.enableSwitching();
		if (LOG.isDebugEnabled())
			LOG.debug("<refreshOnEDT " + (System.currentTimeMillis() - start) + "ms " + fileEditor.getName() + ", displayedGroup=" + displayedGroup);
	}


	private boolean updateVisibility(@NotNull EditorGroup rendering) {
		boolean visible;
		ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.state();
		hideGlobally = !applicationConfiguration.isShowPanel();
		if (!applicationConfiguration.isShowPanel() || rendering instanceof EmptyGroup || rendering == EditorGroup.EMPTY) {
			visible = false;
		} else if (applicationConfiguration.isHideEmpty() && !rendering.isStub()) {
			boolean hide = rendering instanceof AutoGroup && ((AutoGroup) rendering).isEmpty();
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
		myTaskExecutor.shutdownNow();
		tabs.dispose();
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

	private boolean isSelected() {
		boolean selected = false;
		for (FileEditor selectedEditor : fileEditorManager.getSelectedEditors()) {
			if (selectedEditor == fileEditor) {
				selected = true;
				break;
			}
		}
		return selected;
	}

}
