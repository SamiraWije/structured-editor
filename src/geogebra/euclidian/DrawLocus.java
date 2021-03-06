/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.euclidian;

import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoLocus;
import geogebra.kernel.MyPoint;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

public final class DrawLocus extends Drawable {

  private final GeoLocus locus;

  private boolean isVisible, labelVisible;
  private final GeneralPath gp;
  private double[] lastPointCoords;

  protected DrawLocus(EuclidianView view, GeoLocus locus) {
    this.view = view;
    this.locus = locus;
    geo = locus;

    gp = new GeneralPath();
    update();
  }

  private void buildGeneralPath(ArrayList<MyPoint> pointList) {
    gp.reset();
    double[] coords = new double[2];

    int size = pointList.size();
    for (int i = 0; i < size; i++) {
      MyPoint p = pointList.get(i);
      coords[0] = p.x;
      coords[1] = p.y;
      view.toScreenCoords(coords);

      if (coords[0] < Float.MAX_VALUE && coords[1] < Float.MAX_VALUE)
        if (p.lineTo)
          gp.lineTo((float) coords[0], (float) coords[1]);
        else
          gp.moveTo((float) coords[0], (float) coords[1]);
    }

    lastPointCoords = coords;
  }

  @Override
  final public void draw(Graphics2D g2) {
    if (isVisible) {
      if (geo.doHighlighting()) {
        // draw locus
        g2.setPaint(geo.getSelColor());
        g2.setStroke(selStroke);
        Drawable.drawWithValueStrokePure(gp, g2);
      }

      // draw locus
      g2.setPaint(geo.getObjectColor());
      g2.setStroke(objStroke);
      Drawable.drawWithValueStrokePure(gp, g2);

      // label
      if (labelVisible) {
        g2.setFont(view.fontLine);
        g2.setColor(geo.getLabelColor());
        drawLabel(g2);
      }
    }
  }

  /**
   * Returns the bounding box of this DrawPoint in screen coordinates.
   */
  @Override
  final public Rectangle getBounds() {
    if (!geo.isDefined() || !locus.isClosedPath() || !geo.isEuclidianVisible())
      return null;
    else
      return gp.getBounds();
  }

  @Override
  final public GeoElement getGeoElement() {
    return geo;
  }

  /**
   * was this object clicked at? (mouse pointer location (x,y) in screen coords)
   */
  @Override
  final public boolean hit(int x, int y) {
    if (strokedShape == null)
      strokedShape = objStroke.createStrokedShape(gp);
    return strokedShape.intersects(x - 3, y - 3, 6, 6);

    /*
     * return gp.intersects(x-2,y-2,4,4) && !gp.contains(x-2,y-2,4,4);
     */
  }

  @Override
  final public boolean isInside(Rectangle rect) {
    return rect.contains(gp.getBounds());
  }

  @Override
  final public void setGeoElement(GeoElement geo) {
    this.geo = geo;
  }

  @Override
  final public void update() {
    isVisible = geo.isEuclidianVisible();
    if (!isVisible)
      return;

    buildGeneralPath(locus.getMyPointList());

    // line on screen?
    if (!gp.intersects(0, 0, view.width, view.height))
      isVisible = false;
    // don't return here to make sure that getBounds() works for offscreen
    // points too
    updateStrokes(geo);

    labelVisible = geo.isLabelVisible();
    if (labelVisible) {
      labelDesc = geo.getLabelDescription();
      xLabel = (int) (lastPointCoords[0] - 5);
      yLabel = (int) (lastPointCoords[1] + 4 + view.fontSize);
      addLabelOffset();
    }
  }

}
