package krasa.editorGroups.gui;


import krasa.editorGroups.model.RegExpGroupModels;
import krasa.editorGroups.model.RegexGroupModel;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class MyListDataListener implements ListDataListener {
	private DefaultListModel<RegexGroupModel> model;
	private RegExpGroupModels models;

	public MyListDataListener(DefaultListModel<RegexGroupModel> model, RegExpGroupModels models) {
		this.model = model;
		this.models = models;
	}

	@Override
	public void intervalAdded(ListDataEvent e) {
		listChanged();
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		listChanged();
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		listChanged();
	}

	private void listChanged() {
		List<RegexGroupModel> list = this.models.getRegexGroupModels();
		list.clear();
		for (int i = 0; i < model.getSize(); i++) {
			list.add(model.getElementAt(i));
		}
	}
}
