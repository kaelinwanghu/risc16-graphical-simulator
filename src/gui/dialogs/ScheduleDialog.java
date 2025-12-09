package gui.dialogs;

import gui.Simulator;
import gui.components.ResizableTable;
import gui.components.UnitsSettings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

@SuppressWarnings("serial")
public class ScheduleDialog extends JDialog {
	
	private UnitsSettings unitsSettings;
	private ResizableTable resizableTable;
	private JTextArea data;
	
	public ScheduleDialog(Simulator simulator) {
		super(simulator, "Instruction Scheduling");
		
		setIconImage(simulator.getIconImage());
		
		unitsSettings = new UnitsSettings(this);
		
		resizableTable = new ResizableTable(new int[]{35, 5, 5, 5, 0});
		resizableTable.setRowHeight(21);
		resizableTable.setIntercellSpacing(new Dimension(10, 0));
		resizableTable.setPreferredScrollableViewportSize(new Dimension(0, 220));
		resizableTable.getTableHeader().setResizingAllowed(false);
		
		JScrollPane scrollPane = new JScrollPane(resizableTable);
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setFocusable(false);
		scrollPane.getVerticalScrollBar().setUnitIncrement(7);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				
		data = new JTextArea(3, 63);
		data.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 10, 5, 5)));
		data.setEnabled(false);
		data.setDisabledTextColor(new Color(100, 100, 100));
		
		JButton ok = new JButton("OK");
		ok.setFocusable(false);
		ok.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p2.add(ok);
		
		JPanel p3 = new JPanel(new BorderLayout(0, 10)); 
        p3.add(data);
        p3.add(p2, BorderLayout.SOUTH);
        
        JPanel p4 = new JPanel(new BorderLayout(0, 10)); 
		p4.add(unitsSettings, BorderLayout.NORTH);
		p4.add(scrollPane);
        p4.add(p3, BorderLayout.SOUTH);
        p4.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(p4);
		setResizable(false);
		pack();
	}
	
	public void showSchedule() {
		refresh();
		unitsSettings.setConfiguration(Simulator.processor.getUnitSet().getConfiguration());
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void refresh() {
		Object[] text = Simulator.processor.getUnitSet().displaySchedule();
		resizableTable.setData((String[][])text[0], (String[])text[1]);
		data.setText((String)text[2]);
	}
	
}

