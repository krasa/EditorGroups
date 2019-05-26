package krasa.editorGroups;

import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.ide.favoritesTreeView.FavoritesListener;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.indexing.FileBasedIndex;
import krasa.editorGroups.index.EditorGroupIndex;
import krasa.editorGroups.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class PanelRefresher {
	private static final Logger LOG = Logger.getInstance(PanelRefresher.class);

	private final Project project;
	private AtomicBoolean cacheReady = new AtomicBoolean();
	private final ExecutorService ourThreadExecutorsService;
	private IndexCache cache;
	private FavoritesListener favoritesListener;

	public PanelRefresher(Project project) {
		this.project = project;
		cache = IndexCache.getInstance(project);
		ourThreadExecutorsService = AppExecutorUtil.createBoundedApplicationPoolExecutor("EditorGroups-" + project.getName(), 1);
		project.getMessageBus().connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
			@Override
			public void enteredDumbMode() {
			}

			@Override
			public void exitDumbMode() {
				onSmartMode();
			}
		});
		addFavouritesListener();
		addBookmarksListener();
	}

	private void addBookmarksListener() {
		project.getMessageBus().connect(project).subscribe(BookmarksListener.TOPIC, new BookmarksListener() {
			@Override
			public void bookmarkAdded(@NotNull Bookmark b) {
				refresh();
			}

			@Override
			public void bookmarkRemoved(@NotNull Bookmark b) {
				refresh();
			}

			@Override
			public void bookmarkChanged(@NotNull Bookmark b) {
				refresh();
			}

			@Override
			public void bookmarksOrderChanged() {
				refresh();
			}

			private void refresh() {
				iteratePanels((panel, displayedGroup) -> {
					if (displayedGroup instanceof BookmarkGroup) {
						LOG.debug("BookmarksListener refreshing " + panel.getFile().getName());
						panel.refresh(true, displayedGroup);
					}
				});
			}
		});
	}

	private void addFavouritesListener() {
		if (favoritesListener == null) {
			favoritesListener = new FavoritesListener() {
				@Override
				public void rootsChanged() {
					if (LOG.isDebugEnabled()) {
						LOG.debug("FavoritesListener rootsChanged");
					}
					iteratePanels((panel, displayedGroup) -> {
						if (displayedGroup instanceof FavoritesGroup) {
							LOG.debug("FavoritesListener refreshing " + panel.getFile().getName());
							panel.refresh(true, displayedGroup);
						}
					});
				}

				@Override
				public void listAdded(String listName) {

				}

				@Override
				public void listRemoved(String listName) {

				}
			};
			FavoritesManager.getInstance(project).addFavoritesListener(favoritesListener, project);
		}
	}

	private void iteratePanels(BiConsumer<EditorGroupPanel, EditorGroup> biConsumer) {
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		for (FileEditor selectedEditor : manager.getAllEditors()) {
			EditorGroupPanel panel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				EditorGroup displayedGroup = panel.getDisplayedGroup();
				biConsumer.accept(panel, displayedGroup);
			}
		}
	}

	public static PanelRefresher getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, PanelRefresher.class);
	}

	void onSmartMode() {
		if (!cacheReady.get()) {
			return;
		}
		ApplicationManager.getApplication().invokeLater(new Runnable() {
			@Override
			public void run() {
				if (project.isDisposed()) {
					return;
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug(">onSmartMode");
				}

				long start = System.currentTimeMillis();
				final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);

				for (FileEditor selectedEditor : manager.getSelectedEditors()) {   //refreshing not selected one fucks up tabs scrolling

					EditorGroupPanel panel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
					if (panel != null) {
						EditorGroup displayedGroup = panel.getDisplayedGroup();
						if (displayedGroup instanceof FolderGroup) {
							continue;
						}

						if (LOG.isDebugEnabled()) {
							LOG.debug("onSmartMode: refreshing panel for " + panel.getFile());
						}

						panel.refresh(false, null);
					}
				}
				if (LOG.isDebugEnabled())
					LOG.debug("onSmartMode " + (System.currentTimeMillis() - start) + "ms " + Thread.currentThread().getName());
			}
		});

	}

	public void refresh(String owner) {
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		for (FileEditor selectedEditor : manager.getAllEditors()) {
			EditorGroupPanel panel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				if (panel.getDisplayedGroup().isOwner(owner)) {
					panel.refresh(false, null);

				}
			}
		}

	}

	public EditorGroupIndexValue onIndexingDone(@NotNull String ownerPath, @NotNull EditorGroupIndexValue group) {
		group = cache.onIndexingDone(ownerPath, group);
		if (DumbService.isDumb(project)) { //optimization
			return group;
		}

		long start = System.currentTimeMillis();
		final FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManagerEx.getInstance(project);
		for (FileEditor selectedEditor : manager.getAllEditors()) {
			EditorGroupPanel panel = selectedEditor.getUserData(EditorGroupPanel.EDITOR_PANEL);
			if (panel != null) {
				panel.onIndexingDone(ownerPath, group);
			}
		}

		if (LOG.isDebugEnabled())
			LOG.debug("onIndexingDone " + ownerPath + " - " + (System.currentTimeMillis() - start) + "ms " + Thread.currentThread().getName());
		return group;
	}


	public void initCache() {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			DumbService.getInstance(project).waitForSmartMode();
			ApplicationManager.getApplication().runReadAction(
				() -> {
					if (project.isDisposed()) {
						return;
					}
					long start = System.currentTimeMillis();
					FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
					IndexCache cache = IndexCache.getInstance(project);
					try {
						fileBasedIndex.processAllKeys(EditorGroupIndex.NAME, new Processor<String>() {
							@Override
							public boolean process(String s) {
								List<EditorGroupIndexValue> values = fileBasedIndex.getValues(EditorGroupIndex.NAME, s, GlobalSearchScope.allScope(project));
								for (EditorGroupIndexValue value : values) {
									cache.initGroup(value);
								}
								return true;
							}
						}, project);
					} catch (IndexNotReadyException e) {
						if (LOG.isDebugEnabled())
							LOG.debug("initCache failed on IndexNotReadyException, will be executed again");
						initCache();
						return;
					}
					cacheReady();
					if (LOG.isDebugEnabled()) LOG.debug("initCache " + (System.currentTimeMillis() - start));
				}
			);
		});
	}

	public void cacheReady() {
		cacheReady.set(true);
		onSmartMode();
	}

	public void refreshOnBackground(Runnable task) {
		ourThreadExecutorsService.submit(task);
	}


}
