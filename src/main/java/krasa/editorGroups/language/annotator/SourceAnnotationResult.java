package krasa.editorGroups.language.annotator;

import com.intellij.lang.annotation.AnnotationHolder;

import java.util.ArrayList;
import java.util.Collection;

public class SourceAnnotationResult {
	private int sourceOffset;

	private Collection<SyntaxHighlightAnnotation> annotations = new ArrayList<SyntaxHighlightAnnotation>();

	public SourceAnnotationResult(int sourceOffset) {
		this.sourceOffset = sourceOffset;
	}

	public void addAll(Collection<SyntaxHighlightAnnotation> sourceAnnotations) {
		annotations.addAll(sourceAnnotations);
	}

	public void annotate(AnnotationHolder holder) {
		for (SyntaxHighlightAnnotation annotation : annotations) {
			annotation.annotate(holder, sourceOffset);
		}
	}

}
