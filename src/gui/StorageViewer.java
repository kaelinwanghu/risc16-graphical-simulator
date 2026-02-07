package gui;

import gui.components.ResizableTable;
import gui.facade.EngineFacade;
import gui.dialogs.StorageSettingsDialog;
import gui.dialogs.ValueEditorDialog;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;
import engine.execution.ExecutionResult;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
	private StorageSettingsDialog settingsDialog;
	private JButton debugButton;

	private int memoryViewStart = 0;
	private int memoryViewCount = 512;

	private EngineFacade engineFacade;
	private Simulator simulator;

	// Change tracking for highlighting
	private short[] lastRegisterValues = new short[8];
	private Set<Integer> changedRegisters = new HashSet<>();
	private Set<Integer> changedMemoryAddresses = new HashSet<>();
	private Map<Integer, Short> lastMemoryValues = new HashMap<>();
	private Set<Integer> initialLoadedAddresses = new HashSet<>();
	private Set<Integer> editedRegisters = new HashSet<>();
	private Set<Integer> editedMemoryAddresses = new HashSet<>();
	private ValueEditorDialog valueEditorDialog;

	public StorageViewer(final Simulator simulator, EngineFacade engineFacade) {
		super(new BorderLayout(0, 10));

		this.simulator = simulator;
		this.engineFacade = engineFacade;

		this.valueEditorDialog = new ValueEditorDialog(simulator);

		// Initialize last values
		for (int i = 0; i < 8; i++) {
			lastRegisterValues[i] = 0;
		}

		// Table for displaying registers or memory
		resizableTable = new ResizableTable(new int[] { 20, 15, 0 }) {
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
					if (editedRegisters.contains(rowIndex)) {
						shouldHighlight = true;
						highlightColor = new Color(255, 200, 200); // Light red for manual edits
					}
					if (rowIndex == 0 && lastRegisterValues[0] != 0) {
						// R0 attempted write (special case - yellow)
						shouldHighlight = true;
						highlightColor = new Color(255, 255, 150);
					} else if (changedRegisters.contains(rowIndex)) {
						// Normal register change
						shouldHighlight = true;
					}
				} else if (type.getSelectedIndex() == 1) {
					// Memory view
					int startAddr = memoryViewStart;
					int address = startAddr + (rowIndex * 2); // Each row is one word (2 bytes)

					if (editedMemoryAddresses.contains(address)) {
						shouldHighlight = true;
						highlightColor = new Color(255, 200, 200); // Light red for manual edits
					}
					// Check if this address was just written to (green)
					if (changedMemoryAddresses.contains(address)) {
						shouldHighlight = true;
						highlightColor = new Color(200, 255, 200); // Light green
					}
					// Check if this address was part of initial program load (light blue)
					else if (initialLoadedAddresses.contains(address)) {
						shouldHighlight = true;
						highlightColor = new Color(200, 220, 255); // Light blue
					}
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

		resizableTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) { // Double-click
					handleDoubleClick(e);
				}
			}
		});

		resizableTable.setRowHeight(20);
		resizableTable.setIntercellSpacing(new Dimension(0, 1));

		JScrollPane scrollPane = new JScrollPane(resizableTable);
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setOpaque(false);

		// Type selector (Registers or Memory)
		type = new JComboBox<String>(new String[] { "Registers", "Memory" });
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

		this.settingsDialog = new StorageSettingsDialog(simulator, this, engineFacade);
		// Settings button
		JButton settings = new JButton("Settings");
		settings.setFocusable(false);

		JButton debuggerButton = new JButton("Debug");
		debuggerButton.setFocusable(false);
		debuggerButton.setVisible(false);
		JPopupMenu debugMenu = new JPopupMenu();
		JMenuItem snapshotsItem = new JMenuItem("View Snapshots...");
		snapshotsItem.addActionListener(e -> simulator.showSnapshotsDialog());
		debugMenu.add(snapshotsItem);
		JMenuItem breakpointsItem = new JMenuItem("Manage Breakpoints...");
		breakpointsItem.addActionListener(e -> simulator.showBreakpointsDialog());
		debugMenu.add(breakpointsItem);
		debuggerButton.addActionListener(e -> {
			debugMenu.show(debuggerButton, 0, debuggerButton.getHeight());
		});

		// Store reference to update visibility
		this.debugButton = debuggerButton;
		settings.addActionListener(e -> settingsDialog.setVisible(true));

		// Info text area
		data = new JTextArea(3, 10);
		data.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(Color.GRAY, 1),
				BorderFactory.createEmptyBorder(5, 10, 5, 5)));
		data.setEnabled(false);
		data.setDisabledTextColor(new Color(100, 100, 100));

		// Layout
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p1.add(debugButton);
		p1.add(settings);
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
	 * 
	 * @param state  the new processor state
	 * @param result the execution result (null if manual edit)
	 */
	public void updateState(ProcessorState state, ExecutionResult result) {
		// Only clear highlights if this is from instruction execution (not manual edit)
		if (result != null) {
			// Clear initial load highlighting after first step
			if (!initialLoadedAddresses.isEmpty()) {
				clearInitialLoadHighlight();
			}

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

			// Track memory changes
			if (result.isMemoryWritten() && result.hasMemoryAccess()) {
				int address = result.getMemoryAddress();
				changedMemoryAddresses.add(address);

				// Store current value for future comparisons
				Memory memory = engineFacade.getMemory();
				if (memory.isValidAddress(address) && memory.isValidAddress(address + 1)) {
					short value = memory.readWord(address);
					lastMemoryValues.put(address, value);
				}
			}
		} else {
			// Manual edit - just update last values without clearing highlights
			for (int i = 0; i < 8; i++) {
				lastRegisterValues[i] = state.getRegister(i);
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

		resizableTable.setData(tableData, new String[] { "Reg", "Value" });

		// Update info text
		StringBuilder info = new StringBuilder();
		info.append("PC: ").append(formatValue(state.getPC(), isHex)).append("\n");
		info.append("Instructions: ").append(state.getInstructionCount()).append("\n");
		info.append("Halted: ").append(state.isHalted() ? "Yes" : "No");

		data.setText(info.toString());
	}

	/**
	 * Displays memory contents
	 * 
	 * @param isHex whether to display in hexadecimal format
	 */
	private void displayMemory(boolean isHex) {
		Memory memory = engineFacade.getMemory();
		ProgramMetadata metadata = engineFacade.getMetadata();

		List<String[]> rows = new ArrayList<>();

		// Use configured range
		int startAddr = memoryViewStart;
		int endAddr = startAddr + (memoryViewCount * 2); // Convert words to bytes

		// Clamp to valid memory range
		if (startAddr >= memory.getSize()) {
			// Start is beyond memory, show from beginning
			startAddr = 0;
			endAddr = Math.min(memoryViewCount * 2, memory.getSize());
		} else {
			// Clamp end to memory size
			endAddr = Math.min(endAddr, memory.getSize());
		}

		int wordsShown = 0;
		for (int addr = startAddr; addr < endAddr; addr += 2) {
			if (memory.isValidAddress(addr) && memory.isValidAddress(addr + 1)) {
				short value = memory.readWord(addr);

				String addrStr = " " + formatValue(addr, isHex);
				String valueStr = " " + formatValue(value & 0xFFFF, isHex);

				// Check for label at this address
				String label = "";
				if (metadata != null && metadata.hasLabel(addr)) {
					label = " " + metadata.getLabel(addr);
				}

				rows.add(new String[] { addrStr, valueStr, label });
				wordsShown++;
			}
		}

		resizableTable.setData(
				rows.toArray(new String[0][]),
				new String[] { "Address", "Value", "Label" });

		// Update info text with actual range shown
		StringBuilder info = new StringBuilder();
		info.append("Showing ").append(wordsShown).append(" word(s)\n");

		if (wordsShown > 0) {
			info.append("Range: ").append(formatValue(startAddr, isHex));
			info.append(" - ").append(formatValue(endAddr - 2, isHex)).append("\n");
		} else {
			info.append("No data in range\n");
		}

		info.append("Memory size: ").append(memory.getSize()).append(" bytes");

		if (metadata != null) {
			info.append("\nInstructions: ").append(metadata.getInstructionCount());
			info.append("\nData words: ").append(metadata.getDataCount());
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
		return new Dimension(450, super.getPreferredSize().height);
	}

	public Dimension getMinimumSize() {
		return new Dimension(350, 400);
	}

	public int getMemoryViewStart() {
		return memoryViewStart;
	}

	public int getMemoryViewCount() {
		return memoryViewCount;
	}

	/**
	 * Sets the memory view range
	 * 
	 * @param startAddress the start address in bytes (must be word-aligned)
	 * @param wordCount    the number of words to display
	 */
	public void setMemoryViewRange(int startAddress, int wordCount) {
		if (startAddress % 2 != 0) {
			throw new IllegalArgumentException("Start address must be word-aligned");
		}
		if (wordCount < 1) {
			throw new IllegalArgumentException("Word count must be at least 1");
		}

		this.memoryViewStart = startAddress;
		this.memoryViewCount = wordCount;

		// Refresh display if currently showing memory
		if (type.getSelectedIndex() == 1) {
			refresh();
		}
	}

	public void setEngineFacade(EngineFacade engineFacade) {
		this.engineFacade = engineFacade;
		this.settingsDialog.setEngineFacade(engineFacade);

		for (int i = 0; i < 8; i++) {
			lastRegisterValues[i] = 0;
		}

		clearChanges();
	}

	/**
	 * Marks addresses as initially loaded (for highlighting on first display)
	 * Called when a new program is loaded
	 */
	public void markInitialLoad(Set<Integer> addresses) {
		initialLoadedAddresses.clear();
		initialLoadedAddresses.addAll(addresses);
	}

	/**
	 * Clears initial load highlighting
	 * Called after first refresh or when user executes a step
	 */
	public void clearInitialLoadHighlight() {
		initialLoadedAddresses.clear();
	}

	/**
	 * Updates debug button visibility based on debug state
	 */
	public void updateDebugButtonVisibility(boolean debugEnabled) {
		if (debugButton != null) {
			debugButton.setVisible(debugEnabled);
		}
	}

	/**
	 * Handles double-click on register or memory cell for editing
	 */
	private void handleDoubleClick(MouseEvent e) {
		// Check if debugging is enabled
		if (!engineFacade.getDebugManager().isEnabled()) {
			JOptionPane.showMessageDialog(
					this,
					"Enable debugging in Settings to edit values.",
					"Debugging Disabled",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Check if a program is loaded and running
		if (engineFacade.getLastAssembly() == null || !engineFacade.getLastAssembly().isSuccess()) {
			JOptionPane.showMessageDialog(
					this,
					"Load a program first before editing values.",
					"No Program Loaded",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if (engineFacade.isHalted()) {
			JOptionPane.showMessageDialog(
					this,
					"Cannot edit values after program has halted.\nRestart the program to edit values.",
					"Program Halted",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		int row = resizableTable.rowAtPoint(e.getPoint());
		int col = resizableTable.columnAtPoint(e.getPoint());

		if (row == -1 || col != 1) { // Only "Value" column is editable (column 1)
			return;
		}

		if (type.getSelectedIndex() == 0) {
			// Registers view
			editRegister(row);
		} else if (type.getSelectedIndex() == 1) {
			// Memory view
			editMemory(row);
		}
	}

	/**
	 * Edits a register value
	 */
	private void editRegister(int regNum) {
		if (regNum == 0) {
			JOptionPane.showMessageDialog(
					this,
					"R0 is read-only and always returns 0. Cannot edit R0.",
					"Invalid Edit",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		ProcessorState state = engineFacade.getState();
		short currentValue = state.getRegister(regNum);

		Integer newValue = valueEditorDialog.editRegister(regNum, currentValue);

		if (newValue != null) {
			// Apply the new value
			try {
				engineFacade.setRegister(regNum, (short) newValue.intValue());
				editedRegisters.add(regNum);
				refresh();
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(
						this,
						"Failed to set register: " + ex.getMessage(),
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Edits a memory word
	 */
	private void editMemory(int rowIndex) {
		int startAddr = memoryViewStart;
		int address = startAddr + (rowIndex * 2); // Each row is one word (2 bytes)

		Memory memory = engineFacade.getMemory();

		if (!memory.isValidAddress(address) || !memory.isValidAddress(address + 1)) {
			return;
		}

		short currentValue = memory.readWord(address);

		Integer newValue = valueEditorDialog.editMemory(address, currentValue);

		if (newValue != null) {
			// Apply the new value
			try {
				memory.writeWord(address, (short) newValue.intValue());
				editedMemoryAddresses.add(address);
				refresh();
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(
						this,
						"Failed to set memory: " + ex.getMessage(),
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Clears manual edit tracking
	 */
	public void clearEditTracking() {
		editedRegisters.clear();
		editedMemoryAddresses.clear();
	}
}