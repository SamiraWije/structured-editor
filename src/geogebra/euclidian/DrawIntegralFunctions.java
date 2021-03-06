/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.euclidian;

import geogebra.kernel.*;
import geogebra.kernel.arithmetic.NumberValue;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

/**
 * Draws definite Integral of a GeoFunction
 * 
 * @author Markus Hohenwarter
 */
public class DrawIntegralFunctions extends Drawable {

  private final GeoNumeric n;
  private final GeoFunction f, g;
  private final NumberValue a, b;

  private final GeneralPath gp;
  private boolean isVisible, labelVisible;

  protected DrawIntegralFunctions(EuclidianView view, GeoNumeric n) {
    this.view = view;
    this.n = n;
    geo = n;

    n.setDrawable(true);

    AlgoIntegralFunctions algo = (AlgoIntegralFunctions) n.getParentAlgorithm();
    f = algo.getF();
    g = algo.getG();
    a = algo.getA();
    b = algo.getB();

    gp = new GeneralPath();

    update();
  }

  @Override
  final public void draw(Graphics2D g2) {
    if (isVisible) {
      if (geo.doHighlighting()) {
        g2.setPaint(n.getSelColor());
        g2.setStroke(selStroke);
        Drawable.drawWithValueStrokePure(gp, g2);
      }

      g2.setPaint(n.getFillColor());
      Drawable.fillWithValueStrokePure(gp, g2);

      g2.setPaint(n.getObjectColor());
      g2.setStroke(objStroke);
      Drawable.drawWithValueStrokePure(gp, g2);

      if (labelVisible) {
        g2.setFont(view.fontConic);
        g2.setPaint(geo.getLabelColor());
        drawLabel(g2);
      }
    }
  }

  /**
   * Returns the bounding box of this DrawPoint in screen coordinates.
   */
  @Override
  final public Rectangle getBounds() {
    if (!geo.isDefined() || !geo.isEuclidianVisible())
      return null;
    else
      return gp.getBounds();
  }

  @Override
  public GeoElement getGeoElement() {
    return geo;
  }

  @Override
  final public boolean hit(int x, int y) {
    return false;
  }

  @Override
  final public boolean isInside(Rectangle rect) {
    return false;
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
    updateStrokes(n);

    // init gp
    double aRW = a.getDouble();
    double bRW = b.getDouble();

    // init first point of gp as (ax, ay)
    int ax = view.toClippedScreenCoordX(aRW);
    int ay = view.toClippedScreenCoordY(f.evaluate(aRW));

    // plot area between f and g
    gp.reset();
    gp.moveTo(ax, ay);
    DrawParametricCurve.plotCurve(f, aRW, bRW, view, gp, false, false);
    DrawParametricCurve.plotCurve(g, bRW, aRW, view, gp, false, false);
    gp.closePath();

    // gp on screen?
    if (!gp.intersects(0, 0, view.width, view.height))
      isVisible = false;
    // don't return here to make sure that getBounds() works for offscreen
    // points too

    if (labelVisible) {
      int bx = view.toClippedScreenCoordX(bRW);
      xLabel = (ax + bx) / 2;
      aRW = view.toRealWorldCoordX(xLabel);
      double y = (f.evaluate(aRW) + g.evaluate(aRW)) / 2;
      yLabel = view.toClippedScreenCoordY(y);
      labelDesc = geo.getLabelDescription();
      addLabelOffset();
    }
  }
}
