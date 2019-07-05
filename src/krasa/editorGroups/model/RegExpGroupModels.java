package krasa.editorGroups.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RegExpGroupModels {
	private List<RegexGroupModel> regexGroupModels = new ArrayList<>();

	public List<RegexGroupModel> getRegexGroupModels() {
		return regexGroupModels;
	}

	public void setRegexGroupModels(List<RegexGroupModel> regexGroupModels) {
		this.regexGroupModels = regexGroupModels;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RegExpGroupModels that = (RegExpGroupModels) o;

		return regexGroupModels != null ? regexGroupModels.equals(that.regexGroupModels) : that.regexGroupModels == null;
	}

	@Override
	public int hashCode() {
		return regexGroupModels != null ? regexGroupModels.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "RegExpGroupModels{" +
			"models=" + regexGroupModels +
			'}';
	}

	public RegexGroupModel findFirstMatching(String fileName) {
		for (RegexGroupModel regexGroupModel : regexGroupModels) {
			if (regexGroupModel.matches(fileName)) {
				return regexGroupModel;
			}
		}
		return null;
	}

	public List<RegexGroupModel> findMatching(String fileName) {
		List<RegexGroupModel> result = new ArrayList<>();
		for (RegexGroupModel regexGroupModel : regexGroupModels) {
			if (regexGroupModel.matches(fileName)) {
				result.add(regexGroupModel);
			}
		}
		return result;
	}

	@Nullable
	public RegexGroupModel find(String substring) {
		RegexGroupModel deserialize = RegexGroupModel.deserialize(substring);
		if (deserialize == null) {
			return null;
		}
		for (RegexGroupModel regexGroupModel : regexGroupModels) {
			if (regexGroupModel.isEnabled() && regexGroupModel.getRegex().equals(deserialize.getRegex())) { //todo compare scope?
				return regexGroupModel;
			}
		}
		return null;
	}
}
