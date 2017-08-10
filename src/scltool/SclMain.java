package scltool;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;
import org.jdom2.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


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
    protected final static int FLAG_SERVICES = 0x01;
    protected final static int FLAG_DSRCB = 0x02;
    private static final String OUTPUTXML = "output.xml";
    private static final String SCHEMAFILE = "IEC61850cl.xsd";
    public static final JTextArea textAttrib = new JTextArea();
    public static final JTextArea textSelected = new JTextArea();
    SCLtree_node myTree;
    private Document scldoc;
    private JMenuItem menuItemAbout;
    private JMenuItem menuItemOpen;
    private JMenuItem menuItemExit;
    private JMenu menuShow;
    private JCheckBoxMenuItem menuItemServices, menuItemDsRcb;
    public static int vflags = FLAG_DSRCB;
    private static final String SWVERSION = "V1.0";
    private static String buildDate = "Not available";
    private static final String MAINTITLE = "SCL browser";
    private static final String COPYRIGHT = "© 2017 Londelec UK Ltd\nThis program comes with absolutely no warranty.";
    private static File lastpath;
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    TableXml tableDI, tableAI, tableDO;
    private static final JButton buttonAdd = new JButton("Add");
    private static final JButton buttonDel = new JButton("Delete");
    private static final JButton buttonExpXML = new JButton("Export XML");
    private static final JButton buttonExpCsv = new JButton("Export CSV");
    private static final JLabel labelDI = new JLabel("Digital Inputs");
    private static final JLabel labelAI = new JLabel("Analog Inputs");
    private static final JLabel labelDO = new JLabel("Digital Outputs");
    private static final JLabel fwversion = new JLabel("Firmware V?");
    private static final FileNameExtensionFilter cidfilter = new FileNameExtensionFilter("CID & SCD Files", "cid", "scd");
    private static final JScrollPane treePane = new JScrollPane();

    /**
     * Creates new form SCLmain
     */
    public SclMain() {
        initComponents();
        initMenu();
        if (initSchemas() == false)
             System.exit(0);
        initPanel();
        initRevision();
        //System.gc();

        /*IEC61850ClConfig iec = new IEC61850ClConfig();
        DITableType diTable = new DITableType();
        DIType di = new DIType();
        iec.setDITable(diTable);
        diTable.getDI().add(di);*/
    }


    private boolean initSchemas() {
        Document schema;
        NodeList nList;

        if ((schema = readFile(new File(SCHEMAFILE), true)) != null) {
            if ((nList = getNodesXpath(schema, "schema/attribute::version")) != null) {
                if (nList.getLength() == 1) {
                    fwversion.setText("Firmware V" + nList.item(0).getNodeValue());
                }
            }
            if ((nList = getNodesXpath(schema, "//complexType[@name='DIType']/attribute/attribute::name")) != null) {
                tableDI = new TableXml(nList, "DITable", "DI");
            }
            if ((nList = getNodesXpath(schema, "//complexType[@name='AIType']/attribute/attribute::name")) != null) {
                tableAI = new TableXml(nList, "AITable", "AI");
            }
            if ((nList = getNodesXpath(schema, "//complexType[@name='DOType']/attribute/attribute::name")) != null) {
                tableDO = new TableXml(nList, "DOTable", "DO");
            }
            return true;
        }
        return false;
    }


    private void initPanel() {
        JPanel panelText = new JPanel();
        JPanel panelTabs = new JPanel();
        JPanel panelBut = new JPanel();
        JPanel panelSpring = new JPanel();


        //createTree(new File("vamp300.cid"));
        /*if ((scldoc = readFile(new File("H4_9021_L24.cid"))) != null) {
            vflags |= FLAG_DSRCB;
            createTree();
        }*/
        treePane.setPreferredSize(new Dimension(200, 10));


        JScrollPane scrollDI = new JScrollPane(tableDI);
        JScrollPane scrollAI = new JScrollPane(tableAI);
        JScrollPane scrollDO = new JScrollPane(tableDO);
        Dimension tdim = scrollDI.getPreferredSize();
        tdim.height = 200;
        scrollDI.setPreferredSize(tdim);
        scrollAI.setPreferredSize(tdim);
        scrollDO.setPreferredSize(tdim);


        fwversion.setToolTipText("Compatible with leandc firmware version");
        buttonAdd.setEnabled(false);
        buttonAdd.setToolTipText("Add selected element to table");
        buttonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 buttonAddClick(ae);
            }
        });
        buttonDel.setToolTipText("Delete selected rows from all tables");
        buttonDel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 buttonDelClick(ae);
            }
        });
        buttonExpXML.setToolTipText("Export configuration data to '" + OUTPUTXML + "' file");
        buttonExpXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 buttonExpClick(ae);
            }
        });
        buttonExpCsv.setToolTipText("Export configuration data to 'DI.csv', 'AI.csv', 'DO.csv' files");
        buttonExpCsv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 buttonCsvClick(ae);
            }
        });
        //FlowLayout fl = new FlowLayout(FlowLayout.CENTER);
        //panelBut.setLayout(fl);
        panelBut.setLayout(new FlowLayout(FlowLayout.CENTER));
        panelBut.add(buttonDel);
        panelBut.add(buttonExpXML);
        panelBut.add(buttonExpCsv);


        textAttrib.setPreferredSize(new Dimension(100, 200));
        //textAttrib.setMinimumSize(new Dimension(170, 200));
        JScrollPane scrollAttrib = new JScrollPane(textAttrib);
        //scrollAttrib.setViewportView(textAttrib);
        TitledBorder tbAttrib = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Attributes", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP);
        tbAttrib.setTitleFont(UIManager.getFont("TitledBorder.font").deriveFont(Font.PLAIN));
        scrollAttrib.setBorder(tbAttrib);

        //textSelected.setPreferredSize(new Dimension(100, 200));
        //scrollSelected.setViewportView(textSelected);
        JScrollPane scrollSelected = new JScrollPane(textSelected);
        TitledBorder tbSelected = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Selected", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP);
        tbSelected.setTitleFont(UIManager.getFont("TitledBorder.font").deriveFont(Font.PLAIN));
        scrollSelected.setBorder(tbSelected);


        JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollAttrib, scrollSelected);

        GroupLayout textgl = new GroupLayout(panelText);
        panelText.setLayout(textgl);
        textgl.setHorizontalGroup(textgl.createParallelGroup(CENTER)
            .addComponent(splitPane1)
            .addComponent(buttonAdd, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
        );
        textgl.setVerticalGroup(textgl.createParallelGroup(LEADING)
            .addGroup(textgl.createSequentialGroup()
                .addComponent(splitPane1)
                .addComponent(buttonAdd, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addContainerGap())
        );


        SpringLayout slayout = new SpringLayout();
        panelSpring.setLayout(slayout);
        panelSpring.setMaximumSize(new Dimension(32767, 35));
        panelSpring.add(panelBut);
        panelSpring.add(fwversion);
        slayout.putConstraint(SpringLayout.HORIZONTAL_CENTER, panelBut, 0, SpringLayout.HORIZONTAL_CENTER, panelSpring);
        slayout.putConstraint(SpringLayout.EAST, fwversion, -5, SpringLayout.EAST, panelSpring);
        slayout.putConstraint(SpringLayout.VERTICAL_CENTER, fwversion, 0, SpringLayout.VERTICAL_CENTER, panelSpring);
        //slayout.putConstraint(SpringLayout.VERTICAL_CENTER, panelBut, 0, SpringLayout.VERTICAL_CENTER, panelSpring);
        slayout.putConstraint(SpringLayout.HEIGHT, panelSpring, 0, SpringLayout.HEIGHT, panelBut);
        //Spring aaa = Spring.constant(0, -2, Spring.UNSET);
        //slayout.putConstraint(SpringLayout.WEST, fwversion, aaa, SpringLayout.EAST, panelBut);


        GroupLayout tabgl = new GroupLayout(panelTabs);
        panelTabs.setLayout(tabgl);
        //Dimension aaa = panelSpring.getPreferredSize();
        panelSpring.setPreferredSize(new Dimension(560, 35));
        tabgl.setHorizontalGroup(tabgl.createParallelGroup(CENTER)
            .addGroup(tabgl.createParallelGroup(LEADING)
                .addComponent(labelDI)
                .addComponent(scrollDI))
            .addGroup(tabgl.createParallelGroup(LEADING)
                .addComponent(labelAI)
                .addComponent(scrollAI))
            .addGroup(tabgl.createParallelGroup(LEADING)
                .addComponent(labelDO)
                .addComponent(scrollDO))
            .addComponent(panelSpring)
        );
        tabgl.setVerticalGroup(tabgl.createParallelGroup(LEADING)
            .addGroup(tabgl.createSequentialGroup()
                .addComponent(labelDI)
                .addComponent(scrollDI)
                .addComponent(labelAI)
                .addComponent(scrollAI)
                .addComponent(labelDO)
                .addComponent(scrollDO)
                .addComponent(panelSpring)
                .addContainerGap())
        );


        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePane, panelText);
        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane2, panelTabs);

        BoxLayout blayout = new BoxLayout(getContentPane(), BoxLayout.X_AXIS);
        getContentPane().setLayout(blayout);
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


    private void createTree() {
        myTree = new SCLtree_node(scldoc);
        treePane.setViewportView(myTree.scdTree);

        /*SCLtree_jaxb myTree = new SCLtree_jaxb();
        //JTree scdTree = new JTree(myTree.treeInit("vamp300.cid"));
        DefaultMutableTreeNode ttt = myTree.treeInit("vamp300.cid");
        JTree scdTree = new JTree();
        treePane.setViewportView(scdTree);*/
    }


    private void buttonAddClick(ActionEvent ae) {
        switch (selectedSCL.chooseType()) {
        case 1:
            tableDI.addRow();
            break;
        case 2:
            tableAI.addRow();
            break;
        case 3:
            tableDO.addRow();
            break;
        }
        //myTree.scdTree.clearSelection();
    }


    private void buttonDelClick(ActionEvent ae) {
        tableDI.removeRow();
        tableAI.removeRow();
        tableDO.removeRow();
    }


    private void buttonExpClick(ActionEvent ae) {
        Element root = new Element("objects");

        root.addContent(tableDI.exportXml());
        root.addContent(tableAI.exportXml());
        root.addContent(tableDO.exportXml());

        org.jdom2.Document newDocument = new org.jdom2.Document(root);
        Format ff = Format.getPrettyFormat();
        ff.setIndent("\t");
        XMLOutputter serializer = new XMLOutputter(ff);

        try {
            serializer.output(newDocument, new FileOutputStream(OUTPUTXML));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void buttonCsvClick(ActionEvent ae) {
        writeCsv("DI.csv", tableDI.exportCsv());
        writeCsv("AI.csv", tableAI.exportCsv());
        writeCsv("DO.csv", tableDO.exportCsv());
    }


    private void writeCsv(String filename, String csv) {
        FileWriter writer;
        try {
            writer = new FileWriter(filename);
            writer.append(csv);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    protected NodeList getNodesXpath(Document doc, String expr) {
        XPathExpression result;
        NodeList nList = null;

        XPath xpath = XPathFactory.newInstance().newXPath();
        //XPathExpression ccc = xpath.compile("//complexType[@name='DIType']/attribute/attribute::name");

        try {
            result = xpath.compile(expr);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }


        try {
            nList = (NodeList) result.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(TableXml.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nList;
    }


    private Document readFile(File filename, boolean checkexists) {
        Document doc = null;
        DocumentBuilder dBuilder;
        File checkPath;

        if (checkexists) {
            if (!(filename.exists())) {
                CodeSource cs = SclMain.class.getProtectionDomain().getCodeSource();
                try {
                    File jarFile = new File(cs.getLocation().toURI().getPath());
                    checkPath = new File(jarFile.getParentFile(), "/" + filename.getName());

                    if (checkPath.exists()) {
                        filename = checkPath;
                    }
                    //JOptionPane.showMessageDialog(this, checkPath, "About SCL tool", JOptionPane.PLAIN_MESSAGE);
                } catch (URISyntaxException ex) {
                }
            }
        }

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(filename);
            doc.getDocumentElement().normalize();
                //String sroot = doc.getDocumentElement().getNodeName();
        } catch (SAXException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ParserConfigurationException ex) {
            JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            //Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return doc;
    }


    public static void newSelected(boolean valid) {

        buttonAdd.setEnabled(valid);
        if (valid == true) {
            textSelected.setText(selectedSCL.update());
        }
        else {
            textSelected.setText("");
        }
    }


    private void initMenu() {
        setTitle(MAINTITLE);
        menuItemAbout = new JMenuItem("About", KeyEvent.VK_A);
        //menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        //menuItemAbout.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menuItemAbout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionAbout(ae);
            }
        });
        jMenuHelp.add(menuItemAbout);

        menuItemOpen = new JMenuItem("Open", KeyEvent.VK_O);
        menuItemOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionOpen(ae);
            }
        });
        jMenuFile.add(menuItemOpen);

        menuShow = new JMenu("Show");
        menuShow.setMnemonic(KeyEvent.VK_S);
        jMenuView.add(menuShow);
        menuItemServices = new JCheckBoxMenuItem("Services");
        menuShow.add(menuItemServices);
        menuItemDsRcb = new JCheckBoxMenuItem("Datasets & Report Blocks");
        menuShow.add(menuItemDsRcb);
        menuItemDsRcb.setState(((vflags & FLAG_DSRCB) == 0) ? false : true);

        ActionListener alShow = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionShow(ae);
            }
        };
        menuItemServices.addActionListener(alShow);
        menuItemDsRcb.addActionListener(alShow);

        menuItemExit = new JMenuItem("Exit", KeyEvent.VK_X);
        menuItemExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionExit(ae);
            }
        });
        jMenuFile.add(menuItemExit);
    }


    private void actionAbout(ActionEvent ae) {
        Object message = "SCL file browser " + SWVERSION + "\nBuild date: " + buildDate + "\n" + COPYRIGHT;
        JOptionPane.showMessageDialog(this, message, "About SCL tool", JOptionPane.PLAIN_MESSAGE);
    }

    private void actionOpen(ActionEvent ae) {
        JFileChooser fc = new JFileChooser(lastpath);   // Navigate to the location of the last file open
        fc.setFileFilter(cidfilter);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastpath = fc.getSelectedFile();
            setTitle(lastpath.getName() + " - " + MAINTITLE);   // Sets dialo title
            if ((scldoc = readFile(lastpath, false)) != null) {
                createTree();
            }
        }
    }

    private void actionShow(ActionEvent ae) {
        Object src = ae.getSource();

        if (src == menuItemServices) {
            if (menuItemServices.getState()) {
                vflags |= FLAG_SERVICES;
            }
            else {
                vflags &= ~FLAG_SERVICES;
            }

        }
        else if (src == menuItemDsRcb) {
            if (menuItemDsRcb.getState()) {
                vflags |= FLAG_DSRCB;
            }
            else {
                vflags &= ~FLAG_DSRCB;
            }
        }
        if (scldoc != null)
            createTree();
    }

    private void actionExit(ActionEvent ae) {
        System.exit(0);
    }


    private void initRevision() {
        long dLong = 0;
        FileSystem jarFS;

        String jpath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
        if (jpath.startsWith("jar:")) {  // When executing from JAR
            //System.out.println("fullpath " + filepath);
            jpath = jpath.substring(0, jpath.indexOf("!/"));
        }
        else {  // When Debugging
            jpath = "jar:file:/home/dell/Documents/Electronics/java/SCLtool/dist/scltool.jar";
        }
        //System.out.println("trimmed " + filepath);


        try {
            jarFS = FileSystems.newFileSystem(URI.create(jpath), Collections.<String, Object>emptyMap());
            Path resourcePath = jarFS.getPath("/META-INF/MANIFEST.MF");
            FileTime fileTime = Files.getLastModifiedTime(resourcePath);
            dLong = fileTime.toMillis();
        } catch (IOException ex) {
            Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
        }


        if (dLong > 0) {
            Date modDate = new Date(dLong);
            buildDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(modDate);
        }
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
