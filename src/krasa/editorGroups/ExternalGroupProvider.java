package krasa.editorGroups;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Pair;
import com.intellij.util.TreeItem;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.FavoritesGroup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExternalGroupProvider {
	private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ExternalGroupProvider.class);

	private FavoritesManager favoritesManager;
	private final Project project;
	private final ProjectFileIndex fileIndex;

	public static ExternalGroupProvider getInstance(@NotNull Project project) {
		return ServiceManager.getService(project, ExternalGroupProvider.class);
	}

	public ExternalGroupProvider(FavoritesManager favoritesManager, Project project, ProjectFileIndex fileIndex) {
		this.favoritesManager = favoritesManager;
		this.project = project;
		this.fileIndex = fileIndex;
	}

	public Collection<FavoritesGroup> getFavoritesGroups() {
		List<String> availableFavoritesListNames = favoritesManager.getAvailableFavoritesListNames();

		ArrayList<FavoritesGroup> favoritesGroups = new ArrayList<>();
		for (String name : availableFavoritesListNames) {
			List<TreeItem<Pair<AbstractUrl, String>>> favoritesListRootUrls = favoritesManager.getFavoritesListRootUrls(name);
			if (favoritesListRootUrls.isEmpty()) {
				continue;

			}
			FavoritesGroup e = new FavoritesGroup(name, favoritesListRootUrls, project, fileIndex);
			if (e.size(project) > 0) {
				favoritesGroups.add(e);
			}
		}

		return favoritesGroups;
	}

	public EditorGroup getFavoritesGroup(String title) {
		List<TreeItem<Pair<AbstractUrl, String>>> favoritesListRootUrls = favoritesManager.getFavoritesListRootUrls(title);
		if (favoritesListRootUrls.isEmpty()) {
			return EditorGroup.EMPTY;
		}

		return new FavoritesGroup(title, favoritesListRootUrls, project, fileIndex);
	}

	public List<EditorGroup> findGroups(String currentFilePath) {
		List<EditorGroup> favoritesGroups = new ArrayList<>();
		long start = System.currentTimeMillis();

		for (FavoritesGroup group : getFavoritesGroups()) {
			if (group.containsLink(project, currentFilePath)) {
				favoritesGroups.add(group);
			}
		}


		if (LOG.isDebugEnabled()) {
			LOG.debug("findGroups " + (System.currentTimeMillis() - start) + "ms");
		}


		return favoritesGroups;

	}
}
