package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import krasa.editorGroups.IndexCache;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorGroupIndexValue extends EditorGroup {

	/*definitions*/
	private String id = "";
	private String root = "";
	private String title = "";
	private String backgroundColor = "";
	private String foregroundColor = "";
	private List<String> relatedPaths = new ArrayList<>();

	/*runtime data*/
	private transient volatile List<String> links;
	private transient volatile boolean valid = true;
	private transient volatile Color bgColorInstance = null;
	private transient volatile Color fgColorInstance = null;

	public EditorGroupIndexValue() {
	}

	public EditorGroupIndexValue(String id, String title, boolean valid) {
		this.id = id;
		this.title = title;
		this.valid = valid;
	}

	public EditorGroupIndexValue setTitle(String title) {
		this.title = StringUtil.notNullize(title);
		return this;
	}

	@NotNull
	public String getId() {
		return id;
	}


	public EditorGroupIndexValue setId(String id) {
		this.id = StringUtil.notNullize(id);
		return this;
	}

	public String getRoot() {
		return root;
	}

	public EditorGroupIndexValue setRoot(String root) {
		this.root = root;
		return this;
	}

	public List<String> getRelatedPaths() {
		return relatedPaths;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	public void invalidate() {
		this.valid = false;
	}

	@Override
	public int size(Project project) {
		return getLinks(project).size();
	}


	@Override
	@NotNull
	public List<String> getLinks(Project project) {
		if (links == null) {
			IndexCache.getInstance(project).initGroup(this);
		}

		return links;
	}

	public EditorGroupIndexValue setBackgroundColor(String value) {
		backgroundColor = StringUtil.notNullize(value).toLowerCase();
		return this;
	}

	public EditorGroupIndexValue setForegroundColor(String value) {
		this.foregroundColor = StringUtil.notNullize(value).toLowerCase();
		return this;
	}


	public EditorGroupIndexValue addRelated(String value) {
		relatedPaths.add(value);
		return this;
	}

	@Override
	public boolean isOwner(@NotNull String canonicalPath) {
		return id.equals(canonicalPath);
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}


	@Override
	public Color getBgColor() {
		if (bgColorInstance == null) {
			if (!backgroundColor.isEmpty()) {
				try {
					if (backgroundColor.startsWith("0x") || backgroundColor.startsWith("#")) {
						bgColorInstance = Color.decode(backgroundColor);
					} else {
						bgColorInstance = Utils.getColorInstance(backgroundColor);
					}
				} catch (Exception e) {
				}
			}
		}
		return bgColorInstance;
	}

	@Override
	public Color getFgColor() {
		if (fgColorInstance == null) {
			if (!foregroundColor.isEmpty()) {
				try {
					if (foregroundColor.startsWith("0x") || foregroundColor.startsWith("#")) {
						fgColorInstance = Color.decode(foregroundColor);
					} else {
						fgColorInstance = Utils.getColorInstance(foregroundColor);
					}
				} catch (Exception e) {
				}
			}
		}
		return fgColorInstance;
	}


	@Override
	public String getRootPath() {
		return root;
	}

	public String getForegroundColor() {
		return foregroundColor;
	}


	public EditorGroupIndexValue setLinks(List<String> links) {
		this.links = links;
		return this;
	}

	/**
	 * FOR INDEX STORE
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EditorGroupIndexValue that = (EditorGroupIndexValue) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (root != null ? !root.equals(that.root) : that.root != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (backgroundColor != null ? !backgroundColor.equals(that.backgroundColor) : that.backgroundColor != null)
			return false;
		if (foregroundColor != null ? !foregroundColor.equals(that.foregroundColor) : that.foregroundColor != null)
			return false;
		return relatedPaths != null ? relatedPaths.equals(that.relatedPaths) : that.relatedPaths == null;
	}

	/**
	 * FOR INDEX STORE
	 */
	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (root != null ? root.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (backgroundColor != null ? backgroundColor.hashCode() : 0);
		result = 31 * result + (foregroundColor != null ? foregroundColor.hashCode() : 0);
		result = 31 * result + (relatedPaths != null ? relatedPaths.hashCode() : 0);
		return result;
	}


	@Override
	public String toString() {
		return "EditorGroupIndexValue{" +
			"id='" + id + '\'' +
			", root='" + root + '\'' +
			", title='" + title + '\'' +
			", backgroundColor='" + backgroundColor + '\'' +
			", foregroundColor='" + foregroundColor + '\'' +
			", relatedPaths=" + relatedPaths +
			", valid=" + valid +
			'}';
	}
}
