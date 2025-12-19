package gui.dialogs;

import gui.Simulator;
import gui.components.InputBox;
import gui.components.MemorySettings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class StorageSettingsDialog extends JDialog {
	
	private Simulator simulator;
	private MemorySettings memorySettings;
	private InputBox instructionLimit;
	
	public StorageSettingsDialog(Simulator simulator) {
		super(simulator, "Storage Settings", true);
		
		this.simulator = simulator;
		
		setIconImage(simulator.getIconImage());
		
		memorySettings = new MemorySettings();
		memorySettings.setConfiguration(new int[]{1, 1, 0, 32, 100});
		
		instructionLimit = new InputBox("Instruction Limit", 140, 10, "");
		instructionLimit.setInput(65535); // 2^16 - 1 as default
		
		JLabel execLabel = new JLabel("Execution Settings");
		execLabel.setFont(new Font("Consolas", Font.PLAIN, 19));
		execLabel.setForeground(Color.RED);
		
		JPanel executionPanel = new JPanel(new GridLayout(2, 1, 0, 5));
		executionPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder(null, "", TitledBorder.LEFT, TitledBorder.TOP), 
			BorderFactory.createEmptyBorder(5, 0, 5, 0)
		));
		executionPanel.add(execLabel);
		executionPanel.add(instructionLimit);
		
		JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
		mainPanel.add(memorySettings);
		mainPanel.add(executionPanel);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
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
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		buttonPanel.add(apply);
		buttonPanel.add(exit);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		
		// Assemble dialog
		JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
		contentPanel.add(mainPanel, BorderLayout.CENTER);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		add(contentPanel);
		
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
	}
	
	private void apply() {
		// Hardcode cache
		int[][] config = new int[5][];
		try {
			config[0] = memorySettings.getConfiguration();
			// Hardcoded L1 Instruction Cache: 2B line, 8 lines, 8-way, 5 cycles
			config[1] = new int[]{2, 0, 8, 8, 5};
			// Hardcoded L1 Data Cache: 4B line, 16 lines, direct-mapped, 5 cycles, write-back, write-allocate
			config[2] = new int[]{4, 0, 16, 1, 5, 0, 0};
			// Hardcoded L2 Data Cache: 8B line, 32 lines, 2-way, 10 cycles, write-back, write-allocate
			config[3] = new int[]{8, 0, 32, 2, 10, 0, 0};
			// Hardcoded L3 Data Cache: 16B line, 64 lines, 4-way, 20 cycles, write-back, write-allocate
			config[4] = new int[]{16, 0, 64, 4, 20, 0, 0};
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
			
			// Apply instruction limit (Note: Currently bad)
			int limit = instructionLimit.getValue();
			if (limit < 1) {
				simulator.errorDialog.showError("Instruction limit must be at least 1");
				return;
			}
			Simulator.processor.setInstructionLimit(limit);
			
			Simulator.processor.clear();
			memorySettings.setConfiguration(config[0]);
		} catch (Exception ex) {
			simulator.errorDialog.showError(ex.getMessage());
			return;
		}
		
		simulator.edit(false);
		setVisible(false);
	}
	
	private void exit() {
		memorySettings.refresh();
		instructionLimit.setInput(Simulator.processor.getInstructionLimit());
		setVisible(false);
	}
	
}