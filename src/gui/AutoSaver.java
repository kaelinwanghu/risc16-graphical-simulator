package gui;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Handles automatic saving of program text at regular intervals
 */
public class AutoSaver
{
	private static final int AUTO_SAVE_INTERVAL_MS = 30000; // 30 seconds
	
	private Timer timer;
	private FileManager fileManager;
	private InputPanel inputPanel;
	private Simulator simulator;
	private boolean enabled;
	
	public AutoSaver(FileManager fileManager, InputPanel inputPanel, Simulator simulator)
	{
		this.fileManager = fileManager;
		this.inputPanel = inputPanel;
		this.simulator = simulator;
		this.enabled = true;
		
		// 30 second timer
		timer = new Timer(AUTO_SAVE_INTERVAL_MS, e -> {
			if (enabled)
			{
				performAutoSave();
			}
		});
	}
	
	/**
	 * Starts the auto-save timer
	 */
	public void start()
	{
		timer.start();
	}
	
	/**
	 * Stops the auto-save timer
	 */
	public void stop()
	{
		timer.stop();
	}
	
	/**
	 * Enables or disables auto-save
	 */
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	/**
	 * Performs an auto-save operation
	 */
	private void performAutoSave()
	{
		try
		{
			String content = inputPanel.getProgram();
			fileManager.autoSave(content);
			simulator.setModified(false);
			System.out.println("Auto-saved at " + new java.util.Date());
		}
		catch (Exception e)
		{
			// Silent fail - don't interrupt user
			System.err.println("Auto-save error: " + e.getMessage());
		}
	}
	
	/**
	 * Forces an immediate auto-save (useful before risky operations)
	 */
	public void saveNow()
	{
		if (enabled)
		{
			performAutoSave();
		}
	}
}