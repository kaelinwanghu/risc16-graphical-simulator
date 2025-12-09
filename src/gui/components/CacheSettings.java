package gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class CacheSettings extends JPanel {

	private SizeInputBox lineSize;
	private InputBox cacheLines;
	private InputBox associativity;
	private InputBox accessTime;
	private JComboBox<String> hitPolicy;
	private JComboBox<String> missPolicy;
	private int[] configuration;
	
	public CacheSettings(String title, boolean dataCache) {
		super(new GridLayout((dataCache)? 6 : 4, 1, 0, 5));
		
		Border b1 = BorderFactory.createTitledBorder(null, title, TitledBorder.LEFT, TitledBorder.TOP, 
				new Font("Consolas", Font.PLAIN, 19), Color.RED);
		setBorder(BorderFactory.createCompoundBorder(b1, BorderFactory.createEmptyBorder(5, 0, 5, 0)));
		
		lineSize = new SizeInputBox("Line Size", 125, 5);
		cacheLines = new InputBox("Cache Lines", 125, 5, "");
		associativity = new InputBox("Associativity", 125, 5, "");
		accessTime = new InputBox("Access Time", 125, 5, "Cycles");
		
		add(lineSize);
		add(cacheLines);
		add(associativity);
		add(accessTime);
		
		if (!dataCache) 
			return;
		
		JLabel l1 = new JLabel("Hit Policy");
		l1.setPreferredSize(new Dimension(125, l1.getPreferredSize().height));
		
		hitPolicy = new JComboBox<String>(new String[]{"Write Back", "Write Through"});
		hitPolicy.setPreferredSize(new Dimension(155, hitPolicy.getPreferredSize().height));
		hitPolicy.setFocusable(false);
		
		JLabel l2 = new JLabel("Miss Policy");
		l2.setPreferredSize(new Dimension(125, l2.getPreferredSize().height));
		
		missPolicy = new JComboBox<String>(new String[]{"Write Allocate", "Write Around"});
		missPolicy.setPreferredSize(new Dimension(155, missPolicy.getPreferredSize().height));
		missPolicy.setFocusable(false);
		
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		p1.add(l1);
		p1.add(hitPolicy);
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		p2.add(l2);
		p2.add(missPolicy);
		
		add(p1);
		add(p2);
	}
	
	public int[] getConfiguration(){
		if (hitPolicy == null)
			return new int[]{lineSize.getInput()[0], lineSize.getInput()[1], cacheLines.getValue(), 
				associativity.getValue(), accessTime.getValue()};
		
		return new int[]{lineSize.getInput()[0], lineSize.getInput()[1], cacheLines.getValue(), 
				associativity.getValue(), accessTime.getValue(), hitPolicy.getSelectedIndex(), 
				missPolicy.getSelectedIndex()};
	}
	
	public void setConfiguration(int[] configuration) {
		this.configuration = configuration;
		refresh();
	}
	
	public void refresh() {
		if (configuration == null) {
			lineSize.clear();
			cacheLines.clear();
			associativity.clear();
			accessTime.clear();
		} else {
			lineSize.setInput(configuration[0], configuration[1]);
			cacheLines.setInput(configuration[2]);
			associativity.setInput(configuration[3]);
			accessTime.setInput(configuration[4]);
		}
		
		if (hitPolicy == null)
			return;
		
		hitPolicy.setSelectedIndex((configuration == null)? 0 : configuration[5]);
		missPolicy.setSelectedIndex((configuration == null)? 0 : configuration[6]);
	}
	
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			lineSize.clear();
			cacheLines.clear();
			associativity.clear();
			accessTime.clear();
		}
		
		lineSize.setEnabled(enabled);
		cacheLines.setEnabled(enabled);
		associativity.setEnabled(enabled);
		accessTime.setEnabled(enabled);
		hitPolicy.setEnabled(enabled);
		missPolicy.setEnabled(enabled);
	}
	
}
