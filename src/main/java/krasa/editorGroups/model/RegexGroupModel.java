package krasa.editorGroups.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class RegexGroupModel {
  private static final Logger LOG = Logger.getInstance(RegexGroupModel.class);

  private String regex;
  private String notComparingGroups = "";
  private Scope scope = Scope.CURRENT_FOLDER;
  private boolean enabled = true;
  @Transient
  private transient Pattern regexPattern;
  @Transient
  private transient int[] comparingGroupsAsIntArray;

  public RegexGroupModel() {
  }

  public RegexGroupModel(String regex, Scope scope, String notComparingGroups) {
    this.regex = regex;
    this.scope = scope;
    setNotComparingGroups(notComparingGroups);
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

  public String getNotComparingGroups() {
    return notComparingGroups;
  }

  public void setNotComparingGroups(String notComparingGroups) {
    if (isNotBlank(notComparingGroups) && notComparingGroups.contains("|")) {
      throw new IllegalArgumentException("notComparingGroups must not contain '|'");
    }
    comparingGroupsAsIntArray = null;
    this.notComparingGroups = notComparingGroups;
  }

  public void setScope(Scope scope) {
    if (scope == null) {
      scope = Scope.CURRENT_FOLDER;
    }
    this.scope = scope;
  }

  public String serialize() {
    return "v1|" + scope + "|" + notComparingGroups + "|" + regex;
  }

  public static RegexGroupModel deserialize(String s) {
    try {
      if (s.startsWith("v0")) {
        int scopeEnd = s.indexOf("|", 3);
        String scope = s.substring(3, scopeEnd);
        String regex = s.substring(scopeEnd + 1);
        return new RegexGroupModel(regex, Scope.valueOf(scope), "");
      }
      if (s.startsWith("v1")) {
        int scopeEnd = s.indexOf("|", 3);
        String scope = s.substring(3, scopeEnd);

        int notComparingGroupsEnd = s.indexOf("|", scopeEnd + 1);
        String notComparingGroups = s.substring(scopeEnd + 1, notComparingGroupsEnd);

        String regex = s.substring(notComparingGroupsEnd + 1);
        return new RegexGroupModel(regex, Scope.valueOf(scope), notComparingGroups);
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
    return new RegexGroupModel(regex, scope, notComparingGroups);
  }

  public boolean isComparingGroup(int j) {
    if (comparingGroupsAsIntArray == null) {
      comparingGroupsAsIntArray = getComparingGroupsAsIntArray();
    }
    for (int s : comparingGroupsAsIntArray) {
      if (s == j)
        return false;
    }
    return true;
  }

  @Transient
  private int[] getComparingGroupsAsIntArray() {
    if (isBlank(notComparingGroups)) {
      return new int[0];
    }
    String[] split = notComparingGroups.split(",");
    int size = split.length;
    int[] arr = new int[size];
    for (int i = 0; i < size; i++) {
      try {
        String s = split[i];
        if (isBlank(s)) {
          s = "-1";
        }
        arr[i] = Integer.parseInt(s);
      } catch (Exception e) {
        LOG.error(e);
        arr[i] = -1;
      }
    }
    return arr;
  }


  public enum Scope {
    CURRENT_FOLDER, INCLUDING_SUBFOLDERS, WHOLE_PROJECT
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RegexGroupModel that = (RegexGroupModel) o;

    if (enabled != that.enabled) return false;
    if (!Objects.equals(regex, that.regex)) return false;
    if (!Objects.equals(notComparingGroups, that.notComparingGroups))
      return false;
    return scope == that.scope;
  }

  @Override
  public int hashCode() {
    int result = regex != null ? regex.hashCode() : 0;
    result = 31 * result + (notComparingGroups != null ? notComparingGroups.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    result = 31 * result + (enabled ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "RegexGroupModel{" +
      "regex='" + regex + '\'' +
      ", notComparingGroups='" + notComparingGroups + '\'' +
      ", scope=" + scope +
      ", enabled=" + enabled +
      '}';
  }
}
