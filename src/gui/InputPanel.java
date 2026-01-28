package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.undo.UndoManager;

@SuppressWarnings("serial")
public class InputPanel extends JPanel{

	private JTextArea program;
	private Simulator simulator;
	private UndoManager undoManager;

	public InputPanel(final Simulator simulator, int programRows, int columns) {
		super(new BorderLayout());
		this.simulator = simulator;
		program = new JTextArea(programRows, columns);
		program.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

		undoManager = new UndoManager();
		program.getDocument().addUndoableEditListener(undoManager);

		program.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                simulator.setModified(true);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                simulator.setModified(true);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                simulator.setModified(true);
            }
        });

		JScrollPane scrollPane = new JScrollPane(program);
		scrollPane.setWheelScrollingEnabled(true);
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		JButton instructionSet = new JButton("Instruction Set");
		instructionSet.setFocusable(false);
		instructionSet.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				simulator.instructionSetDialog.setVisible(true);
			}

		});
		
		JLabel l1 = new JLabel("Program");
		l1.setFont(new Font("Consolas", Font.PLAIN, 19));
		l1.setForeground(Color.RED);
				
		JPanel p1 = new JPanel(new BorderLayout(0, 10));
		p1.add(l1, BorderLayout.WEST);
		p1.add(instructionSet, BorderLayout.EAST);
				
		JPanel p3 = new JPanel(new BorderLayout(0, 10));
		p3.add(p1, BorderLayout.NORTH);
		p3.add(scrollPane);
		
		add(p3);
	}
		
	public String getProgram() {
		return program.getText();
	}

	public void setProgram(String text)
	{
		program.setText(text);
		program.setCaretPosition(0);
		undoManager.discardAllEdits();
	}
	
	public void clear() {
		program.setText("");
		program.setCaretPosition(0);
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
}
