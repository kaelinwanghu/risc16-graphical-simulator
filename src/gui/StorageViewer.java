package gui;

import gui.components.ResizableTable;
import gui.facade.EngineFacade;
import gui.dialogs.StorageSettingsDialog;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class StorageViewer extends JPanel {
	
	public JButton hex;
	private JTextArea data;
	private JComboBox<String> type;
	private ResizableTable resizableTable;
	
	private EngineFacade engineFacade;
	private Simulator simulator;
	
	// Change tracking for highlighting
	private short[] lastRegisterValues = new short[8];
	private Set<Integer> changedRegisters = new HashSet<>();
	private Set<Integer> changedMemoryAddresses = new HashSet<>();
	
	public StorageViewer(final Simulator simulator, EngineFacade engineFacade) {
		super(new BorderLayout(0, 10));
		
		this.simulator = simulator;
		this.engineFacade = engineFacade;
		
		// Initialize last values
		for (int i = 0; i < 8; i++) {
			lastRegisterValues[i] = 0;
		}
		
		// Table for displaying registers or memory
		resizableTable = new ResizableTable(new int[]{20, 15, 0}) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				
				// Default appearance
				c.setBackground(Color.WHITE);
				c.setFont(c.getFont().deriveFont(Font.PLAIN));
				
				boolean shouldHighlight = false;
				Color highlightColor = new Color(200, 255, 200); // Light green
				
				// Check if this row should be highlighted
				if (type.getSelectedIndex() == 0) {
					// Registers view
					if (rowIndex == 0 && lastRegisterValues[0] != 0) {
						// R0 attempted write (special case - yellow)
						shouldHighlight = true;
						highlightColor = new Color(255, 255, 150);
					} else if (changedRegisters.contains(rowIndex)) {
						// Normal register change
						shouldHighlight = true;
					}
				} else if (type.getSelectedIndex() == 1) {
					// Memory view - could highlight changed addresses
					// (Not implemented yet - would need to track memory changes)
				}
				
				// Apply highlighting with padding
				if (c instanceof JLabel) {
					JLabel label = (JLabel) c;
					if (shouldHighlight) {
						label.setBackground(highlightColor);
						label.setFont(label.getFont().deriveFont(Font.BOLD));
						label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
						label.setOpaque(true);
					} else {
						label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
						label.setOpaque(false);
					}
				}
				
				return c;
			}
		};
		
		resizableTable.setRowHeight(20);
		resizableTable.setIntercellSpacing(new Dimension(0, 1));

		JScrollPane scrollPane = new JScrollPane(resizableTable); 
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setOpaque(false);
		
		// Type selector (Registers or Memory)
		type = new JComboBox<String>(new String[]{"Registers", "Memory"});
		type.setFocusable(false);
		type.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				refresh();
			}
		});

		// Hex/Dec toggle button
		hex = new JButton("HEX");
		hex.setFocusable(false);
		hex.addActionListener(e -> {
			hex.setText(hex.getText().equals("HEX") ? "DEC" : "HEX");
			refresh();
			if (simulator.assemblyPanel != null) {
				simulator.assemblyPanel.setFormat(hex.getText().equals("HEX"));
			}
		});
		
		// Settings button
		JButton settings = new JButton("Settings");
		settings.setFocusable(false);
		settings.addActionListener(e -> {
			StorageSettingsDialog dialog = new StorageSettingsDialog(simulator, engineFacade);
			dialog.setVisible(true);
		});
		
		// Info text area
		data = new JTextArea(3, 10);
		data.setBorder(BorderFactory.createCompoundBorder(
			new LineBorder(Color.GRAY, 1), 
			BorderFactory.createEmptyBorder(5, 10, 5, 5)));
		data.setEnabled(false);
		data.setDisabledTextColor(new Color(100, 100, 100));
		
		// Layout
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p1.add(settings);
		p1.add(Box.createRigidArea(new Dimension(5, 0)));
		p1.add(hex);
		
		JPanel typePanel = new JPanel(new BorderLayout(0, 10));
		typePanel.add(type, BorderLayout.WEST);
		typePanel.add(p1, BorderLayout.EAST);
		
		JLabel l1 = new JLabel("Storage");
		l1.setFont(new Font("Consolas", Font.PLAIN, 19));
		l1.setForeground(Color.RED);
		
		JPanel p3 = new JPanel(new BorderLayout(0, 10));
		p3.add(l1, BorderLayout.NORTH);
		p3.add(typePanel);
		p3.add(data, BorderLayout.SOUTH);
		
		add(p3, BorderLayout.NORTH);
		add(scrollPane);
		
		refresh();
	}

	/**
	 * Updates the display based on new processor state
	 * Called by observer when state changes
	 */
	public void updateState(ProcessorState state) {
		// Clear change tracking for next step
		clearChanges();
		
		// Track which registers changed
		for (int i = 0; i < 8; i++) {
			short currentValue = state.getRegister(i);
			if (currentValue != lastRegisterValues[i]) {
				changedRegisters.add(i);
				lastRegisterValues[i] = currentValue;
			}
		}
		
		refresh();
	}
	
	/**
	 * Clears change tracking (called before assembly or reset)
	 */
	public void clearChanges() {
		changedRegisters.clear();
		changedMemoryAddresses.clear();
	}

	/**
	 * Refreshes the current view (registers or memory)
	 */
	public void refresh() {
		boolean isHex = hex.getText().equals("HEX");
		
		switch (type.getSelectedIndex()) {
			case 0: // Registers
				displayRegisters(isHex);
				break;
			case 1: // Memory
				displayMemory(isHex);
				break;
		}
	}
	
	/**
	 * Displays register contents
	 */
	private void displayRegisters(boolean isHex) {
		ProcessorState state = engineFacade.getState();
		
		// Build table data
		String[][] tableData = new String[8][2];
		for (int i = 0; i < 8; i++) {
			tableData[i][0] = " R" + i;
			short value = state.getRegister(i);
			tableData[i][1] = " " + formatValue(value, isHex);
		}
		
		resizableTable.setData(tableData, new String[]{"Reg", "Value"});
		
		// Update info text
		StringBuilder info = new StringBuilder();
		info.append("PC: ").append(formatValue(state.getPC(), isHex)).append("\n");
		info.append("Instructions: ").append(state.getInstructionCount()).append("\n");
		info.append("Halted: ").append(state.isHalted() ? "Yes" : "No");
		
		data.setText(info.toString());
	}
	
	/**
	 * Displays memory contents
	 */
	private void displayMemory(boolean isHex) {
		Memory memory = engineFacade.getMemory();
		ProgramMetadata metadata = engineFacade.getMetadata();
		
		// Determine range to display
		int maxAddress = Math.min(1024, memory.getSize()); // Show first 512 words (1024 bytes)
		
		List<String[]> rows = new ArrayList<>();
		
		for (int addr = 0; addr < maxAddress; addr += 2) {
			if (memory.isValidAddress(addr + 1)) {
				short value = memory.readWord(addr);
				
				String addrStr = " " + formatValue(addr, isHex);
				String valueStr = " " + formatValue(value & 0xFFFF, isHex);
				
				// Check for label at this address
				String label = "";
				if (metadata != null && metadata.hasLabel(addr)) {
					label = " " + metadata.getLabel(addr);
				}
				
				rows.add(new String[]{addrStr, valueStr, label});
			}
		}
		
		resizableTable.setData(
			rows.toArray(new String[0][]), 
			new String[]{"Address", "Value", "Label"});
		
		// Update info text
		StringBuilder info = new StringBuilder();
		info.append("Showing first ").append(maxAddress / 2).append(" words\n");
		info.append("Memory size: ").append(memory.getSize()).append(" bytes\n");
		
		if (metadata != null) {
			info.append("Instructions: ").append(metadata.getInstructionCount()).append("\n");
			info.append("Data words: ").append(metadata.getDataCount());
		}
		
		data.setText(info.toString());
	}
	
	/**
	 * Formats a value for display (hex or decimal)
	 */
	private String formatValue(int value, boolean isHex) {
		if (isHex) {
			return String.format("0x%04X", value & 0xFFFF);
		} else {
			// Display as signed 16-bit for readability
			short signed = (short) value;
			return String.valueOf(signed);
		}
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(500, super.getPreferredSize().height);
	}
}