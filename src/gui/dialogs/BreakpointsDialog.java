package gui.dialogs;

import engine.debug.Breakpoint;
import engine.debug.DebugManager;
import gui.Simulator;
import gui.facade.EngineFacade;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

@SuppressWarnings("serial")
public class BreakpointsDialog extends JDialog {
    
    private final Simulator simulator;
    private final EngineFacade engineFacade;
    
    private JTable breakpointTable;
    private DefaultTableModel tableModel;
    private JButton editButton;
    private JButton deleteButton;
    private JButton clearAllButton;
    private JButton closeButton;
    private JLabel infoLabel;
    
    public BreakpointsDialog(Simulator simulator, EngineFacade engineFacade) {
        super(simulator, "Breakpoints", false);
        
        this.simulator = simulator;
        this.engineFacade = engineFacade;
        
        setIconImage(simulator.getIconImage());
        
        initializeComponents();
        layoutComponents();
        
        setSize(700, 400);
        setLocationRelativeTo(simulator);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }
    
    private void initializeComponents() {
        // Table
        String[] columnNames = {"Line", "Enabled", "Type", "Condition"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 1 ? Boolean.class : String.class;
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;  // Only "Enabled" is editable
            }
        };
        
        breakpointTable = new JTable(tableModel);
        breakpointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        breakpointTable.setFont(new Font("Consolas", Font.PLAIN, 14));
        breakpointTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 14));
        breakpointTable.setRowHeight(22);
        
        // Column widths
        breakpointTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Line
        breakpointTable.getColumnModel().getColumn(1).setPreferredWidth(70);   // Enabled
        breakpointTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Type
        breakpointTable.getColumnModel().getColumn(3).setPreferredWidth(400);  // Condition
        
        // Handle enabled checkbox changes
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 1) {  // Enabled column
                int row = e.getFirstRow();
                int lineNumber = Integer.parseInt((String) tableModel.getValueAt(row, 0));
                boolean enabled = (Boolean) tableModel.getValueAt(row, 1);
                engineFacade.getDebugManager().setBreakpointEnabled(lineNumber, enabled);
            }
        });
        
        // Double-click to edit
        breakpointTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedBreakpoint();
                }
            }
        });
        
        // Selection listener
        breakpointTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        // Info label
        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        
        // Buttons
        editButton = new JButton("Edit Condition...");
        editButton.setEnabled(false);
        editButton.addActionListener(e -> editSelectedBreakpoint());
        
        deleteButton = new JButton("Delete");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedBreakpoint());
        
        clearAllButton = new JButton("Clear All");
        clearAllButton.addActionListener(e -> clearAllBreakpoints());
        
        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> BreakpointsDialog.this.setVisible(false));
    }
    
    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Breakpoint Management");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        titleLabel.setForeground(Color.RED);
        
        // Table
        JScrollPane scrollPane = new JScrollPane(breakpointTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        infoPanel.add(infoLabel, BorderLayout.WEST);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearAllButton);
        buttonPanel.add(closeButton);
        
        // Assembly
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.add(infoPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    public void refreshTable() {
        tableModel.setRowCount(0);
        
        DebugManager debugManager = engineFacade.getDebugManager();
        
        if (!debugManager.isEnabled()) {
            infoLabel.setText("Debugging is disabled.");
            updateButtonStates();
            return;
        }
        
        List<Breakpoint> breakpoints = debugManager.getAllBreakpoints();
        
        for (Breakpoint bp : breakpoints) {
            String type = bp.isConditional() ? "Conditional" : "Unconditional";
            String condition = bp.isConditional() ? bp.getDescription() : "Always break";
            
            tableModel.addRow(new Object[]{
                String.valueOf(bp.getLineNumber()),
                bp.isEnabled(),
                type,
                condition
            });
        }
        
        infoLabel.setText(String.format("Showing %d breakpoint%s", 
            breakpoints.size(), breakpoints.size() == 1 ? "" : "s"));
        
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean hasSelection = breakpointTable.getSelectedRow() != -1;
        boolean hasBreakpoints = tableModel.getRowCount() > 0;
        
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
        clearAllButton.setEnabled(hasBreakpoints);
    }
    
    private void editSelectedBreakpoint() {
        int selectedRow = breakpointTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        int lineNumber = Integer.parseInt((String) tableModel.getValueAt(selectedRow, 0));
        Breakpoint bp = engineFacade.getDebugManager().getBreakpoint(lineNumber);
        
        if (bp == null) {
            return;
        }
        
        // Show condition editor dialog
        BreakpointConditionDialog conditionDialog = new BreakpointConditionDialog(
            this, simulator, engineFacade, lineNumber, bp);
        conditionDialog.setVisible(true);
        
        // Refresh after editing
        refreshTable();
        simulator.getInputPanel().getLineNumberedTextArea().setBreakpoint(lineNumber, 
            engineFacade.getDebugManager().hasBreakpoint(lineNumber));
    }
    
    private void deleteSelectedBreakpoint() {
        int selectedRow = breakpointTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        int lineNumber = Integer.parseInt((String) tableModel.getValueAt(selectedRow, 0));
        
        engineFacade.getDebugManager().removeBreakpoint(lineNumber);
        simulator.getInputPanel().getLineNumberedTextArea().setBreakpoint(lineNumber, false);
        
        refreshTable();
    }
    
    private void clearAllBreakpoints() {
        if (tableModel.getRowCount() == 0) {
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "Clear all breakpoints?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            engineFacade.getDebugManager().clearBreakpoints();
            simulator.getInputPanel().getLineNumberedTextArea().clearBreakpoints();
            refreshTable();
        }
    }
    
    public void showDialog() {
        refreshTable();
        setVisible(true);
    }
}