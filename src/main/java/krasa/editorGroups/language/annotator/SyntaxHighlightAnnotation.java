package krasa.editorGroups.language.annotator;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

public class SyntaxHighlightAnnotation {
  private final int startSourceOffset;
  private final int endSourceOffset;
  private final TextAttributesKey textAttributesKey;

  public SyntaxHighlightAnnotation(int startSourceOffset, int endSourceOffset, TextAttributesKey textAttributesKey) {
    this.startSourceOffset = startSourceOffset;
    this.endSourceOffset = endSourceOffset;
    this.textAttributesKey = textAttributesKey;
  }

  public void annotate(AnnotationHolder holder, int sourceOffset) {
    TextRange fileRange = TextRange.create(startSourceOffset + sourceOffset, endSourceOffset + sourceOffset);
    @NotNull AnnotationBuilder infoAnnotation = holder.newAnnotation(HighlightSeverity.INFORMATION, null).range(fileRange);
    infoAnnotation.textAttributes(textAttributesKey);
    infoAnnotation.create();
  }
}
