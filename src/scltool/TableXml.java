/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DropMode;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 *
 * @author dell
 */
public class TableXml extends JTable {
    private final static int FLAG_EDITABLE = 0x01;
    private final ModelXml model;
    private final ArrayList<Integer> colflags = new ArrayList<>();
    private final ArrayList<String> colNames;
    public ArrayList<Object[]> tdata = new ArrayList<>();
    private final ArrayList<Integer> visibleCols = new ArrayList<>();
    private final ArrayList<TableColumn> columms = new ArrayList<>();
    public final String groupName, elName, descr;
    private final RowPopMenu rowpopmenu;
    private final ColVisibleMenu colVisibleMenu;
    public final ColMenuListener colMenuListener = new ColMenuListener();

    public TableXml(ArrayList<String> attrs, String gName, String eName, String des) {
        colNames = attrs;
        groupName = gName;
        elName = eName;
        descr = des;

        initColumns();

        model = new ModelXml();
        setModel(model);
        //setPreferredSize(new Dimension(5000, 100));
        //setSize(new Dimension(5000, 100));
        //setMinimumSize(new Dimension(5000, 100));
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setColumnSelectionAllowed(true);
        setRowSelectionAllowed(true);
        setDragEnabled(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
        addMouseListener(new RowMouseListener());
        colVisibleMenu = new ColVisibleMenu();
        rowpopmenu = new RowPopMenu();


        /*testTab.setDropTarget(new DropTarget(){
           @Override
           public synchronized void drop(DropTargetDropEvent dtde) {
               Point point = dtde.getLocation();
               int column = testTab.columnAtPoint(point);
               int row = testTab.rowAtPoint(point);
               // handle drop inside current table

               dtde.acceptDrop(DnDConstants.ACTION_MOVE);
               Transferable t = dtde.getTransferable();
               Object ttt;

               try {
                   ttt = t.getTransferData(DataFlavor.stringFlavor);
               } catch (UnsupportedFlavorException ex) {
                   Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
               } catch (IOException ex) {
                   Logger.getLogger(SclMain.class.getName()).log(Level.SEVERE, null, ex);
               }

               super.drop(dtde);
           }
       });*/
    }


    private class ModelXml extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return tdata.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.size();
        }

        @Override
        public String getColumnName(int colIndex) {
            return colNames.get(colIndex);
        }

        @Override
        public Class getColumnClass(int colIndex) {
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            if ((colflags.get(colIndex) & FLAG_EDITABLE) > 0)
                return true;
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            if (colIndex == 0)
                return rowIndex;
            return tdata.get(rowIndex)[colIndex];
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
               return;
            tdata.get(rowIndex)[colIndex] = value;
        }
    }


    @Override
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel() {
            @Override
            public void addColumn(TableColumn tc) {

                //if ((colflags[tc.getModelIndex()] & FLAG_VISIBLE) > 0) {
                    tc.setPreferredWidth(60);
                //}
                //else {
                //    tc.setPreferredWidth(0);
                //    tc.setMinWidth(0);
                //    tc.setMaxWidth(0);
                //}


                //if (tc.getModelIndex() == (colCount - 1)) {
                //    tc.setPreferredWidth(50);
                //    //tc.setCellEditor(new DefaultCellEditor(rowCheckBox));
                //}
                super.addColumn(tc);
                columms.add(tc);
            }
        };
    }


    private class RowPopMenu extends JPopupMenu {

        public RowPopMenu() {
            JMenuItem mItem = new JMenuItem("Delete " + elName);
            mItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    deleteRow();
                }
            });
            add(mItem);
        }
    }


    private class ColVisibleMenu extends JPopupMenu {

        public ColVisibleMenu() {
            VisibleMenuListener al = new VisibleMenuListener();

            for (int i = 0; i < colNames.size(); i++) {
                JCheckBoxMenuItem mItem = new JCheckBoxMenuItem(colNames.get(i));

                if (visibleCols.indexOf(i) >= 0)
                    mItem.setSelected(true);
                else
                    mItem.setSelected(false);

                mItem.addActionListener(al);
                add(mItem);
            }
        }
    }


    private class RowMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                /* isPopupTrigger() is true only in Mouse Pressed event on Linux */
                //System.out.println("mousePressed: " + me.toString());
                raisePopup(me);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                /* isPopupTrigger() is true only in Mouse Released event on Windows */
                //System.out.println("mouseReleased: " + me.toString());
                raisePopup(me);
            }
        }

        private void raisePopup(MouseEvent me) {
            TableXml src = (TableXml) me.getSource();

            if (src.getSelectedRow() >= 0)
                rowpopmenu.show(me.getComponent(), me.getX(), me.getY());
        }
    }


    public class ColMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            JComponent src = (JComponent) ae.getSource();
            colVisibleMenu.show(src, 0, 0);
        }
    }


    private class VisibleMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            JCheckBoxMenuItem src = (JCheckBoxMenuItem) ae.getSource();
            int col = colNames.indexOf(src.getText());

            if (col >= 0)
                colVisibilityAction(col, src.isSelected());

        }
    }


    private void colVisibilityAction(int col, boolean show) {
        int ix;

        if (show) {
            if (visibleCols.indexOf(col) >= 0)
                return;

            getColumnModel().addColumn(columms.get(col));

            for (int i = col - 1; i >= 0; i--) {
                if ((ix = visibleCols.indexOf(i)) >= 0) {
                    if (visibleCols.size() != (ix + 1))
                        getColumnModel().moveColumn(visibleCols.size(), ix + 1);
                    visibleCols.add(ix + 1, col);
                    return;
                }
            }

            getColumnModel().moveColumn(visibleCols.size(), 0);
            visibleCols.add(0, col);
        }
        else {
            if (visibleCols.indexOf(col) < 0)
                return;

            visibleCols.remove((Integer) col);    // Must specify Integer, so it is not confused with remove(int index)
            getColumnModel().removeColumn(columms.get(col));
        }
    }


    public void addRow() {
        Object[] rowdt = new Object[colNames.size()];

        tdata.add(rowdt);

        for (int i = 0; i < colNames.size(); i++) {
            switch (colNames.get(i).toLowerCase()) {
            case "ldinst":
                rowdt[i] = SelectedSCL.ldname;
                break;
            case "prefix":
                rowdt[i] = SelectedSCL.lnobj.prefix;
                break;
            case "lnclass":
                rowdt[i] = SelectedSCL.lnobj.lnClass;
                break;
            case "lninst":
                rowdt[i] = SelectedSCL.lnobj.lnInst;
                break;
            case "doname":
                rowdt[i] = SelectedSCL.doname;
                break;
            case "fc":
                rowdt[i] = SelectedSCL.fc;
                break;
            case "daname":
                rowdt[i] = SelectedSCL.daname;
                break;
            }
        }

        model.fireTableDataChanged();
        //setRowSelectionInterval(0, 0);
        //addRowSelectionInterval(0, 0);
    }


    private void deleteRow() {
        int row = getSelectedRow();

        if (row >= 0) {
            tdata.remove(row);
            model.fireTableDataChanged();
        }
    }


    public Element exportXml(Namespace lens) {
        Object[] rowdt;
        Element el, group = new Element(groupName, lens);

        for (int row = 0; row < tdata.size(); row++) {
            rowdt = tdata.get(row);
            el = new Element(elName, lens);

            for (int col = 0; col < colNames.size(); col++) {
                if (col == 0) {
                    el.setAttribute(colNames.get(col), Integer.toString(row));
                }
                else if (rowdt[col] != null) {
                    el.setAttribute(colNames.get(col), rowdt[col].toString());
                }
            }
            group.addContent(el);
        }
        return group;
    }


    public String exportCsv() {
        boolean first = true;
        boolean quotes = true;
        Object[] rowdt;
        String wrdt;
        StringBuilder sb = new StringBuilder();

        for (int col = 0; col < colNames.size(); col++) {
            if (!first)
                sb.append(",");

            if (quotes)
                sb.append("\"").append(colNames.get(col)).append("\"");
            else
                sb.append(colNames.get(col));
            first = false;
        }
        sb.append("\n");

        for (int row = 0; row < tdata.size(); row++) {
            rowdt = tdata.get(row);
            first = true;
            for (int col = 0; col < colNames.size(); col++) {
                wrdt = "";
                if (col == 0) {
                    wrdt = Integer.toString(row);
                }
                else if (rowdt[col] != null) {
                    wrdt = rowdt[col].toString();
                }

                if (!first)
                    sb.append(",");

                if (quotes)
                    sb.append("\"").append(wrdt).append("\"");
                else
                    sb.append(wrdt);
                first = false;
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    public int importXml(Element rootel, Namespace lens) {
        Element el;
        int col;
        Object[] rowdt;
        List<Element> children;
        List<Attribute> alist;
        Attribute attr;

        tdata.clear();
        if ((el = rootel.getChild(groupName, lens)) == null)
            return 0;

        children = el.getChildren();

        for (int i = 0; i < children.size(); i++) {
            el = children.get(i);
            if (elName.equals(el.getName())) {
                rowdt = new Object[colNames.size()];
                alist = el.getAttributes();
                for (int a = 0; a < alist.size(); a++) {
                    attr = alist.get(a);
                    if ((col = colNames.indexOf(attr.getName())) >= 0) {
                        rowdt[col] = attr.getValue();
                    }
                }
                // Index="" attribute is not checked, rows are imported sequentially.
                tdata.add(rowdt);
            }
        }
        model.fireTableDataChanged();
        return tdata.size();
    }


    public int importCsv(String csv) {
        int cols[];
        Object[] rowdt;
        String[] txtr;
        csv = csv.replaceAll("\"", "");
        csv = csv.replaceAll("\r", "");
        String[] rows = csv.split("\n");

        tdata.clear();
        if (rows.length < 2)
            return 0;

        txtr = rows[0].split(",");
        if (txtr.length == 0)
            return 0;

        cols = new int[txtr.length];
        for (int i = 0; i < txtr.length; i++) {
           cols[i] = colNames.indexOf(txtr[i]);
        }

        for (int r = 1; r < rows.length; r++) {
            txtr = rows[r].split(",");
            rowdt = new Object[colNames.size()];
            for (int i = 0; i < txtr.length; i++) {
                if (cols[i] >= 0) {
                    rowdt[cols[i]] = txtr[i];
                }
            }
            // Index="" attribute is not checked, rows are imported sequentially.
            tdata.add(rowdt);
        }
        model.fireTableDataChanged();
        return tdata.size();
    }


    private void initColumns() {
        for (int i = 0; i < colNames.size(); i++) {
            visibleCols.add(i);
            switch (colNames.get(i).toLowerCase()) {
            case "index":
            case "prefix":
            case "lnclass":
            case "lninst":
            case "doname":
            case "fc":
            case "daname":
            case "ldinst":
                colflags.add(0);
                break;

            default:
                colflags.add(FLAG_EDITABLE);
                break;
            }
        }
    }
}
