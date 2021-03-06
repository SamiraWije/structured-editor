/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * DrawVector.java
 *
 * Created on 16. Oktober 2001, 15:13
 */

package geogebra.euclidian;

import geogebra.kernel.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * 
 * @author Markus
 * @version
 */
public class DrawVector extends Drawable implements Previewable {

  private GeoVector v;
  private GeoPoint P;

  private double x1, y1, x2, y2;
  private double length, fx, fy, vx, vy, factor;
  private boolean isVisible, labelVisible;
  private boolean traceDrawingNeeded = false;

  private final Line2D.Float line = new Line2D.Float();
  private final GeneralPath gp = new GeneralPath(); // for arrow
  private final double[] coords = new double[2];
  private ArrayList<Object> points;

  private final Point2D.Double endPoint = new Point2D.Double();

  DrawVector(EuclidianView view, ArrayList<Object> points) {
    this.view = view;
    this.points = points;
    updatePreview();
  }

  /** Creates new DrawVector */
  protected DrawVector(EuclidianView view, GeoVector v) {
    this.view = view;
    this.v = v;
    geo = v;

    update();
  }

  public void disposePreview() {
  }

  @Override
  public void draw(Graphics2D g2) {
    if (isVisible) {
      if (geo.doHighlighting()) {
        g2.setPaint(v.getSelColor());
        g2.setStroke(selStroke);
        g2.draw(line);
      }

      if (traceDrawingNeeded) {
        traceDrawingNeeded = false;
        Graphics2D g2d = view.getBackgroundGraphics();
        if (g2d != null)
          drawTrace(g2d);
      }

      g2.setPaint(v.getObjectColor());
      g2.setStroke(objStroke);
      g2.draw(line);
      g2.fill(gp);

      if (labelVisible) {
        g2.setFont(view.fontVector);
        g2.setPaint(v.getLabelColor());
        drawLabel(g2);
      }
    }
  }

  final public void drawPreview(Graphics2D g2) {
    if (isVisible) {
      g2.setPaint(ConstructionDefaults.colPreview);
      g2.setStroke(objStroke);
      g2.fill(gp);
      g2.draw(line);
    }
  }

  private final void drawTrace(Graphics2D g2) {
    g2.setPaint(v.getObjectColor());
    g2.setStroke(objStroke);
    g2.draw(line);
    g2.fill(gp);
  }

  /**
   * Returns the bounding box of this Drawable in screen coordinates.
   */
  @Override
  final public Rectangle getBounds() {
    if (!geo.isDefined() || !geo.isEuclidianVisible())
      return null;
    else
      return line.getBounds();
  }

  @Override
  public GeoElement getGeoElement() {
    return geo;
  }

  @Override
  final public boolean hit(int x, int y) {
    return line.intersects(x - 3, y - 3, 6, 6)
        || gp.intersects(x - 3, y - 3, 6, 6);
  }

  @Override
  final public boolean isInside(Rectangle rect) {
    return rect.contains(line.getBounds());
  }

  private void setArrow(float lineThickness) {
    // arrow for endpoint
    vx = x2 - x1;
    vy = y2 - y1;
    factor = 12.0 + lineThickness;
    length = GeoVec2D.length(vx, vy);
    if (length > 0.0) {
      vx = vx * factor / length;
      vy = vy * factor / length;
    }

    // build arrow
    fx = x2 - vx;
    fy = y2 - vy;
    line.setLine(x1, y1, fx, fy);
    vx /= 4.0;
    vy /= 4.0;
    gp.reset();
    gp.moveTo((float) x2, (float) y2); // end point
    gp.lineTo((float) (fx - vy), (float) (fy + vx));
    gp.lineTo((float) (fx + vy), (float) (fy - vx));
    gp.closePath();
  }

  @Override
  public void setGeoElement(GeoElement geo) {
    this.geo = geo;
  }

  @Override
  final public void update() {
    isVisible = geo.isEuclidianVisible();
    if (!isVisible)
      return;
    labelVisible = geo.isLabelVisible();

    updateStrokes(v);

    // start point
    P = v.getStartPoint();
    if (P != null && !P.isInfinite()) {
      P.getInhomCoords(coords);
      x2 = coords[0];
      y2 = coords[1];
      view.toScreenCoords(coords);
    } else {
      x2 = 0.0;
      y2 = 0.0;
      coords[0] = view.xZero;
      coords[1] = view.yZero;
    }
    x1 = coords[0];
    y1 = coords[1];

    // end point
    coords[0] = x2 + v.x;
    coords[1] = y2 + v.y;
    view.toScreenCoords(coords);
    x2 = coords[0];
    y2 = coords[1];

    setArrow(v.lineThickness); // uses x1, y1, x2, y2

    // line on screen?
    if (!line.intersects(0, 0, view.width, view.height))
      isVisible = false;
    // don't return here to make sure that getBounds() works for offscreen
    // points too

    // label position
    if (labelVisible) {
      labelDesc = geo.getLabelDescription();
      xLabel = (int) ((x1 + x2) / 2.0 + vy);
      yLabel = (int) ((y1 + y2) / 2.0 - vx);
      addLabelOffset();
    }

    if (v == view.getEuclidianController().recordObject)
      recordToSpreadsheet(v);

    // draw trace
    // a vector is a Locateable and it might
    // happen that there are several update() calls
    // before the new trace should be drawn
    // so the actual drawing is moved to draw()
    traceDrawingNeeded = v.trace;
    if (v.trace)
      isTracing = true;
    else if (isTracing) {
      isTracing = false;
      view.updateBackground();
    }
  }

  final public void updateMousePos(int x, int y) {
    if (isVisible) {
      x2 = x;
      y2 = y;

      // round angle to nearest 15 degrees if alt pressed
      if (points.size() == 1 && view.getEuclidianController().altDown) {
        double xRW = view.toRealWorldCoordX(x);
        double yRW = view.toRealWorldCoordY(y);
        GeoPoint p = (GeoPoint) points.get(0);
        double px = p.inhomX;
        double py = p.inhomY;
        double angle = Math.atan2(yRW - py, xRW - px) * 180 / Math.PI;
        double radius = Math.sqrt((py - yRW) * (py - yRW) + (px - xRW)
            * (px - xRW));

        // round angle to nearest 15 degrees
        angle = Math.round(angle / 15) * 15;

        xRW = px + radius * Math.cos(angle * Math.PI / 180);
        yRW = py + radius * Math.sin(angle * Math.PI / 180);

        endPoint.x = xRW;
        endPoint.y = yRW;
        view.getEuclidianController().setLineEndPoint(endPoint);

        // don't use view.toScreenCoordX/Y() as we don't want rounding
        x2 = view.xZero + xRW * view.xscale;
        y2 = view.yZero - yRW * view.yscale;

      } else
        view.getEuclidianController().setLineEndPoint(null);

      line.setLine(x1, y1, x2, y2);
      setArrow(1);
    }
  }

  final public void updatePreview() {
    isVisible = points.size() == 1;
    if (isVisible) {
      // start point
      GeoPoint P = (GeoPoint) points.get(0);
      P.getInhomCoords(coords);
      view.toScreenCoords(coords);
      x1 = coords[0];
      y1 = coords[1];
      line.setLine(x1, y1, x1, y1);
    }
  }
}
