package gui.components;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class SizeInputBox extends JPanel {

	private JTextField input;
	private JComboBox<String> type;
	
	public SizeInputBox(String label, int labelWidth, int inputColumns) {
		super(new FlowLayout(FlowLayout.LEFT, 10, 0));
		type = new JComboBox<String>(new String[]{"B", "KiB", "MiB"});
		type.setFocusable(false);
		
		input = new JTextField(inputColumns);
		
		JLabel label1 = new JLabel(label);
		label1.setPreferredSize(new Dimension(labelWidth, label1.getPreferredSize().height));
		
		add(label1);
		add(input);
		add(type);
	}
	
	public int[] getInput() {
		return new int[]{Integer.parseInt(input.getText()), type.getSelectedIndex()};
	}
	
	public void setInput(int number, int value) {
		input.setText(number + "");
		type.setSelectedIndex(value);
	}
	
	public void clear() {
		input.setText("");
		type.setSelectedIndex(0);
	}
	
	public void setEnabled(boolean enabled) {
		input.setEnabled(enabled);
		type.setEnabled(enabled);
	}
	
}
