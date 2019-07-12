package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class RegexGroup extends AutoGroup {
	public static final String ID_PREFIX = "RegexGroup: ";
	@NotNull
	private final RegexGroupModel regexGroupModel;
	@Nullable
	private final String folderPath;
	@Nullable
	private final String fileName;

	public RegexGroup(RegexGroupModel regexGroupModel, @Nullable String folderPath, List<Link> links, @Nullable String fileName) {
		super(links);
		this.regexGroupModel = regexGroupModel.copy();
		this.folderPath = folderPath != null ? FileUtil.toSystemIndependentName(folderPath) : null;
		if (folderPath != null && !new File(folderPath).isDirectory()) {
			throw new IllegalArgumentException("not a folder: " + folderPath);
		}
		this.fileName = fileName;
	}

	public RegexGroup(RegexGroupModel model) {
		this(model, null, Collections.emptyList(), null);
		setStub(true);
	}

	public RegexGroup(RegexGroupModel model, String path, String fileName) {
		this(model, path, Collections.emptyList(), fileName);
		setStub(true);
	}

	@NotNull
	public RegexGroupModel getRegexGroupModel() {
		return regexGroupModel;
	}

	@Nullable
	public String getFolderPath() {
		return folderPath;
	}

	@Nullable
	public String getFileName() {
		return fileName;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public Icon icon() {
		return MyIcons.regex;
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		return "Regex: " + regexGroupModel.getRegexPattern();
	}

	@Override
	public boolean isSelected(EditorGroup groupLink) {
		if (groupLink instanceof RegexGroup) {
			return this.regexGroupModel.equals(((RegexGroup) groupLink).getRegexGroupModel());
		} else {
			return super.isSelected(groupLink);
		}
	}

	@NotNull
	@Override
	public String getId() {
		return ID_PREFIX + regexGroupModel.serialize();
	}

	@Override
	public String getTitle() {
		return getId();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		RegexGroup that = (RegexGroup) o;

		if (regexGroupModel != null ? !regexGroupModel.equals(that.regexGroupModel) : that.regexGroupModel != null)
			return false;
		return folderPath != null ? folderPath.equals(that.folderPath) : that.folderPath == null;
	}

	@Override
	public int hashCode() {
		int result = regexGroupModel != null ? regexGroupModel.hashCode() : 0;
		result = 31 * result + (folderPath != null ? folderPath.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RegexGroup{" +
			"model='" + regexGroupModel + '\'' +
			", fileName='" + fileName + '\'' +
			", folderPath='" + folderPath + '\'' +
			", links=" + links.size() +
			'}';
	}

}
