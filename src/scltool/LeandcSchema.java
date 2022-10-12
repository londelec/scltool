/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


/**
 *
 * @author dell
 */
public class LeandcSchema extends JDialog {
    private static final int XSD_USER = -1;
    private static final String XSDFILENAME = "IEC61850cl.xsd";
    private final JComboBox<String> combofw = new JComboBox();
    private final ArrayList<Integer> inclxsd = new ArrayList<>();
    private final JLabel labelinfo;
    private final ButtonPanel okCancelButtons = new ButtonPanel("OK", "Cancel", new ButtonListener(), null, null);
    private final TableXml objTables[];
    private int current;
    private boolean okclick;
    private File userxsd = new File(XSDFILENAME);

    public LeandcSchema(SclMain frame, TableXml objtab[], FileSystem jarfs) {
        super(frame, "Compatible leandc application version", true);

        setResizable(false);
        objTables = objtab;
        String jpath = "?";
        if (frame.jarpath != null)
            jpath = frame.jarpath.getParent();
        labelinfo = new JLabel("<html>Note: You can add new application version to this list<br>by placing IEC61850cl.xsd file in the SCL browser directory:<br>" + jpath);

        okCancelButtons.button1.setToolTipText("Application version will be changed to the selected version. Data will be removed from all tables.");
        JPanel panelCombo = SclMain.createTitledPanel("Selected version", 0);
        Dimension cdim = combofw.getPreferredSize();
        cdim.width = 150;
        combofw.setMaximumSize(cdim);
        panelCombo.add(Box.createRigidArea(new Dimension(5, cdim.height + 10)));
        panelCombo.add(combofw);
        panelCombo.add(Box.createRigidArea(new Dimension(5, cdim.height + 10)));
        //panelFileType.add(Box.createHorizontalGlue());

        GroupLayout gl = new GroupLayout(getContentPane());
        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addContainerGap()
                .addGroup(gl.createParallelGroup(CENTER)
                        .addComponent(panelCombo)
                        .addComponent(labelinfo)
                        .addComponent(okCancelButtons))
                .addContainerGap()
        );
        gl.setVerticalGroup(gl.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelCombo)
                .addGap(3)
                .addComponent(labelinfo)
                .addGap(3)
                .addComponent(okCancelButtons)
                .addContainerGap()
        );
        getContentPane().setLayout(gl);
        getRootPane().setDefaultButton(okCancelButtons.button1);
        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionExit();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);


        Path xsdpath = jarfs.getPath("/scltool/xsd");
        try {
            Files.walkFileTree(xsdpath, new XsdFileVisitor());
        } catch (IOException ex) {
            Logger.getLogger(LeandcSchema.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        pack();
        Dimension dialogSize = getSize();
        setMinimumSize(dialogSize);
        //setVisible(true);
    }


    private class XsdFileVisitor<Path> extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            String filep = path.toString();
            int uver;
            int startix = filep.indexOf("_V");
            int endix = filep.indexOf(".xsd");

            if ((startix > 0) && (endix > 0)) {
                try {
                    if ((uver = Integer.parseInt(filep.substring(startix + 2, endix))) > 0) {
                        for (int i = 0; i < inclxsd.size(); i++) {
                            if (uver > inclxsd.get(i)) {
                                inclxsd.add(i, uver);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                        inclxsd.add(uver);
                    }
                }
                catch (NumberFormatException ex) {
                    Logger.getLogger(LeandcSchema.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }


    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            JComponent src = (JComponent) ae.getSource();
            if (src == okCancelButtons.button1) {
                okclick = true;
                actionExit();
            }
            else if (src == okCancelButtons.button2) {
                actionExit();
            }
        }
    }


    public String initSchemas() {
        Float fver;
        DecimalFormat myFormatter = new DecimalFormat("#.00");
        SclMain frame = (SclMain) getOwner();

        if (!inclxsd.isEmpty()) {
            combofw.addItem("V" + myFormatter.format(((float) inclxsd.get(0)) / 100));
            for (int i = 1; i < inclxsd.size(); i++) {
                if ((inclxsd.get(i) + 1) < inclxsd.get(i - 1)) {
                    combofw.addItem("V" +
                            myFormatter.format(((float) inclxsd.get(i)) / 100) + "..V" +
                            myFormatter.format(((float) inclxsd.get(i - 1) - 1) / 100));
                }
                else
                    combofw.addItem("V" + myFormatter.format(((float) inclxsd.get(i)) / 100));
            }
        }

        if (!(userxsd.exists())) {
            if (frame.jarpath != null) {
                File fpath = new File(frame.jarpath.getParentFile(), "/" + userxsd.getName());
                //System.out.println("xsd find: " + fpath.toString());
                if (fpath.exists())
                    userxsd = fpath;
                else
                    userxsd = null;
            }
            else
                userxsd = null;
        }

        if (userxsd != null) {
            if ((fver = getXsdVersion()) != null) {
                if (inclxsd.indexOf((int) (fver * 100)) < 0) {
                    inclxsd.add(0, XSD_USER);
                    combofw.insertItemAt("V" + myFormatter.format(fver), 0);
                }
                else
                    userxsd = null;
            }
            else
                userxsd = null;
        }

        if (inclxsd.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "JAR doesn't contain any schemas and " + XSDFILENAME + " file is not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        current = 0;
        loadSchema(current);
        return combofw.getItemAt(current).substring(0, 5);
    }


    private Float getXsdVersion() {
        Document doc;
        NodeList nlist;
        String sver;
        SclMain frame = (SclMain) getOwner();

        if ((doc = frame.readXmlFile(userxsd, null)) == null)
            return null;

        if (
                ((nlist = getNodesXpath(doc, "schema/attribute::version")) == null) ||
                (nlist.getLength() != 1)) {
            JOptionPane.showMessageDialog(frame, "Schema version=\"\" not found in " + userxsd.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        try {
            sver = nlist.item(0).getNodeValue();
            return Float.parseFloat(sver);
        } catch (DOMException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid schema version=\"\" in " + userxsd.getName() + "\n" + ex, "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(FirmwareSchemas.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    private void loadSchema(int selix) {
        int uver;
        Document doc = null;
        NodeList nlist;
        ArrayList<String> attrnames;
        SclMain frame = (SclMain) getOwner();

        if ((uver = inclxsd.get(selix)) == XSD_USER) {  // User supplied schema, located next to JAR
            doc = frame.readXmlFile(userxsd, null);
        }
        else {  // Internal schema, included in JAR
            FileSystem jarfs;
            if ((jarfs = frame.openJar()) != null) {
                Path xsdpath = jarfs.getPath("/scltool/xsd/IEC61850cl_V" + uver + ".xsd");
                doc = frame.readXmlFile(null, xsdpath);
                try {
                    jarfs.close();
                } catch (IOException ex) {
                    Logger.getLogger(LeandcSchema.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (
                (doc != null) &&
                ((nlist = getNodesXpath(doc, "schema/attribute::targetNamespace")) != null) &&
                (nlist.getLength() == 1)) {
            frame.xsdns = nlist.item(0).getNodeValue();
        }
        else
            frame.xsdns = null;

        attrnames = getAttrNames(doc, "//complexType[@name='DIType']/attribute/attribute::name");
        objTables[0] = new TableXml(attrnames, "DITable", "DI", "Digital Input");

        attrnames = getAttrNames(doc, "//complexType[@name='AIType']/attribute/attribute::name");
        objTables[1] = new TableXml(attrnames, "AITable", "AI", "Analog Input");

        attrnames = getAttrNames(doc, "//complexType[@name='DOType']/attribute/attribute::name");
        objTables[2] = new TableXml(attrnames, "DOTable", "DO", "Digital Output");

        attrnames = getAttrNames(doc, "//complexType[@name='AOType']/attribute/attribute::name");
        objTables[3] = new TableXml(attrnames, "AOTable", "AO", "Analog Output");
    }


    private NodeList getNodesXpath(Document doc, String expr) {
        XPathExpression result;
        XPath xpath = XPathFactory.newInstance().newXPath();
        //XPathExpression ccc = xpath.compile("//complexType[@name='DIType']/attribute/attribute::name");
        try {
            result = xpath.compile(expr);
            return (NodeList) result.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    private ArrayList<String> getAttrNames(Document doc, String expr) {
        NodeList nlist;
        ArrayList<String> attrnames = new ArrayList<>();

        if (doc == null)
            return attrnames;

        if ((nlist = getNodesXpath(doc, expr)) != null) {
            for (int i = 0; i < nlist.getLength(); i++) {
                attrnames.add(nlist.item(i).getNodeValue());
            }
        }
        return attrnames;
    }


    public String showDialog() {
        combofw.setSelectedIndex(current);
        okclick = false;
        setVisible(true);

        if (!okclick)
            return null;

        if (current != combofw.getSelectedIndex()) {
            current = combofw.getSelectedIndex();
            loadSchema(current);
            return combofw.getItemAt(current).substring(0, 5);
        }
        return null;
    }


    private void actionExit() {
        setVisible(false);
    }
}
