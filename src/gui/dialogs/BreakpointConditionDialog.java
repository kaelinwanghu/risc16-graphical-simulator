package gui.dialogs;

import engine.debug.Breakpoint;
import gui.Simulator;
import gui.facade.EngineFacade;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class BreakpointConditionDialog extends JDialog {
    
    private final Simulator simulator;
    private final EngineFacade engineFacade;
    private final int lineNumber;
    private final Breakpoint existingBreakpoint;
    
    private JRadioButton unconditionalRadio;
    private JRadioButton conditionalRadio;
    private JComboBox<Breakpoint.WatchType> watchTypeCombo;
    private JTextField watchTargetField;
    private JComboBox<Breakpoint.Operator> operatorCombo;
    private JTextField compareValueField;
    private JPanel conditionalPanel;
    
    public BreakpointConditionDialog(JDialog parent, Simulator simulator, EngineFacade engineFacade, 
                                     int lineNumber, Breakpoint existingBreakpoint) {
        super(parent, "Edit Breakpoint - Line " + lineNumber, true);
        
        this.simulator = simulator;
        this.engineFacade = engineFacade;
        this.lineNumber = lineNumber;
        this.existingBreakpoint = existingBreakpoint;
        
        initializeComponents();
        layoutComponents();
        loadExistingBreakpoint();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        // Radio buttons for type
        unconditionalRadio = new JRadioButton("Unconditional (always break)");
        conditionalRadio = new JRadioButton("Conditional");
        
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(unconditionalRadio);
        typeGroup.add(conditionalRadio);
        
        unconditionalRadio.setSelected(true);
        
        // Conditional breakpoint controls
        watchTypeCombo = new JComboBox<>(Breakpoint.WatchType.values());
        watchTargetField = new JTextField(10);
        operatorCombo = new JComboBox<>(Breakpoint.Operator.values());
        compareValueField = new JTextField(10);
        
        // Enable/disable conditional controls based on selection
        unconditionalRadio.addActionListener(e -> updateConditionalEnabled());
        conditionalRadio.addActionListener(e -> updateConditionalEnabled());
        
        // Update placeholder text based on watch type
        watchTypeCombo.addActionListener(e -> updateWatchTargetPlaceholder());
    }
    
    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Type selection
        JPanel typePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        typePanel.setBorder(BorderFactory.createTitledBorder("Breakpoint Type"));
        typePanel.add(unconditionalRadio);
        typePanel.add(conditionalRadio);
        
        // Conditional panel
        conditionalPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        conditionalPanel.setBorder(BorderFactory.createTitledBorder("Condition"));
        
        conditionalPanel.add(new JLabel("Watch:"));
        conditionalPanel.add(watchTypeCombo);
        
        conditionalPanel.add(new JLabel("Target:"));
        conditionalPanel.add(watchTargetField);
        
        conditionalPanel.add(new JLabel("Operator:"));
        conditionalPanel.add(operatorCombo);
        
        conditionalPanel.add(new JLabel("Value:"));
        conditionalPanel.add(compareValueField);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveBreakpoint());
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Assembly
        mainPanel.add(typePanel, BorderLayout.NORTH);
        mainPanel.add(conditionalPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        updateConditionalEnabled();
        updateWatchTargetPlaceholder();
    }
    
    private void loadExistingBreakpoint() {
        if (existingBreakpoint == null) {
            return;
        }
        
        if (existingBreakpoint.isConditional()) {
            conditionalRadio.setSelected(true);
            watchTypeCombo.setSelectedItem(existingBreakpoint.getWatchType());
            watchTargetField.setText(String.valueOf(existingBreakpoint.getWatchTarget()));
            operatorCombo.setSelectedItem(existingBreakpoint.getOperator());
            compareValueField.setText(String.valueOf(existingBreakpoint.getCompareValue()));
        } else {
            unconditionalRadio.setSelected(true);
        }
        
        updateConditionalEnabled();
    }
    
    private void updateConditionalEnabled() {
        boolean conditional = conditionalRadio.isSelected();
        watchTypeCombo.setEnabled(conditional);
        watchTargetField.setEnabled(conditional);
        operatorCombo.setEnabled(conditional);
        compareValueField.setEnabled(conditional);
    }
    
    private void updateWatchTargetPlaceholder() {
        Breakpoint.WatchType type = (Breakpoint.WatchType) watchTypeCombo.getSelectedItem();
        if (type == null) {
            return;
        }
        
        switch (type) {
            case REGISTER:
                watchTargetField.setToolTipText("Register number (0-7)");
                break;
            case MEMORY:
                watchTargetField.setToolTipText("Memory address (hex or decimal)");
                break;
            case PC:
            case INSTRUCTION_COUNT:
                watchTargetField.setEnabled(false);
                watchTargetField.setText("N/A");
                watchTargetField.setToolTipText("Not applicable for this watch type");
                return;
        }
        
        watchTargetField.setEnabled(conditionalRadio.isSelected());
        if (watchTargetField.getText().equals("N/A")) {
            watchTargetField.setText("");
        }
    }
    
    private void saveBreakpoint() {
        try {
            if (unconditionalRadio.isSelected()) {
                // Unconditional breakpoint
                engineFacade.getDebugManager().setBreakpoint(lineNumber);
                dispose();
                return;
            }
            
            // Conditional breakpoint - validate inputs
            Breakpoint.WatchType watchType = (Breakpoint.WatchType) watchTypeCombo.getSelectedItem();
            
            int watchTarget = 0;
            if (watchType == Breakpoint.WatchType.REGISTER || watchType == Breakpoint.WatchType.MEMORY) {
                String targetText = watchTargetField.getText().trim();
                if (targetText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a target value", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Parse as hex if starts with 0x, otherwise decimal
                if (targetText.toLowerCase().startsWith("0x")) {
                    watchTarget = Integer.parseInt(targetText.substring(2), 16);
                } else {
                    watchTarget = Integer.parseInt(targetText);
                }
                
                // Validate register range
                if (watchType == Breakpoint.WatchType.REGISTER && (watchTarget < 0 || watchTarget > 7)) {
                    JOptionPane.showMessageDialog(this, "Register must be 0-7", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            Breakpoint.Operator operator = (Breakpoint.Operator) operatorCombo.getSelectedItem();
            
            String compareText = compareValueField.getText().trim();
            if (compareText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a comparison value", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int compareValue;
            if (compareText.toLowerCase().startsWith("0x")) {
                compareValue = Integer.parseInt(compareText.substring(2), 16);
            } else {
                compareValue = Integer.parseInt(compareText);
            }
            
            // Set conditional breakpoint
            engineFacade.getDebugManager().setConditionalBreakpoint(lineNumber, watchType, watchTarget, operator, compareValue);
            dispose();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
}