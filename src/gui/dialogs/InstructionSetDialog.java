package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

@SuppressWarnings("serial")
public class InstructionSetDialog extends JDialog {
	
	private JTextArea instructionSet;
	
	public InstructionSetDialog(JFrame main) {
		super(main, "Instruction Set");

		setIconImage(main.getIconImage());
		
		instructionSet = new JTextArea(10, 48);
		instructionSet.setText(readFile("gui/resources/instructionset.txt"));
		instructionSet.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
		instructionSet.setEditable(false);
		instructionSet.setForeground(new Color(100, 100, 100));
		
		JButton ok = new JButton("OK");
		ok.setFocusable(false);
		ok.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});

		JScrollPane scrollPane = new JScrollPane(instructionSet);
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setFocusable(false);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p2.add(ok);
		
		JPanel p3 = new JPanel(new BorderLayout(0, 10)); 
        p3.add(scrollPane);
        p3.add(p2, BorderLayout.SOUTH);
        p3.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(p3);
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
	}
	
	public String readFile(String path) {
		String text = "";
		Scanner sc = null;
		try {
			sc = new Scanner(getClass().getClassLoader().getResourceAsStream(path));
			while (sc.hasNext()) {
				text += sc.nextLine();
				if (sc.hasNext()) {
					text += "\n";
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (sc != null) {
				sc.close();
			}	
		}
		return text;
	}
			
}
