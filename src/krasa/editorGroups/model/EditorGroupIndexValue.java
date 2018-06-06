package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import krasa.editorGroups.IndexCache;
import krasa.editorGroups.support.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorGroupIndexValue extends EditorGroup {

	/*definitions*/
	private String id = "";
	private String title = "";
	private String color = "";
	private List<String> relatedPaths = new ArrayList<>();

	/*runtime data*/
	private transient volatile List<String> links;
	private transient volatile boolean valid = true;
	private transient volatile Color colorInstance = null;

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

	public void setId(String ownerPath, int index) {
		this.id = ownerPath + ";" + index;
	}

	public void setId(String id) {
		this.id = StringUtil.notNullize(id);
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

	public EditorGroupIndexValue setColor(String value) {
		color = StringUtil.notNullize(value).toLowerCase();
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

	public String getColorString() {
		return color;
	}


	@Override
	public Color getColor() {
		if (colorInstance == null) {
			if (!color.isEmpty()) {
				try {
					if (color.startsWith("0x") || color.startsWith("#")) {
						colorInstance = Color.decode(color);
					} else {
						String colorName = color;
						char[] modifier = new char[0];
						int lighterIndex = color.indexOf("-");
						if (lighterIndex > 0) {
							colorName = color.substring(0, lighterIndex);
							modifier = color.substring(lighterIndex).toCharArray();
						}

						int darkerIndex = color.indexOf("+");
						if (darkerIndex > 0) {
							colorName = color.substring(0, darkerIndex);
							modifier = color.substring(darkerIndex).toCharArray();
						}

						Color myColor = Utils.colorMap.get(colorName);
						String number = "";

						for (int i = modifier.length - 1; i >= 0; i--) {
							char c = modifier[i];
							if (Character.isDigit(c)) {
								number = number + c;
							} else if (c == '+') {
								int tones = 1;
								if (!number.isEmpty()) {
									tones = Integer.parseInt(number);
									number = "";
								}
								myColor = ColorUtil.brighter(myColor, tones);
							} else if (c == '-') {
								int tones = 1;
								if (!number.isEmpty()) {
									tones = Integer.parseInt(number);
									number = "";
								}
								myColor = ColorUtil.darker(myColor, tones);
							}
						}

						colorInstance = myColor;
					}
				} catch (Exception e) {
				}
			}
		}
		return colorInstance;
	}

	@Override
	public String getOwnerPath() {
		String ownerPath = super.getOwnerPath();
		return StringUtils.substringBefore(ownerPath, ";");
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
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (color != null ? !color.equals(that.color) : that.color != null) return false;
		return relatedPaths != null ? relatedPaths.equals(that.relatedPaths) : that.relatedPaths == null;
	}

	/**
	 * FOR INDEX STORE
	 */
	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		result = 31 * result + (relatedPaths != null ? relatedPaths.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "EditorGroupIndexValue{" +
			"title='" + title + '\'' +
			"id='" + id + '\'' +
			"related='" + relatedPaths + '\'' +
			'}';
	}


}
