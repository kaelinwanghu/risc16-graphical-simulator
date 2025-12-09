package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class MessageDialog extends JDialog {
	
	private JTextArea text;
	private JLabel image;
	
	private static final String ABOUT_TEXT;
	
	static {
		ABOUT_TEXT = readTextFile("gui/resources/about.txt");
	}
	
	public MessageDialog(JFrame main) {
		super(main, true);
		
		image = new JLabel();

		text = new JTextArea();
		text.setOpaque(false);
		text.setEditable(false);
		text.setFocusable(false);
		
		JButton ok = new JButton("OK");
		ok.setFocusable(false);
		ok.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});
		
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		p1.add(image);
		p1.add(text);
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		p2.add(ok);
		
		JPanel p3 = new JPanel(new BorderLayout());
        p3.add(p1);
		p3.add(p2, BorderLayout.SOUTH);
		
		setResizable(false);
		add(p3);
	}
	
	public void showAbout() {
		setTitle("About");
		image.setIcon(new ImageIcon(getClass().getClassLoader().getResource("gui/resources/microchip2.png")));
		text.setText(ABOUT_TEXT);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);		
	}
	
	public void showError(String message) {
		setTitle("Error");
		image.setIcon(new ImageIcon(getClass().getClassLoader().getResource("gui/resources/error.png")));
		text.setText(message + "!");
		pack();
		setLocationRelativeTo(null);
		setVisible(true);		
	}
	
	public static String readTextFile(String path) {
		String text = "";
		Scanner sc = null;
		try {
			sc = new Scanner(MessageDialog.class.getClassLoader().getResourceAsStream(path));
			while (sc.hasNext()){
				text += sc.nextLine() + (sc.hasNext() ? "\n" : "");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (sc != null){
				sc.close();
			}	
		}
		return text;
	}
		
}

