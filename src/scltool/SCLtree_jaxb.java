/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

//import ch.iec._61850._2003.scl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.w3c.dom.Node;

/**
 *
 * @author dell
 */
public class SCLtree_jaxb {
    private final static int FLAG_DO = 1;
    private final static int FLAG_FCST = 2;
    private final static int FLAG_FCMX = 4;
    private final static int FLAG_FCCO = 8;
    private final static int FLAG_TIMESTAMP = 0x10;
    private final static int FLAG_QUALITY = 0x20;
    //public static JTree scdTree;



    public SCLtree_jaxb() {


/*
       // InputField inputField;
        Annotation[] anns;
        Annotation ann;
        //ann = THeader.class.getDeclaredAnnotations();
        Field[] ff = THeader.class.getDeclaredFields();
        //ann = ff[0].getDeclaredAnnotations()[0];
        XmlElement xmlElementAnnotation = ff[0].getAnnotation(XmlElement.class);
        XmlAttribute xmlAttributeAnnotation = ff[2].getAnnotation(XmlAttribute.class);

        XmlType xmltt = THeader.class.getAnnotation(XmlType.class);
            // get 'required' value
        //String ss = xmltt.name();
        //String ss = ann.toString();
        String ss = xmlElementAnnotation.name();
        ss = xmlAttributeAnnotation.name();
*/


        //THeader header = scl.getHeader();
         //SCL scl = new SCL();
        //ch.iec._61850._2003.scl.ObjectFactory of = new ch.iec._61850._2003.scl.ObjectFactory();
        //SCL scl = ch.iec._61850._2003.scl.ObjectFactory.createSCL();

        //scdTree = new JTree(tnRoot);
        /*scdTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                sclTreeElem tn = (sclTreeElem) scdTree.getLastSelectedPathComponent();


                if (tn == null)
                    return;


                String info = "";
                int length = tn.attrs.length;
                for (int i = 0; i < length; i++) {
                    info += tn.attrs[i][0].toString();
                    info += " : ";
                    info += tn.attrs[i][1].toString();
                    info += "\n";
                }
                if ((tn.flags & FLAG_DO) > 0) {
                    info += "!!!DO!!!\n";
                }
                if ((tn.flags & FLAG_FCST) > 0) {
                    info += "!!!ST!!!\n";
                }
                if ((tn.flags & FLAG_FCMX) > 0) {
                    info += "!!!MX!!!\n";
                }
                if ((tn.flags & FLAG_FCCO) > 0) {
                    info += "!!!CO!!!\n";
                }
                SclMain.textBox.setText((String) info);
            }
        });*/

    }

    protected class sclTreeElem extends DefaultMutableTreeNode {
        public Node xmlNode;
        public String type;
        public Object[][] attrs;
        public int flags = 0;


        public sclTreeElem(String name, String ty, int fl) {
            super(name);
            type = ty;
            //attrs = loadAttributes(nd);
            flags |= fl;
        }
    }


    /*public sclTreeElem treeInit(String filename) {
        SCL iecScl;

        if ((iecScl = unmarshalScl(filename)) == null) {
            return null;
        }

        sclTreeElem tnRoot = new sclTreeElem("SCL", null, 0);


        //scdTree = new JTree(tnRoot);
        return tnRoot;
    }

    private SCL unmarshalScl(String filename) {
        JAXBContext jc;
        Unmarshaller um;
        SCL scl = null;

        try {
            jc = JAXBContext.newInstance("ch.iec._61850._2003.scl");
            um = jc.createUnmarshaller();
        } catch (JAXBException ex) {
            Logger.getLogger(SCLtree_jaxb.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        try {
            scl = (SCL) um.unmarshal(new FileInputStream(filename));
        } catch (JAXBException ex) {
            Logger.getLogger(SCLtree_jaxb.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SCLtree_jaxb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return scl;
    }*/

}
