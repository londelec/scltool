/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

/**
 *
 * @author dell
 */
public class SelectedSCL {
    public static String iedname;
    public static String ldname;
    public static LnName lnobj;
    public static String doname;
    public static String daname;
    public static String fc;
    public static int objtypes[] = new int[2];

    public static void clear() {
        iedname = null;
        ldname = null;
        lnobj = null;
        doname = null;
        daname = null;
        fc = null;
        objtypes[0] = -1;
        objtypes[1] = -1;
    }


    public static String getString() {
        String seltext;

        seltext = "IED = " + SelectedSCL.iedname + "\n";
        seltext += "ldInst = " + SelectedSCL.ldname + "\n";
        if (SelectedSCL.lnobj != null) {
            if (SelectedSCL.lnobj.prefix != null)   // Don't show empty prefixes
                seltext += "prefix = " + SelectedSCL.lnobj.prefix + "\n";
            seltext += "lnClass = " + SelectedSCL.lnobj.lnClass + "\n";
            if (SelectedSCL.lnobj.lnInst != null)  // Don't show empty instances
                seltext += "lnInst = " + SelectedSCL.lnobj.lnInst + "\n";
        }
        seltext += "doName = " + SelectedSCL.doname + "\n";
        seltext += "daName = " + SelectedSCL.daname + "\n";
        return seltext;
    }
}
