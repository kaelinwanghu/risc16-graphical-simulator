package gui.components;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class InputBox extends JPanel {

	private JTextField input;
	
	public InputBox(String label, int labelWidth, int inputColumns, String text) {
		super(new FlowLayout(FlowLayout.LEFT, 10, 0));		
		input = new JTextField(inputColumns);
		
		JLabel l1 = new JLabel(label);
		l1.setPreferredSize(new Dimension(labelWidth, l1.getPreferredSize().height));
		
		add(l1);
		add(input);
		
		if (text != null)
			add(new JLabel(text));
	}
	
	public int getValue() {
		return Integer.parseInt(input.getText());
	}
	
	public void setInput(int number) {
		input.setText(number + "");
	}
	
	public void setEnabled(boolean enabled) {
		input.setEnabled(enabled);
	}
	
	public void clear() {
		input.setText("");
	}
	
}
