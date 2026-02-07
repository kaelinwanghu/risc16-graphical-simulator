package gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Minimalist dialog for editing register or memory values during debugging
 */
public class ValueEditorDialog extends JDialog {

    public enum EditType {
        REGISTER,
        MEMORY
    }

    private JTextField valueField;
    private JLabel infoLabel;
    private boolean confirmed;
    private int parsedValue;

    public ValueEditorDialog(JFrame parent) {
        super(parent, "Edit Value", true);

        setUndecorated(false); // Keep window decorations for clarity

        initializeComponents();
        layoutComponents();

        setSize(300, 240);
        setResizable(false);
    }

    private void initializeComponents() {
        infoLabel = new JLabel();
        infoLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        infoLabel.setForeground(new Color(200, 0, 0));

        valueField = new JTextField(15);
        valueField.setFont(new Font("Consolas", Font.PLAIN, 16));
        valueField.setHorizontalAlignment(JTextField.CENTER);

        // Enter key confirms
        valueField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirm();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
            }
        });
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Info at top
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.add(infoLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Value field in center with label
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JLabel promptLabel = new JLabel("New value:");
        promptLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        centerPanel.add(promptLabel);
        centerPanel.add(valueField);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Hint label
        JLabel hintLabel = new JLabel("(hex: 0x..., decimal, or octal: 0...)");
        hintLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        hintLabel.setForeground(Color.GRAY);
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hintPanel.add(hintLabel);

        // Buttons at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.addActionListener(e -> confirm());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> cancel());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Combine hint and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(hintPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Shows the dialog to edit a register
     * 
     * @param regNum       the register number (0-7)
     * @param currentValue the current value in the register
     * @return the new value, or null if cancelled
     */
    public Integer editRegister(int regNum, short currentValue) {
        infoLabel.setText(String.format("Editing R%d", regNum));
        valueField.setText(String.format("0x%04X", currentValue & 0xFFFF));
        return showDialogAndGetValue();
    }

    /**
     * Shows the dialog to edit a memory word
     * 
     * @param address      the memory address
     * @param currentValue the current value at that address
     * @return the new value, or null if cancelled
     */
    public Integer editMemory(int address, short currentValue) {
        infoLabel.setText(String.format("Editing Memory[0x%04X]", address));
        valueField.setText(String.format("0x%04X", currentValue & 0xFFFF));
        return showDialogAndGetValue();
    }

    private Integer showDialogAndGetValue() {
        confirmed = false;
        parsedValue = 0;

        setLocationRelativeTo(getParent());

        // Select all text for easy replacement
        SwingUtilities.invokeLater(() -> {
            valueField.selectAll();
            valueField.requestFocus();
        });

        setVisible(true); // Blocks until closed

        return confirmed ? parsedValue : null;
    }

    private void confirm() {
        String input = valueField.getText().trim();

        if (input.isEmpty()) {
            showError("Please enter a value");
            return;
        }

        try {
            parsedValue = parseValue(input);
            // Validate 16-bit range
            if (parsedValue < -32768 || parsedValue > 65535) {
                showError("Value must be in range [-32768, 65535] or [0x0000, 0xFFFF]");
                return;
            }

            confirmed = true;
            setVisible(false);

        } catch (NumberFormatException e) {
            showError("Invalid number format. Use decimal, hex (0x...), or octal (0...)");
        }
    }

    private void cancel() {
        confirmed = false;
        setVisible(false);
    }

    /**
     * Parses a value in hex, decimal, or octal format
     */
    private int parseValue(String input) throws NumberFormatException {
        input = input.trim();

        // Hexadecimal: 0x... or 0X...
        if (input.toLowerCase().startsWith("0x")) {
            return Integer.parseInt(input.substring(2), 16);
        }

        // Octal: 0... (but not just "0")
        if (input.length() > 1 && input.startsWith("0")) {
            return Integer.parseInt(input.substring(1), 8);
        }

        // Decimal (including negative)
        return Integer.parseInt(input);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE);
        valueField.selectAll();
        valueField.requestFocus();
    }
}