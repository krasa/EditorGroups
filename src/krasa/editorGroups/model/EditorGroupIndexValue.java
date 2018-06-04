package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import krasa.editorGroups.IndexCache;
import krasa.editorGroups.support.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorGroupIndexValue implements EditorGroup {

	/*definitions*/
	private String ownerPath = "";
	private String title = "";
	private String color = "";
	private List<String> relatedPaths = new ArrayList<>();

	/*runtime data*/
	private transient volatile List<String> links;
	private transient volatile boolean valid = true;
	private transient volatile Color colorInstance = null;

	public EditorGroupIndexValue() {
	}

	public EditorGroupIndexValue(String ownerPath, String title, boolean valid) {
		this.ownerPath = ownerPath;
		this.title = title;
		this.valid = valid;
	}

	public EditorGroupIndexValue setTitle(String title) {
		this.title = StringUtil.notNullize(title);
		return this;
	}

	@NotNull
	public String getOwnerPath() {
		return ownerPath;
	}

	public void setOwnerPath(String ownerPath) {
		this.ownerPath = StringUtil.notNullize(ownerPath);
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

	public VirtualFile getOwnerVirtualFile() {
		if (ownerPath == null) {
			return null;
		}
		return Utils.getFileByPath(ownerPath);
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
		return ownerPath.equals(canonicalPath);
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

	public EditorGroupIndexValue setLinks(List<String> links) {
		this.links = links;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EditorGroupIndexValue that = (EditorGroupIndexValue) o;

		if (ownerPath != null ? !ownerPath.equals(that.ownerPath) : that.ownerPath != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (color != null ? !color.equals(that.color) : that.color != null) return false;
		return relatedPaths != null ? relatedPaths.equals(that.relatedPaths) : that.relatedPaths == null;
	}

	@Override
	public int hashCode() {
		int result = ownerPath != null ? ownerPath.hashCode() : 0;
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		result = 31 * result + (relatedPaths != null ? relatedPaths.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "EditorGroupIndexValue{" +
			"title='" + title + '\'' +
			"owner='" + ownerPath + '\'' +
			"related='" + relatedPaths + '\'' +
			'}';
	}


}
