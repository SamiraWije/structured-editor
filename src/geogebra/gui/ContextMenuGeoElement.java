/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.gui;

import geogebra.Plain;
import geogebra.gui.inputbar.AlgebraInput;
import geogebra.gui.util.AnimatedGifEncoder;
import geogebra.kernel.GeoConic;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoLine;
import geogebra.kernel.GeoNumeric;
import geogebra.kernel.GeoPoint;
import geogebra.kernel.GeoSegment;
import geogebra.kernel.GeoText;
import geogebra.kernel.GeoVector;
import geogebra.kernel.Kernel;
import geogebra.kernel.Traceable;
import geogebra.main.Application;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Context menu for GeoElement objects.
 * 
 * @author Markus Hohenwarter
 * @version
 */
public class ContextMenuGeoElement extends JPopupMenu {

  private static final long serialVersionUID = 1L;
  final static Color bgColor = Color.white;
  final static Color fgColor = Color.black;

  private GeoElement geo;
  private GeoPoint point;
  private GeoLine line;
  private GeoVector vector;
  private GeoConic conic;
  // private GeoNumeric numeric;
  // private Point location;
  Application app;

  private final ArrayList tempArrayList = new ArrayList();

  ContextMenuGeoElement(Application app) {
    this.app = app;
    setBackground(bgColor);
  }

  /** Creates new MyPopupMenu for GeoElement */
  protected ContextMenuGeoElement(Application app, GeoElement geo,
      Point location) {
    this(app);
    this.geo = geo;
    // this.location = location;

    String title = geo.getLongDescriptionHTML(false, true);
    if (title.length() > 80)
      title = geo.getNameDescriptionHTML(false, true);
    setTitle(title);

    if (app.getGuiManager().showAlgebraView()) {
      addPointItems();
      addLineItems();
      addVectorItems();
      addConicItems();
      addNumberItems();
    }

    if (getComponentCount() > 2)
      addSeparator();
    addForAllItems();
  }

  private void addAction(Action ac) {
    JMenuItem mi = this.add(ac);
    mi.setBackground(bgColor);
  }

  private void addConicItems() {
    if (geo.getClass() != GeoConic.class)
      return;
    conic = (GeoConic) geo;

    // there's no need to show implicit equation
    // if you can't select the specific equation
    boolean specificPossible = conic.isSpecificPossible();
    boolean explicitPossible = conic.isExplicitPossible();
    if (!(specificPossible || explicitPossible))
      return;

    int mode = conic.getToStringMode();
    AbstractAction action;
    StringBuffer sb = new StringBuffer();

    if (mode != GeoConic.EQUATION_IMPLICIT) {
      sb.append(Plain.Equation);
      sb.append(' ');
      sb.append(Plain.ImplicitConicEquation);
      action = new AbstractAction(sb.toString()) {
        /**
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          conic.setToImplicit();
          conic.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    if (specificPossible && mode != GeoConic.EQUATION_SPECIFIC) {
      // specific conic string
      String conicEqn = conic.getSpecificEquation();
      if (conicEqn != null) {
        sb.setLength(0);
        sb.append(Plain.Equation);
        sb.append(' ');
        sb.append(conicEqn);
        action = new AbstractAction(sb.toString()) {
          /**
					 * 
					 */
          private static final long serialVersionUID = 1L;

          public void actionPerformed(ActionEvent e) {
            conic.setToSpecific();
            conic.updateRepaint();
            app.storeUndoInfo();
          }
        };
        addAction(action);
      }
    }

    if (explicitPossible && mode != GeoConic.EQUATION_EXPLICIT) {
      sb.setLength(0);
      sb.append(Plain.Equation);
      sb.append(' ');
      sb.append(Plain.ExplicitConicEquation);
      action = new AbstractAction(sb.toString()) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          conic.setToExplicit();
          conic.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }
  }

  private void addForAllItems() {
    // SHOW, HIDE
    if (geo.isDrawable()) {
      JCheckBoxMenuItem cbItem;

      // show object
      if (geo.getShowObjectCondition() == null
          && (!geo.isGeoBoolean() || geo.isIndependent())) {
        cbItem = new JCheckBoxMenuItem(Plain.ShowObject);
        cbItem.setIcon(app.getImageIcon("mode_showhideobject_16.gif"));
        cbItem.setSelected(geo.isSetEuclidianVisible());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            geo.setEuclidianVisible(!geo.isSetEuclidianVisible());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      if (geo.isLabelShowable()) {
        // show label
        cbItem = new JCheckBoxMenuItem(Plain.ShowLabel);
        cbItem.setSelected(geo.isLabelVisible());
        cbItem.setIcon(app.getImageIcon("mode_showhidelabel_16.gif"));
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            geo.setLabelVisible(!geo.isLabelVisible());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      // trace
      if (geo.isTraceable()) {
        cbItem = new JCheckBoxMenuItem(Plain.TraceOn);
        cbItem.setIcon(app.getImageIcon("trace_on.gif"));
        cbItem.setSelected(((Traceable) geo).getTrace());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ((Traceable) geo).setTrace(!((Traceable) geo).getTrace());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      // trace to spreadsheet
      if (geo.isGeoPoint() && app.getGuiManager().showSpreadsheetView()) {
        cbItem = new JCheckBoxMenuItem(Plain.TraceToSpreadsheet);
        cbItem.setIcon(app.getImageIcon("spreadsheettrace.gif"));
        cbItem.setSelected(((GeoPoint) geo).getSpreadsheetTrace());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ((GeoPoint) geo).setSpreadsheetTrace(!((GeoPoint) geo)
                .getSpreadsheetTrace());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      // animation
      if (geo.isAnimatable()) {
        cbItem = new JCheckBoxMenuItem(Plain.Animating);
        cbItem.setIcon(app.getEmptyIcon());
        cbItem.setSelected(((GeoNumeric) geo).isAnimating());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            geo.setAnimating(!geo.isAnimating());
            geo.updateRepaint();
            app.storeUndoInfo();

            // automatically start animation when animating was turned on
            if (geo.isAnimating())
              geo.getKernel().getAnimatonManager().startAnimation();
          }
        });
        addItem(cbItem);
      }

      // AUXILIARY OBJECT

      if (app.getGuiManager().showAlgebraView() && app.showAuxiliaryObjects()
          && geo.isAlgebraShowable()) {

        // show object
        cbItem = new JCheckBoxMenuItem(Plain.AuxiliaryObject);
        cbItem.setIcon(app.getImageIcon("aux_folder.gif"));
        cbItem.setSelected(geo.isAuxiliaryObject());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            geo.setAuxiliaryObject(!geo.isAuxiliaryObject());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      // fix object
      if (geo.isFixable() && (geo.isGeoText() || geo.isGeoImage())) {

        cbItem = new JCheckBoxMenuItem(Plain.FixObject);
        cbItem.setIcon(app.getEmptyIcon());
        cbItem.setSelected(geo.isFixed());
        cbItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            geo.setFixed(!geo.isFixed());
            geo.updateRepaint();
            app.storeUndoInfo();
          }
        });
        addItem(cbItem);
      }

      // text position
      if (geo.isGeoText())
        addTextItems();

      addSeparator();
    }

    // EDIT: copy to input bar
    if (app.showAlgebraInput() && !geo.isGeoImage() && geo.isDefined()) {
      addAction(new AbstractAction(geogebra.Menu.CopyToInputBar, app
          .getImageIcon("edit.png")) {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          AlgebraInput ai = (AlgebraInput) app.getGuiManager()
              .getAlgebraInput();
          if (ai != null) {
            // copy into text field
            ai.getTextField().setText(geo.getValueForInputBar());
            ai.requestFocus();
          }
        }
      });
      addSeparator();
    }

    /*
     * // EDIT in AlgebraView else if (app.showAlgebraView() &&
     * geo.isChangeable() && !geo.isGeoImage()) { addAction(new AbstractAction(
     * app.getPlain("Edit"), app.getImageIcon("edit.png")) { private static
     * final long serialVersionUID = 1L;
     * 
     * public void actionPerformed(ActionEvent e) { app.startEditing(geo); } });
     * }
     */

    // Rename
    if (app.letRename() && geo.isRenameable())
      addAction(new AbstractAction(Plain.Rename, app.getImageIcon("rename.png")) {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          app.getGuiManager().showRenameDialog(geo, true, geo.getLabel(), true);
        }
      });

    // EDITING
    // EDIT Text in special dialog
    if (geo.isTextValue() && !geo.isTextCommand() && !geo.isFixed())
      addAction(new AbstractAction(Plain.Edit, app.getImageIcon("edit.png")) {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          app.getGuiManager().showTextDialog((GeoText) geo);
        }
      });

    // animation
    if (geo.isAnimatable()) {
      JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(Plain.ExportAnimatedGIF);
      cbItem.setIcon(app.getEmptyIcon());
      cbItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {

          app.getKernel().getAnimatonManager().stopAnimation();

          File file = app.getGuiManager().showSaveDialog(
              Application.FILE_EXT_GIF, null,
              Plain.gif + " " + geogebra.Menu.Files);

          GeoNumeric num = (GeoNumeric) geo;

          int type = num.getAnimationType();
          double min = num.getIntervalMin();
          double max = num.getIntervalMax();

          double val;

          double step, time;
          int n;

          switch (type) {
            case GeoElement.ANIMATION_DECREASING :
              step = -num.getAnimationStep();
              n = (int) ((max - min) / -step);
              if (app.getKernel().isZero((max - min) / -step - n))
                n++;
              if (n == 0)
                n = 1;
              time = 13000 / n;
              val = max;
              break;
            case GeoElement.ANIMATION_OSCILLATING :
              step = num.getAnimationStep();
              n = (int) ((max - min) / step) * 2;
              if (app.getKernel().isZero((max - min) / step * 2 - n))
                n++;
              if (n == 0)
                n = 1;
              time = 2 * 13000 / n;
              val = min;
              break;
            default : // GeoElement.ANIMATION_INCREASING:
              step = num.getAnimationStep();
              n = (int) ((max - min) / step);
              if (app.getKernel().isZero((max - min) / step - n))
                n++;
              if (n == 0)
                n = 1;
              time = 13000 / n;
              val = min;
          }

          AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
          // gifEncoder.start("c:\\test2.gif");
          gifEncoder.start(file);
          int delay = (int) (time / Math.abs(num.getAnimationSpeed()));
          if (delay == 0)
            delay = 1;
          gifEncoder.setDelay(delay); // miliseconds
          gifEncoder.setRepeat(0);

          app.setWaitCursor();

          try {

            // while (val < max + 0.00000001 && val > min - 0.00000001) {
            for (int i = 0; i < n; i++) {
              // Application.debug(min+"");
              num.setValue(val);
              num.updateRepaint();

              Image img = app.getEuclidianView().getExportImage(1);
              gifEncoder.addFrame((BufferedImage) img);

              val += step;

              if (val > max + 0.00000001 || val < min - 0.00000001) {
                val -= 2 * step;
                step *= -1;
              }
            }

            gifEncoder.finish();
            // app.showMessage("done");
          } catch (Exception ex) {
            app.showError("SaveFileFailed");
            ex.printStackTrace();
          } finally {
            app.setDefaultCursor();
          }
        }
      });
      addItem(cbItem);
    }

    // DELETE
    if (app.letDelete() && !geo.isFixed())
      addAction(new AbstractAction(Plain.Delete, app
          .getImageIcon("delete_small.gif")) {
        /**
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          // geo.remove();
          geo.removeOrSetUndefinedIfHasFixedDescendent();
          app.storeUndoInfo();
        }
      });

    if (app.letShowPropertiesDialog() && geo.hasProperties()) {
      addSeparator();

      // open properties dialog
      addAction(new AbstractAction(Plain.Properties + " ...", app
          .getImageIcon("document-properties.png")) {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          tempArrayList.clear();
          tempArrayList.add(geo);
          app.getGuiManager().showPropertiesDialog(tempArrayList);
        }
      });
    }
  }

  private void addItem(JMenuItem mi) {
    mi.setBackground(bgColor);
    add(mi);
  }

  private void addLineItems() {
    if (!(geo instanceof GeoLine))
      return;
    if (geo instanceof GeoSegment)
      return;

    line = (GeoLine) geo;
    int mode = line.getMode();
    AbstractAction action;
    StringBuffer sb = new StringBuffer();

    if (mode != GeoLine.EQUATION_IMPLICIT) {
      sb.setLength(0);
      sb.append(Plain.Equation);
      sb.append(' ');
      sb.append(Plain.ImplicitLineEquation);
      action = new AbstractAction(sb.toString()) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          line.setMode(GeoLine.EQUATION_IMPLICIT);
          line.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    if (mode != GeoLine.EQUATION_EXPLICIT) {
      sb.setLength(0);
      sb.append(Plain.Equation);
      sb.append(' ');
      sb.append(Plain.ExplicitLineEquation);
      action = new AbstractAction(sb.toString()) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          line.setMode(GeoLine.EQUATION_EXPLICIT);
          line.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    if (mode != GeoLine.PARAMETRIC) {
      action = new AbstractAction(Plain.ParametricForm) {
        /**
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          line.setMode(GeoLine.PARAMETRIC);
          line.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

  }

  private void addNumberItems() {
  }
  private void addPointItems() {
    if (!(geo instanceof GeoPoint))
      return;
    point = (GeoPoint) geo;
    int mode = point.getMode();
    AbstractAction action;

    if (mode != Kernel.COORD_CARTESIAN && !geo.isFixed()
        && point.getMode() != Kernel.COORD_COMPLEX) {
      action = new AbstractAction(Plain.CartesianCoords) {
        /**
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          point.setMode(Kernel.COORD_CARTESIAN);
          point.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    if (mode != Kernel.COORD_POLAR && !geo.isFixed()
        && point.getMode() != Kernel.COORD_COMPLEX) {
      action = new AbstractAction(Plain.PolarCoords) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          point.setMode(Kernel.COORD_POLAR);
          point.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    /*
     * if (mode != Kernel.COORD_COMPLEX && !geo.isFixed()) { action = new
     * AbstractAction(app.getPlain("ComplexNumber")) { /**
     * 
     * 
     * private static final long serialVersionUID = 1L;
     * 
     * public void actionPerformed(ActionEvent e) {
     * point.setMode(Kernel.COORD_COMPLEX); point.updateRepaint();
     * app.storeUndoInfo(); } }; addAction(action); }
     */
  }

  private void addTextItems() {
    if (geo.isGeoText()) {
      final GeoText geoText = (GeoText) geo;
      // show object
      JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(
          Plain.AbsoluteScreenLocation);
      cbItem.setIcon(app.getEmptyIcon());
      cbItem.setSelected(geoText.isAbsoluteScreenLocActive());
      cbItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          boolean flag = !geoText.isAbsoluteScreenLocActive();
          if (flag) {
            // convert real world to screen coords
            int x = app.getEuclidianView().toScreenCoordX(
                geoText.getRealWorldLocX());
            int y = app.getEuclidianView().toScreenCoordY(
                geoText.getRealWorldLocY());
            geoText.setAbsoluteScreenLoc(x, y);
          } else {
            // convert screen coords to real world
            double x = app.getEuclidianView().toRealWorldCoordX(
                geoText.getAbsoluteScreenLocX());
            double y = app.getEuclidianView().toRealWorldCoordY(
                geoText.getAbsoluteScreenLocY());
            geoText.setRealWorldLoc(x, y);
          }
          geoText.setAbsoluteScreenLocActive(flag);
          geoText.updateRepaint();
          app.storeUndoInfo();
        }
      });
      addItem(cbItem);
    }
  }

  private void addVectorItems() {
    if (!(geo instanceof GeoVector))
      return;
    vector = (GeoVector) geo;
    int mode = vector.getMode();
    AbstractAction action;

    if (mode != Kernel.COORD_CARTESIAN) {
      action = new AbstractAction(Plain.CartesianCoords) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          vector.setMode(Kernel.COORD_CARTESIAN);
          vector.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }

    if (mode != Kernel.COORD_POLAR) {
      action = new AbstractAction(Plain.PolarCoords) {
        /** 
				 * 
				 */
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
          vector.setMode(Kernel.COORD_POLAR);
          vector.updateRepaint();
          app.storeUndoInfo();
        }
      };
      addAction(action);
    }
    /*
     * if (mode != Kernel.COORD_COMPLEX) { action = new
     * AbstractAction(app.getPlain("ComplexNumber")) {
     * 
     * private static final long serialVersionUID = 1L;
     * 
     * public void actionPerformed(ActionEvent e) {
     * vector.setMode(Kernel.COORD_COMPLEX); vector.updateRepaint();
     * app.storeUndoInfo(); } }; addAction(action); }
     */
  }

  void setTitle(String str) {
    JLabel title = new JLabel(str);
    title.setFont(app.getBoldFont());
    title.setBackground(bgColor);
    title.setForeground(fgColor);

    title.setBorder(BorderFactory.createEmptyBorder(5, 10, 2, 5));
    add(title);
    addSeparator();

    title.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        setVisible(false);
      }
    });

  }

}
