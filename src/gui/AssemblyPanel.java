package gui;

import engine.types.Instruction;
import gui.components.ResizableTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class AssemblyPanel extends JPanel {

	private int[] addresses;
	private ResizableTable resizableTable;

	public AssemblyPanel(boolean hex) {
		super(new BorderLayout(0, 10));

		ArrayList<Instruction> instructions = Simulator.processor.getMemory().getInstructions();
		String[][] text = new String[instructions.size()][2];
		addresses = new int[instructions.size()];
		for (int i = 0; i < text.length; i++) {
			addresses[i] = instructions.get(i).getAddress();
			text[i][0] = String.format((hex) ? " 0x%04X" : " %d", addresses[i]);
			text[i][1] = " " + instructions.get(i).toString();
		}

		resizableTable = new ResizableTable(text, new String[] {"Address", "Instruction"}, new int[]{35, 0}) {
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				int i = Simulator.processor.getRegisterFile().getPc() / 2;
				c.setBackground((rowIndex == i)? new Color(255, 255, 153) : getBackground());
				return c;
			}
		};
		resizableTable.setRowHeight(20);
		resizableTable.setForeground(Color.BLACK);
		resizableTable.getTableHeader().setResizingAllowed(false);
		
		JScrollPane scrollPane = new JScrollPane(resizableTable);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		JLabel l1 = new JLabel("Program");
		l1.setFont(new Font("Consolas", Font.PLAIN, 19));
		l1.setForeground(Color.RED);

		add(l1, BorderLayout.NORTH);
		add(scrollPane);
	}

	public void setFormat(boolean hex) {
		for (int i = 0; i < addresses.length; i++)
			resizableTable.setValueAt(String.format((hex) ? " 0x%04X" : " %d", addresses[i]), i, 0);
	}

}
