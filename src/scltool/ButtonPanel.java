/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author dell
 */
public class ButtonPanel extends JPanel {
    public JButton button1, button2;


    public ButtonPanel(String name1, String name2, ActionListener al, String tt1, String tt2) {
        button1 = new JButton(name1);
        button2 = new JButton(name2);

        Dimension buttonSize = new Dimension();
        maxPreferredSize(button1, buttonSize);
        maxPreferredSize(button2, buttonSize);

        button1.setPreferredSize(buttonSize);
        button1.addActionListener(al);
        button2.setPreferredSize(buttonSize);
        button2.addActionListener(al);

        if (tt1 != null)
            button1.setToolTipText(tt1);
        if (tt2 != null)
            button2.setToolTipText(tt2);

        setLayout(new FlowLayout(FlowLayout.CENTER));
        add(button1);
        add(button2);
    }


    public static void maxPreferredSize(JComponent component, Dimension maxSize) {
        Dimension compSize = component.getPreferredSize();

        if ((compSize.height > maxSize.height) || (compSize.width > maxSize.width))
            maxSize.setSize(compSize);
    }

    public void setEna(boolean ena) {
        button1.setEnabled(ena);
        button2.setEnabled(ena);
    }
}
