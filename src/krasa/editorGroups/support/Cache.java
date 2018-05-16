package krasa.editorGroups.support;

import com.google.common.cache.CacheBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import krasa.editorGroups.model.EditorGroup;
import krasa.editorGroups.model.EditorGroupImpl;
import krasa.editorGroups.model.EditorGroups;

public class Cache {
	private com.google.common.cache.Cache<String, EditorGroups> cache = CacheBuilder.newBuilder().maximumSize(10000).build();
	private com.google.common.cache.Cache<String, EditorGroup> cacheByOwner = CacheBuilder.newBuilder().maximumSize(10000).build();

	public EditorGroups getGroups(String canonicalPath) {
		return cache.getIfPresent(canonicalPath);
	}

	public EditorGroup getByOwner(String canonicalPath) {
		EditorGroup result = cacheByOwner.getIfPresent(canonicalPath);
		if (result == null) {
			result = EditorGroupImpl.EMPTY;
		}
		return result;
	}

	public void put(String ownerPath, EditorGroup editorGroup) {
		cacheByOwner.put(ownerPath, editorGroup);
	}

	public void put(String ownerPath, EditorGroups editorGroups) {
		cache.put(ownerPath, editorGroups);
	}

	public void invalidateOwner(String canonicalPath) {
		cacheByOwner.invalidate(canonicalPath);
	}

	public EditorGroup findGroupAsSlave(String currentFilePath) {
		EditorGroups ifPresent = cache.getIfPresent(currentFilePath);
		if (ifPresent != null) {
			return ifPresent.getLastOne();
		}
		return EditorGroup.EMPTY;
	}

	public void evict(String canonicalPath) {
		EditorGroup ownerGroup = getByOwner(canonicalPath);
		if (ownerGroup != null) {
			invalidateOwner(canonicalPath);
			for (String path : ownerGroup.getPaths()) {
				VirtualFile fileByPath = Utils.getFileByPath(path, ownerGroup);
				if (fileByPath != null) {
					EditorGroups ifPresent = getGroups(fileByPath.getCanonicalPath());
					if (ifPresent != null) {
						ifPresent.evict(ownerGroup);
					}
				}
			}
		}
	}

	public void updateCaches(EditorGroup editorGroup, String canonicalPath) {
		evict(canonicalPath);

		if (editorGroup.invalid()) {
			return;
		}

		for (String s : editorGroup.getPaths()) {
			VirtualFile fileByPath = Utils.getFileByPath(s);
			if (fileByPath != null) {
				putToMultiCache(editorGroup, fileByPath.getCanonicalPath());
			} else {
				Notifications.notifyMissingFile(editorGroup, s);
			}
		}
		putToMultiCache(editorGroup, canonicalPath);
		put(editorGroup.getOwnerPath(), editorGroup);
	}

	private void putToMultiCache(EditorGroup editorGroup, String url) {
		EditorGroups editorGroups = getGroups(url);
		if (editorGroups != null) {
			editorGroups.put(editorGroup);
		} else {
			cache.put(url, new EditorGroups(editorGroup));
		}
	}
}
