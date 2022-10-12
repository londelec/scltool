package scltool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author dell
 */
public class SclMain extends javax.swing.JFrame {
    public final static int FLAG_SERVICES = 0x01;
    public final static int FLAG_DSRCB = 0x02;
    public final static int FLAG_COMMS = 0x04;
    public final static int FLAG_SUBST = 0x08;
    private final JTextArea textAttrib = new JTextArea();
    private final JTextArea textSelected = new JTextArea();
    private final JTextArea textFind = new JTextArea();
    private final JTextArea textLog = new JTextArea();
    private SCLtree_node myTree;
    private Document scldoc;
    private JCheckBoxMenuItem menuFind;
    private final ArrayList<JCheckBoxMenuItem> sclMembers = new ArrayList();
    private static final JCheckBoxMenuItem menuPanelEna[] = new JCheckBoxMenuItem[4];
    private final JPanel panelFind = new JPanel();
    public static int vflags = FLAG_SERVICES | FLAG_DSRCB | FLAG_COMMS;
    private static final String SWVERSION = "V3.0";
    private static String buildDate = "Not available";
    private static final String MAINTITLE = "SCL browser";
    private static final String COPYRIGHT = "© 2022 Londelec UK Ltd\nThis program comes with absolutely no warranty.";
    private static final String APPNAME = "Application ";
    private static final String VERTITLE = "Change Version";
    public File jarpath;
    public String xsdns;
    public static File lastScdPath, lastXmlPath;
    private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    private final TableXml[] objTables = new TableXml[4];
    private final LeandcSchema dialogSchemas;
    private final JButton buttonAddPri = new JButton("Add");
    private final JButton buttonAddSec = new JButton("Add");
    private final JButton buttonExport = new JButton("Export");
    private final JButton buttonImport = new JButton("Import");
    private final JButton buttonFind = new JButton("Find");
    private final JButton buttonFClear = new JButton("*");
    private final JPanel[] panelTables = new JPanel[4];
    private final Box boxTables = Box.createVerticalBox();
    private final JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private final JPanel panelSpring = new JPanel();
    private JDialog logDialog;
    private final JLabel labelfwver = new JLabel(APPNAME + "V?");
    private final JCheckBox chkbCaseSens = new JCheckBox("Case Sensitive");
    private static final FileNameExtensionFilter cidfilter = new FileNameExtensionFilter("CID, SCD & ICD Files", "cid", "scd", "icd");
    private static final FileNameExtensionFilter impfilter = new FileNameExtensionFilter("CSV and XML Files", "csv", "xml");
    private static final String[] SCLSCHEMAS = new String[] {
        "_BaseSimpleTypes", "_Enums", "_BaseTypes", "_Communication", "_DataTypeTemplates", "_IED", "_Substation", ""}; // SCL.xsd must be the last
    private final JScrollPane treePane = new JScrollPane();

    public static final boolean GLOBAL_DEBUG = false;


    /**
     * Creates new form SCLmain
     */
    public SclMain() {
        FileSystem jarfs;

        if ((jarfs = initRevision()) == null)
            System.exit(0);

        initComponents();

        dialogSchemas = new LeandcSchema(this, objTables, jarfs);
        try {
            jarfs.close();
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        String sver;
        if ((sver = dialogSchemas.initSchemas()) == null)
            System.exit(0);
        labelfwver.setText(APPNAME + sver);

        initMenu();
        initPanels();
        //System.gc();

        /*IEC61850ClConfig iec = new IEC61850ClConfig();
        DITableType diTable = new DITableType();
        DIType di = new DIType();
        iec.setDITable(diTable);
        diTable.getDI().add(di);*/
    }


    private class SAXErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException saxpe) {
            printLog(saxpe, "Warning");
        }

        @Override
        public void error(SAXParseException saxpe) {
            printLog(saxpe, "Error");
        }

        @Override
        public void fatalError(SAXParseException saxpe) {
            printLog(saxpe, "FatalError");
        }

        private void printLog(SAXParseException saxpe, String msgt) {
            String log = textLog.getText();
            log += msgt + " (at " + saxpe.getLineNumber() + ":" + saxpe.getColumnNumber() + "): " + saxpe.getMessage() + '\n';
            textLog.setText(log);
            //System.out.println(msgt + ": " + saxpe);
        }
    }


    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            JComponent src = (JComponent) ae.getSource();
            if (src == buttonExport) {
                new DialogExport((Frame) src.getRootPane().getParent(), objTables).setVisible(true);
            }
            else if (src == buttonImport) {
                importFile();
            }
            else if (src == buttonAddPri) {
                if (SelectedSCL.objtypes[0] >= 0)
                    objTables[SelectedSCL.objtypes[0]].addRow();
            }
            else if (src == buttonAddSec) {
                if (SelectedSCL.objtypes[1] >= 0)
                    objTables[SelectedSCL.objtypes[1]].addRow();
            }
        }
    }


    private class ChkboxListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            actionObjectPanes();
        }
    }


    private void createTree() {
        myTree = new SCLtree_node(this, scldoc);
        treePane.setViewportView(myTree.scdTree);
        textFind.setText("");   // Clear find text

        /*SCLtree_jaxb myTree = new SCLtree_jaxb();
        //JTree scdTree = new JTree(myTree.treeInit("vamp300.cid"));
        DefaultMutableTreeNode ttt = myTree.treeInit("vamp300.cid");
        JTree scdTree = new JTree();
        treePane.setViewportView(scdTree);*/
    }


    public Document readXmlFile(File filename, Path fpath) {
        Document doc = null;

        try {
            if (filename != null)
                doc = dbFactory.newDocumentBuilder().parse(filename);
            else if (fpath != null)
                doc = SclMain.dbFactory.newDocumentBuilder().parse(Files.newInputStream(fpath, StandardOpenOption.READ));
            //doc.getDocumentElement().normalize();
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return doc;
    }


    private String validateXml(File filename) {
        String errmsg = null, sdir = "/scltool/scl/";
        Path tmpdir = null;

        if (myTree.scledition == null)
            return "SCL Edition is not initialized.";

        switch (myTree.scledition) {
        case SCL_ED1:
            sdir += "2003";
            break;
        case SCL_ED2:
            sdir += "2007B";
            break;
        case SCL_ED21:
            sdir += "2007B4";
            break;
        default:
            return "Unknown SCL Edition.";
        }

        textLog.setText("");
        FileSystem jarfs;
        if ((jarfs = openJar()) == null)
            return "Unable to open JAR to load SCL Schemas.";

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            /* Can't find SCL element in the resulting schema. Probably schema not compiled properly from multiple sources. */
            //StreamSource[] schis = new StreamSource[SCLSCHEMAS.length];
            //for (int i = 0; i < SCLSCHEMAS.length; i++) {
            //    schis[i] = new StreamSource(Files.newInputStream(jarfs.getPath(sdir + "/SCL" + SCLSCHEMAS[i] + ".xsd"), StandardOpenOption.READ));
            //}
            //Schema schema = factory.newSchema(schis);

            tmpdir = Files.createTempDirectory("scl");
            //System.out.println("Tempdir: " + tmpdir.toString());
            File rootxsd = null; // SCL.xsd must be the last
            for (int i = 0; i < SCLSCHEMAS.length; i++) {
                Files.copy(jarfs.getPath(sdir + "/SCL" + SCLSCHEMAS[i] + ".xsd"), ((rootxsd = new File(tmpdir.toString() + "/SCL" + SCLSCHEMAS[i] + ".xsd"))).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            //Schema schema = factory.newSchema(new File("src/" + sdir + "/SCL.xsd"));   // Load from source directory instead of JAR
            Schema schema = factory.newSchema(rootxsd);

            Validator validator = schema.newValidator();
            validator.setErrorHandler(new SAXErrorHandler());
            validator.validate(new StreamSource(filename));

            if (textLog.getText().isEmpty()) {
                if (logDialog.isVisible())
                     logDialog.setVisible(false);
                JOptionPane.showMessageDialog(this, filename.getName() + " has no errors.", "Validation result", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                logDialog.setTitle(filename.getName() + " validation summary");
                JScrollPane scroll = new JScrollPane(textLog);
                logDialog.getRootPane().getContentPane().removeAll();
                logDialog.getRootPane().getContentPane().add(scroll);
                logDialog.setVisible(true);
            }
        } catch (IOException | SAXException ex) {
            //System.out.println("Exception: " + e);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
            errmsg = ex.toString();
        }

        try {
            jarfs.close();
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (tmpdir != null)
            deleteDirectory(new File(tmpdir.toString()));
        return errmsg;
    }


    private String readTextFile(File filename) {
        String text;
        byte[] bbuf;

        try {
            bbuf = Files.readAllBytes(filename.toPath());
            text = new String(bbuf, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return text;
    }


    public FileSystem openJar() {
        String jpath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
        if (jpath.startsWith("jar:")) {  // Executing from JAR
            //System.out.println("fullpath " + jpath);
            jpath = jpath.substring(0, jpath.indexOf("!/"));
        }
        else {  // Debugging
            jpath = "jar:file:/home/dell/Documents/Code/java/SCLtool/dist/scltool.jar";
        }

        try {
            return FileSystems.newFileSystem(URI.create(jpath), Collections.<String, Object>emptyMap());
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    private void deleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        for (File fl : contents) {
            fl.delete();
        }
        dir.delete();
    }


    private void importFile() {
        if (lastXmlPath == null)
             lastXmlPath = lastScdPath;

        JFileChooser fc = new JFileChooser(lastXmlPath);   // Navigate to the location of the last open file
        fc.setFileFilter(impfilter);

        switch (fc.showOpenDialog(this)) {
        case JFileChooser.APPROVE_OPTION:
            lastXmlPath = fc.getSelectedFile();
            break;
        default:
            return;
        }

        if (lastXmlPath.getName().endsWith(".xml")) {
            org.jdom2.Document xmldoc;
            if ((xmldoc = DialogExport.jdomParse(this, lastXmlPath)) == null)
                return;

            Element rootel = xmldoc.getRootElement();
            Namespace lens = DialogExport.parseNamespace(rootel);
            if ((lens != null) && (xsdns != null)) {
                if (!xsdns.equals(lens.getURI())) {
                    switch (JOptionPane.showConfirmDialog(this, "File " + lastXmlPath.getName() + " namespace:\n'" + lens.getURI() + "' is not recognized.\nDo you want to import data anyway?", "Namespace mismatch", JOptionPane.YES_NO_OPTION)) {
                    case JFileChooser.APPROVE_OPTION:
                        break;
                    default:
                        return;
                    }
                }
            }

            String imsg = "";
            for (int i = 0; i < objTables.length; i++) {
                int retval = objTables[i].importXml(rootel, lens);
                imsg += objTables[i].elName + " = " + retval + "\n";
            }
            JOptionPane.showMessageDialog(this, "Data imported from:\n" + lastXmlPath.toString() + "\n" + imsg, "Import summary", JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            String csv, fname = lastXmlPath.getName();
            for (int i = 0; i < objTables.length; i++) {
                if (fname.contains("_" + objTables[i].elName)) {
                    if ((csv = readTextFile(lastXmlPath)) == null)
                        return;
                    int retval = objTables[i].importCsv(csv);
                    JOptionPane.showMessageDialog(this, "Data imported from:\n" + lastXmlPath.toString() + "\n" + objTables[i].elName + " = " + retval, "Import summary", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "CSV file name must contain either '_DI', '_AI', '_DO', '_AO' to import data\ne.g. 'mydata_DI.csv'", "Table name missing", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    public void actionSelected(String attrvals, String errmsg, boolean valid) {
        textAttrib.setText(attrvals);
        buttonAddPri.setVisible(valid);
        buttonAddSec.setVisible(false);

        if ((errmsg != null) && !errmsg.isEmpty()) {
            textSelected.setText("Error: " + errmsg);
            textSelected.setForeground(Color.RED);
        }
        else {
            textSelected.setForeground(UIManager.getColor("TextArea.foreground"));
            if (valid) {
                String seltext = SelectedSCL.getString();
                buttonAddPri.setText("Add " + objTables[SelectedSCL.objtypes[0]].elName);
                if (SelectedSCL.objtypes[1] >= 0) {
                    buttonAddSec.setText("Add " + objTables[SelectedSCL.objtypes[1]].elName);
                    buttonAddSec.setVisible(true);
                    seltext = objTables[SelectedSCL.objtypes[0]].descr + " / " + objTables[SelectedSCL.objtypes[1]].descr + ":\n" + seltext;
                    //"\n(Consider specifying OnValues and OffValues attributes if adding this element as a Digital Input)";
                }
                else {
                    seltext = objTables[SelectedSCL.objtypes[0]].descr + ":\n" + seltext;
                }
                textSelected.setText(seltext);
            }
            else
                textSelected.setText("");
        }
    }


    private void actionObjectPanes() {
        ArrayList<JPanel> visiblep = new ArrayList<>();
        JSplitPane splitp1, splitp2, splitp3;

        boxTables.removeAll();

        for (int i = 0; i < panelTables.length; i++) {
            if (menuPanelEna[i].isSelected())
                visiblep.add(panelTables[i]);
        }

        switch (visiblep.size()) {
        case 1:
            boxTables.add(visiblep.get(0));
            break;

        case 2:
            splitp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visiblep.get(0), visiblep.get(1));
            splitp1.setResizeWeight(0.5);
            boxTables.add(splitp1);
            break;

        case 3:
            splitp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visiblep.get(0), visiblep.get(1));
            splitp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitp1, visiblep.get(2));
            splitp1.setResizeWeight(0.5);
            splitp2.setResizeWeight(0.666667);
            boxTables.add(splitp2);
            break;

        case 4:
            splitp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visiblep.get(0), visiblep.get(1));
            splitp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visiblep.get(2), visiblep.get(3));
            splitp3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitp1, splitp2);
            splitp1.setResizeWeight(0.5);
            splitp2.setResizeWeight(0.5);
            splitp3.setResizeWeight(0.5);
            boxTables.add(splitp3);
            break;

        default:
            break;
        }

        if (visiblep.isEmpty()) {
            splitMain.remove(boxTables);
        }
        else {
            boxTables.add(panelSpring);
            if (splitMain.getRightComponent() == null)
                splitMain.setRightComponent(boxTables);
        }
        revalidate();
    }


    private void drawTables() {
        JScrollPane scroll;

        for (int i = 0; i < objTables.length; i++) {
            panelTables[i] = new JPanel();
            JLabel jlabel = new JLabel(objTables[i].descr + "s");
            scroll = new JScrollPane(objTables[i]);
            JButton jbutt = new JButton("C");
            jbutt.setToolTipText("Select visible columns");
            jbutt.addActionListener(objTables[i].colMenuListener);

            GroupLayout tabgl = new GroupLayout(panelTables[i]);
            panelTables[i].setLayout(tabgl);
            tabgl.setHorizontalGroup(tabgl.createParallelGroup(LEADING)
                    .addGroup(tabgl.createSequentialGroup()
                            .addGap(3)
                            .addComponent(jlabel)
                            .addGap(5)
                            .addComponent(jbutt))
                    .addComponent(scroll)
            );
            tabgl.setVerticalGroup(tabgl.createSequentialGroup()
                    .addGroup(tabgl.createParallelGroup(CENTER)
                            .addComponent(jlabel)
                            .addComponent(jbutt))
                    .addComponent(scroll)
            );
        }
        actionObjectPanes();
    }


    private void actionAbout() {
        Object message = "SCL file browser " + SWVERSION + "\nBuild date: " + buildDate + "\n" + COPYRIGHT;
        JOptionPane.showMessageDialog(this, message, "About SCL tool", JOptionPane.PLAIN_MESSAGE);
    }


    private void actionOpen() {
        String sedn = "";

        JFileChooser fc = new JFileChooser(lastScdPath);   // Navigate to the location of the last file open
        fc.setFileFilter(cidfilter);
        switch (fc.showOpenDialog(this)) {
        case JFileChooser.APPROVE_OPTION:
            lastScdPath = fc.getSelectedFile();
            if ((scldoc = readXmlFile(lastScdPath, null)) != null) {
                createTree();
                if (myTree.scledition != null) {
                    switch (myTree.scledition) {
                    case SCL_ED1:
                        sedn = " [Edition 1]";
                        break;
                    case SCL_ED2:
                        sedn = " [Edition 2]";
                        break;
                    case SCL_ED21:
                        sedn = " [Edition 2.1]";
                        break;
                    default:
                        break;
                    }
                }
            }
            setTitle(lastScdPath.getName() + sedn + " - " + MAINTITLE);   // Sets dialog title
            break;

        default:
            break;
        }
    }


    private void actionValidate() {
        String errmsg = validateXml(lastScdPath);
        if (errmsg != null) {
            JOptionPane.showMessageDialog(this, lastScdPath.getName() + " validation failed.\n" + errmsg, "Validation error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void actionShow(ActionEvent ae) {
        JCheckBoxMenuItem src = (JCheckBoxMenuItem) ae.getSource();
        int ix;

        if ((ix = sclMembers.indexOf(src)) < 0)
            return;

        if (sclMembers.get(ix).getState())
            vflags |= (1 << ix);
        else
            vflags &= ~(1 << ix);

        if (scldoc != null)
            createTree();
    }


    private void actionExit() {
        System.exit(0);
    }


    public static JPanel createTitledPanel(String title, int vertical) {
        JPanel panel = new JPanel();

        if (vertical > 0) {
            BoxLayout mybox = new BoxLayout(panel, BoxLayout.Y_AXIS);
            panel.setLayout(mybox);
        }
        else {
            BoxLayout mybox = new BoxLayout(panel, BoxLayout.X_AXIS);
            panel.setLayout(mybox);
            //FlowLayout flayout = new FlowLayout(FlowLayout.LEFT);
            //panel.setLayout(flayout);
        }

        TitledBorder titled = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP);
        //TitledBorder titled = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP, new java.awt.Font("Ubuntu", 0, 12));
        titled.setTitleFont(UIManager.getFont("TitledBorder.font").deriveFont(Font.PLAIN));
        panel.setBorder(titled);
        return panel;
    }


    private FileSystem initRevision() {
        long dLong = 0;
        FileSystem jarfs;

        CodeSource cs = SclMain.class.getProtectionDomain().getCodeSource();
        try {
            /* Path is used for locating XSD file next to JAR */
            jarpath = new File(cs.getLocation().toURI().getPath());
            //System.out.println("jarpath: " + jarpath.toString());
        } catch (URISyntaxException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
        }

        if ((jarfs = openJar()) == null)
            return null;

        try {
            Path resourcePath = jarfs.getPath("/META-INF/MANIFEST.MF");
            FileTime fileTime = Files.getLastModifiedTime(resourcePath);
            dLong = fileTime.toMillis();
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (dLong > 0) {
            Date modDate = new Date(dLong);
            buildDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(modDate);
        }
        return jarfs;
    }


    private void initPanels() {
        setTitle(MAINTITLE);
        JPanel panelText = new JPanel();
        JPanel panelButt = new JPanel();
        ButtonListener buttListener = new ButtonListener();

        if (GLOBAL_DEBUG) {
            //createTree(new File("vamp300.cid"));
            lastScdPath = new File("ref630_.cid");
            if ((scldoc = readXmlFile(lastScdPath, null)) != null) {
                vflags |= FLAG_DSRCB | FLAG_SUBST;
                createTree();
            }
        }
        treePane.setPreferredSize(new Dimension(250, 10));

        textLog.setEditable(false);
        logDialog = new JDialog(this, false);
        logDialog.setMinimumSize(new Dimension(500, 200));
        logDialog.add(Box.createVerticalBox());

        labelfwver.setToolTipText("Exported/Imported configuration is compatible with this leandc application version. Click 'Help->" + VERTITLE + "' to change.");
        buttonAddPri.setVisible(false);
        buttonAddPri.setToolTipText("Add selected element to table");
        buttonAddPri.addActionListener(buttListener);
        buttonAddSec.setVisible(false);
        buttonAddSec.setToolTipText("Add selected element to table");
        buttonAddSec.addActionListener(buttListener);
        buttonExport.setToolTipText("Export configuration data to XML or CSV file");
        buttonExport.addActionListener(buttListener);
        buttonImport.setToolTipText("Import configuration data from XML or CSV file");
        buttonImport.addActionListener(buttListener);

        //FlowLayout fl = new FlowLayout(FlowLayout.CENTER);
        //panelBut.setLayout(fl);
        panelButt.setLayout(new FlowLayout(FlowLayout.CENTER));
        panelButt.add(buttonExport);
        panelButt.add(buttonImport);

        textFind.setToolTipText("Search SCL element (LD/LN/DO/DA/RCB/DS) by its name. Object references e.g. LD0/LLN0.Mod are not supported.");
        //Dimension aaa = textFind.getMaximumSize();
        buttonFind.setToolTipText("Click to find entered text in the SCL tree");
        buttonFind.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (myTree != null) {
                    myTree.filterTree(textFind.getText(), !chkbCaseSens.isSelected());
                    treePane.setViewportView(myTree.scdTree);
                }
            }
        });
        buttonFClear.setToolTipText("Clear entered text");
        buttonFClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!textFind.getText().isEmpty()) {
                    textFind.setText("");
                    if (myTree != null) {
                        myTree.filterTree("", true);
                        treePane.setViewportView(myTree.scdTree);
                    }
                }
            }
        });
        chkbCaseSens.setToolTipText("Case sensitive search");

        TitledBorder tbFind = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Search", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP);
        tbFind.setTitleFont(UIManager.getFont("TitledBorder.font").deriveFont(Font.PLAIN));
        panelFind.setBorder(tbFind);
        GroupLayout textfngl = new GroupLayout(panelFind);
        panelFind.setLayout(textfngl);
        textfngl.setHorizontalGroup(textfngl.createParallelGroup(CENTER)
                .addComponent(textFind, 0, DEFAULT_SIZE, DEFAULT_SIZE)
                .addComponent(chkbCaseSens)
                .addGroup(textfngl.createSequentialGroup()
                        .addComponent(buttonFind, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                        .addGap(5)
                        .addComponent(buttonFClear, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
        );
        textfngl.setVerticalGroup(textfngl.createParallelGroup(LEADING)
                .addGroup(textfngl.createSequentialGroup()
                        .addComponent(textFind, 26, 26, 26)
                        .addGap(3)
                        .addGroup(textfngl.createParallelGroup(CENTER)
                                .addComponent(chkbCaseSens))
                        .addGap(3)
                        .addGroup(textfngl.createParallelGroup(CENTER)
                                .addComponent(buttonFind, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                                .addComponent(buttonFClear, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                        .addGap(3))
        //.addContainerGap())
        );
        panelFind.setVisible(menuFind.getState());

        textAttrib.setEditable(false);
        JPanel panelAttrib = createTitledPanel("Attributes", 1);
        panelAttrib.add(new JScrollPane(textAttrib));
        panelAttrib.setPreferredSize(new Dimension(170, 0));

        textSelected.setEditable(false);
        JPanel panelSelected = createTitledPanel("Selected", 1);
        panelSelected.add(new JScrollPane(textSelected));

        JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelAttrib, panelSelected);
        splitPane1.setResizeWeight(0.5);

        GroupLayout textgl = new GroupLayout(panelText);
        panelText.setLayout(textgl);
        textgl.setHorizontalGroup(textgl.createParallelGroup(CENTER)
                .addComponent(panelFind)
                .addComponent(splitPane1)
                .addGroup(textgl.createSequentialGroup()
                        .addComponent(buttonAddPri, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                        .addComponent(buttonAddSec, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
        );
        textgl.setVerticalGroup(textgl.createParallelGroup(LEADING)
                .addGroup(textgl.createSequentialGroup()
                        .addComponent(panelFind)
                        .addComponent(splitPane1)
                        .addGroup(textgl.createParallelGroup(CENTER)
                                .addComponent(buttonAddPri, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                                .addComponent(buttonAddSec, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                        .addContainerGap())
        );

        SpringLayout slayout = new SpringLayout();
        panelSpring.setLayout(slayout);
        panelSpring.setMaximumSize(new Dimension(32767, 35));
        panelSpring.add(panelButt);
        panelSpring.add(labelfwver);
        slayout.putConstraint(SpringLayout.HORIZONTAL_CENTER, panelButt, 0, SpringLayout.HORIZONTAL_CENTER, panelSpring);
        slayout.putConstraint(SpringLayout.EAST, labelfwver, -5, SpringLayout.EAST, panelSpring);
        slayout.putConstraint(SpringLayout.VERTICAL_CENTER, labelfwver, 0, SpringLayout.VERTICAL_CENTER, panelSpring);
        //slayout.putConstraint(SpringLayout.VERTICAL_CENTER, panelBut, 0, SpringLayout.VERTICAL_CENTER, panelSpring);
        slayout.putConstraint(SpringLayout.HEIGHT, panelSpring, 0, SpringLayout.HEIGHT, panelButt);
        //Spring aaa = Spring.constant(0, -2, Spring.UNSET);
        //slayout.putConstraint(SpringLayout.WEST, fwversion, aaa, SpringLayout.EAST, panelBut);

        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePane, panelText);
        //splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane2, boxTables);
        //splitMain.setResizeWeight(0.5);
        splitMain.setLeftComponent(splitPane2);
        drawTables();

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        getContentPane().add(splitMain);
        /*javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitMain)
        );
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitMain)
        );*/
        pack();
    }


    private void initMenu() {
        JMenuItem mabout = new JMenuItem("About", KeyEvent.VK_A);
        //menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        //mabout.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        mabout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionAbout();
            }
        });

        JMenuItem mfwver = new JMenuItem(VERTITLE, KeyEvent.VK_V);
        mfwver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String sver;
                if ((sver = dialogSchemas.showDialog()) == null)
                    return;

                labelfwver.setText(APPNAME + sver);
                drawTables();

            }
        });
        jMenuHelp.add(mfwver);
        jMenuHelp.add(mabout);

        JMenuItem mopen = new JMenuItem("Open", KeyEvent.VK_O);
        mopen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionOpen();
            }
        });
        jMenuFile.add(mopen);

        JMenuItem mvalidate = new JMenuItem("Validate", KeyEvent.VK_V);
        mvalidate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (scldoc != null)
                    actionValidate();
            }
        });
        jMenuFile.add(mvalidate);

        JMenu mshow = new JMenu("Show");
        mshow.setMnemonic(KeyEvent.VK_S);
        ActionListener alshow = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionShow(ae);
            }
        };

        sclMembers.add(new JCheckBoxMenuItem("Services"));
        sclMembers.add(new JCheckBoxMenuItem("Datasets & Report Blocks"));
        sclMembers.add(new JCheckBoxMenuItem("Communication"));
        sclMembers.add(new JCheckBoxMenuItem("Substation"));
        for (int i = 0; i < sclMembers.size(); i++) {
            sclMembers.get(i).setState(((vflags & (1 << i)) > 0));
            sclMembers.get(i).addActionListener(alshow);
            mshow.add(sclMembers.get(i));
        }
        jMenuView.add(mshow);

        menuFind = new JCheckBoxMenuItem("Find");
        menuFind.setMnemonic(KeyEvent.VK_F);
        menuFind.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!menuFind.getState()) {
                    if (!textFind.getText().isEmpty()) {
                        textFind.setText("");
                        if (myTree != null) {
                            myTree.filterTree("", true);
                            treePane.setViewportView(myTree.scdTree);
                        }
                    }
                }
                panelFind.setVisible(menuFind.getState());
            }
        });
        jMenuView.add(menuFind);

        JMenuItem mpanels = new JMenu("Panels");
        mpanels.setMnemonic(KeyEvent.VK_P);
        ChkboxListener clistener = new ChkboxListener();
        for (int i = 0; i < objTables.length; i++) {
            menuPanelEna[i] = new JCheckBoxMenuItem(objTables[i].descr + "s");
            menuPanelEna[i].setSelected(true);
            menuPanelEna[i].addActionListener(clistener);
            mpanels.add(menuPanelEna[i]);
        }
        jMenuView.add(mpanels);

        JMenuItem mexit = new JMenuItem("Exit", KeyEvent.VK_X);
        mexit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionExit();
            }
        });
        jMenuFile.add(mexit);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuView = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jMenuFile.setMnemonic('f');
        jMenuFile.setText("File");
        jMenuBar.add(jMenuFile);

        jMenuView.setMnemonic('v');
        jMenuView.setText("View");
        jMenuBar.add(jMenuView);

        jMenuHelp.setMnemonic('h');
        jMenuHelp.setText("Help");
        jMenuBar.add(jMenuHelp);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 633, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 427, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SclMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SclMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SclMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SclMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SclMain().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenu jMenuView;
    // End of variables declaration//GEN-END:variables
}
