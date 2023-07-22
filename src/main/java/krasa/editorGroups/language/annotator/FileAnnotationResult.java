package krasa.editorGroups.language.annotator;

import com.intellij.lang.annotation.AnnotationHolder;

import java.util.ArrayList;
import java.util.Collection;

public class FileAnnotationResult {
  private final Collection<SourceAnnotationResult> sourceAnnotationResults = new ArrayList<SourceAnnotationResult>();

  public boolean add(SourceAnnotationResult sourceAnnotationResult) {
    return sourceAnnotationResults.add(sourceAnnotationResult);
  }

  public void annotate(AnnotationHolder holder) {
    for (SourceAnnotationResult sourceAnnotationResult : sourceAnnotationResults) {
      sourceAnnotationResult.annotate(holder);
    }
  }
}
