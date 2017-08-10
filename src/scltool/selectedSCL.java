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
public class selectedSCL {
    public static String ldname;
    public static LnName lnobj;
    public static String doname;
    public static String daname;
    public static String fc;


    public static void clear() {
        ldname = null;
        lnobj = null;
        doname = null;
        daname = null;
        fc = null;
    }


    public static String update() {
        String selText = "";

        switch (fc) {
        case "ST":
            selText += "Digital Input:\n";
            break;
        case "MX":
            selText += "Analog Input:\n";
            break;
        case "CO":
            selText += "Digital Output:\n";
            break;
        }

        selText += "ldInst = " + selectedSCL.ldname + "\n";
        if (selectedSCL.lnobj != null) {
            if (selectedSCL.lnobj.prefix != null)   // Don't show empty prefixes
                selText += "prefix = " + selectedSCL.lnobj.prefix + "\n";
            selText += "lnClass = " + selectedSCL.lnobj.lnClass + "\n";
            if (selectedSCL.lnobj.lnInst != null)  // Don't show empty instances
                selText += "lnInst = " + selectedSCL.lnobj.lnInst + "\n";
        }
        selText += "doName = " + selectedSCL.doname + "\n";
        selText += "daName = " + selectedSCL.daname + "\n";
        return selText;
    }


    public static int chooseType() {
        int type = 0;

        if (fc != null) {
            switch (fc) {
            case "ST":
                type = 1;
                break;
            case "MX":
                type = 2;
                break;
            case "CO":
                type = 3;
                break;
            }
        }
        return type;
    }
}
