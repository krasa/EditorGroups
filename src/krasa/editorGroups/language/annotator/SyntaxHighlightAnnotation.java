package krasa.editorGroups.language.annotator;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;

public class SyntaxHighlightAnnotation {
	private int startSourceOffset;
	private int endSourceOffset;
	private TextAttributesKey textAttributesKey;

	public SyntaxHighlightAnnotation(int startSourceOffset, int endSourceOffset, TextAttributesKey textAttributesKey) {
		this.startSourceOffset = startSourceOffset;
		this.endSourceOffset = endSourceOffset;
		this.textAttributesKey = textAttributesKey;
	}

	public void annotate(AnnotationHolder holder, int sourceOffset) {
		TextRange fileRange = TextRange.create(startSourceOffset + sourceOffset, endSourceOffset + sourceOffset);
		Annotation infoAnnotation = holder.createInfoAnnotation(fileRange, null);
		infoAnnotation.setTextAttributes(textAttributesKey);
	}
}
