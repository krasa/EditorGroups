package krasa.editorGroups.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import krasa.editorGroups.icons.MyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class RegexGroup extends AutoGroup {
	public static final String ID_PREFIX = "RegexGroup: ";

	private final RegexGroupModel regexGroupModel;
	private final String folderPath;
	private final String fileName;

	public RegexGroup(RegexGroupModel regexGroupModel, String folderPath, List<Link> links, String fileName) {
		super(links);
		this.regexGroupModel = regexGroupModel.copy();
		this.folderPath = FileUtil.toSystemIndependentName(folderPath);
		this.fileName = fileName;
	}

	public RegexGroupModel getRegexGroupModel() {
		return regexGroupModel;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public boolean isValid() {
		return valid && new File(folderPath).isDirectory();
	}

	@Override
	public Icon icon() {
		return MyIcons.regex;
	}

	@Override
	public String getPresentableTitle(Project project, String presentableNameForUI, boolean showSize) {
		return "Regex: " + regexGroupModel.getRegexPattern();
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
