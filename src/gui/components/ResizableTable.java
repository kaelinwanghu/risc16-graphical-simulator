package gui.components;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("serial")
public class ResizableTable extends JTable {

	private int[] spacing;
		
	public ResizableTable(int[] spacing) {
		this.spacing = spacing;
		getTableHeader().setReorderingAllowed(false);	
		setFillsViewportHeight(true);
		setShowGrid(false);
		setEnabled(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}
	
	public ResizableTable(Object[][] data, Object[] headers, int[] spacing) {
		this(spacing);
		setData(data, headers);
	}
	
	public void setData(Object[][] data, Object[] headers) {
		setModel(new DefaultTableModel(data, headers));
		pack();
	}

	private void pack() {
		if (getColumnCount() == 0)
			return;

		int width[] = new int[getColumnCount()];
		int total = 0;
		for (int col = 0; col < width.length; col++) {
			width[col] = preferredWidth(col) + spacing[col];
			total += width[col];
		}

		int extra = getVisibleRect().width - total;
		if (extra > 0)
			width[width.length - 1] += extra;
		
		TableColumnModel columnModel = getColumnModel();
		for (int col = 0; col < width.length; col++) {
			TableColumn tableColumn = columnModel.getColumn(col);
			getTableHeader().setResizingColumn(tableColumn);
			tableColumn.setWidth(width[col]);
		}
	}
	
	private int preferredWidth(int col) {
		TableColumn tableColumn = getColumnModel().getColumn(col);
		int width = (int) getTableHeader().getDefaultRenderer().getTableCellRendererComponent(this, tableColumn.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();

		for (int row = 0; row < getRowCount(); row++) {
			int preferedWidth = (int) getCellRenderer(row, col).getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
			width = Math.max(width, preferedWidth);
		}
		return width + getIntercellSpacing().width;
	}
	
	public boolean getScrollableTracksViewportWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

}
