package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles all file I/O operations for the RiSC-16 simulator
 */
public class FileManager
{
	private static final String FILE_EXTENSION = "as16";
	private static final String FILE_DESCRIPTION = "RiSC-16 Assembly Files (*.as16)";
	
	private JFrame parent;
	private File currentFile;
	private JFileChooser fileChooser;
	
	public FileManager(JFrame parent)
	{
		this.parent = parent;
		this.currentFile = null;
		
		// Setup file chooser with filter
		fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(FILE_DESCRIPTION, FILE_EXTENSION);
		fileChooser.setFileFilter(filter);
		
		// Set default directory to user's home/Documents
		String userHome = System.getProperty("user.home");
		File defaultDir = new File(userHome, "Documents");
		if (defaultDir.exists())
		{
			fileChooser.setCurrentDirectory(defaultDir);
		}
	}
	
	/**
	 * Opens a file chooser and loads the selected file
	 * @return The loaded program text, or null if cancelled
	 */
	public String openFile()
	{
		int result = fileChooser.showOpenDialog(parent);
		
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			return loadFile(file);
		}
		
		return null;
	}
	
	/**
	 * Loads a file from disk
	 * @param file The file to load
	 * @return The file contents, or null on error
	 */
	public String loadFile(File file)
	{
		try
		{
			Path path = file.toPath();
			String content = Files.readString(path);
			currentFile = file;
			return content;
		}
		catch (IOException e)
		{
			showError("Failed to load file: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Saves to the current file, or prompts for location if no file is set
	 * @param content The program text to save
	 * @return true if saved successfully
	 */
	public boolean save(String content)
	{
		if (currentFile == null)
		{
			return saveAs(content);
		}
		
		return writeToFile(currentFile, content);
	}
	
	/**
	 * Prompts for a save location and saves the file
	 * @param content The program text to save
	 * @return true if saved successfully
	 */
	public boolean saveAs(String content)
	{
		int result = fileChooser.showSaveDialog(parent);
		
		if (result == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			
			// Ensure .as16 extension
			if (!file.getName().toLowerCase().endsWith("." + FILE_EXTENSION))
			{
				file = new File(file.getAbsolutePath() + "." + FILE_EXTENSION);
			}
			
			boolean success = writeToFile(file, content);
			if (success)
			{
				currentFile = file;
			}
			return success;
		}
		
		return false;
	}
	
	/**
	 * Writes content to a file
	 * @param file The file to write to
	 * @param content The content to write
	 * @return true if successful
	 */
	private boolean writeToFile(File file, String content)
	{
		try (FileWriter writer = new FileWriter(file))
		{
			writer.write(content);
			return true;
		}
		catch (IOException e)
		{
			showError("Failed to save file: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Saves to current file if it exists, otherwise to the autosave
	 * @param content The content to auto-save
	 */
	public void autoSave(String content)
	{
		if (content == null || content.trim().isEmpty())
		{
			return; // Don't auto-save empty content
		}
		
		// If working on a named file, save to that file
		if (currentFile != null)
		{
			writeToFile(currentFile, content);
			return;
		}
		
		// Otherwise, save to auto-save location
		try
		{
			// Create auto-save directory in user's home
			Path autoSaveDir = Paths.get(System.getProperty("user.home"), ".risc16-simulator");
			Files.createDirectories(autoSaveDir);
			
			// Auto-save file
			File autoSaveFile = autoSaveDir.resolve("autosave.as16").toFile();
			writeToFile(autoSaveFile, content);
		}
		catch (IOException e)
		{
			// Silent fail for auto-save - don't interrupt user
			System.out.println("Auto-save failed: " + e.getMessage());
		}
	}	
	/**
	 * Attempts to recover from auto-save
	 * @return The recovered content, or null if no auto-save exists
	 */
	public String recoverAutoSave()
	{
		try
		{
			Path autoSavePath = Paths.get(System.getProperty("user.home"), 
				".risc16-simulator", "autosave.as16");
			File autoSaveFile = autoSavePath.toFile();
			
			if (autoSaveFile.exists())
			{
				String content = Files.readString(autoSavePath);
				if (!content.trim().isEmpty())
				{
					return content;
				}
			}
		}
		catch (IOException e)
		{
			// No recovery available
		}
		
		return null;
	}
	
	/**
	 * Deletes the auto-save file (call after successful manual save)
	 */
	public void clearAutoSave()
	{
		try
		{
			Path autoSavePath = Paths.get(System.getProperty("user.home"), 
				".risc16-simulator", "autosave.as16");
			Files.deleteIfExists(autoSavePath);
		}
		catch (IOException e)
		{
			// Ignore
		}
	}
	
	/**
	 * Creates a new file (clears current file reference)
	 */
	public void newFile()
	{
		currentFile = null;
	}
	
	/**
	 * Gets the current file
	 */
	public File getCurrentFile()
	{
		return currentFile;
	}
	
	/**
	 * Gets the current filename for display
	 */
	public String getCurrentFileName()
	{
		if (currentFile == null)
		{
			return "Untitled";
		}
		return currentFile.getName();
	}
	
	/**
	 * Shows an error dialog
	 */
	private void showError(String message)
	{
		if (parent instanceof Simulator)
		{
			((Simulator) parent).errorDialog.showError(message);
		}
	}
}