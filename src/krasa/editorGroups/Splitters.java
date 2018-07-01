package krasa.editorGroups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.BitUtil;

import javax.swing.*;
import java.awt.event.InputEvent;

public enum Splitters {
	NONE, VERTICAL, HORIZONTAL;

	public static Splitters from(boolean alt, boolean button2) {
		if (alt && button2) {
			return HORIZONTAL;
		}
		if (alt) {
			return VERTICAL;
		}
		return NONE;
	}

	public static Splitters from(boolean set) {
		if (set) {
			return VERTICAL;
		}
		return NONE;
	}

	public static Splitters from(InputEvent e) {
		return from(BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK));
	}

	public static Splitters from(AnActionEvent e) {
		return from(BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK));
	}

	boolean isSplit() {
		return this != NONE;
	}

	int getOrientation() {
		return this == HORIZONTAL ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
	}
}
