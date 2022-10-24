/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;


/**
 *
 * @author dell
 */
public class SCLtree_node {
    private static final int FLAG_TIMESTAMP = 0x01;
    private static final int FLAG_QUALITY = 0x02;
    private static final String S_FCDA = "FCDA";
    public static enum sclEditions {SCL_ED1, SCL_ED2, SCL_ED21};
    public JTree scdTree;
    public sclEditions scledition;
    private final SclMain mainframe;
    private static boolean visibleRoot;
    private Node ndTypeTemplates;
    private SclTreeElem treeRoot = null;
    private static enum sclEnum {
        SCL_SCL, SCL_IED, SCL_AP, SCL_AP_CH, SCL_SERV, SCL_SERV_CH,
        SCL_COMMS, SCL_COM_CH, SCL_COM_P,
        SCL_SUBST, SCL_SUB_CH,
        SCL_SVCES, SCL_SVC_CH,
        SCL_LD, SCL_LN, SCL_DO, SCL_DA, SCL_VAL,
        SCL_RCB, SCL_RCB_CH, SCL_DS, SCL_FCDA
    }


    public SCLtree_node(SclMain frame, Document xmldoc) {
        mainframe = frame;
        procMain(xmldoc);
        newTree(treeRoot);
    }


    private class SclTreeElem extends DefaultMutableTreeNode {
        public Node xmlNode, xmlEnum;   // xmlNode must not be null
        public String dtype;
        public Object sclname;
        public String fc;               // null or FC string, don't use empty string ""
        public int flags = 0;
        public sclEnum sclty;
        public String errMsg;

        public SclTreeElem(String name, Node nd, String dty, Object nm, sclEnum sty) {
            super(name);        // Name to show in the tree e.g. 'LN: LLN0'
            this.xmlNode = nd;  // XML DOM node
            this.dtype = dty;   // Used to find Data Type Templates
            this.sclname = nm;  // Name from SCL document, this is array of strings for LN names
            this.sclty = sty;   // SCL type enum
        }


        /**
         * Must override because DefaultMutableTreeNode.getFirstChild() throws exception if there are no children
         */
        @Override
        public TreeNode getFirstChild() {
            if (this.getChildCount() > 0)
                return super.getFirstChild();
            return null;
        }


        public Object cloneDA() {
            SclTreeElem newel, te;
            int chcount;

            newel = (SclTreeElem) super.clone();    // Clone this element

            chcount = this.getChildCount();

            for (int i = 0; i < chcount; i++) { // Clone children
                te = (SclTreeElem) this.getChildAt(i);
                newel.add((SclTreeElem) te.cloneDA());
            }
            return newel;
        }
    }


    private class FcdaContent extends LnName {
        public String ldInst;
        public String doName;
        public String daName;

        public FcdaContent(String ldInst, String prefix, String lnClass, String lnInst, String doName, String daName) {
            super(prefix, lnClass, lnInst);
            this.ldInst = ldInst;
            this.doName = doName;
            this.daName = daName;
        }
    }


    private class SclCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree jtree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component rcomp = super.getTreeCellRendererComponent(jtree, value, selected, expanded, leaf, row, hasFocus);

            if (((SclTreeElem) value).errMsg != null) {
                rcomp.setForeground(Color.RED);
            }
            return rcomp;
        }
    }


    private class SclParser {
        public String names[];
        public sclEnum sclty;

        public SclParser(String[] names, sclEnum sty) {
            this.names = names;
            this.sclty = sty;
        }

        public SclTreeElem parser(Element parent, boolean recur) {
            return null;
        }
    }


    private class ConnectedAPParser extends SclParser {
        public ConnectedAPParser(String[] names, sclEnum sty) {
            super(names, sty);
        }

        @Override
        public SclTreeElem parser(Element parent, boolean recur) {
            return new SclTreeElem(parent.getNodeName() + ": " + parent.getAttribute("iedName"), parent, null, null, this.sclty);
        }
    }


    private class AddressPParser extends SclParser {
        public AddressPParser(String[] names, sclEnum sty) {
            super(names, sty);
        }

        @Override
        public SclTreeElem parser(Element parent, boolean recur) {
            Node nd;
            String chval;

            SclTreeElem te = new SclTreeElem("P: " + parent.getAttribute("type"), parent, null, null, this.sclty);

            if (
                    ((nd = parent.getFirstChild()) != null) &&
                    ((chval = nd.getNodeValue()) != null)) {
                te.add(new SclTreeElem(chval, nd, null, null, this.sclty));
            }
            return te;
        }
    }


    private class GSETimeParser extends SclParser {
        public GSETimeParser(String[] names, sclEnum sty) {
            super(names, sty);
        }

        @Override
        public SclTreeElem parser(Element parent, boolean recur) {
            Node nd;
            String chval;

            SclTreeElem te = new SclTreeElem(parent.getNodeName(), parent, null, null, this.sclty);

            if (
                    ((nd = parent.getFirstChild()) != null) &&
                    ((chval = nd.getNodeValue()) != null)) {
                te.add(new SclTreeElem(chval, nd, null, null, this.sclty));
            }
            return te;
        }
    }


    private void newTree(SclTreeElem root) {
        scdTree = new JTree(root);
        scdTree.setRootVisible(visibleRoot);
        mainframe.actionSelected("", null, false);     // Clear selected text

        scdTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                SclTreeElem te = (SclTreeElem) scdTree.getLastSelectedPathComponent();

                /* if nothing is selected */
                if (te == null)
                    return;

                /* retrieve the node that was selected */
                mainframe.actionSelected(loadAttributes(te.xmlNode), te.errMsg, updateSelected(te));
            }
        });

        scdTree.setCellRenderer(new SclCellRenderer());
    }

    // TODO search by Object reference e.g. LD0/LLN0.Mod
    public void filterTree(String flt, boolean ignCase) {
        if (flt.isEmpty()) {
            newTree(treeRoot);
        }
        else {
            SclTreeElem fte = (SclTreeElem) treeRoot.clone();    // Clone this element
            if (ignCase)
                 flt = flt.toLowerCase();
            filterTraverse(treeRoot, fte, flt, ignCase);
            newTree(fte);
        }
    }


    private boolean filterTraverse(SclTreeElem pmte, SclTreeElem pfte, String flt, boolean ignCase) {
        SclTreeElem mte, fte;
        boolean included = false;
        String comp;

        for (mte = (SclTreeElem) pmte.getFirstChild(); mte != null; mte = (SclTreeElem) mte.getNextSibling()) {
            if (mte.sclname != null) {
                switch (mte.sclty) {
                case SCL_LN:
                    comp = "";
                    if (((LnName) mte.sclname).prefix != null)
                        comp += ((LnName) mte.sclname).prefix;
                    if (((LnName) mte.sclname).lnClass != null)
                        comp += ((LnName) mte.sclname).lnClass;
                    if (((LnName) mte.sclname).lnInst != null)
                        comp += ((LnName) mte.sclname).lnInst;
                    break;

                default:
                    comp = mte.sclname.toString();
                    break;
                }

                if (ignCase)
                    comp = comp.toLowerCase();

                if (comp.contains(flt)) {
                    pfte.add((SclTreeElem) mte.cloneDA());
                    included = true;
                    continue;
                }
            }

            if (mte.getChildCount() > 0) {
                fte = (SclTreeElem) mte.clone();    // Clone this element
                if (filterTraverse(mte, fte, flt, ignCase)) {
                    pfte.add(fte);
                    included = true;
                }
            }
        }
        return included;
    }


    private void procMain(Document doc) {
        NodeList nList;
        Element el, ndRoot;

        ndRoot = doc.getDocumentElement();
        getEddition(ndRoot);
        treeRoot = new SclTreeElem("SCL", ndRoot, null, null, sclEnum.SCL_SCL);
        visibleRoot = false;
        //visibleRoot = true;   // For Debug

        /* Must be initialized first */
        nList = doc.getElementsByTagName("DataTypeTemplates");
        if (nList.getLength() == 1) {
            ndTypeTemplates = nList.item(0);
        }
        else
            return;

        for (el = getFirstElement(ndRoot); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "IED":
                procIED(el);
                break;

            case "Communication":
                if ((SclMain.vflags & SclMain.FLAG_COMMS) > 0)
                    procCommunication(el);
                break;

            case "Substation":
                if ((SclMain.vflags & SclMain.FLAG_SUBST) > 0) {
                    SclTreeElem te = new SclTreeElem(el.getNodeName(), el, null, null, sclEnum.SCL_SUBST);
                    copyChildren(el, te, null, sclEnum.SCL_SUB_CH, true);
                    treeRoot.add(te);
                }
                break;

            default:
                break;
            }
        }
    }


    private void procCommunication(Element parent) {
        SclTreeElem te;
        SclParser[] parsers = new SclParser[3];

        te = new SclTreeElem(parent.getNodeName(), parent, null, null, sclEnum.SCL_COMMS);

        parsers[0] = new ConnectedAPParser(new String[] {"ConnectedAP"}, sclEnum.SCL_COM_CH);
        parsers[1] = new AddressPParser(new String[] {"P"}, sclEnum.SCL_COM_P);
        parsers[2] = new GSETimeParser(new String[] {"MinTime", "MaxTime"}, sclEnum.SCL_COM_CH);
        copyChildren(parent, te, parsers, sclEnum.SCL_COM_CH, true);
        treeRoot.add(te);
    }


    private void procIED(Element parent) {
        Element el;
        String iedname;

        if ((iedname = getAttrNull(parent, "name")) == null)
            return;

        SclTreeElem iedte = new SclTreeElem(parent.getNodeName() + ": " + iedname, parent, null, iedname, sclEnum.SCL_IED);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "AccessPoint":
                procAP(el, iedte);
                break;

            case "Services":
                if ((SclMain.vflags & SclMain.FLAG_SERVICES) > 0)
                    procServices(el, iedte);
                break;

            default:
                break;
            }
        }

        treeRoot.add(iedte);

        // Resolve DS contents and show them as FCDA children
        if ((SclMain.vflags & SclMain.FLAG_DSRCB) > 0)
            populateDS(iedte, iedte);
    }


    private void procServices(Element parent, SclTreeElem pte) {

        SclTreeElem te = new SclTreeElem(parent.getNodeName(), parent, null, null, sclEnum.SCL_SVCES);

        copyChildren(parent, te, null, sclEnum.SCL_SVC_CH, true);
        pte.add(te);
    }


    private void procAP(Element parent, SclTreeElem pte) {
        Element el;
        String apname;

        if ((apname = getAttrNull(parent, "name")) == null)    // Mandatory
            return;

        SclTreeElem apte = new SclTreeElem(parent.getNodeName() + ": " + apname, parent, null, apname, sclEnum.SCL_AP);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "Server":
                procServer(el, apte);
                break;

            case "Services":
                if ((SclMain.vflags & SclMain.FLAG_SERVICES) > 0)
                    procServices(el, apte);
                break;

            case "ServerAt":
            case "GOOSESecurity":
            case "SMVSecurity":
                SclTreeElem te = new SclTreeElem(el.getNodeName(), el, null, null, sclEnum.SCL_AP_CH);
                copyChildren(el, te, null, sclEnum.SCL_AP_CH, true);
                apte.add(te);
                break;

            case "LN":  /* Implement multiple LNs */
                break;

            default:
                break;
            }
        }
        pte.add(apte);
    }


    private void procServer(Element parent, SclTreeElem pte) {
        Element el;
        SclTreeElem servte = new SclTreeElem(parent.getNodeName(), parent, null, null, sclEnum.SCL_SERV);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "LDevice":
                procLD(el, servte);
                break;

            case "Authentication":
            case "Association":
                SclTreeElem te = new SclTreeElem(el.getNodeName(), el, null, null, sclEnum.SCL_SERV_CH);
                copyChildren(el, te, null, sclEnum.SCL_SERV_CH, true);
                servte.add(te);
                break;

            default:
                break;
            }
        }
        pte.add(servte);
    }


    private void procLD(Element parent, SclTreeElem pte) {
        Element el;
        String ldname;

        if ((ldname = getAttrNull(parent, "inst")) == null)
            return;

        SclTreeElem ldte = new SclTreeElem("LD: " + ldname, parent, null, ldname, sclEnum.SCL_LD);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "LN":
            case "LN0":
                procLN(el, ldte);
                break;

            case "AccessControl":   /* Implement tAnyContentFromOtherNamespace */
                break;

            default:
                break;
            }
        }
        pte.add(ldte);
    }


    private void procLN(Element parent, SclTreeElem pte) {
        Element el, tn;
        String lnClass, prefix, lnname, lninst, lnType;

        if ((lnClass = getAttrNull(parent, "lnClass")) == null)
            return;
        if ((lnType = getAttrNull(parent, "lnType")) == null)
            return;

        prefix = parent.getAttribute("prefix");
        lninst = parent.getAttribute("inst");
        lnname = "LN: ";
        lnname += prefix;
        lnname += lnClass;
        lnname += lninst;

        LnName sclname = new LnName(prefix, lnClass, lninst);
        SclTreeElem te = new SclTreeElem(lnname, parent, lnType, sclname, sclEnum.SCL_LN);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (el.getNodeName()) {
            case "DataSet":
                if ((SclMain.vflags & SclMain.FLAG_DSRCB) > 0)
                    procDS(el, te);
                break;

            case "ReportControl":
                if ((SclMain.vflags & SclMain.FLAG_DSRCB) > 0)
                    procRCB(el, te);
                break;

            default:
                break;
            }
        }

        if ((tn = getDataType(te, "LNodeType")) != null) {
            procDODA(tn, te);
            copyAttributes(tn, (Element) te.xmlNode, "id"); // Copy <LNodeType> => <LDevice><LN></LDevice> (this is pointed to by 'te.xmlNode' and its attributes are shown in textbox)

            for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
                switch (el.getNodeName()) {
                case "DOI":
                    procDOI(el, te);
                    break;

                default:
                    break;
                }
            }
            pte.add(te);
        }
    }


    private void procDOI(Element parent, SclTreeElem pte) {
        Element el;
        String name;
        SclTreeElem te;

        if ((name = getAttrNull(parent, "name")) == null)   // use="required"
            return;

        for (int i = 0; i < pte.getChildCount(); i++) {
            te = (SclTreeElem) pte.getChildAt(i);
            if (name.equals(te.sclname)) {
                copyAttributes(parent, (Element) te.xmlNode, "name");   // Copy <DOI> => <LNodeType><DO></LNodeType> (this is pointed to by 'te.xmlNode' and its attributes are shown in textbox)
                for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
                    switch (el.getNodeName()) {
                    case "SDI":
                        procDOI(el, te);
                        break;

                    case "DAI":
                        procDAI(el, te);
                        break;

                    default:
                        break;
                    }
                }
                return;
            }
        }
    }


    private void procDAI(Element parent, SclTreeElem pte) {
        String name;
        SclTreeElem te;

        if ((name = getAttrNull(parent, "name")) == null)   // use="required"
            return;

        for (int i = 0; i < pte.getChildCount(); i++) {
            te = (SclTreeElem) pte.getChildAt(i);
            if (name.equals(te.sclname)) {
                copyAttributes(parent, (Element) te.xmlNode, "name");   // Copy <DAI> => <DOType><DA></DOType> (this is pointed to by 'te.xmlNode' and its attributes are shown in textbox)
                procDAvals(parent, te, true);   // Overwrite existing <Val>s specified in template <DA>
                return;
            }
        }
    }


    private void procDODA(Element parent, SclTreeElem pte) {
        Element el, tn;
        String nName, dodaName, dodaType, bType;

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (nName = el.getNodeName()) {
            case "DO":      // DO can only appear in <LNodeType> node
            case "SDO":     // SDO can only appear in <DOType> node
                if (
                        ((dodaName = getAttrNull(el, "name")) != null) &&   // use="required"
                        ((dodaType = getAttrNull(el, "type")) != null)) {   // use="required"

                    SclTreeElem te = new SclTreeElem(nName + ": " + dodaName, el, dodaType, dodaName, sclEnum.SCL_DO);

                    if ((tn = getDataType(te, "DOType")) != null) {
                        copyAttributes(tn, (Element) te.xmlNode, "id"); // Copy <DOType> => <LNodeType><DO></LNodeType> (this is pointed to by 'te.xmlNode' and its attributes are shown in textbox)
                        procDODA(tn, te);
                        pte.add(te);
                    }
                }
                break;

            case "DA":      // DA can only appear in <DOType> node
            case "BDA":     // BDA can only appear in <DAType> node
                if (
                        ((dodaName = getAttrNull(el, "name")) != null) &&   // use="required"
                        ((bType = getAttrNull(el, "bType")) != null)) {     // use="required"

                    dodaType = getAttrNull(el, "type");    // Only used if bType="Enum" or bType="Struct"
                    SclTreeElem te = new SclTreeElem(nName + ": " + dodaName, el, dodaType, dodaName, sclEnum.SCL_DA);
                    te.fc = getAttrNull(el, "fc");  // use="required" for <DA>
                    te.flags = procFlags(bType);
                    procDAvals(el, te, false);

                    if (dodaType != null) {
                        switch (bType) {
                        case "Struct":
                            if ((tn = getDataType(te, "DAType")) != null) {
                                copyAttributes(tn, (Element) te.xmlNode, "id"); // Copy <DAType> => <DOType><DA></DOType> (this is pointed to by 'te.xmlNode' and its attributes are shown in textbox)
                                procDODA(tn, te);
                                pte.add(te);
                            }
                            break;

                        case "Enum":
                            if ((te.xmlEnum = getDataType(te, "EnumType")) != null) {
                                pte.add(te);
                            }
                            break;

                        default:
                            break;
                        }
                    }
                    else
                        pte.add(te);
                }
                break;

            default:
                break;
            }
        }
    }


    private void procDAvals(Element parent, SclTreeElem pte, boolean overwrite) {
        Element el;
        String val;

        if (overwrite) {
            for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
                if (el.getNodeName().equals("Val")) {
                    pte.removeAllChildren();
                    break;
                }
            }
        }

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            if (el.getNodeName().equals("Val")) {
                if (
                        (el.getFirstChild() != null) &&
                        ((val = el.getFirstChild().getNodeValue()) != null)) {
                    pte.add(new SclTreeElem(val, el, null, null, sclEnum.SCL_VAL));
                }
            }
        }
    }


    private void procRCB(Element parent, SclTreeElem pte) {
        String name;

        if ((name = getAttrNull(parent, "name")) == null)
            return;

        SclTreeElem te = new SclTreeElem("RCB: " + name, parent, null, name, sclEnum.SCL_RCB);
        copyChildren(parent, te, null, sclEnum.SCL_RCB_CH, false);
        pte.add(te);
    }


    private void procDS(Element parent, SclTreeElem pte) {
        Element el;
        String name, fcc, fcdan;
        String ldInst, prefix, lnClass, lnInst, doName, daName;

        if ((name = getAttrNull(parent, "name")) == null)
            return;

        SclTreeElem dste = new SclTreeElem("DS: " + name, parent, null, name, sclEnum.SCL_DS);

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            if (!S_FCDA.equals(el.getNodeName()))
                continue;

            fcdan = "";
            if ((ldInst = getAttrNull(el, "ldInst")) != null)
                fcdan += ldInst + "/";
            else
                fcdan += "*/";

            if ((lnClass = getAttrNull(el, "lnClass")) != null) {
                prefix = el.getAttribute("prefix");
                fcdan += prefix;

                fcdan += lnClass;

                lnInst = el.getAttribute("lnInst");
                fcdan += lnInst;
            }
            else {
                prefix = null;
                lnInst = null;
                fcdan += "*";
            }

            if ((doName = getAttrNull(el, "doName")) != null)
                fcdan += "." + doName;

            if ((daName = getAttrNull(el, "daName")) != null)
                fcdan += "." + daName;

            if ((fcc = getAttrNull(el, "fc")) == null)
                fcc = "?";
            fcdan += " [" + fcc + "]";

            FcdaContent sclname = new FcdaContent(ldInst, prefix, lnClass, lnInst, doName, daName);
            SclTreeElem te = new SclTreeElem(fcdan, el, null, sclname, sclEnum.SCL_FCDA);
            te.fc = fcc;
            dste.add(te);
        }
        pte.add(dste);
    }


    private Element getDataType(SclTreeElem pte, String nType) {
        Element el;

        for (el = getFirstElement(ndTypeTemplates); el != null; el = getNextElement(el)) {
            if (nType.equals(el.getNodeName())) {
                if (pte.dtype.equals(el.getAttribute("id")))
                    return el;
            }
        }
        return null;
    }


    private void populateDS(SclTreeElem iedte, SclTreeElem pte) {
        SclTreeElem te;
        int chcount = pte.getChildCount();

        for (int i = 0; i < chcount; i++) {
            te = (SclTreeElem) pte.getChildAt(i);

            switch (pte.sclty) {
            case SCL_DS:
                resolveFCDA(iedte, te, te, null, null);
                if (te.getChildCount() == 0) {
                    if (te.errMsg == null)
                        te.errMsg = "FCDA is not found";
                }
                break;

            case SCL_LN:
                if (te.sclty == sclEnum.SCL_DS)
                     populateDS(iedte, te);
                break;

            default:
                populateDS(iedte, te);
                break;
            }
        }
    }


    private void resolveFCDA(SclTreeElem pte, SclTreeElem fcdae, SclTreeElem newp, String doref, String daref) {
        SclTreeElem te, newel;
        int chcount = pte.getChildCount();
        int delim;
        String doname = null, daname = null, chdon = null, chdan = null;

        for (int i = 0; i < chcount; i++) {
            te = (SclTreeElem) pte.getChildAt(i);

            switch (te.sclty) {
            case SCL_LD:
                if (((FcdaContent) fcdae.sclname).ldInst != null) {
                    if (compareNameRef((String) te.sclname, ((FcdaContent) fcdae.sclname).ldInst, false)) {
                        resolveFCDA(te, fcdae, fcdae, null, null);
                        return;
                    }
                }
                else { // FCDA doesn't have ldInst=""
                    newel = (SclTreeElem) te.clone();
                    resolveFCDA(te, fcdae, newel, null, null);

                    if (newel.getChildCount() > 0)
                        newp.add(newel);
                }
                break;

            case SCL_LN:
                if (((FcdaContent) fcdae.sclname).lnClass != null) {
                    if (!compareNameRef(((LnName) te.sclname).prefix, ((FcdaContent) fcdae.sclname).prefix, false))
                        continue;
                    if (!compareNameRef(((LnName) te.sclname).lnClass, ((FcdaContent) fcdae.sclname).lnClass, false))
                        continue;
                    if (!compareNameRef(((LnName) te.sclname).lnInst, ((FcdaContent) fcdae.sclname).lnInst, false))
                        continue;

                    resolveFCDA(te, fcdae, fcdae, ((FcdaContent) fcdae.sclname).doName, null);
                    return;
                }
                else { // FCDA doesn't have lnClass=""
                    newel = (SclTreeElem) te.clone();
                    resolveFCDA(te, fcdae, newel, ((FcdaContent) fcdae.sclname).doName, null);

                    if (newel.getChildCount() > 0)
                        newp.add(newel);
                }
                break;

            case SCL_DO:
                if (doref != null) {    // DO name passed as argument
                    if (doname == null) {
                        if ((delim = doref.indexOf('.')) > -1) {     // Structured name, contains dot '.'
                            doname = doref.substring(0, delim);
                            chdon = doref.substring(delim + 1);
                        }
                        else
                            doname = doref;
                    }

                    if (compareNameRef((String) te.sclname, doname, true)) {
                        if (!checkArrayRef(fcdae, te, doname))
                            return;

                        resolveFCDA(te, fcdae, newp, chdon, ((FcdaContent) fcdae.sclname).daName);
                        return;
                    }
                }
                else {  // DO name not passed, add all DOs at this level.
                    newel = (SclTreeElem) te.clone();
                    resolveFCDA(te, fcdae, newel, null, ((FcdaContent) fcdae.sclname).daName);

                    if (newel.getChildCount() > 0)
                        newp.add(newel);
                }
                break;

            case SCL_DA:
                if (daref != null) {    // DA name passed as argument
                     if (daname == null) {
                        if ((delim = daref.indexOf('.')) > -1) {     // Structured name, contains dot '.'
                            daname = daref.substring(0, delim);
                            chdan = daref.substring(delim + 1);
                        }
                        else
                            daname = daref;
                    }

                    if (compareNameRef((String) te.sclname, daname, true)) {
                        if (!checkArrayRef(fcdae, te, daname))
                            return;

                        if ((te.fc == null) || fcdae.fc.equals(te.fc)) {
                            if (chdan == null)
                                newp.add((SclTreeElem) te.cloneDA());
                            else
                                resolveFCDA(te, fcdae, newp, null, chdan);
                        }
                        return;
                    }
                }
                else {  // DA name not passed, add all DAs.
                    if ((te.fc == null) || fcdae.fc.equals(te.fc)) {
                        newp.add((SclTreeElem) te.cloneDA());
                    }
                }
                break;

            case SCL_DS:
            case SCL_RCB:
                break;

            default:
                resolveFCDA(te, fcdae, newp, doref, daref);
                break;
            }
        }
    }


    private boolean checkArrayRef(SclTreeElem fcdae, SclTreeElem te, String name) {
        int lpar, rpar, inum, icount;
        String dix, snum, scount;

        if ((lpar = name.indexOf('(')) < 0)
            return true;

        if ((rpar = name.substring(lpar + 1).indexOf(')')) < 0) {
            fcdae.errMsg = "Malformed array specification - ')' missing";
            return false;
        }

        if (rpar == 0) {
            fcdae.errMsg = "Incorrect array specification - array element number missing";
            return false;
        }

        if ((dix = getAttrNull((Element) fcdae.xmlNode, "ix")) == null) {
            fcdae.errMsg = "Incorrect array specification - ix=\"\" attribute missing";
            return false;
        }

        snum = name.substring(lpar + 1, lpar + 1 + rpar);
        if (!snum.equals(dix)) {
            fcdae.errMsg = "Incorrect array specification - ix=\"" + dix + "\" doesn't match array element number (" + snum + ")";
            return false;
        }

        if ((scount = getAttrNull((Element) te.xmlNode, "count")) == null) {
            fcdae.errMsg = "Incorrect array target - count=\"\" attribute missing";
            return false;
        }

        try {
            inum = Integer.parseUnsignedInt(snum);
            icount = Integer.parseUnsignedInt(scount);
        }
        catch (NumberFormatException ex) {
            fcdae.errMsg = "Invalid array specification - " + ex.getMessage();
            return false;
        }

        if (inum == 0) {
            fcdae.errMsg = "Invalid array specification - element number (" + snum + ") must be greater than 0";
            return false;
        }

        if (inum >= icount) {
            fcdae.errMsg = "Invalid array specification - element number (" + snum + ") exceeds count=\"" + scount + "\"";
            return false;
        }
        return true;
    }


    private void copyChildren(Element parent, SclTreeElem pte, SclParser[] parsers, sclEnum sty, boolean recur) {
        Element el;
        String nname, aname;
        SclTreeElem te;
        boolean psFound;

        for (el = getFirstElement(parent); el != null; el = getNextElement(el)) {
            switch (nname = el.getNodeName()) {
            case "Private":
                break;

            default:
                te = null;
                if (parsers == null) {
                    if ((aname = getAttrNull(el, "name")) != null)
                        nname += ": " + aname;

                    te = new SclTreeElem(nname, el, null, null, sty);
                    pte.add(te);
                }
                else {
                    psFound = false;
                    for (int i = 0; i < parsers.length; i++) {
                        for (int j = 0; j < parsers[i].names.length; j++) {
                            if (parsers[i].names[j].equals(nname)) {
                                psFound = true;
                                if ((te = parsers[i].parser(el, recur)) != null)
                                    pte.add(te);
                                break;
                            }
                        }
                        if (psFound)
                            break;
                    }

                    if (!psFound) {
                        te = new SclTreeElem(nname, el, null, null, sty);
                        pte.add(te);
                    }
                }

                if (recur && (te != null))
                    copyChildren(el, te, parsers, sty, recur);
                break;
            }
        }
    }


    private void copyAttributes(Element source, Element dest, String exclude) {
        NamedNodeMap attrs = source.getAttributes();
        Attr attr;

        for (int i = 0; i < attrs.getLength(); i++) {
            attr = (Attr) attrs.item(i);
            if (exclude != null) {
                if (exclude.equals(attr.getName()))
                    continue;
            }
            dest.setAttribute(attr.getName(), attr.getValue());
        }
    }


    private Element getNextElement(Node nd) {
        for (nd = nd.getNextSibling(); nd != null; nd = nd.getNextSibling()) {
            if (nd.getNodeType() == Node.ELEMENT_NODE)
                return (Element) nd;
        }
        return null;
    }


    private Element getFirstElement(Node parent) {
        Node nd;

        for (nd = parent.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (nd.getNodeType() == Node.ELEMENT_NODE)
                return (Element) nd;
        }
        return null;
    }


    private String getAttrNull(Element parent, String name) {

        if (parent.hasAttribute(name))
            return parent.getAttribute(name);
        return null;
    }


    private void getEddition(Element roote) {
        String sver = roote.getAttribute("version");
        //String srev = roote.getAttribute("revision");
        String srel = roote.getAttribute("release");

        switch (sver) {
        case "2007":
            switch (srel) {
            case "4":
                scledition = sclEditions.SCL_ED21;
                break;

            default:
                scledition = sclEditions.SCL_ED2;
                break;
            }
            break;

        default:
            scledition = sclEditions.SCL_ED1;
            break;
        }
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
        }
        return flags;
    }


    private boolean updateSelected(SclTreeElem current) {
        int daflags;
        String btype;

        SelectedSCL.clear();

        daflags = traverseParents(current);

        if (SelectedSCL.fc == null)             // Selected element is shown only if FC is found and
            return false;
        if (current.sclty != sclEnum.SCL_DA)    // it is DA
            return false;

        switch (SelectedSCL.fc) {
        case "ST":
            if ((daflags & (FLAG_QUALITY | FLAG_TIMESTAMP)) != (FLAG_QUALITY | FLAG_TIMESTAMP))
                return false;
            btype = ((Element) current.xmlNode).getAttribute("bType");
            if (current.getChildCount() > 0) {
                if ((btype = traverseChildren(current, btype)) == null)
                    return false;
            }

            switch (btype) {
            case "BOOLEAN":
            case "Dbpos":
            case "Tcmd":
                SelectedSCL.objtypes[0] = 0;
                break;

            case "INT8":
            case "INT16":
            case "INT24":
            case "INT32":
            case "INT64":
            case "INT128":
            case "INT8U":
            case "INT16U":
            case "INT24U":
            case "INT32U":
            case "INT64U":
            case "INT128U":
            case "FLOAT32":
            case "FLOAT64":
                SelectedSCL.objtypes[0] = 1;
                break;

            case "Enum":
                Integer largest;
                if ((largest = checkEnums(current)) == null)
                    return false;

                if (largest > 1)
                    SelectedSCL.objtypes[1] = 1;
                SelectedSCL.objtypes[0] = 0;    // Suggest OnValues and OffValues
                break;

            //case "Struct": // Should have already been traversed
            default:
                return false;
            }
            return true;

        case "MX":
            if ((daflags & (FLAG_QUALITY | FLAG_TIMESTAMP)) != (FLAG_QUALITY | FLAG_TIMESTAMP))
                return false;
            if (traverseChildren(current, "") == null)
                return false;
            SelectedSCL.objtypes[0] = 1;
            return true;

        case "CO":
            SclTreeElem cate, bte;
            if (current.fc != null)
                cate = current;
            else {
                cate = (SclTreeElem) current.getParent();
                if (cate.fc == null)    // orCat or orIdent selected
                    cate = (SclTreeElem) cate.getParent();
            }
            //SelectedSCL.daname = (String) cate.sclname;

            for (bte = (SclTreeElem) cate.getFirstChild(); bte != null; bte = (SclTreeElem) bte.getNextSibling()) {
                if ("ctlVal".equals(bte.sclname)) {
                    switch (((Element) bte.xmlNode).getAttribute("bType")) {
                    case "BOOLEAN":
                    case "Dbpos":
                    case "Tcmd":
                        SelectedSCL.objtypes[0] = 2;
                        break;

                    case "Enum":
                        Integer largest;
                        if ((largest = checkEnums(bte)) == null)
                            return false;
                        if (largest > 1)
                            SelectedSCL.objtypes[0] = 3;
                        else
                            SelectedSCL.objtypes[0] = 2;
                        break;

                    default:
                        SelectedSCL.objtypes[0] = 3;
                        break;
                    }
                    return true;
                }
            }
            break;

        default:
            break;
        }
        return false;
    }


    private int traverseParents(SclTreeElem te) {
        int daflags = 0;

        for (; te != null; te = (SclTreeElem) te.getParent()) {
            switch (te.sclty) {
            case SCL_IED:
                SelectedSCL.iedname = (String) te.sclname;
                break;

            case SCL_LD:
                SelectedSCL.ldname = (String) te.sclname;
                break;

            case SCL_LN:
                SelectedSCL.lnobj = (LnName) te.sclname;
                break;

            case SCL_DO:
                if (SelectedSCL.doname == null)
                    SelectedSCL.doname = (String) te.sclname;
                else
                    SelectedSCL.doname = (String) te.sclname + "." + SelectedSCL.doname;
                break;

            case SCL_DA:
                if ((te.flags & (FLAG_QUALITY | FLAG_TIMESTAMP)) == 0) { // Selected DA is not 'q' nor 't'
                    if (SelectedSCL.daname == null)
                        SelectedSCL.daname = (String) te.sclname;
                    else
                        SelectedSCL.daname = (String) te.sclname + "." + SelectedSCL.daname;

                    if (te.fc != null) {            // Selected DA has FC
                        SelectedSCL.fc = te.fc;
                        daflags |= checkQT(te);     // Check if there are 'q' and 't' attributes with the same FC
                    }
                }
                break;

            case SCL_FCDA:
                if (SelectedSCL.doname == null)
                    SelectedSCL.doname = ((FcdaContent) te.sclname).doName;
                else
                    SelectedSCL.doname = ((FcdaContent) te.sclname).doName + "." + SelectedSCL.doname;

                SelectedSCL.lnobj = (LnName) te.sclname;
                SelectedSCL.ldname = ((FcdaContent) te.sclname).ldInst;
                for (; te != null; te = (SclTreeElem) te.getParent()) {
                    if (te.sclty == sclEnum.SCL_IED) {
                        SelectedSCL.iedname = (String) te.sclname;
                        break;
                    }
                }
                return daflags;

            default:
                break;
            }
        }
        return daflags;
    }


    /**
     * Traverse structured MX attributes such as "cVal.mag.f"
     */
    private String traverseChildren(SclTreeElem pte, String btype) {
        SclTreeElem te;
        boolean found = false;

        for (te = (SclTreeElem) pte.getFirstChild(); te != null; te = (SclTreeElem) te.getNextSibling()) {
            if (te.sclty == sclEnum.SCL_DA) {   // Exclude <DA><Val></DA>
                if (found)
                    return null;
                SelectedSCL.daname += "." + (String) te.sclname;
                btype = ((Element) te.xmlNode).getAttribute("bType");
                if ((btype = traverseChildren(te, btype)) == null)
                    return null;
                found = true;
            }
        }
        return btype;
    }


    private Integer checkEnums(SclTreeElem pte) {
        Element enval;
        String enord;
        int largest = 0, ord;

        if (pte.xmlEnum == null)
            return null;

        for (enval = getFirstElement(pte.xmlEnum); enval != null; enval = getNextElement(enval)) {
            if (!"EnumVal".equals(enval.getNodeName()))
                return null;
            if ((enord = getAttrNull(enval, "ord")) == null)
                return null;

            try {
                ord = Integer.parseInt(enord);
                if (ord > largest)
                    largest = ord;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return largest;
    }


    private int checkQT(SclTreeElem current) {
        SclTreeElem te;
        int daflags = 0;

        for (te = (SclTreeElem) ((SclTreeElem) current.getParent()).getFirstChild(); te != null; te = (SclTreeElem) te.getNextSibling()) {
            if (SelectedSCL.fc.equals(te.fc)) {
                daflags |= (te.flags & (FLAG_QUALITY | FLAG_TIMESTAMP));
            }
        }
        return daflags;
    }


    private boolean compareNameRef(String name, String ref, boolean checkpar) {
        int lpar;

        if (name != null) {
            if (ref != null) {
                if (checkpar && ((lpar = ref.indexOf('(')) > -1))
                    ref = ref.substring(0, lpar);

                if (name.equals(ref))
                    return true;
            }
            else if (name.isEmpty())
                return true;
        }
        else if (ref != null) {
            if (ref.isEmpty())
                return true;
        }
        else
            return true;
        return false;
    }


    private String loadAttributes(Node nd) {
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
