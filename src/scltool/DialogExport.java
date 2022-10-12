/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import static scltool.ButtonPanel.maxPreferredSize;


/**
 *
 * @author dell
 */
public class DialogExport extends JDialog {
    private final JRadioButton radioXml = new JRadioButton("XML");
    private final JRadioButton radioCsv = new JRadioButton("CSV");
    private final ArrayList<JCheckBox> chkbObjType = new ArrayList<>();
    private static final FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("XML Files", "xml");
    private static final FileNameExtensionFilter csvfilter = new FileNameExtensionFilter("CSV Files", "csv");
    private final TableXml objTables[];
    private final ButtonPanel okCancelButtons = new ButtonPanel("Export", "Cancel", new ButtonListener(), null, null);

    public DialogExport(Frame frame, TableXml objtab[]) {
        super(frame, "Data Export", true);

        objTables = objtab;
        JPanel panelMain = new JPanel();
        GroupLayout layoutMain = new GroupLayout(panelMain);
        panelMain.setLayout(layoutMain);

        ButtonGroup bgroupft = new ButtonGroup();
        Dimension radioSize = new Dimension();

        maxPreferredSize(radioXml, radioSize);
        maxPreferredSize(radioCsv, radioSize);
        radioXml.setPreferredSize(radioSize);
        radioCsv.setPreferredSize(radioSize);
        radioXml.setToolTipText("Export configuration data to XML file");
        radioCsv.setToolTipText("Export configuration data to CSV file");
        bgroupft.add(radioXml);
        bgroupft.add(radioCsv);
        radioXml.setSelected(true);

        /*radioXml.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ie) {
                switch (ie.getStateChange()) {
                case ItemEvent.SELECTED:
                    break;

                case ItemEvent.DESELECTED:
                    break;

                default:
                    break;
                }

            }
        });*/

        JPanel panelFileType = SclMain.createTitledPanel("File Type", 0);
        panelFileType.add(Box.createRigidArea(new Dimension(2, 25)));
        panelFileType.add(radioXml);
        panelFileType.add(Box.createRigidArea(new Dimension(5, 0)));
        panelFileType.add(radioCsv);
        panelFileType.add(Box.createHorizontalGlue());

        JPanel panelObjType = SclMain.createTitledPanel("Selected data to export", 0);
        Box boxObjType = Box.createVerticalBox();
        panelObjType.add(Box.createRigidArea(new Dimension(2, 0)));

        ActionListener cboxal = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionObjtypeClick((JCheckBox) ae.getSource());
            }
        };

        for (int i = 0; i < objTables.length; i++) {
            JCheckBox chkbox = new JCheckBox(objTables[i].elName);
            chkbObjType.add(chkbox);

            if (objTables[i].tdata.size() > 0) {
                chkbox.setText(objTables[i].elName + " = " + objTables[i].tdata.size());
                chkbox.setSelected(true);
            }
            else {
                chkbox.setText(objTables[i].elName + " = 0");
            }
            actionObjtypeClick(chkbox);
            chkbox.addActionListener(cboxal);
            boxObjType.add(chkbox);
            boxObjType.add(Box.createRigidArea(new Dimension(0, 1)));
        }
        boxObjType.add(Box.createRigidArea(new Dimension(0, 1)));

        panelObjType.add(boxObjType);
        panelObjType.add(Box.createHorizontalGlue());
        Dimension pdim = panelObjType.getPreferredSize();
        pdim.width = 200;
        panelObjType.setMinimumSize(pdim);

        GroupLayout glMain = new GroupLayout(getContentPane());
        glMain.setHorizontalGroup(glMain.createSequentialGroup()
                .addContainerGap()
                .addGroup(glMain.createParallelGroup(LEADING)
                        .addComponent(panelFileType)
                        .addComponent(panelObjType)
                        .addComponent(okCancelButtons))
                .addContainerGap()
        );

        glMain.setVerticalGroup((glMain.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelFileType, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addGap(3)
                .addComponent(panelObjType)
                .addContainerGap()
                .addComponent(okCancelButtons)
                .addContainerGap())
        );

        getContentPane().setLayout(glMain);
        getRootPane().setDefaultButton(okCancelButtons.button1);
        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionExit();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        Dimension dialogSize = getSize();
        setMinimumSize(dialogSize);
        //dialogSize.width = 300;
        //setMaximumSize(dialogSize);
    }


    private class ConfirmDialog extends JOptionPane {
        public final JDialog dialog;
        private final JButton okbutton;

        public ConfirmDialog(Component parent, String filename, String summary, String xmlns) {
            super("File " + filename + " already exists.\nData will be exported as follows:" + summary + "\nDo you want to continue?", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            dialog = createDialog(parent, "Confirm overwrite");
            okbutton = getRootPane().getDefaultButton();

            if (xmlns == null)
                return;

            Dimension dims, ldim;
            JPanel spanel = SclMain.createTitledPanel("", 1);
            JLabel label = new JLabel("<html>File " + filename + " namespace:<br>'" + xmlns + "' is not recognized.<br>Tick if you want to export data anyway.");
            ldim = label.getPreferredSize();

            JCheckBox chkbox = new JCheckBox("Ignore namespace");
            chkbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    okbutton.setEnabled(((JCheckBox) ae.getSource()).isSelected());
                }
            });

            spanel.add(Box.createRigidArea(new Dimension(10, 5)));
            spanel.add(label);
            spanel.add(Box.createRigidArea(new Dimension(10, 2)));
            spanel.add(chkbox);
            spanel.add(Box.createRigidArea(new Dimension(10, 5)));
            dims = spanel.getPreferredSize();
            dims.width = ldim.width + 20;
            spanel.setPreferredSize(dims);

            JPanel gbox = new JPanel();
            GroupLayout gl = new GroupLayout(gbox);
            gl.setHorizontalGroup(gl.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spanel)
                    .addContainerGap()
            );

            gl.setVerticalGroup((gl.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spanel)
                    .addContainerGap())
            );
            gbox.setLayout(gl);

            dialog.add(gbox, BorderLayout.NORTH);
            dims = dialog.getPreferredSize();
            dialog.setMinimumSize(dims);
            okbutton.setEnabled(false);
        }
    }


    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            JComponent src = (JComponent) ae.getSource();
            if (src == okCancelButtons.button1) {
                actionExport();
            }
            else if (src == okCancelButtons.button2) {
                actionExit();
            }
        }
    }


    private void actionExit() {
        dispose();
    }


    private void actionObjtypeClick(JCheckBox chkbox) {
        int ix;

        if ((ix = chkbObjType.indexOf(chkbox)) < 0)
            return;

        if (chkbox.isSelected()) {
            chkbox.setToolTipText(objTables[ix].tdata.size() + " " + objTables[ix].elName + " elements will be exported");
            okCancelButtons.button1.setEnabled(true);
        }
        else {
            chkbox.setToolTipText(objTables[ix].elName + " data will not be exported");
            for (int i = 0; i < chkbObjType.size(); i++) {
                if (chkbObjType.get(i).isSelected())
                    return;
            }
            okCancelButtons.button1.setEnabled(false);
        }
    }


    private void actionExport() {
        File filename;

        if (SclMain.lastXmlPath == null)
            SclMain.lastXmlPath = SclMain.lastScdPath;

        JFileChooser fc = new JFileChooser(SclMain.lastXmlPath);   // Navigate to the location of the last saved file
        if (radioXml.isSelected())
            fc.setFileFilter(xmlfilter);
        else
            fc.setFileFilter(csvfilter);

        switch (fc.showSaveDialog(this)) {
        case JFileChooser.APPROVE_OPTION:
            break;

        default:
            return;
        }

        filename = fc.getSelectedFile();

        if (radioXml.isSelected()) {
            if (filename.exists()) {
                if (!exportXml(filename, false))
                    return;
            }
            else {
                if (!filename.getName().endsWith(".xml"))
                    filename = new File(filename.toString() + ".xml");

                if (!exportXml(filename, true))
                    return;
            }
            JOptionPane.showMessageDialog(this, "Data exported to:\n" + filename.toString(), "Export summary", JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            if (!exportCsv(filename))
                return;
        }

        SclMain.lastXmlPath = filename;
        dispose();
    }


    private boolean exportXml(File filename, boolean newfl) {
        Document xmldoc;
        Element rootel, el, expgr;
        String confmsg = "";
        Namespace lens = null;
        SclMain frame = (SclMain) getOwner();

        if (newfl) {
            rootel = new Element("objects");
            xmldoc = new Document(rootel);
        }
        else {
            if ((xmldoc = jdomParse(this, filename)) == null)
                return false;

            rootel = xmldoc.getRootElement();
            lens = parseNamespace(rootel);

            if (!prepareXml(filename, rootel, lens))
                return false;
        }


        for (int i = 0; i < objTables.length; i++) {
            if (chkbObjType.get(i).isSelected()) {
                expgr = objTables[i].exportXml(lens);
                confmsg += "\n" + objTables[i].groupName + " - replaced with " + objTables[i].tdata.size() + " new elements";

                if (newfl) {
                    rootel.addContent(expgr);
                }
                else {
                    if ((el = rootel.getChild(objTables[i].groupName, lens)) != null) {
                        el.setContent(expgr.removeContent());
                    }
                }
            }
            else {
                confmsg += "\n" + objTables[i].groupName + " - Unchanged";
            }
        }

        if (SclMain.GLOBAL_DEBUG) {
            //filename = new File("output.xml");
        }

        if (!newfl) {
            Object retval;
            ConfirmDialog confdial;
            if ((lens != null) && (frame.xsdns != null) && !frame.xsdns.equals(lens.getURI()))
                confdial = new ConfirmDialog(this, filename.toString(), confmsg, lens.getURI());
            else
                confdial = new ConfirmDialog(this, filename.toString(), confmsg, null);

            confdial.dialog.setVisible(true);
            if ((retval = confdial.getValue()) == null)
                return false;

            switch (((Integer) retval)) {
            case JFileChooser.APPROVE_OPTION:
                break;
            default:
                return false;
            }
        }
        return writeFile(filename, jdomSerialize(xmldoc), false);
    }


    public static Namespace parseNamespace(Element rootel) {
        Namespace lens = null;
        List<Namespace> nslist = rootel.getNamespacesInScope();

        for (int i = 0; i < nslist.size(); i++) {
            if (nslist.get(i).getPrefix().isEmpty()) {
                lens = nslist.get(i);
                if (lens.getURI().isEmpty())
                    lens = null;
                else
                    break;
            }
        }
        return lens;
    }


    private boolean prepareXml(File filename, Element rootel, Namespace lens) {
        Element el;
        String sval;
        int ix;

        if (lens != null) {
            if ((el = rootel.getChild("VersionControl", lens)) != null) {
                Float confv = new Float(0);

                if ((sval = el.getAttributeValue("conf")) != null) {
                    try {
                        confv = Float.parseFloat(sval);
                        confv = (float) (confv.longValue());
                    } catch (NumberFormatException ex) {
                        // Ignore this exception - Log info only
                        Logger.getLogger(DialogExport.class.getName()).log(Level.INFO, null, ex);
                    }
                }
                confv += 1;
                DecimalFormat myFormatter = new DecimalFormat("#.00");
                el.setAttribute("conf", myFormatter.format(confv));

                Date curDate = new Date();
                el.setAttribute("date", new SimpleDateFormat("yyyy-MM-dd").format(curDate));
                el.setAttribute("time", new SimpleDateFormat("HH:mm:ss").format(curDate));
            }
            else {
                JOptionPane.showMessageDialog(this, "<VersionControl> node is not found in file:\n" + filename.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        for (int i = 0; i < objTables.length; i++) {
            if (chkbObjType.get(i).isSelected()) {
                if ((el = rootel.getChild(objTables[i].groupName, lens)) != null) {
                    el.removeContent();
                }
                else {
                    boolean found = false;
                    for (int j = i - 1; j >= 0; j--) {
                         if ((el = rootel.getChild(objTables[j].groupName, lens)) != null) {
                            found = true;
                            List<Content> content = rootel.getContent();
                            ix = content.indexOf(el);
                            el = new Element(objTables[i].groupName, lens);
                            rootel.addContent(ix + 1, el);
                            break;
                         }
                    }
                    if (found)
                        continue;

                    for (int j = i + 1; j < 4; j++) {
                         if ((el = rootel.getChild(objTables[j].groupName, lens)) != null) {
                            found = true;
                            List<Content> content = rootel.getContent();
                            ix = content.indexOf(el);
                            el = new Element(objTables[i].groupName, lens);
                            rootel.addContent(ix, el);
                            break;
                         }
                    }
                    if (found)
                        continue;

                    el = new Element(objTables[i].groupName, lens);
                    rootel.addContent(rootel.getContentSize(), el);
                }
            }
        }
        return true;
    }


    private boolean exportCsv(File filename) {
        boolean found = false;
        String expstat = "";
        String fname = filename.getName().replaceAll(".csv", "");

        for (int i = 0; i < objTables.length; i++) {
            if (fname.toUpperCase().endsWith("_" + objTables[i].elName)) {
                fname = fname.substring(0, fname.length() - 3);
                break;
            }
        }

        for (int i = 0; i < objTables.length; i++) {
            if (chkbObjType.get(i).isSelected()) {
                if (writeFile(new File(filename.getParent() + "/" + fname + "_" + objTables[i].elName + ".csv"), objTables[i].exportCsv(), true)) {
                    expstat += fname + "_" + objTables[i].elName + ".csv\n";
                    found = true;
                }
                else
                    return false;
            }
        }

        if (found)
            JOptionPane.showMessageDialog(this, "Data exported to:\n" + expstat, "Info", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }


    private boolean writeFile(File filename, String str, boolean append) {
        FileWriter writer;
        try {
            writer = new FileWriter(filename);

            if (append)
                writer.append(str);
            else
                writer.write(str);

            writer.flush();
            writer.close();
        } catch (IOException ex) {
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }


    public static Document jdomParse(Component parent, File filename) {
        SAXBuilder sax = new SAXBuilder();

        try {
            return sax.build(filename);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, ex, "Error", JOptionPane.ERROR_MESSAGE);
        } catch (JDOMException ex) {
            JOptionPane.showMessageDialog(parent, ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }


    private String jdomSerialize(Document doc) {
        //Format ff = Format.getPrettyFormat();
        Format ff = Format.getRawFormat();
        ff.setIndent("\t");
        ff.setLineSeparator(LineSeparator.UNIX);
        ff.setTextMode(Format.TextMode.TRIM);
        XMLOutputter serializer = new XMLOutputter(ff);
        return serializer.outputString(doc).replaceAll(" />", "/>");
    }
}
