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
public class LnName extends Object {
    public String prefix;
    public String lnClass;
    public String lnInst;

    public LnName(String prefix, String lnClass, String lnInst) {
        this.prefix = prefix;
        this.lnClass = lnClass;
        this.lnInst = lnInst;
    }
}
