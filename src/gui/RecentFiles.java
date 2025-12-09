package gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages the list of recently opened files
 */
public class RecentFiles
{
	private static final int MAX_RECENT_FILES = 5;
	private static final String PREF_KEY_PREFIX = "recent_file_";
	
	private Preferences prefs;
	private List<File> recentFiles;
	
	public RecentFiles()
	{
		// Use Java Preferences API to persist recent files
		prefs = Preferences.userNodeForPackage(RecentFiles.class);
		recentFiles = new ArrayList<>();
		loadRecentFiles();
	}
	
	/**
	 * Adds a file to the recent files list
	 */
	public void addFile(File file)
	{
		if (file == null || !file.exists())
		{
			return;
		}
		
		// Remove if already in list (will be re-added at top)
		recentFiles.remove(file);
		
		// Add to front of list
		recentFiles.add(0, file);
		
		// Trim to max size
		while (recentFiles.size() > MAX_RECENT_FILES)
		{
			recentFiles.remove(recentFiles.size() - 1);
		}
		
		// Save to preferences
		saveRecentFiles();
	}
	
	/**
	 * Gets the list of recent files
	 */
	public List<File> getRecentFiles()
	{
		// Filter out files that no longer exist
		List<File> existing = new ArrayList<>();
		for (File file : recentFiles)
		{
			if (file.exists())
			{
				existing.add(file);
			}
		}
		
		// Update list if any were removed
		if (existing.size() != recentFiles.size())
		{
			recentFiles = existing;
			saveRecentFiles();
		}
		
		return new ArrayList<>(recentFiles);
	}
	
	/**
	 * Clears the recent files list
	 */
	public void clear()
	{
		recentFiles.clear();
		saveRecentFiles();
	}
	
	/**
	 * Loads recent files from preferences
	 */
	private void loadRecentFiles()
	{
		recentFiles.clear();
		
		for (int i = 0; i < MAX_RECENT_FILES; i++)
		{
			String path = prefs.get(PREF_KEY_PREFIX + i, null);
			if (path != null)
			{
				File file = new File(path);
				if (file.exists())
				{
					recentFiles.add(file);
				}
			}
		}
	}
	
	/**
	 * Saves recent files to preferences
	 */
	private void saveRecentFiles()
	{
		// Clear all existing entries
		for (int i = 0; i < MAX_RECENT_FILES; i++)
		{
			prefs.remove(PREF_KEY_PREFIX + i);
		}
		
		// Save current list
		for (int i = 0; i < recentFiles.size() && i < MAX_RECENT_FILES; i++)
		{
			prefs.put(PREF_KEY_PREFIX + i, recentFiles.get(i).getAbsolutePath());
		}
	}
}