package geogebra.gui.view.spreadsheet;

import geogebra.Plain;
import geogebra.kernel.GeoElement;
import geogebra.main.Application;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;

public class ContextMenu extends JPopupMenu {

  public class ActionListenerClear implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean succ = table.copyPasteCut.delete(column1, row1, column2, row2);
      if (succ)
        app.storeUndoInfo();
    }
  }

  public class ActionListenerCopy implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      table.copyPasteCut.copy(column1, row1, column2, row2, false);
    }
  }

  private class ActionListenerCreateList implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      Application.debug("CreateList " + column1 + " - " + column2 + "   "
          + row1 + " - " + row2);

      TableModel model = table.getModel();
      int r1 = row1 == -1 ? 0 : row1;
      int r2 = row2 == -1 ? model.getRowCount() : row2;

      int c1 = column1 == -1 ? 0 : column1;
      int c2 = column2 == -1 ? model.getColumnCount() : column2;

      String text = "";
      try {
        if (r1 == r2) {
          text = "{";
          for (int j = c1; j <= c2; ++j) {
            GeoElement v2 = RelativeCopy.getValue(table, j, r1);
            if (v2 != null)
              text += v2.getLabel() + ",";
            else {
              // app.showErrorDialog(app.getPlain("CellAisNotDefined",GeoElement.getSpreadsheetCellName(j,row1)));
              // return;
            }
          }

          text = removeComma(text) + "}";

          Application.debug(text);
          GeoElement[] geos = table.kernel.getAlgebraProcessor()
              .processAlgebraCommandNoExceptionHandling(text, false);

          // set list name
          String listName = geos[0].getIndexLabel("L");
          geos[0].setLabel(listName);
        } else if (c1 == c2) {
          text = "{";
          for (int j = r1; j <= r2; ++j) {
            GeoElement v2 = RelativeCopy.getValue(table, c1, j);
            if (v2 != null)
              text += v2.getLabel() + ",";
            else {
              // app.showErrorDialog(app.getPlain("CellAisNotDefined",GeoElement.getSpreadsheetCellName(column1,j)));
              // return;
            }
          }

          text = removeComma(text) + "}";

          Application.debug(text);
          GeoElement[] geos = table.kernel.getAlgebraProcessor()
              .processAlgebraCommandNoExceptionHandling(text, false);

          // set list name
          String listName = geos[0].getIndexLabel("L");
          geos[0].setLabel(listName);
        } else {
          Application.debug("creating list failed " + text);
          return;
        }

        app.storeUndoInfo();
      } catch (Exception ex) {
        Application.debug("creating list failed with exception " + text);
      }
    }
  }
  private class ActionListenerCreateMatrix implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      // Application.debug("CreateMatrix " + column1 + " - " +
      // column2+"   "+row1+" - "+row2);
      // if (selected == null) throw new RuntimeException("error state");
      String text = "";
      try {
        text = "{";
        for (int j = row1; j <= row2; ++j) {
          // if (selected.length > j && ! selected[j]) continue;
          String row = "{";
          for (int i = column1; i <= column2; ++i) {
            GeoElement v2 = RelativeCopy.getValue(table, i, j);
            if (v2 != null)
              row += v2.getLabel() + ",";
            else {
              app.showErrorDialog(app.getPlain("CellAisNotDefined", GeoElement
                  .getSpreadsheetCellName(j, i)));
              return;
            }
          }
          row = removeComma(row);
          text += row + "}" + ",";
        }

        text = removeComma(text) + "}";

        table.kernel.getAlgebraProcessor()
            .processAlgebraCommandNoExceptionHandling(text, false);

        app.storeUndoInfo();
      } catch (Exception ex) {
        Application.debug("creating matrix failed " + text);
        ex.printStackTrace();
      }

    }
  }
  private class ActionListenerCreatePoints implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      // Application.debug("CreatePoints " + column1 + " - " +
      // column2+"   "+row1+" - "+row2);
      // if (selected == null) throw new RuntimeException("error state");
      StringBuffer text = new StringBuffer();
      LinkedList<String> list = new LinkedList<String>();
      TableModel model = table.getModel();

      // String s="columnsSelected ";
      // for (int i=0 ; i<selectedColumns.length ; i++)
      // if (selectedColumns[i]) s=s+"1"; else s=s+"0";
      // Application.debug(s);

      boolean error = false;

      try {

        if (row1 == -1 && row2 == -1) {
          Application.debug("multiple columns selected");
          int r1 = 0;
          int r2 = model.getRowCount();

          int c1 = -1, c2 = -1;
          for (int i = 0; i < selectedColumns.length; i++)
            if (selectedColumns[i])
              if (c1 == -1)
                c1 = i;
              else if (c2 == -1) {
                c2 = i;
                break;
              }
          Application.debug(c1 + " - " + c2 + "   " + r1 + " - " + r2);

          for (int i = r1; i <= r2; ++i) {
            GeoElement v1 = RelativeCopy.getValue(table, c1, i);
            GeoElement v2 = RelativeCopy.getValue(table, c2, i);
            if (v1 != null && v2 != null
                && (!v1.isGeoNumeric() || !v2.isGeoNumeric()))
              error = true;
            if (v1 != null && v2 != null && v1.isGeoNumeric()
                && v2.isGeoNumeric()) {

              text.setLength(0);
              text.append("(");
              text.append(v1.getLabel());
              text.append(",");
              text.append(v2.getLabel());
              text.append(")");

              GeoElement[] geos = table.kernel.getAlgebraProcessor()
                  .processAlgebraCommandNoExceptionHandling(text.toString(),
                      false);
              // set label P_1, P_2, etc.
              String pointName = geos[0].getIndexLabel("P");
              geos[0].setLabel(pointName);
              geos[0].setAuxiliaryObject(true);
              list.addLast(pointName);
            }
          }

        }

        else if (column2 - column1 == 1) {
          Application.debug("adjacent columns selected");
          int r1 = row1;
          int r2 = row2;
          int c1 = column1;
          int c2 = column2;

          for (int i = r1; i <= r2; ++i) {
            GeoElement v1 = RelativeCopy.getValue(table, c1, i);
            GeoElement v2 = RelativeCopy.getValue(table, c2, i);
            if (v1 != null && v2 != null
                && (!v1.isGeoNumeric() || !v2.isGeoNumeric()))
              error = true;
            if (v1 != null && v2 != null && v1.isGeoNumeric()
                && v2.isGeoNumeric()) {

              text.setLength(0);
              text.append("(");
              text.append(v1.getLabel());
              text.append(",");
              text.append(v2.getLabel());
              text.append(")");

              GeoElement[] geos = table.kernel.getAlgebraProcessor()
                  .processAlgebraCommandNoExceptionHandling(text.toString(),
                      false);
              // set label P_1, P_2, etc.
              String pointName = geos[0].getIndexLabel("P");
              geos[0].setLabel(pointName);
              geos[0].setAuxiliaryObject(true);
              list.addLast(pointName);
            }
          }

        } else if (row2 - row1 == 1) {
          Application.debug("adjacent rows selected");
          int r1 = row1;
          int r2 = row2;
          int c1 = column1;
          int c2 = column2;

          for (int i = c1; i <= c2; ++i) {
            GeoElement v1 = RelativeCopy.getValue(table, i, r1);
            GeoElement v2 = RelativeCopy.getValue(table, i, r2);
            if (v1 != null && v2 != null
                && (!v1.isGeoNumeric() || !v2.isGeoNumeric()))
              error = true;
            if (v1 != null && v2 != null && v1.isGeoNumeric()
                && v2.isGeoNumeric()) {

              text.setLength(0);
              text.append("(");
              text.append(v1.getLabel());
              text.append(",");
              text.append(v2.getLabel());
              text.append(")");

              GeoElement[] geos = table.kernel.getAlgebraProcessor()
                  .processAlgebraCommandNoExceptionHandling(text.toString(),
                      false);
              // set label P_1, P_2, etc.
              String pointName = geos[0].getIndexLabel("P");
              geos[0].setLabel(pointName);
              geos[0].setAuxiliaryObject(true);
              list.addLast(pointName);
            }
          }

        }

        /*
         * int xColumn = -1; for (int j = column1; j <= column2; ++ j) { //if
         * (selected.length > j && ! selected[j]) { // continue; //} if (xColumn
         * == -1) { xColumn = j; continue; } for (int i = row1; i <= row2; ++ i)
         * { GeoElement v1 = RelativeCopy.getValue(table, xColumn, i);
         * GeoElement v2 = RelativeCopy.getValue(table, j, i); if (v1 != null &&
         * v2 != null && v1.isGeoNumeric() && v2.isGeoNumeric()) { String text =
         * "(" + v1.getLabel() + "," + v2.getLabel() + ")"; GeoElement [] geos =
         * table
         * .kernel.getAlgebraProcessor().processAlgebraCommandNoExceptionHandling
         * (text, false); // set label P_1, P_2, etc. String pointName =
         * geos[0].getIndexLabel("P"); geos[0].setLabel(pointName);
         * list.addLast(pointName); } } }
         */

        if (list.size() > 0) {
          String[] points = list.toArray(new String[0]);
          text.setLength(0);
          text.append("{");
          for (int i = 0; i < points.length; ++i) {
            text.append(points[i]);
            if (i != points.length - 1)
              text.append(",");
          }
          text.append("}");
          Application.debug(text.toString());
          // GeoElement[] values =
          table.kernel.getAlgebraProcessor()
              .processAlgebraCommandNoExceptionHandling(text.toString(), false);
          // set list label
          // String listName =
          // values[0].getIndexLabel(app.getPlain("Name.list"));
          // values[0].setLabel(listName);

          // DON'T want the list to be auxiliary
          // for (int i = 0; i < values.length; ++ i) {
          // values[i].setAuxiliaryObject(true);
          // }
        }

        if (error)
          app.showError("NumberExpected");

        app.storeUndoInfo();
      } catch (Exception ex) {
        // Just abort the process
        if (error)
          app.showError("NumberExpected");
      }
    }
  }
  public class ActionListenerCut implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean succ = table.copyPasteCut.cut(column1, row1, column2, row2);
      if (succ)
        app.storeUndoInfo();
    }
  }
  public class ActionListenerPaste implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean succ = table.copyPasteCut.paste(column1, row1, column2, row2);
      if (succ)
        app.storeUndoInfo();
      table.getView().getRowHeader().revalidate();
    }
  }
  private class ActionListenerProperties implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      app.getGuiManager().showPropertiesDialog();
    }
  }

  private static final long serialVersionUID = -7749575525048631798L;

  final static Color bgColor = Color.white;

  private static String removeComma(String s) {
    if (s.endsWith(","))
      s = s.substring(0, s.length() - 1);
    return s;
  }

  protected MyTable table = null;

  protected int row1 = -1;

  protected int row2 = -1;

  protected int column1 = -1;

  protected int column2 = -1;

  protected boolean[] selectedColumns = null;

  protected Application app;

  protected ContextMenu(MyTable table0, int column01, int row01, int column02,
      int row02, boolean[] selected0) {
    // Application.debug("showPopupMenu <<<<<<<<<<<<<<<<<<<");
    table = table0;
    column1 = column01;
    column2 = column02;
    row1 = row01;
    row2 = row02;
    selectedColumns = selected0;
    app = table.kernel.getApplication();

    initMenu();
  }

  protected void initMenu() {
    // JCheckBoxMenuItem item1 = new JCheckBoxMenuItem(
    // app.getPlain("ShowObject"));
    JMenuItem item1 = new JMenuItem(geogebra.Menu.Copy);
    item1.setIcon(app.getImageIcon("edit-copy.png"));
    item1.addActionListener(new ActionListenerCopy());
    add(item1);
    JMenuItem item2 = new JMenuItem(geogebra.Menu.Paste);
    item2.setIcon(app.getImageIcon("edit-paste.png"));
    item2.addActionListener(new ActionListenerPaste());
    add(item2);
    JMenuItem item3 = new JMenuItem(geogebra.Menu.Cut);
    item3.setIcon(app.getImageIcon("edit-cut.png"));
    item3.addActionListener(new ActionListenerCut());
    add(item3);

    // don't show "Delete" if all selected objects fixed
    ArrayList geos = app.getSelectedGeos();

    boolean allFixed = true;

    if (geos != null && geos.size() > 0)
      for (int i = 0; i < geos.size() && allFixed; i++) {
        GeoElement geo = (GeoElement) geos.get(i);
        if (!geo.isFixed())
          allFixed = false;
      }

    if (!allFixed) {
      JMenuItem item4 = new JMenuItem(geogebra.Menu.Delete);
      item4.setIcon(app.getImageIcon("delete_small.gif"));
      item4.addActionListener(new ActionListenerClear());
      add(item4);
    }

    addSeparator();

    if (column1 + 1 <= column2) {

      JMenuItem item5 = new JMenuItem(geogebra.Menu.CreateListOfPoints);
      item5.setIcon(app.getEmptyIcon());
      item5.addActionListener(new ActionListenerCreatePoints());
      add(item5);
    }

    if (column1 != -1 && column2 != -1 && row1 != -1 && row2 != -1) {
      JMenuItem item6 = new JMenuItem(geogebra.Menu.CreateMatrix);
      item6.setIcon(app.getEmptyIcon());
      item6.addActionListener(new ActionListenerCreateMatrix());
      add(item6);
    }

    if (column1 == column2 && column1 != -1 || row1 == row2 && row1 != -1) {
      JMenuItem item7 = new JMenuItem(geogebra.Menu.CreateList);
      item7.setIcon(app.getEmptyIcon());
      item7.addActionListener(new ActionListenerCreateList());
      add(item7);
    }

    if (app.selectedGeosSize() > 0) {

      addSeparator();

      JMenuItem item8 = new JMenuItem(Plain.Properties + "...");
      item8.setIcon(app.getImageIcon("document-properties.png"));
      item8.addActionListener(new ActionListenerProperties());
      add(item8);
    }
  }

}
