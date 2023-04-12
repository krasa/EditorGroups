package krasa.editorGroups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.BitUtil;

import javax.swing.*;
import java.awt.event.InputEvent;

public enum Splitters {
	NONE, VERTICAL, HORIZONTAL;

	public static Splitters from(boolean alt, boolean shift) {
		if (alt && shift) {
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
		boolean alt = BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK);
		boolean shift = BitUtil.isSet(e.getModifiers(), InputEvent.SHIFT_MASK);
		if (alt && shift) {
			return HORIZONTAL;
		}
		return from(alt);
	}

	boolean isSplit() {
		return this != NONE;
	}

	int getOrientation() {
		return this == HORIZONTAL ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
	}
}
