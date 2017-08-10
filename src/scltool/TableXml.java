/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scltool;

import java.util.Arrays;
import javax.swing.DropMode;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.jdom2.Element;
import org.w3c.dom.NodeList;


/**
 *
 * @author dell
 */
public class TableXml extends JTable {
    protected final static int FLAG_VISIBLE = 0x01;
    protected final static int FLAG_EDITABLE = 0x02;
    protected final static int TABLE_ROWS = 128;
    protected ModelXml model;
    protected String[] colNames;
    protected int[] colflags;
    protected int colCount;
    protected int ldcolumn = 1;
    protected Object[][] tdata;
    private final String groupName, elName;
    protected int rowcount = 0;


    public TableXml(NodeList nList, String gName, String eName) {
        colCount = nList.getLength() + 1;
        colNames = new String[colCount];
        colflags = new int[colCount];
        tdata = new Object[TABLE_ROWS][colCount];
        groupName = gName;
        elName = eName;

        for (int i = 0; i < colCount; i++) {
            if (i < (colCount - 1))
                colNames[i] = nList.item(i).getNodeValue();
            else
                colNames[i] = "Select";
            //if (i < 7)
                colflags[i] |= FLAG_VISIBLE;
        }

        resolveColNames();
        //colVisible[0] = false;
        model = new ModelXml();
        setModel(model);
        //setPreferredSize(new Dimension(5000, 100));
        //setSize(new Dimension(5000, 100));
        //setMaximumSize(new Dimension(5000, 100));
        //setMinimumSize(new Dimension(5000, 100));
        //Dimension aaa = getPreferredSize();
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setColumnSelectionAllowed(true);
        setRowSelectionAllowed(true);
        setDragEnabled(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);


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


    protected class ModelXml extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rowcount;
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int colIndex) {
            return colNames[colIndex];
        }

        @Override
        public Class getColumnClass(int colIndex) {
            if (colIndex == (colCount - 1))
                return Boolean.class;   // Last column has CheckBox for delete selection
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            if ((colflags[colIndex] & FLAG_EDITABLE) > 0)
                return true;

            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Object retValue = null;

            if (colIndex == 0) {
                retValue = rowIndex;
            }
            else {
                retValue = tdata[rowIndex][colIndex];
            }
            return retValue;
        }


        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {

            if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
               return;
            tdata[rowIndex][colIndex] = value;
        }
    }


    @Override
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel() {
            @Override
            public void addColumn(TableColumn tc) {

                if ((colflags[tc.getModelIndex()] & FLAG_VISIBLE) > 0) {
                    tc.setPreferredWidth(60);
                }
                else {
                    tc.setPreferredWidth(0);
                    tc.setMinWidth(0);
                    tc.setMaxWidth(0);
                }


                if (tc.getModelIndex() == (colCount - 1)) {
                    tc.setPreferredWidth(50);
                    //tc.setCellEditor(new DefaultCellEditor(rowCheckBox));
                }
                super.addColumn(tc);
            }
        };
    }


    public void addRow() {
        if (rowcount == TABLE_ROWS) {
            return;
        }

        for (int i = 0; i < colNames.length; i++) {
            switch (colNames[i].toLowerCase()) {
            case "ldinst":
                tdata[rowcount][i] = selectedSCL.ldname;
                break;
            case "prefix":
                tdata[rowcount][i] = selectedSCL.lnobj.prefix;
                break;
            case "lnclass":
                tdata[rowcount][i] = selectedSCL.lnobj.lnClass;
                break;
            case "lninst":
                tdata[rowcount][i] = selectedSCL.lnobj.lnInst;
                break;
            case "doname":
                tdata[rowcount][i] = selectedSCL.doname;
                break;
            case "fc":
                tdata[rowcount][i] = selectedSCL.fc;
                break;
            case "daname":
                tdata[rowcount][i] = selectedSCL.daname;
                break;
            }
        }
        rowcount++;

        model.fireTableDataChanged();
        //setRowSelectionInterval(0, 0);
        //addRowSelectionInterval(0, 0);
    }


    public void removeRow() {
        Object check;

        restart:
        for (int row = 0; row < tdata.length; row++) {
            check = tdata[row][colCount - 1];
            if (check != null) {
                if ((Boolean) check == true) {
                    shiftup(row);
                    row--;      // Recheck current element after remainder of the array has been shifted up
                }
            }
        }
        model.fireTableDataChanged();
    }


    private void shiftup(int row) {
        for (int i = row; i < tdata.length; i++) {
            if (i == (tdata.length - 1)) {
                Arrays.fill(tdata[i], null);
            }
            else {
                System.arraycopy(tdata[i + 1], 0, tdata[i], 0, colCount);
            }
        }
        rowcount--;
    }


    public Element exportXml() {
        Element el;
        Element group = new Element(groupName);


        for (int row = 0; row < tdata.length; row++) {
            if (tdata[row][ldcolumn] != null) {
                el = new Element(elName);
                for (int col = 0; col < colCount - 1; col++) {
                    if (col == 0) {
                        el.setAttribute(colNames[col], Integer.toString(row));
                    }
                    else if (tdata[row][col] != null) {
                        el.setAttribute(colNames[col], tdata[row][col].toString());
                    }
                }
                group.addContent(el);
            }
        }
        return group;
    }


    public String exportCsv() {
        boolean first = true;
        boolean csvQuotes = true;
        String wrData;
        StringBuilder sb = new StringBuilder();


        for (int col = 0; col < colCount - 1; col++) {
            if (!first) {
                sb.append(",");
            }
            if (csvQuotes) {
                sb.append("\"").append(colNames[col]).append("\"");
            } else {
                sb.append(colNames[col]);
            }
            first = false;
        }
        sb.append("\n");


        for (int row = 0; row < tdata.length; row++) {
            if (tdata[row][ldcolumn] != null) {
                first = true;
                for (int col = 0; col < colCount - 1; col++) {
                    wrData = "";
                    if (col == 0) {
                        wrData = Integer.toString(row);
                    }
                    else if (tdata[row][col] != null) {
                        wrData =tdata[row][col].toString();
                    }

                    if (!first) {
                        sb.append(",");
                    }
                    if (csvQuotes) {
                        sb.append("\"").append(wrData).append("\"");
                    } else {
                        sb.append(wrData);
                    }
                    first = false;
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    private void resolveColNames() {
        for (int i = 0; i < colNames.length; i++) {
            switch (colNames[i].toLowerCase()) {
            case "index":
            case "prefix":
            case "lnclass":
            case "lninst":
            case "doname":
            case "fc":
            case "daname":
                colflags[i] &= ~FLAG_EDITABLE;
                break;

            case "ldinst":
                ldcolumn = i;
                colflags[i] &= ~FLAG_EDITABLE;
                break;

            default:
                colflags[i] |= FLAG_EDITABLE;
                break;
            }
        }
    }
}
