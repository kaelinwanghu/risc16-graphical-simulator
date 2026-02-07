package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.undo.UndoManager;

import gui.components.LineNumberedTextArea;

@SuppressWarnings("serial")
public class InputPanel extends JPanel {

	private LineNumberedTextArea lineNumberedTextArea;  // Changed from JTextArea
	private Simulator simulator;
	private UndoManager undoManager;

	public InputPanel(final Simulator simulator, int programRows, int columns) {
		super(new BorderLayout());
		this.simulator = simulator;
		
		// Create line-numbered text area
		lineNumberedTextArea = new LineNumberedTextArea(programRows, columns);
		
		// Set up undo manager
		undoManager = new UndoManager();
		lineNumberedTextArea.getDocument().addUndoableEditListener(undoManager);
		
		// Set up document listener for modification tracking
		lineNumberedTextArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				simulator.setModified(true);
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				simulator.setModified(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				simulator.setModified(true);
			}
		});
				
		// Create instruction set button
		JButton instructionSet = new JButton("Instruction Set");
		instructionSet.setFocusable(false);
		instructionSet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				simulator.instructionSetDialog.setVisible(true);
			}
		});
		
		// Create header panel
		JLabel l1 = new JLabel("Program");
		l1.setFont(new Font("Consolas", Font.PLAIN, 19));
		l1.setForeground(Color.RED);
		
		JPanel p1 = new JPanel(new BorderLayout(0, 10));
		p1.add(l1, BorderLayout.WEST);
		p1.add(instructionSet, BorderLayout.EAST);
		
		JPanel p3 = new JPanel(new BorderLayout(0, 10));
		p3.add(p1, BorderLayout.NORTH);
		p3.add(lineNumberedTextArea);
		
		add(p3);
	}
	
	public String getProgram() {
		return lineNumberedTextArea.getText();
	}

	public void setProgram(String text) {
		lineNumberedTextArea.setText(text);
		lineNumberedTextArea.setCaretPosition(0);
		undoManager.discardAllEdits();
	}
	
	public void clear() {
		lineNumberedTextArea.clear();
		lineNumberedTextArea.setCaretPosition(0);
	}

	public void undo() {
		if (undoManager.canUndo()) {
			undoManager.undo();
		}
	}

	public void redo() {
		if (undoManager.canRedo()) {
			undoManager.redo();
		}
	}

	public boolean canUndo() {
		return undoManager.canUndo();
	}

	public boolean canRedo() {
		return undoManager.canRedo();
	}
	
	/**
	 * Gets the line-numbered text area component
	 */
	public LineNumberedTextArea getLineNumberedTextArea() {
		return lineNumberedTextArea;
	}
}