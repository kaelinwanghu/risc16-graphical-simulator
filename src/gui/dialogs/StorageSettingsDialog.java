package gui.dialogs;

import gui.Simulator;
import gui.components.CacheSettings;
import gui.components.MemorySettings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class StorageSettingsDialog extends JDialog {
	
	private Simulator simulator;
	private JComboBox<String> levels;
	private MemorySettings memorySettings;
	private CacheSettings l1Instruction;
	private CacheSettings l1Data;
	private CacheSettings l2Data;
	private CacheSettings l3Data;
	private int selectedLevel;
	
	public StorageSettingsDialog(Simulator simulator) {
		super(simulator, "Storage Settings", true);
		
		this.simulator = simulator;
		
		setIconImage(simulator.getIconImage());
		
		memorySettings = new MemorySettings();
		l1Instruction = new CacheSettings("L1 Instruction Cache", false);
		l1Data = new CacheSettings("L1 Data Cache", true);
		l2Data = new CacheSettings("L2 Data Cache", true);
		l2Data.setEnabled(false);
		l3Data = new CacheSettings("L3 Data Cache", true);
		l3Data.setEnabled(false);
		
		memorySettings.setConfiguration(new int[]{1, 1, 0, 32, 100});
		l1Instruction.setConfiguration(new int[]{2, 0, 8, 8, 5});
		l1Data.setConfiguration(new int[]{4, 0, 16, 1, 5, 0, 0});
		
		levels = new JComboBox<String>(new String[]{"1 Level", "2 Levels", "3 Levels"});
		levels.addItemListener(new ItemListener() {
			
			public void itemStateChanged(ItemEvent arg0) {
				l2Data.setEnabled(levels.getSelectedIndex() > 0);
				l3Data.setEnabled(levels.getSelectedIndex() > 1);
			}
			
		});
		levels.setFocusable(false);
		
		JLabel label1 = new JLabel("Data Cache");
		label1.setFont(new Font("Consolas", Font.PLAIN, 19));
		label1.setForeground(Color.RED);
		
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		p1.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(7, 0, 7, 0)));
		p1.add(label1);
		p1.add(levels);
		
		JPanel p = new JPanel(new GridBagLayout());
		p.add(p1);

		JPanel p2 = new JPanel(new GridLayout(1, 3, 10, 0));
		p2.add(memorySettings);
		p2.add(p);
		p2.add(l1Instruction);
		
		JPanel p3 = new JPanel(new GridLayout(1, 3, 10, 0));
		p3.add(l1Data);
		p3.add(l2Data);
		p3.add(l3Data);
		
		JButton exit = new JButton("Exit");
		exit.setFocusable(false);
		exit.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				exit();
			}
			
		});
		
		addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				exit();
			}
			
		});
		
		JButton apply = new JButton("Apply");
		apply.setFocusable(false);
		apply.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				apply();
			}
			
		});
		
		JPanel p4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p4.add(apply);
		p4.add(exit);
		p4.setBorder(BorderFactory.createEmptyBorder(7, 0, 5, 0));
		
		JPanel p5 = new JPanel(new BorderLayout(0, 5));
		p5.add(p2);
		p5.add(p3, BorderLayout.SOUTH);
		p5.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		
		JPanel p6 = new JPanel(new BorderLayout(0, 0));
		p6.add(p5);
		p6.add(p4, BorderLayout.SOUTH);
		p6.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		add(p6);
		
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
	}
	
	private void apply() {
		int[][] config = new int[3 + levels.getSelectedIndex()][];
		try {
			config[0] = memorySettings.getConfiguration();
			config[1] = l1Instruction.getConfiguration();
			config[2] = l1Data.getConfiguration();
			if (levels.getSelectedIndex() > 0) 
				config[3] = l2Data.getConfiguration();
			if (levels.getSelectedIndex() > 1) 
				config[4] = l3Data.getConfiguration();
		} catch (Exception ex) {
			simulator.errorDialog.showError("Invalid/Missing input");
			return;
		}
		int[][] newConfig = new int[config.length][6];
		for (int i = 0; i < newConfig.length; i++) {
			newConfig[i][0] = config[i][0] * (int)Math.pow(1024, config[i][1]);
			newConfig[i][1] = config[i][2];
			newConfig[i][2] = config[i][3];
			newConfig[i][3] = config[i][4];
			if (config[i].length > 5) {
				newConfig[i][4] = config[i][5];
				newConfig[i][5] = config[i][6] + 2;
			}
		}
		try {
			Simulator.processor.configureStorage(newConfig);
			Simulator.processor.clear();
			memorySettings.setConfiguration(config[0]);
			l1Instruction.setConfiguration(config[1]);
			l1Data.setConfiguration(config[2]);
			if (levels.getSelectedIndex() > 0) 
				l2Data.setConfiguration(config[3]);
			if (levels.getSelectedIndex() > 1) 
				l3Data.setConfiguration(config[4]);
			selectedLevel = levels.getSelectedIndex();
		} catch (Exception ex) {
			simulator.errorDialog.showError(ex.getMessage());
			return;
		}
		simulator.storageViewer.refreshTypes();
		simulator.edit(false);
		setVisible(false);
	}
	
	private void exit() {
		memorySettings.refresh();
		l1Instruction.refresh();
		l1Data.refresh();
		l2Data.refresh();
		l3Data.refresh();
		levels.setSelectedIndex(selectedLevel);
		l2Data.setEnabled(selectedLevel > 0);
		l3Data.setEnabled(selectedLevel > 1);
		
		setVisible(false);
	}
	
}
