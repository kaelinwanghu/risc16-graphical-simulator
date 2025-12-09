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
	private InputBox programAddress;
	private InputBox dataAddress;
	private InputBox accessTime;
	private int[] configuration;
	
	public MemorySettings() {
		super(new GridLayout(4, 1, 0, 5));
		
		Border b1 = BorderFactory.createTitledBorder(null, "Memory", TitledBorder.LEFT, TitledBorder.TOP, 
				new Font("Consolas", Font.PLAIN, 19), Color.RED);
		setBorder(BorderFactory.createCompoundBorder(b1, BorderFactory.createEmptyBorder(5, 0, 5, 0)));
		
		size = new SizeInputBox("Size", 140, 5);
		programAddress = new InputBox("Program Address", 140, 5, "");
		dataAddress = new InputBox("Data Address", 140, 5, "");
		accessTime = new InputBox("Access Time", 140, 5, "Cycles");
		
		add(size);
		add(programAddress);
		add(dataAddress);
		add(accessTime);
	}
	
	public int[] getConfiguration(){
		return new int[]{size.getInput()[0], size.getInput()[1], programAddress.getValue(), dataAddress.getValue(), accessTime.getValue()};
	}
	
	public void setConfiguration(int[] configuration) {
		this.configuration = configuration;
		refresh();
	}
	
	public void refresh() {
		size.setInput(configuration[0], configuration[1]);
		programAddress.setInput(configuration[2]);
		dataAddress.setInput(configuration[3]);
		accessTime.setInput(configuration[4]);
	}
	
}
