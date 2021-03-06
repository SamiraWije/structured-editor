/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * ZoomMenu.java
 *
 * Created on 24. J�nner 2002, 14:11
 */

package geogebra.gui;

import geogebra.Plain;
import geogebra.euclidian.EuclidianView;
import geogebra.kernel.Kernel;
import geogebra.main.Application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * 
 * @author markus
 * @version
 */
public class ContextMenuGraphicsWindow extends ContextMenuGeoElement
    implements
      ActionListener {

  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private final double px, py;
  // private JMenuItem miStandardView, miProperties;

  private static double[] zoomFactors = {4.0, 2.0, 1.5, 1.25, 1.0 / 1.25,
      1.0 / 1.5, 0.5, 0.25};

  private static double[] axesRatios = {1.0 / 1000.0, 1.0 / 500.0, 1.0 / 200.0,
      1.0 / 100.0, 1.0 / 50.0, 1.0 / 20.0, 1.0 / 10.0, 1.0 / 5.0, 1.0 / 2.0, 1,
      2, 5, 10, 20, 50, 100, 200, 500, 1000};

  private final ImageIcon iconZoom;

  /** Creates new ZoomMenu */
  protected ContextMenuGraphicsWindow(Application app, double px, double py) {
    super(app);

    iconZoom = app.getImageIcon("zoom16.gif");

    // zoom point
    this.px = px;
    this.py = py;

    setTitle("<html>" + Plain.DrawingPad + "</html>");

    // checkboxes for axes and grid
    EuclidianView ev = app.getEuclidianView();
    JCheckBoxMenuItem cbShowAxes = new JCheckBoxMenuItem(app.getGuiManager()
        .getShowAxesAction());
    cbShowAxes.setSelected(ev.getShowXaxis() && ev.getShowYaxis());
    cbShowAxes.setBackground(getBackground());
    add(cbShowAxes);

    JCheckBoxMenuItem cbShowGrid = new JCheckBoxMenuItem(app.getGuiManager()
        .getShowGridAction());
    cbShowGrid.setSelected(ev.getShowGrid());
    cbShowGrid.setBackground(getBackground());
    add(cbShowGrid);

    addSeparator();

    // zoom for both axes
    JMenu zoomMenu = new JMenu(geogebra.Menu.Zoom);
    zoomMenu.setIcon(iconZoom);
    zoomMenu.setBackground(getBackground());
    addZoomItems(zoomMenu);
    add(zoomMenu);

    // zoom for y-axis
    JMenu yaxisMenu = new JMenu(Plain.xAxis + " : " + Plain.yAxis);
    yaxisMenu.setIcon(app.getEmptyIcon());
    yaxisMenu.setBackground(getBackground());
    addAxesRatioItems(yaxisMenu);
    add(yaxisMenu);

    addSeparator();

    JMenuItem miShowAllObjectsView = new JMenuItem(Plain.ShowAllObjects);
    miShowAllObjectsView.setIcon(app.getEmptyIcon());
    miShowAllObjectsView.setActionCommand("showAllObjects");
    miShowAllObjectsView.addActionListener(this);
    miShowAllObjectsView.setBackground(bgColor);
    add(miShowAllObjectsView);

    JMenuItem miStandardView = new JMenuItem(Plain.StandardView);
    miStandardView.setIcon(app.getEmptyIcon());
    miStandardView.setActionCommand("standardView");
    miStandardView.addActionListener(this);
    miStandardView.setBackground(bgColor);
    add(miStandardView);

    addSeparator();

    JMenuItem miProperties = new JMenuItem(Plain.DrawingPad + " ...");
    miProperties.setIcon(app.getImageIcon("document-properties.png"));
    miProperties.setActionCommand("properties");
    miProperties.addActionListener(this);
    miProperties.setBackground(bgColor);
    add(miProperties);
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();

    if (cmd.equals("standardView"))
      app.setStandardView();
    else if (cmd.equals("showAllObjects"))
      app.setViewShowAllObjects();
    else if (cmd.equals("properties"))
      app.getGuiManager().showOptionsDialog(true);
    // app.getGuiManager().showDrawingPadPropertiesDialog();
  }

  private void addAxesRatioItems(JMenu menu) {
    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          zoomYaxis(Double.parseDouble(e.getActionCommand()));
        } catch (Exception ex) {
        }
      }
    };

    // get current axes ratio
    double scaleRatio = app.getEuclidianView().getScaleRatio();
    Kernel kernel = app.getKernel();

    JMenuItem mi;
    // int perc;
    // ImageIcon icon;
    boolean separatorAdded = false;
    StringBuffer sb = new StringBuffer();
    for (double axesRatio : axesRatios) {
      // build text like "1 : 2"
      sb.setLength(0);
      if (axesRatio > 1.0) {
        sb.append((int) axesRatio);
        sb.append(" : 1");
        if (!separatorAdded) {
          menu.addSeparator();
          separatorAdded = true;
        }

      } else { // factor
        if (axesRatio == 1)
          menu.addSeparator();
        sb.append("1 : ");
        sb.append((int) (1.0 / axesRatio));
      }

      mi = new JCheckBoxMenuItem(sb.toString());
      mi.setSelected(kernel.isEqual(axesRatio, scaleRatio));
      mi.setActionCommand("" + axesRatio);
      mi.addActionListener(al);
      mi.setBackground(getBackground());
      menu.add(mi);
    }
  }

  private void addZoomItems(JMenu menu) {
    int perc;

    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          zoom(Double.parseDouble(e.getActionCommand()));
        } catch (Exception ex) {
        }
      }
    };

    // ImageIcon icon;
    JMenuItem mi;
    boolean separatorAdded = false;
    StringBuffer sb = new StringBuffer();
    for (double zoomFactor : zoomFactors) {
      perc = (int) (zoomFactor * 100.0);

      // build text like "125%" or "75%"
      sb.setLength(0);
      if (perc > 100) {

      } else if (!separatorAdded) {
        menu.addSeparator();
        separatorAdded = true;
      }
      sb.append(perc);
      sb.append("%");

      mi = new JMenuItem(sb.toString());
      mi.setActionCommand("" + zoomFactor);
      mi.addActionListener(al);
      mi.setBackground(getBackground());
      menu.add(mi);
    }
  }

  private void zoom(double zoomFactor) {
    app.zoom(px, py, zoomFactor);
  }

  // ratio: yaxis / xaxis
  private void zoomYaxis(double axesRatio) {
    app.zoomAxesRatio(axesRatio);
  }
}
