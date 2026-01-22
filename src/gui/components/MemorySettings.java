package gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class MemorySettings extends JPanel {

	private SizeInputBox size;
	private int[] configuration;
	
	public MemorySettings() {
		super(new GridLayout(1, 1, 0, 5));
		
		Border b1 = BorderFactory.createTitledBorder(null, "Memory", TitledBorder.LEFT, TitledBorder.TOP, 
				new Font("Consolas", Font.PLAIN, 19), Color.RED);
		setBorder(BorderFactory.createCompoundBorder(b1, BorderFactory.createEmptyBorder(5, 0, 5, 0)));
		
		size = new SizeInputBox("Size", 140, 5);
		
		add(size);
	}
	
	/**
	 * Gets the configuration as [size, unit]
	 * where unit is 0=Bytes, 1=KiB, 2=MiB
	 */
	public int[] getConfiguration() {
		return new int[]{size.getInput()[0], size.getInput()[1]};
	}
	
	/**
	 * Sets the configuration
	 * 
	 * @param configuration [size, unit]
	 */
	public void setConfiguration(int[] configuration) {
		this.configuration = configuration;
		refresh();
	}
	
	public void refresh() {
		if (configuration != null && configuration.length >= 2) {
			size.setInput(configuration[0], configuration[1]);
		}
	}
}