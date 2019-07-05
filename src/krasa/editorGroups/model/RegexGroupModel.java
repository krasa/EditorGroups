package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class RegexGroupModel {
	private static final Logger LOG = Logger.getInstance(RegexGroupModel.class);

	private String regex;
	private Scope scope = Scope.CURRENT_FOLDER;
	private boolean enabled = true;
	@Transient
	private transient Pattern regexPattern;

	public RegexGroupModel() {
	}

	public RegexGroupModel(String regex, Scope scope) {
		this.regex = regex;
		this.scope = scope;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	public String getRegex() {
		return regex;
	}

	@Transient
	@NotNull
	public Pattern getRegexPattern() {
		if (regexPattern == null) {
			regexPattern = Pattern.compile(regex);
		}
		return regexPattern;
	}


	public Scope getScope() {
		return scope;
	}

	public void setRegex(String regex) {
		regexPattern = null;
		this.regex = regex;
	}

	public void setScope(Scope scope) {
		if (scope == null) {
			scope = Scope.CURRENT_FOLDER;
		}
		this.scope = scope;
	}

	@Override
	public String toString() {
		return "Model{" +
			"regex='" + regex + '\'' +
			", scope=" + scope +
			", enabled=" + enabled +
			'}';
	}

	public String serialize() {
		return "v0|" + scope + "|" + regex;
	}

	public static RegexGroupModel deserialize(String s) {
		try {
			if (s.startsWith("v0")) {
				int i = s.indexOf("|", 3);
				String scope = s.substring(3, i);
				String regex = s.substring(i + 1);
				return new RegexGroupModel(regex, Scope.valueOf(scope));
			} else {
				throw new RuntimeException("not supported");
			}
		} catch (Throwable e) {
			LOG.warn(e + "; source='" + s + "'");
			return null;
		}
	}

	public boolean matches(String name) {
		try {
			return getRegexPattern().matcher(name).matches();
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}

	public RegexGroupModel copy() {
		return new RegexGroupModel(regex, scope);
	}

	public static enum Scope {
		CURRENT_FOLDER, INCLUDING_SUBFOLDERS, WHOLE_PROJECT
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RegexGroupModel that = (RegexGroupModel) o;

		if (enabled != that.enabled) return false;
		if (regex != null ? !regex.equals(that.regex) : that.regex != null) return false;
		return scope == that.scope;
	}

	@Override
	public int hashCode() {
		int result = regex != null ? regex.hashCode() : 0;
		result = 31 * result + (scope != null ? scope.hashCode() : 0);
		result = 31 * result + (enabled ? 1 : 0);
		return result;
	}
}
