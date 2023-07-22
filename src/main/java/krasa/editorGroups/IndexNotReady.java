package krasa.editorGroups;

import com.intellij.openapi.diagnostic.Logger;

public class IndexNotReady extends Exception {
  private static final Logger LOG = Logger.getInstance(IndexNotReady.class);

  public IndexNotReady(String s, Exception e) {
    super(s, e);
  }
}
