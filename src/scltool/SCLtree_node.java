/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author dell
 */
public class SCLtree_node {
    private static final int FLAG_TIMESTAMP = 0x01;
    private static final int FLAG_QUALITY = 0x02;
    private static final int FLAG_STRUCT = 0x04;
    private static final int FLAG_DS = 0x080;
    private static final int FLAG_DA = 0x100;
    private static final int FLAG_DO = 0x200;
    private static final int FLAG_LN = 0x400;
    private static final int FLAG_LD = 0x800;
    private static final int FLAG_TYMASK = 0xF80;
    private static final String S_FCDA = "FCDA";
    public JTree scdTree;
    private static boolean visibleRoot;
    private static Node ndTypeTemplates;
    private static sclTreeElem treeRoot = null;


    public SCLtree_node(Document xmldoc) {
        procMain(xmldoc);

        scdTree = new JTree(treeRoot);
        scdTree.setRootVisible(visibleRoot);
        scdTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                sclTreeElem te = (sclTreeElem) scdTree.getLastSelectedPathComponent();

                /* if nothing is selected */
                if (te == null)
                    return;

                /* retrieve the node that was selected */
                /*String info = "";
                int length = te.attrs.length;
                for (int i = 0; i < length; i++) {
                    info += te.attrs[i][0].toString();
                    info += " : ";
                    info += te.attrs[i][1].toString();
                    info += "\n";
                }*/
                SclMain.textAttrib.setText(loadAttributes(te.xmlNode));
                updateSelected(te);
            }
        });

        //if (leafIcon != null) {
        //DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        //Icon aaa = renderer.getDefaultLeafIcon();
        //renderer.setOpenIcon(aaa);
        //scdTree.setCellRenderer(renderer);
        //}
    }


    protected class sclTreeElem extends DefaultMutableTreeNode {
        public Node xmlNode;
        public String type;
        //public Object[][] attrs;
        public Object sclname;
        public String fc;
        public int flags = 0;

        public sclTreeElem(String name, Node nd, String ty, Object nm, String fcc, int fl) {
            super(name);    // Name to show in the tree e.g. 'LN: LLN0'
            xmlNode = nd;
            type = ty;      // Type is used to find Data Type Templates
            sclname = nm;   // Name from SCL document, this is array of strings for LN names
            //attrs = loadAttributes(nd);
            fc = fcc;
            flags |= fl;
        }

        public Object cloneDA() {
            sclTreeElem newel, te;
            int chcount;

            newel = (sclTreeElem) super.clone();    // Clone this element

            chcount = this.getChildCount();

            for (int i = 0; i < chcount; i++) { // Clone children
                te = (sclTreeElem) this.getChildAt(i);
                newel.add((sclTreeElem) te.cloneDA());
            }
            return newel;
        }
    }

    protected class dsContents extends LnName {
        public String ldInst;
        public String doName;
        public String daName;

        public dsContents(String ldInst, String prefix, String lnClass, String lnInst, String doName, String daName) {
            super(prefix, lnClass, lnInst);
            this.ldInst = ldInst;
            this.doName = doName;
            this.daName = daName;
        }
    }


    private void procMain(Document doc) {
        NodeList nList;

        nList = doc.getElementsByTagName("DataTypeTemplates");
        if (nList.getLength() == 1) {
            ndTypeTemplates = nList.item(0);
        }
        else
            return;

        // Process IED nodes
        procIED(doc);

        // Find DO and DA elements specified in datasets and clone them as children of the dataset
        if ((SclMain.vflags & SclMain.FLAG_DSRCB) > 0)
            procfindDS(treeRoot);
    }


    private void procIED(Document doc) {
        NodeList nList;
        String iedname;
        Node nd;

        nList = doc.getElementsByTagName("IED");
        int iedcount = nList.getLength();
        switch (iedcount) {
        case 0:     // System Specification Document (SSD) may contain no IED nodes
            break;

        case 1:
            nd = nList.item(0);
            iedname = getAttribute(nd, "name");
            treeRoot = new sclTreeElem("IED: " + iedname, nd, null, iedname, "", 0);
            visibleRoot = true;
            procAP(nd, treeRoot);
            break;

        default:
            treeRoot = new sclTreeElem("SCL", doc, null, null, "", 0);
            visibleRoot = false;
            for (int i = 0; i < iedcount; i++) {
                nd = nList.item(i);
                iedname = getAttribute(nd, "name");
                sclTreeElem iedte = new sclTreeElem("IED: " + iedname, nd, null, iedname, "", 0);
                if ((SclMain.vflags & SclMain.FLAG_SERVICES) > 0)
                    procServices(nd, iedte);
                procAP(nd, iedte);
                treeRoot.add(iedte);
            }
            break;
        }
    }


    private void procServices(Node parent, sclTreeElem pte) {
        Node ndServ, nd;
        NodeList children;
        int len;

        if ((ndServ = getChildNode(parent, "Services")) != null) {
            sclTreeElem servte = new sclTreeElem("Services", ndServ, null, null, "", 0);
            children = ndServ.getChildNodes();
            len = children.getLength();
            for (int i = 0; i < len; i++) {
                nd = children.item(i);
                if (nd.getNodeType() == Node.ELEMENT_NODE) {
                    sclTreeElem te = new sclTreeElem(nd.getNodeName(), nd, null, null, "", 0);
                    servte.add(te);
                }
            }
            pte.add(servte);
        }
    }


    private void procAP(Node parent, sclTreeElem pte) {
        Node ndAP, ndServ;
        String apname;

        for (ndAP = getChildNode(parent, "AccessPoint"); ndAP != null; ndAP = getNextNode(ndAP, "AccessPoint")) {
            apname = getAttribute(ndAP, "name");
            sclTreeElem tnap = new sclTreeElem("AccessPoint", ndAP, null, apname, "", 0);

            for (ndServ = getChildNode(ndAP, "Server"); ndServ != null; ndServ = getNextNode(ndServ, "Server")) {
                //nname = servNode.getNodeName();
                sclTreeElem tnserv = new sclTreeElem("Server", ndServ, null, null, "", 0);
                procLD(ndServ, tnserv);
                tnap.add(tnserv);
            }
            pte.add(tnap);
        }
    }


    private void procLD(Node parent, sclTreeElem pte) {
        NodeList children = parent.getChildNodes();
        Node nd;
        int len;
        String ldname;


        if (children != null) {
            len = children.getLength();
            for (int i = 0; i < len; i++) {
                nd = children.item(i);
                switch (nd.getNodeName()) {
                case "LDevice":
                    if ((ldname = getAttribute(nd, "inst")) != null) {
                        sclTreeElem te = new sclTreeElem("LD: " + ldname, nd, null, ldname, "", FLAG_LD);
                        procLN(nd, te);
                        pte.add(te);
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }


    private void procLN(Node parent, sclTreeElem pte) {
        NodeList children = parent.getChildNodes();
        Node nd;
        int len;
        String lnClass, prefix, lnname, lninst, lnType;


        if (children != null) {
            len = children.getLength();
            for (int i = 0; i < len; i++) {
                nd = children.item(i);
                switch (nd.getNodeName()) {
                case "LN":
                case "LN0":
                    if ((lnClass = getAttribute(nd, "lnClass")) != null) {
                        prefix = getAttribute(nd, "prefix");
                        lninst = getAttribute(nd, "inst");
                        lnType = getAttribute(nd, "lnType");

                        lnname = "LN: ";
                        if (prefix != null) {
                            lnname += prefix;
                        }
                        lnname += lnClass;
                        if (lninst != null) {
                            //if (lninst.equals(""))
                            //    lninst = null;  // Make null if empty
                            //else
                                lnname += lninst;
                        }

                        LnName sclname = new LnName(prefix, lnClass, lninst);
                        sclTreeElem te = new sclTreeElem(lnname, nd, lnType, sclname, "", FLAG_LN);
                        if ((SclMain.vflags & SclMain.FLAG_DSRCB) > 0) {
                            procRCB(nd, te);
                            procDS(nd, te);
                        }
                        procLNtypes(te);
                        procDOI(nd, te, "DOI");
                        pte.add(te);
                    }
                    break;

                default:
                    break;
                }
            }
        }
    }


    private void procDOI(Node parent, sclTreeElem pte, String name) {
        Node nddoi;
        String doname;
        sclTreeElem te;

        for (nddoi = getChildNode(parent, name); nddoi != null; nddoi = getNextNode(nddoi, name)) {
            doname = getAttribute(nddoi, "name");
            for (int i = 0; i < pte.getChildCount(); i++) {
                te = (sclTreeElem) pte.getChildAt(i);
                if (doname.equals(te.sclname)) {
                    procDOI(nddoi, te, "SDI");
                    procDAI(nddoi, te);
                }
            }
        }
    }


    private void procDAI(Node parent, sclTreeElem pte) {
        Node nddai, ndval;
        String daname, val;
        sclTreeElem te;

        for (nddai = getChildNode(parent, "DAI"); nddai != null; nddai = getNextNode(nddai, "DAI")) {
            daname = getAttribute(nddai, "name");
            for (int i = 0; i < pte.getChildCount(); i++) {
                te = (sclTreeElem) pte.getChildAt(i);
                if (daname.equals(te.sclname)) {
                    for (ndval = getChildNode(nddai, "Val"); ndval != null; ndval = getNextNode(ndval, "Val")) {
                        if (
                                (ndval.getFirstChild() != null) &&
                                ((val = ndval.getFirstChild().getNodeValue()) != null)) {
                            sclTreeElem vale = new sclTreeElem(val, ndval, daname, val, "", 0);
                            te.add(vale);
                        }
                    }
                }
            }
        }
    }


    private void procRCB(Node parent, sclTreeElem pte) {
        Node nd;
        String name;

        for (nd = getChildNode(parent, "ReportControl"); nd != null; nd = getNextNode(nd, "ReportControl")) {
            name = getAttribute(nd, "name");
            sclTreeElem te = new sclTreeElem("RCB: " + name, nd, "ReportControl", name, "", 0);
            addChildNode(nd, te, "TrgOps");
            addChildNode(nd, te, "OptFields");
            addChildNode(nd, te, "RptEnabled");
            pte.add(te);
        }
    }


    private void procDS(Node parent, sclTreeElem pte) {
        Node nd, fnd;
        String name, fcc, fcda;
        String ldInst, prefix, lnClass, lnInst, doName, daName;

        for (nd = getChildNode(parent, "DataSet"); nd != null; nd = getNextNode(nd, "DataSet")) {
            name = getAttribute(nd, "name");
            sclTreeElem te = new sclTreeElem("DS: " + name, nd, "DataSet", name, "", 0);
            for (fnd = getChildNode(nd, S_FCDA); fnd != null; fnd = getNextNode(fnd, S_FCDA)) {
                fcda = "";
                if ((ldInst = getAttribute(fnd, "ldInst")) != null)
                    fcda += ldInst + "/";
                if ((prefix = getAttribute(fnd, "prefix")) != null)
                    fcda += prefix;
                if ((lnClass = getAttribute(fnd, "lnClass")) != null)
                    fcda += lnClass;
                if ((lnInst = getAttribute(fnd, "lnInst")) != null)
                    fcda += lnInst;
                if ((doName = getAttribute(fnd, "doName")) != null)
                    fcda += "." + doName;
                if ((daName = getAttribute(fnd, "daName")) != null)
                    fcda += "." + daName;
                fcc = getAttribute(fnd, "fc");
                fcda += " [" + fcc + "]";

                dsContents sclname = new dsContents(ldInst, prefix, lnClass, lnInst, doName, daName);
                sclTreeElem fte = new sclTreeElem(fcda, fnd, S_FCDA, sclname, fcc, FLAG_DS);
                te.add(fte);
            }
            pte.add(te);
        }
    }


    private void procLNtypes(sclTreeElem pte) {
        Node nd;

        for (nd = getChildNode(ndTypeTemplates, "LNodeType"); nd != null; nd = getNextNode(nd, "LNodeType")) {
            if (getAttribute(nd, "id").equals(pte.type)) {
                 procDODA(nd, pte);
            }
        }
    }


    private void procDODA(Node parent, sclTreeElem pte) {
        NodeList children = parent.getChildNodes();
        Node nd;
        int len;
        String nName, dodaName, dodaType, bType;

         if (children != null) {
            len = children.getLength();
            for (int i = 0; i < len; i++) {
                nd = children.item(i);
                nName = nd.getNodeName();
                switch (nName) {
                case "DO":      // DO can only appear in <LNodeType> node
                case "SDO":     // SDO can only appear in <DOType> node
                    if (
                            ((dodaName = getAttribute(nd, "name")) != null) &&
                            ((dodaType = getAttribute(nd, "type")) != null)) {
                        sclTreeElem tn = new sclTreeElem(nName + ": " + dodaName, nd, dodaType, dodaName, "", FLAG_DO);
                        procDODAtypes(tn, "DOType");
                        pte.add(tn);
                    }
                    break;

                case "DA":      // DA can only appear in <DOType> node
                case "BDA":     // BDA can only appear in <DAType> node
                    if ((dodaName = getAttribute(nd, "name")) != null) {
                        if ((bType = getAttribute(nd, "bType")) != null) {
                            dodaType = getAttribute(nd, "type");
                            int flags = procFlags(bType) | FLAG_DA;
                            sclTreeElem tn = new sclTreeElem(nName + ": " + dodaName, nd, dodaType, dodaName, getAttribute(nd, "fc"), flags);
                            if (dodaType != null) {
                                procDODAtypes(tn, "DAType");
                            }
                            pte.add(tn);
                        }
                    }
                    break;

                default:
                    break;
                }
            }
        }
    }


    private void procDODAtypes(sclTreeElem pte, String dodatype) {
        Node nd;

        for (nd = getChildNode(ndTypeTemplates, dodatype); nd != null; nd = getNextNode(nd, dodatype)) {
            if (getAttribute(nd, "id").equals(pte.type)) {
                 procDODA(nd, pte);
            }
        }
    }


    private void procfindDS(sclTreeElem pte) {
        sclTreeElem te;
        int chcount = pte.getChildCount();

        for (int i = 0; i < chcount; i++) {
            te = (sclTreeElem) pte.getChildAt(i);

            if ((te.flags & FLAG_DS) > 0) {   // This is a Dataset
                proclinkDS(treeRoot, te, te, null, FLAG_LD);
            }
            else {
                procfindDS(te);
            }
        }
    }


    private void proclinkDS(sclTreeElem pte, sclTreeElem ds, sclTreeElem newp, String doname, int level) {
        sclTreeElem te, newel;
        int chcount = pte.getChildCount();
        int dot;

        for (int i = 0; i < chcount; i++) {
            te = (sclTreeElem) pte.getChildAt(i);

            if ((te.flags & level) > 0) {   // Required level found
                switch (level) {
                case FLAG_LD:
                    if (compareNames((String) te.sclname, ((dsContents) ds.sclname).ldInst)) {
                        proclinkDS(te, ds, ds, null, FLAG_LN);
                        return;
                    }
                    break;

                case FLAG_LN:
                    if (!compareNames(((LnName) te.sclname).prefix, ((dsContents) ds.sclname).prefix))
                        continue;
                    if (!compareNames(((LnName) te.sclname).lnClass, ((dsContents) ds.sclname).lnClass))
                        continue;
                     if (!compareNames(((LnName) te.sclname).lnInst, ((dsContents) ds.sclname).lnInst))
                        continue;

                    proclinkDS(te, ds, ds, ((dsContents) ds.sclname).doName, FLAG_DO);
                    return;

                case FLAG_DO:
                    if (doname != null) {    // DO name passed as argument
                        if ((dot = doname.indexOf('.')) != -1) {     // Structured name, contains dot '.'
                            if (compareNames((String) te.sclname, doname.substring(0, dot))) {
                                proclinkDS(te, ds, newp, doname.substring(dot + 1), FLAG_DO);
                                return;
                            }
                        }
                        else {  // Name doesn't contain dot '.'
                            if (compareNames((String) te.sclname, doname)) {
                                proclinkDS(te, ds, newp, null, FLAG_DO);
                                if (newp.getChildCount() == 0) {
                                    proclinkDS(te, ds, newp, ((dsContents) ds.sclname).daName, FLAG_DA);
                                }
                                return;
                            }
                        }
                    }
                    else {  // DO name not passed, add all DOs at this level. This happens when dataset contains structured name e.g. 'PPV'
                        newel = (sclTreeElem) te.clone();
                        proclinkDS(te, ds, newel, ((dsContents) ds.sclname).daName, FLAG_DA);
                        newp.add(newel);
                    }
                    break;

                case FLAG_DA:
                    if (((dsContents) ds.sclname).daName != null) {
                        if (!compareNames((String) te.sclname, ((dsContents) ds.sclname).daName))
                            continue;
                    }

                    if (ds.fc.equals(te.fc)) {
                        newp.add((sclTreeElem) te.cloneDA());
                    }
                    break;
                }
            }
            else {
                proclinkDS(te, ds, newp, doname, level);
            }
        }
    }


    private Node getChildNode(Node parent, String name) {
        NodeList children = parent.getChildNodes();
        Node nd;
        int len;

        if (children != null) {
            len = children.getLength();
            for (int i = 0; i < len; i++) {
                nd = children.item(i);
                if (nd.getNodeName().equals(name)) {
                    return nd;
                }
            }
        }
        return null;
    }


    private void addChildNode(Node parent, sclTreeElem pte, String name) {
        Node nd;

        if ((nd = getChildNode(parent, name)) != null) {
            sclTreeElem te = new sclTreeElem(name, nd, null, name, "", 0);
            pte.add(te);
        }
    }


    private Node getNextNode(Node current, String name) {
        Node nd;
        for (nd = current.getNextSibling(); nd != null; nd = nd.getNextSibling()) {
            if (nd.getNodeName().equals(name)) {
                return nd;
            }
        }
        return null;
    }


    private String getAttribute(Node parent, String name) {
        NamedNodeMap attrs;
        Node nd;

        if ((attrs = parent.getAttributes()) != null) {
            if ((nd = attrs.getNamedItem(name)) != null) {
                return nd.getNodeValue();
            }
        }
        return null;
    }


    private int procFlags(String bType) {
        int flags = 0;

        switch (bType) {
        case "Quality":
            flags |= FLAG_QUALITY;
            break;
        case "Timestamp":
            flags |= FLAG_TIMESTAMP;
            break;
        case "Struct":
            flags |= FLAG_STRUCT;
            break;
        }
        return flags;
    }


    private void updateSelected(sclTreeElem current) {
        int daflags;

        selectedSCL.clear();

        daflags = traverseParents(current);

        if (
                (selectedSCL.fc != null) &&         // Selected element is shown only if FC was found and
                ((current.flags & FLAG_DA) > 0)) {  // it has DA flag, this excludes DAI <Val></Val>
            switch (selectedSCL.fc) {
            case "ST":
            case "MX":
                if ((daflags & FLAG_STRUCT) > 0) {  // Data attribute other than bType="Struct" must be selected
                    SclMain.newSelected(true);
                    return;
                }
                break;
            case "CO":
                SclMain.newSelected(true);          // Doesn't matter which data attribute is selected for CO
                return;
            default:
                break;
            }
        }
        SclMain.newSelected(false);
    }


    private int traverseParents(sclTreeElem current) {
        sclTreeElem te;
        int daflags = 0;


        for (te = current; te != null; te = (sclTreeElem) te.getParent()) {
            switch (te.flags & FLAG_TYMASK) {
            case FLAG_LD:
                selectedSCL.ldname = (String) te.sclname;
                break;

            case FLAG_LN:
                selectedSCL.lnobj = (LnName) te.sclname;
                break;

            case FLAG_DO:
                if (selectedSCL.doname == null)
                    selectedSCL.doname = (String) te.sclname;
                else
                    selectedSCL.doname = (String) te.sclname + "." + selectedSCL.doname;
                break;

            case FLAG_DA:
                if ((te.flags & (FLAG_QUALITY | FLAG_TIMESTAMP)) == 0) { // Selected DA is not 'q' nor 't'
                    if (selectedSCL.daname == null)
                        selectedSCL.daname = (String) te.sclname;
                    else
                        selectedSCL.daname = (String) te.sclname + "." + selectedSCL.daname;

                    if ((te.flags & FLAG_STRUCT) == 0) {    // Selected DA does not have bType="Struct"
                        daflags |= FLAG_STRUCT;             // Inverse use of the flag here
                    }
                    if (te.fc != null) {            // Selected DA has FC
                        selectedSCL.fc = te.fc;
                        daflags |= checkQT(te);     // Check if there are 'q' and 't' attributes with the same FC
                    }
                }
                break;

            case FLAG_DS:
                if (selectedSCL.doname == null)
                    selectedSCL.doname = ((dsContents) te.sclname).doName;
                else
                    selectedSCL.doname = ((dsContents) te.sclname).doName + "." + selectedSCL.doname;

                selectedSCL.lnobj = (LnName) te.sclname;
                selectedSCL.ldname = ((dsContents) te.sclname).ldInst;
                return daflags;
            }
        }
        return daflags;
    }


    private int checkQT(sclTreeElem current) {
        //int chcount = current.getChildCount();
        sclTreeElem te;
        int flags = 0;

        for (te = (sclTreeElem) ((sclTreeElem) current.getParent()).getFirstChild(); te != null; te = (sclTreeElem) te.getNextSibling()) {
            if (selectedSCL.fc.equals(te.fc)) {
                flags |= (te.flags & (FLAG_QUALITY | FLAG_TIMESTAMP));
            }
        }
        return flags;
    }


    private boolean compareNames(String s1, String s2) {

        if (s1 != null) {
            if (s2 != null) {
                if (s1.equals(s2))
                     return true;
            }
            else if (s1.isEmpty())
                return true;
        }
        else if (s2 != null) {
            if (s2.isEmpty())
                return true;
        }
        else
            return true;
        return false;
    }


    private static String loadAttributes(Node nd) {
        NamedNodeMap nnm;
        Node attr;
        String result = "";
        int len;

        if ((nnm = nd.getAttributes()) != null) {
            len = nnm.getLength();
            for (int i = 0; i < len; i++) {
                attr = nnm.item(i);
                result += attr.getNodeName() + ": " + attr.getNodeValue() + "\n";
            }
        }
        return result;
    }
}
