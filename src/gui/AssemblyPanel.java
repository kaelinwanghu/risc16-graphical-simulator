package gui;

import engine.assembly.AssemblyResult;
import engine.isa.InstructionFormat;
import gui.components.ResizableTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class AssemblyPanel extends JPanel {

	private int[] addresses;
	private ResizableTable resizableTable;
	private int highlightedRow = -1;

	/**
	 * Creates an assembly panel displaying assembled instructions
	 * 
	 * @param result the assembly result containing instructions
	 * @param hex whether to display addresses in hexadecimal
	 */
	public AssemblyPanel(AssemblyResult result, boolean hex) {
		super(new BorderLayout(0, 10));

		// Extract instructions from result
		List<InstructionFormat> instructions = result.getInstructions();
		String[][] text = new String[instructions.size()][2];
		addresses = new int[instructions.size()];
		
		// Build table data
		for (int i = 0; i < text.length; i++) {
			InstructionFormat instr = instructions.get(i);
			addresses[i] = instr.getAddress();
			text[i][0] = formatAddress(instr.getAddress(), hex);
			text[i][1] = " " + instr.toAssembly();
		}

		// Create table with custom renderer for highlighting
		resizableTable = new ResizableTable(text, 
			new String[] {"Address", "Instruction"}, 
			new int[]{35, 0}) {
			
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				
				// Highlight the row corresponding to current PC
				if (rowIndex == highlightedRow) {
					c.setBackground(new Color(255, 255, 153)); // Yellow highlight
				} else {
					c.setBackground(getBackground());
				}
				
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

	/**
	 * Highlights the instruction at the given PC address
	 * 
	 * @param pc the program counter value
	 */
	public void highlightInstruction(int pc) {
		// Find the row index corresponding to this PC
		for (int i = 0; i < addresses.length; i++) {
			if (addresses[i] == pc) {
				highlightedRow = i;
				resizableTable.repaint();
				
				// Scroll to make this row visible
				resizableTable.scrollRectToVisible(
					resizableTable.getCellRect(i, 0, true));
				return;
			}
		}
		
		// PC not found in instructions (might be halted or in data section)
		highlightedRow = -1;
		resizableTable.repaint();
	}

	/**
	 * Changes the address display format between hex and decimal
	 * 
	 * @param hex true for hexadecimal, false for decimal
	 */
	public void setFormat(boolean hex) {
		for (int i = 0; i < addresses.length; i++) {
			resizableTable.setValueAt(formatAddress(addresses[i], hex), i, 0);
		}
	}
	
	/**
	 * Formats an address for display
	 * 
	 * @param address the address to format
	 * @param hex whether to use hexadecimal format
	 * @return formatted address string
	 */
	private String formatAddress(int address, boolean hex) {
		return hex 
			? String.format(" 0x%04X", address)
			: String.format(" %d", address);
	}
}