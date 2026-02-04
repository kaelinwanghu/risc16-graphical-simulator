package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.AbstractAction;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Set;

import gui.facade.EngineFacade;
import gui.facade.EngineObserver;
import engine.assembly.AssemblyResult;
import engine.assembly.AssemblyError;
import engine.execution.ProcessorState;
import engine.execution.ExecutionException;
import engine.execution.ExecutionResult;

import gui.dialogs.MessageDialog;
import gui.dialogs.InstructionSetDialog;

@SuppressWarnings("serial")
public class Simulator extends JFrame implements EngineObserver {

	// NEW: Private facade instead of static processor
	private EngineFacade engineFacade;

	private InputPanel inputPanel;
	public AssemblyPanel assemblyPanel;
	public StorageViewer storageViewer;

	public MessageDialog messageDialog;
	public InstructionSetDialog instructionSetDialog;

	private FileManager fileManager;
	private AutoSaver autoSaver;
	private RecentFiles recentFiles;
	private boolean isModified = false;
	private JMenu fileMenuRef;

	private JPanel main;
	private JButton execute;
	private JButton executeStep;
	private JButton assemble;
	private JButton edit;

	public Simulator() {
		super("Architectural Simulator");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {

		}
		UIManager.put("Label.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Label.foreground", Color.BLUE);
		UIManager.put("Button.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TextField.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("ComboBox.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Table.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TableHeader.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Table.foreground", new Color(100, 100, 100));

		ImageIcon i1 = new ImageIcon(getClass().getClassLoader().getResource("gui/resources/microchip1.png"));
		ImageIcon i2 = new ImageIcon(getClass().getClassLoader().getResource("gui/resources/microchip2.png"));
		setIconImages(Arrays.asList(i1.getImage(), i2.getImage()));

		engineFacade = new EngineFacade(1024);
		engineFacade.addObserver(this);

		messageDialog = new MessageDialog(this);
		instructionSetDialog = new InstructionSetDialog(this);
		fileManager = new FileManager(this);
		recentFiles = new RecentFiles();

		inputPanel = new InputPanel(this, 25, 35);
		setupTextEditingShortcuts();
		storageViewer = new StorageViewer(this, engineFacade);
		storageViewer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		autoSaver = new AutoSaver(fileManager, inputPanel, this);
		autoSaver.start();
		String recovered = fileManager.recoverAutoSave();
		if (recovered != null) {
			int response = JOptionPane.showConfirmDialog(
					this,
					"An auto-saved file was found. Would you like to recover it?",
					"Auto-Save Recovery",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if (response == JOptionPane.YES_OPTION) {
				inputPanel.setProgram(recovered);
			} else {
				fileManager.clearAutoSave();
			}
		}

		execute = new JButton("Execute");
		execute.setFocusable(false);
		execute.setEnabled(false);
		execute.addActionListener(e -> execute(false));

		executeStep = new JButton("Execute Step");
		executeStep.setFocusable(false);
		executeStep.setEnabled(false);
		executeStep.addActionListener(e -> execute(true));

		assemble = new JButton("Assemble");
		assemble.setFocusable(false);
		assemble.addActionListener(e -> assembleProgram());

		JButton clear = new JButton("Clear");
		clear.setFocusable(false);
		clear.addActionListener(e -> edit(true));

		edit = new JButton("Edit");
		edit.setFocusable(false);
		edit.setEnabled(false);
		edit.addActionListener(e -> edit(false));

		JButton about = new JButton("About");
		about.setFocusable(false);
		about.addActionListener(e -> messageDialog.showAbout());

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p1.add(assemble);
		p1.add(edit);
		p1.add(clear);

		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p2.add(execute);
		p2.add(executeStep);
		p2.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		JPanel p3 = new JPanel(new BorderLayout(0, 5));
		p3.add(p1, BorderLayout.NORTH);
		p3.add(p2);
		p3.add(about, BorderLayout.SOUTH);

		main = new JPanel(new BorderLayout(0, 10));
		main.add(inputPanel);
		main.add(p3, BorderLayout.SOUTH);
		main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				main,
				storageViewer);

		// Set initial divider location (60% to left panel, 40% to right)
		splitPane.setResizeWeight(0.6);

		// Set divider appearance
		splitPane.setDividerSize(8);
		splitPane.setOneTouchExpandable(true); // Adds arrows to quickly collapse panels
		splitPane.setContinuousLayout(true); // Smooth resizing as you drag

		add(splitPane);
		setResizable(true);
		setMinimumSize(new Dimension(900, 600));

		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem newItem = new JMenuItem("New");
		newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		newItem.addActionListener(e -> newFile());
		fileMenu.add(newItem);

		JMenuItem openItem = new JMenuItem("Open...");
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		openItem.addActionListener(e -> openFile());
		fileMenu.add(openItem);

		fileMenu.addSeparator();

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		saveItem.addActionListener(e -> saveFile());
		fileMenu.add(saveItem);

		JMenuItem saveAsItem = new JMenuItem("Save As...");
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		saveAsItem.addActionListener(e -> saveFileAs());
		fileMenu.add(saveAsItem);

		fileMenu.addSeparator();

		// Recent files submenu
		JMenu recentMenu = new JMenu("Recent Files");
		updateRecentFilesMenu(recentMenu);
		fileMenu.add(recentMenu);

		menuBar.add(fileMenu);

		// Help menu
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');

		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(e -> messageDialog.showAbout());
		helpMenu.add(aboutItem);

		JMenuItem instructionSetItem = new JMenuItem("Instruction Set");
		instructionSetItem.addActionListener(e -> instructionSetDialog.setVisible(true));
		helpMenu.add(instructionSetItem);

		menuBar.add(helpMenu);
		setJMenuBar(menuBar);

		pack();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				handleExit();
			}
		});
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
	}

	// =================================================================
	// NEW: EngineObserver Implementation
	// =================================================================

	@Override
	public void onStateChanged(ProcessorState oldState, ProcessorState newState, ExecutionResult result) {
		// Update storage viewer with new state
		storageViewer.updateState(newState, result);

		// Update assembly panel highlighting
		if (assemblyPanel != null) {
			assemblyPanel.highlightInstruction(newState.getPC());
		}

		// Check if halted (will trigger onHalt, but we can also check here)
		if (newState.isHalted()) {
			execute.setEnabled(false);
			executeStep.setEnabled(false);
		}
	}

	@Override
	public void onProgramLoaded(AssemblyResult result) {
		// Program successfully assembled and loaded!

		// Clear change tracking for new program
		storageViewer.clearChanges();

		Set<Integer> loadedAddresses = new HashSet<>();

		// Both instruction and data are written to memory on load
		for (engine.isa.InstructionFormat instr : result.getInstructions()) {
			loadedAddresses.add(instr.getAddress());
		}
		for (engine.assembly.AssemblyResult.DataSegment data : result.getDataSegments()) {
			loadedAddresses.add(data.getAddress());
		}

		storageViewer.markInitialLoad(loadedAddresses);

		// Update storage viewer
		storageViewer.refresh();

		// Swap panels: InputPanel → AssemblyPanel
		main.remove(inputPanel);
		try {
			main.remove(assemblyPanel);
		} catch (Exception ex) {
			// assemblyPanel might not exist yet
		}

		// Create new assembly panel with result
		boolean isHex = storageViewer.hex.getText().equals("HEX");
		assemblyPanel = new AssemblyPanel(result, isHex);
		ProcessorState initialState = engineFacade.getState();
		assemblyPanel.highlightInstruction(initialState.getPC());
		main.add(assemblyPanel);
		main.validate();

		// Enable execution buttons
		execute.setEnabled(true);
		executeStep.setEnabled(true);
		edit.setEnabled(true);
		assemble.setEnabled(false);
	}

	@Override
	public void onAssemblyError(AssemblyResult result) {
		// Assembly failed - show all errors to user
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append("Assembly failed with ")
				.append(result.getErrors().size())
				.append(" error(s):\n\n");

		for (AssemblyError error : result.getErrors()) {
			errorMsg.append(error.getFormattedMessage()).append("\n");
		}

		messageDialog.showError(errorMsg.toString());

		// Ensure processor is cleared
		engineFacade.clear();
	}

	@Override
	public void onExecutionError(ExecutionException error) {
		// Runtime error during execution
		storageViewer.refresh();
		if (assemblyPanel != null) {
			assemblyPanel.repaint();
		}

		messageDialog.showError(error.getMessage());

		// Disable execution buttons
		execute.setEnabled(false);
		executeStep.setEnabled(false);
	}

	@Override
	public void onHalt() {
		// Program halted normally
		execute.setEnabled(false);
		executeStep.setEnabled(false);
		assemble.setEnabled(true);
		ProcessorState finalState = engineFacade.getState();
		String message = String.format(
				("""
				Program halted
				Instructions executed: %d
				Final PC: 0x%04X
				""").stripTrailing(),
				finalState.getInstructionCount(),
				finalState.getPC());

		messageDialog.showInfo(message);
	}

	// =================================================================
	// NEW: Assembly Method (V2)
	// =================================================================

	private void assembleProgram() {
		try {
			// Get source code from input panel
			String sourceCode = inputPanel.getProgram();

			// Assemble through facade
			// Facade will call observer methods (onProgramLoaded or onAssemblyError)
			AssemblyResult result = engineFacade.assemble(sourceCode);

			// Note: Observer handles all UI updates!
			// - If successful: onProgramLoaded() swaps panels, enables buttons
			// - If failed: onAssemblyError() shows errors

		} catch (Exception ex) {
			// Unexpected error (shouldn't happen with new assembler)
			messageDialog.showError("Unexpected assembly error: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	// =================================================================
	// NEW: Execution Methods (V2)
	// =================================================================

	private void execute(boolean stepped) {
		try {
			if (stepped) {
				// Execute one instruction
				engineFacade.step();
				// Observer (onStateChanged) handles UI updates
			} else {
				// Execute until halt
				engineFacade.run();
				// Observer (onStateChanged + onHalt) handles UI updates
			}
		} catch (ExecutionException ex) {
			// Observer (onExecutionError) already handled this
			// No need to do anything here
		}
	}

	// =================================================================
	// Edit Method (Updated)
	// =================================================================

	public void edit(boolean clear) {
		if (clear) {
			inputPanel.clear();
		}

		// Clear change tracking
		storageViewer.clearChanges();
		storageViewer.clearInitialLoadHighlight();

		execute.setEnabled(false);
		edit.setEnabled(false);
		executeStep.setEnabled(false);
		assemble.setEnabled(true);

		try {
			main.remove(assemblyPanel);
		} catch (Exception ex) {
			// assemblyPanel might not exist
		}
		main.add(inputPanel);
		main.validate();
		repaint();
	}

	// =================================================================
	// NEW: Accessor for EngineFacade (used by StorageSettingsDialog)
	// =================================================================

	/**
	 * Gets the engine facade
	 * Used by StorageSettingsDialog to query/configure processor
	 * 
	 * @return the engine facade
	 */
	public EngineFacade getEngineFacade() {
		return engineFacade;
	}

	/**
	 * Recreates the engine facade with new memory size
	 * This is called when memory size is changed in settings
	 * 
	 * @param newMemorySize the new memory size in bytes
	 */
	public void recreateEngineFacade(int newMemorySize) {
		if (engineFacade != null) {
			engineFacade.removeObserver(this);
		}

		// Create new facade
		engineFacade = new EngineFacade(newMemorySize);
		engineFacade.addObserver(this);

		// Update storage viewer reference
		storageViewer.setEngineFacade(engineFacade);

		// Clear any displayed state
		storageViewer.clearChanges();
		storageViewer.clearInitialLoadHighlight();
		storageViewer.refresh();
	}

	public static void main(String[] args) {
		new Simulator().setVisible(true);
	}

	// =================================================================
	// File Operations
	// =================================================================

	private void newFile() {
		if (isModified && !promptSaveIfNeeded()) {
			return; // User cancelled
		}

		inputPanel.clear();
		fileManager.newFile();
		setModified(false);
		edit(false);
	}

	private void openFile() {
		if (isModified && !promptSaveIfNeeded()) {
			return; // User cancelled
		}

		String content = fileManager.openFile();
		if (content != null) {
			inputPanel.clear();
			inputPanel.setProgram(content);
			setModified(false);
			edit(false);

			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null) {
				recentFiles.addFile(currentFile);
			}
		}
	}

	private boolean promptSaveIfNeeded() {
		String filename = fileManager.getCurrentFileName();
		int response = JOptionPane.showConfirmDialog(
				this,
				"Do you want to save changes to '" + filename + "'?",
				"Unsaved Changes",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);

		if (response == JOptionPane.YES_OPTION) {
			String content = inputPanel.getProgram();
			return fileManager.save(content);
		} else if (response == JOptionPane.CANCEL_OPTION) {
			return false;
		}

		return true;
	}

	private void saveFile() {
		String content = inputPanel.getProgram();
		boolean success = fileManager.save(content);

		if (success) {
			setModified(false);
			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null) {
				recentFiles.addFile(currentFile);
			}
			fileManager.clearAutoSave();
		}
	}

	private void saveFileAs() {
		String content = inputPanel.getProgram();
		boolean success = fileManager.saveAs(content);

		if (success) {
			setModified(false);
			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null) {
				recentFiles.addFile(currentFile);
			}
			fileManager.clearAutoSave();
		}
	}

	private void updateRecentFilesMenu(JMenu recentMenu) {
		recentMenu.removeAll();

		List<File> recent = recentFiles.getRecentFiles();

		if (recent.isEmpty()) {
			JMenuItem emptyItem = new JMenuItem("(No recent files)");
			emptyItem.setEnabled(false);
			recentMenu.add(emptyItem);
		} else {
			for (final File file : recent) {
				JMenuItem item = new JMenuItem(file.getName());
				item.setToolTipText(file.getAbsolutePath());
				item.addActionListener(e -> {
					String content = fileManager.loadFile(file);
					if (content != null) {
						inputPanel.clear();
						inputPanel.setProgram(content);
						setModified(false);
						edit(false);
						recentFiles.addFile(file);
					}
				});
				recentMenu.add(item);
			}

			recentMenu.addSeparator();

			JMenuItem clearItem = new JMenuItem("Clear Recent Files");
			clearItem.addActionListener(e -> recentFiles.clear());
			recentMenu.add(clearItem);
		}
	}

	public void setModified(boolean modified) {
		this.isModified = modified;
		updateTitle();
	}

	public boolean isModified() {
		return isModified;
	}

	private void updateTitle() {
		String filename = fileManager.getCurrentFileName();
		String modified = isModified ? "*" : "";
		setTitle("RiSC-16 Simulator - " + modified + filename);
	}

	private void handleExit() {
		if (isModified) {
			String filename = fileManager.getCurrentFileName();
			int response = JOptionPane.showConfirmDialog(
					this,
					"Do you want to save changes to '" + filename + "'?",
					"Unsaved Changes",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);

			if (response == JOptionPane.YES_OPTION) {
				String content = inputPanel.getProgram();
				boolean success = fileManager.save(content);

				if (!success) {
					return;
				}
			} else if (response == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		autoSaver.stop();
		System.exit(0);
	}

	private void setupTextEditingShortcuts() {
		// Undo (Ctrl+Z)
		inputPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
				"undo");
		inputPanel.getActionMap().put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				inputPanel.undo();
			}
		});

		// Redo (Ctrl+Y)
		inputPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK),
				"redo");
		inputPanel.getActionMap().put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				inputPanel.redo();
			}
		});

		inputPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "redo");
	}
}