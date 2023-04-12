package krasa.editorGroups.gui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.table.JBTable;
import krasa.editorGroups.ApplicationConfiguration;
import krasa.editorGroups.model.RegexGroupModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RegexModelTable extends JBTable {
	private static final Logger LOG = Logger.getInstance(RegexModelTable.class);
	private final MyTableModel myTableModel = new MyTableModel();
	private static final int REGEX_COLUMN = 0;
	private static final int SCOPE_COLUMN = 1;

	private final List<RegexGroupModel> myRegexGroupModels = new ArrayList<>();

	public RegexModelTable() {
		setModel(myTableModel);
		TableColumn column = getColumnModel().getColumn(REGEX_COLUMN);
		column.setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//				final RegexGroup.Model.Scope enabled = getAliasValueAt(row);
//				component.setForeground(enabled
//					? JBColor.GRAY
//					: isSelected ? table.getSelectionForeground() : table.getForeground());
				return component;
			}
		});
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public RegexGroupModel.Scope getRegexModelValueAt(int row) {
		return (RegexGroupModel.Scope) getValueAt(row, SCOPE_COLUMN);
	}

	public void addRegexModel() {
		final RegexModelEditor regexModelEditor = new RegexModelEditor("Add RegexGroup", "", "", RegexGroupModel.Scope.CURRENT_FOLDER);
		if (regexModelEditor.showAndGet()) {
			final String name = regexModelEditor.getRegex();
			myRegexGroupModels.add(new RegexGroupModel(name, regexModelEditor.getScopeCombo(), regexModelEditor.getNotComparingGroups()));
			final int index = indexOfRegexModelWithName(name);
			LOG.assertTrue(index >= 0);
			myTableModel.fireTableDataChanged();
			setRowSelectionInterval(index, index);
		}
	}

	private boolean isValidRow(int selectedRow) {
		return selectedRow >= 0 && selectedRow < myRegexGroupModels.size();
	}

	public void moveUp() {
		int selectedRow = getSelectedRow();
		int index1 = selectedRow - 1;
		if (selectedRow != -1) {
			Collections.swap(myRegexGroupModels, selectedRow, index1);
		}
		setRowSelectionInterval(index1, index1);
	}

	public void moveDown() {
		int selectedRow = getSelectedRow();
		int index1 = selectedRow + 1;
		if (selectedRow != -1) {
			Collections.swap(myRegexGroupModels, selectedRow, index1);
		}
		setRowSelectionInterval(index1, index1);
	}


	public void removeSelectedRegexModels() {
		final int[] selectedRows = getSelectedRows();
		if (selectedRows.length == 0) return;
		Arrays.sort(selectedRows);
		final int originalRow = selectedRows[0];
		for (int i = selectedRows.length - 1; i >= 0; i--) {
			final int selectedRow = selectedRows[i];
			if (isValidRow(selectedRow)) {
				myRegexGroupModels.remove(selectedRow);
			}
		}
		myTableModel.fireTableDataChanged();
		if (originalRow < getRowCount()) {
			setRowSelectionInterval(originalRow, originalRow);
		} else if (getRowCount() > 0) {
			final int index = getRowCount() - 1;
			setRowSelectionInterval(index, index);
		}
	}

	public void commit(ApplicationConfiguration settings) {
		settings.getRegexGroupModels().setRegexGroupModels(new ArrayList<>(myRegexGroupModels));
	}


	public void reset(ApplicationConfiguration settings) {
		obtainRegexModels(myRegexGroupModels, settings);
		myTableModel.fireTableDataChanged();
	}


	private int indexOfRegexModelWithName(String name) {
		for (int i = 0; i < myRegexGroupModels.size(); i++) {
			final RegexGroupModel pair = myRegexGroupModels.get(i);
			if (name.equals(pair.getRegex())) {
				return i;
			}
		}
		return -1;
	}

	private void obtainRegexModels(@NotNull List<RegexGroupModel> regexModels, ApplicationConfiguration settings) {
		regexModels.clear();
		List<RegexGroupModel> regexGroupModels = settings.getRegexGroupModels().getRegexGroupModels();
		for (RegexGroupModel regexGroupModel : regexGroupModels) {
			regexModels.add(regexGroupModel.copy());
		}
	}

	public boolean editRegexModel() {
		if (getSelectedRowCount() != 1) {
			return false;
		}
		final int selectedRow = getSelectedRow();
		final RegexGroupModel regexGroupModel = myRegexGroupModels.get(selectedRow);
		final RegexModelEditor editor = new RegexModelEditor("Edit RegexGroup", regexGroupModel.getRegex(), regexGroupModel.getNotComparingGroups(), regexGroupModel.getScope());
		if (editor.showAndGet()) {
			regexGroupModel.setRegex(editor.getRegex());
			regexGroupModel.setNotComparingGroups(editor.getNotComparingGroups());
			regexGroupModel.setScope(editor.getScopeCombo());
			myTableModel.fireTableDataChanged();
		}
		return true;
	}

	public boolean isModified(ApplicationConfiguration settings) {
		final ArrayList<RegexGroupModel> regexGroupModels = new ArrayList<>();
		obtainRegexModels(regexGroupModels, settings);
		return !regexGroupModels.equals(myRegexGroupModels);
	}


	private class MyTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			return myRegexGroupModels.size();
		}

		@Override
		public Class getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			final RegexGroupModel pair = myRegexGroupModels.get(rowIndex);
			switch (columnIndex) {
				case REGEX_COLUMN:
					return pair.getRegex();
				case SCOPE_COLUMN:
					return pair.getScope();
			}
			LOG.error("Wrong indices");
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case REGEX_COLUMN:
					return "Regex";
				case SCOPE_COLUMN:
					return "Scope";
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}


}
