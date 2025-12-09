package gui.components;

import engine.types.FunctionType;
import gui.Simulator;
import gui.dialogs.ScheduleDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class UnitsSettings extends JPanel {

	private JTextField[][] input;
	private InputBox rob;
	
	public UnitsSettings(final ScheduleDialog scheduleDialog) {
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		
		input = new JTextField[6][];
		
		JPanel p1 = new JPanel(new GridLayout(7, 3, 10, 5));
		p1.add(new JLabel(" Number "));
		p1.add(new JLabel("Stations"));
		p1.add(new JLabel(" Cycles "));
		for (int i = 0; i < 6; i++) {
			input[i] = new JTextField[i > 3? 2 : 3];
			for (int j = 0; j < 3; j++) {
				if (i > 3 && j == 2)
					p1.add(Box.createRigidArea(null));
				else {
					input[i][j] = new JTextField(6);
					p1.add(input[i][j]);
				}
			}
		}
		
		JPanel p2 = new JPanel(new GridLayout(7, 1, 0, 5));
		p2.add(Box.createRigidArea(null));
		for (int i = 0; i < 6; i++) {
			String text = FunctionType.values()[i].toString();
			p2.add(new JLabel(text.charAt(0) + text.substring(1).toLowerCase()));
		}
		
		Border b1 = BorderFactory.createTitledBorder(null, "Functional Units", TitledBorder.LEFT, TitledBorder.TOP, 
				new Font("Consolas", Font.PLAIN, 19), Color.RED);
		
		JPanel p3 = new JPanel(new BorderLayout(20, 0));
		p3.add(p1);
		p3.add(p2, BorderLayout.WEST);
		p3.setBorder(BorderFactory.createCompoundBorder(b1, BorderFactory.createEmptyBorder(0, 10, 5, 10)));
		
		JButton apply = new JButton("Apply");
		apply.setFocusable(false);
		apply.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int[][] config;
				Simulator simulator = (Simulator)scheduleDialog.getOwner();
				try {
					config = getConfiguration();
				} catch (Exception ex) {
					simulator.errorDialog.showError("Invalid/Missing input");
					return;
				}
				
				try {
					Simulator.processor.getUnitSet().setConfiguration(config);
					scheduleDialog.refresh();
				} catch (Exception ex) {
					simulator.errorDialog.showError(ex.getMessage());
					return;
				}
			}

		});
		
		rob = new InputBox("ROB Entries", 100, 8, null);
		
		JPanel p4 = new JPanel(new BorderLayout(0, 5));
		p4.add(rob);
		p4.add(apply, BorderLayout.SOUTH);
		p4.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(7, 10, 7, 10)));

		setConfiguration(new int[][]{
				{6},
				{1, 2, 1},
				{1, 2, 2},
				{1, 2, 5},
				{1, 2, 10},
				{1, 2},
				{1, 2},
		});
		
		add(p3);
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(p4);
	}
	
	public int[][] getConfiguration(){
		int[][] data = new int[input.length + 1][];
		data[0] = new int[]{rob.getValue()};
		for (int i = 0; i < input.length; i++) {
			data[i + 1] = new int[input[i].length];
			for (int j = 0; j < input[i].length; j++)
				data[i + 1][j] = Integer.parseInt(input[i][j].getText());
		}
		return data;
	}
	
	public void setConfiguration(int[][] configuration) {
		rob.setInput(configuration[0][0]);
		for (int i = 0; i < input.length; i++)
			for (int j = 0; j < input[i].length; j++)
				input[i][j].setText(configuration[i + 1][j] + "");		
	}
	
}
