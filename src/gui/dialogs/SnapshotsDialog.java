package gui.dialogs;

import engine.debug.DebugManager;
import engine.debug.ExecutionSnapshot;
import engine.execution.ProcessorState;
import gui.Simulator;
import gui.facade.EngineFacade;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

@SuppressWarnings("serial")
public class SnapshotsDialog extends JDialog {

    private final Simulator simulator;
    private final EngineFacade engineFacade;

    private JTable snapshotTable;
    private DefaultTableModel tableModel;
    private JButton restoreButton;
    private JButton deleteButton;
    private JButton clearAllButton;
    private JButton closeButton;
    private JLabel infoLabel;

    public SnapshotsDialog(Simulator simulator, EngineFacade engineFacade) {
        super(simulator, "Execution Snapshots", false); // Non-modal

        this.simulator = simulator;
        this.engineFacade = engineFacade;

        setIconImage(simulator.getIconImage());

        initializeComponents();
        layoutComponents();

        setSize(800, 500);
        setLocationRelativeTo(simulator);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initializeComponents() {
        // Table for snapshots
        String[] columnNames = { "#", "Time", "PC", "Instructions", "Description" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }
        };

        snapshotTable = new JTable(tableModel);
        snapshotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snapshotTable.setFont(new Font("Consolas", Font.PLAIN, 14));
        snapshotTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 14));
        snapshotTable.setRowHeight(22);

        // Set column widths
        TableColumnModel columnModel = snapshotTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40); // #
        columnModel.getColumn(1).setPreferredWidth(100); // Time
        columnModel.getColumn(2).setPreferredWidth(80); // PC
        columnModel.getColumn(3).setPreferredWidth(100); // Instructions
        columnModel.getColumn(4).setPreferredWidth(400); // Description

        // Double-click to restore
        snapshotTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && simulator.isSnapshotRestorationAllowed()) {
                    restoreSelectedSnapshot();
                } else if (e.getClickCount() == 2) {
                    // Show message if double-clicked but can't restore
                    JOptionPane.showMessageDialog(
                            SnapshotsDialog.this,
                            "Cannot restore snapshot while program is halted or not loaded.\n" +
                                    "Please assemble and start executing a program first.",
                            "Cannot Restore",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        // Selection listener for button states
        snapshotTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Info label
        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(100, 100, 100));

        // Buttons
        restoreButton = new JButton("Restore Selected");
        restoreButton.setEnabled(false);
        restoreButton.addActionListener(e -> restoreSelectedSnapshot());

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedSnapshot());

        clearAllButton = new JButton("Clear All");
        clearAllButton.addActionListener(e -> clearAllSnapshots());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
    }

    private void layoutComponents() {
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title label
        JLabel titleLabel = new JLabel("Execution State Snapshots");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        titleLabel.setForeground(Color.RED);

        // Table scroll pane
        JScrollPane scrollPane = new JScrollPane(snapshotTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        infoPanel.add(infoLabel, BorderLayout.WEST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(restoreButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearAllButton);
        buttonPanel.add(closeButton);

        // Assemble
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Refreshes the snapshot table with current data
     */
    public void refreshTable() {
        // Clear existing rows
        tableModel.setRowCount(0);

        DebugManager debugManager = engineFacade.getDebugManager();

        if (!debugManager.isEnabled()) {
            infoLabel.setText("Debugging is disabled. Enable in Settings to capture snapshots.");
            updateButtonStates();
            return;
        }

        List<ExecutionSnapshot> snapshots = debugManager.getSnapshots();

        // Populate table
        for (int i = 0; i < snapshots.size(); i++) {
            ExecutionSnapshot snapshot = snapshots.get(i);
            ProcessorState state = snapshot.getState();

            Object[] row = {
                    i + 1, // Index (1-based for display)
                    snapshot.getFormattedTimestamp(),
                    String.format("0x%04X", state.getPC()),
                    state.getInstructionCount(),
                    snapshot.getDescription()
            };

            tableModel.addRow(row);
        }

        // Update info label
        int count = snapshots.size();
        int limit = debugManager.getSnapshotLimit();
        infoLabel.setText(String.format("Showing %d snapshot%s (limit: %d)",
                count, count == 1 ? "" : "s", limit));

        updateButtonStates();
    }

    /**
     * Updates button enabled states based on selection and snapshot count
     */
    private void updateButtonStates() {
        boolean hasSelection = snapshotTable.getSelectedRow() != -1;
        boolean hasSnapshots = tableModel.getRowCount() > 0;
        boolean canRestore = simulator.isSnapshotRestorationAllowed();

        restoreButton.setEnabled(hasSelection && canRestore);
        deleteButton.setEnabled(hasSelection);
        clearAllButton.setEnabled(hasSnapshots);
    }

    /**
     * Restores the selected snapshot
     */
    private void restoreSelectedSnapshot() {
        int selectedRow = snapshotTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        if (!simulator.isSnapshotRestorationAllowed()) {
            JOptionPane.showMessageDialog(
                    this,
                    """
                            Cannot restore snapshot:

                            - Program must be assembled and loaded
                            - Program must not be halted
                            - Program must be in execution mode (not edit mode)

                            Please assemble a program and begin execution before restoring snapshots.
                            """,
                    "Cannot Restore",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ExecutionSnapshot snapshot = engineFacade.getDebugManager().getSnapshot(selectedRow);

            // Confirm with user
            String message = String.format(
                    """
                            Restore execution state to:

                            PC: 0x%04X
                            Instructions: %d
                            Time: %s

                            This will overwrite current processor state and memory.
                            """,
                    snapshot.getState().getPC(),
                    snapshot.getState().getInstructionCount(),
                    snapshot.getFormattedTimestamp());

            int result = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Restore Snapshot",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                engineFacade.restoreSnapshot(snapshot);

                JOptionPane.showMessageDialog(
                        this,
                        "Snapshot restored successfully.",
                        "Snapshot Restored",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog after successful restore
                setVisible(false);
            }

        } catch (IllegalStateException ex) {
            // Handle state validation errors
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot restore snapshot:\n" + ex.getMessage(),
                    "Restoration Error",
                    JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to restore snapshot: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Clears all snapshots
     */
    private void clearAllSnapshots() {
        if (tableModel.getRowCount() == 0) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Clear all snapshots? This cannot be undone.",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            engineFacade.getDebugManager().clearSnapshots();
            refreshTable();

            JOptionPane.showMessageDialog(
                    this,
                    "All snapshots cleared.",
                    "Snapshots Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Deletes the selected snapshot
     */
    private void deleteSelectedSnapshot() {
        int selectedRow = snapshotTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete this snapshot?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                engineFacade.getDebugManager().deleteSnapshot(selectedRow);
                refreshTable();

                JOptionPane.showMessageDialog(
                        this,
                        "Snapshot deleted.",
                        "Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to delete snapshot: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows the dialog and refreshes content
     */
    public void showDialog() {
        refreshTable();
        setVisible(true);
    }
}