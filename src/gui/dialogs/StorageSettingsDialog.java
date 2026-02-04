package gui.dialogs;

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

import gui.Simulator;
import gui.components.InputBox;
import gui.components.MemorySettings;
import gui.facade.EngineFacade;
import gui.StorageViewer;

@SuppressWarnings("serial")
public class StorageSettingsDialog extends JDialog {
	
	private Simulator simulator;
	private StorageViewer storageViewer;
	private MemorySettings memorySettings;
	private InputBox instructionLimit;
	private InputBox memoryViewStart;
	private InputBox memoryViewCount;
	private EngineFacade engineFacade;
	
	public StorageSettingsDialog(Simulator simulator, StorageViewer storageViewer, EngineFacade engineFacade) {
		super(simulator, "Storage Settings", true);
		
		this.simulator = simulator;
		this.engineFacade = engineFacade;
		this.storageViewer = storageViewer;
		
		setIconImage(simulator.getIconImage());
		
		// Memory size configuration
		memorySettings = new MemorySettings();
		memorySettings.setConfiguration(new int[]{engineFacade.getProcessor().getMemorySize(), 0}); // Default: 1 KiB
		
		// Instruction limit configuration
		instructionLimit = new InputBox("Instruction Limit", 180, 10, "");
		instructionLimit.setInput(engineFacade.getInstructionLimit()); // Default: 65,535

		memoryViewStart = new InputBox("Memory View Start", 180, 10, "(address)");
		memoryViewStart.setInput(storageViewer.getMemoryViewStart());
    	memoryViewCount = new InputBox("Memory View Count", 180, 10, "(words)");
		memoryViewCount.setInput(storageViewer.getMemoryViewCount());
		
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
		
		JLabel memViewLabel = new JLabel("Memory Viewer");
		memViewLabel.setFont(new Font("Consolas", Font.PLAIN, 19));
		memViewLabel.setForeground(Color.RED);
		JPanel memViewPanel = new JPanel(new GridLayout(3, 1, 0, 5));
    	memViewPanel.setBorder(BorderFactory.createCompoundBorder(
        	BorderFactory.createTitledBorder(null, "", TitledBorder.LEFT, TitledBorder.TOP), 
        	BorderFactory.createEmptyBorder(5, 0, 5, 0)
    	));
		memViewPanel.add(memViewLabel);
    	memViewPanel.add(memoryViewStart);
    	memViewPanel.add(memoryViewCount);

		JPanel mainPanel = new JPanel(new GridLayout(1, 3, 30, 0));
		mainPanel.add(memorySettings);
		mainPanel.add(executionPanel);
		mainPanel.add(memViewPanel);
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
		// Get memory configuration
		int[] config;
		try {
			config = memorySettings.getConfiguration();
		} catch (Exception ex) {
			simulator.messageDialog.showError("Invalid/Missing memory size");
			return;
		}
		
		// Calculate memory size in bytes
		int memorySize = config[0] * (int)Math.pow(1024, config[1]);
		
		// Validate memory size is a power of 2
		if ((memorySize & (memorySize - 1)) != 0) {
			simulator.messageDialog.showError("Memory size must be a power of 2");
			return;
		}
				
		if (memorySize < 128 || memorySize > 4 * 1024 * 1024) {
			simulator.messageDialog.showError("Memory size must be between 128 bytes and 4 MiB");
			return;
		}
		
		// Get instruction limit
		int limit;
		try {
			limit = instructionLimit.getValue();
			if (limit < 1) {
				simulator.messageDialog.showError("Instruction limit must be at least 1");
				return;
			}
		} catch (Exception ex) {
			simulator.messageDialog.showError("Invalid instruction limit");
			return;
		}

		int viewStart, viewCount;
    	try {
			viewStart = memoryViewStart.getValue();
			viewCount = memoryViewCount.getValue();
			
			if (viewStart < 0) {
				simulator.messageDialog.showError("Memory view start must be non-negative");
				return;
			}
			
			if (viewStart % 2 != 0) {
				simulator.messageDialog.showError("Memory view start must be word-aligned (even address)");
				return;
			}
			
			if (viewCount < 1 || viewCount > 65535) {
				simulator.messageDialog.showError("Memory view count must be between 1 and 65,535 words");
				return;
			}
		} catch (Exception ex) {
			simulator.messageDialog.showError("Invalid memory view settings");
			return;
		}
		
		// Check if memory size changed
		int currentSize = engineFacade.getMemory().getSize();
		if (memorySize != currentSize) {			
			// Create new facade with new memory size
			simulator.recreateEngineFacade(memorySize);
			memorySettings.setConfiguration(new int[]{memorySize / (int)Math.pow(1024, config[1]), config[1]});
			// Return to edit mode
			simulator.edit(false);
		}
		
		// Apply instruction limit (works regardless of memory change)
		engineFacade.setInstructionLimit(limit);
		
		storageViewer.setMemoryViewRange(viewStart, viewCount);

		setVisible(false);
	}	
	private void exit() {
		// Reset to current values
		int currentSize = engineFacade.getMemory().getSize();
		int currentLimit = engineFacade.getInstructionLimit();
		
		// Figure out the unit (bytes, KiB, MiB)
		int size, unit;
		if (currentSize % (1024 * 1024) == 0) {
			size = currentSize / (1024 * 1024);
			unit = 2; // MiB
		} else if (currentSize % 1024 == 0) {
			size = currentSize / 1024;
			unit = 1; // KiB
		} else {
			size = currentSize;
			unit = 0; // Bytes
		}
		
		memorySettings.setConfiguration(new int[]{size, unit});
		instructionLimit.setInput(currentLimit);
		memoryViewStart.setInput(storageViewer.getMemoryViewStart());
        memoryViewCount.setInput(storageViewer.getMemoryViewCount());
		
		setVisible(false);
	}

	public void setEngineFacade(EngineFacade engineFacade) {
		this.engineFacade = engineFacade;
	}
}