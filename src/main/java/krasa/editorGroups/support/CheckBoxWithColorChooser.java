/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package krasa.editorGroups.support;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Konstantin Bulenkov
 */
public class CheckBoxWithColorChooser extends JPanel {
	private JCheckBox myCheckbox;
	protected MyColorButton myColorButton;
	private Color myColor;
	private JButton defaultButton;
	private Dimension colorDimension;

	public CheckBoxWithColorChooser(String text, Boolean selected, Color defaultColor) {
		this(text, selected, Color.WHITE, defaultColor);
	}

	public CheckBoxWithColorChooser(String text, Color defaultColor) {
		this(text, false, defaultColor);
	}

	public CheckBoxWithColorChooser(String text, Boolean selected, Color color, Color defaultColor) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		myColor = color;
		if (selected != null) {
			myCheckbox = new JCheckBox(text, selected);
			add(myCheckbox);
		}
		myColorButton = new MyColorButton();
		add(myColorButton);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				myColorButton.mouseAdapter.mousePressed(e);
			}
		});

		if (defaultColor != null) {
			JPanel comp = new JPanel();
			comp.setSize(20, 0);
			add(comp);
			defaultButton = new JButton("Reset to default");
			add(defaultButton);
			defaultButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setColor(defaultColor);
					CheckBoxWithColorChooser.this.repaint();
				}
			});
		}
		colorDimension = new Dimension(18, 18);
	}

	public CheckBoxWithColorChooser setColorDimension(Dimension colorDimension) {
		this.colorDimension = colorDimension;
		return this;
	}

	public void setMnemonic(char c) {
		myCheckbox.setMnemonic(c);
	}

	public Color getColor() {
		return myColor;
	}

	public int getColorAsRGB() {
		return myColor.getRGB();
	}

	public void setColor(Integer color) {
		if (color != null) {
			myColor = new Color(color);
		}
	}

	public void setColor(Color color) {
		myColor = color;
	}

	public boolean isSelected() {
		return myCheckbox.isSelected();
	}

	public void setSelected(boolean selected) {
		myCheckbox.setSelected(selected);
	}

	public void onColorChanged() {
		repaint();
	}

	private class MyColorButton extends JButton {
		protected MouseAdapter mouseAdapter;

		MyColorButton() {
			setMargin(new Insets(0, 0, 0, 0));
			;
			setFocusable(false);
			setDefaultCapable(false);
			setFocusable(false);
			if (SystemInfo.isMac) {
				putClientProperty("JButton.buttonType", "square");
			}

			mouseAdapter = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					final Color color = ColorChooserService.getInstance().showDialog(MyColorButton.this,
							"Choose Color", CheckBoxWithColorChooser.this.myColor);
					if (color != null) {
						if (myCheckbox != null && !myCheckbox.isSelected()) {
							myCheckbox.setSelected(true);
						}
						myColor = color;
						onColorChanged();
					}
				}
			};
			addMouseListener(mouseAdapter);
			;
		}

		@Override
		public void paint(Graphics g) {
			final Color color = g.getColor();

			g.setColor(myColor);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(color);


			g.setColor(JBColor.BLACK);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getPreferredSize() {
			return colorDimension;
		}
	}
}
