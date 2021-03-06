/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.kernel;

import geogebra.Plain;

/**
 * Creates a Polygon from a given list of points or point array.
 * 
 * @author Markus Hohenwarter
 * @version
 */
public class AlgoPolygon extends AlgoElement {

  private static final long serialVersionUID = 1L;
  protected GeoPointInterface[] points; // input
  private final GeoList geoList; // alternative input
  protected GeoPolygon poly; // output

  /** /2D coord sys used for 3D */
  protected GeoElement cs2D;

  /** polyhedron (when segment is part of), used for 3D */
  protected GeoElement polyhedron;

  protected AlgoPolygon(Construction cons, String[] labels, GeoList geoList) {
    this(cons, labels, null, geoList);
  }

  protected AlgoPolygon(Construction cons, String[] labels,
      GeoPointInterface[] points) {
    this(cons, labels, points, null);
  }

  private AlgoPolygon(Construction cons, String[] labels,
      GeoPointInterface[] points, GeoList geoList) {
    this(cons, labels, points, geoList, null, true, null);
  }

  /**
   * @param cons
   *          the construction
   * @param labels
   *          names of the polygon and the segments
   * @param points
   *          vertices of the polygon
   * @param geoList
   *          list of vertices of the polygon (alternative to points)
   * @param cs2D
   *          for 3D stuff : GeoCoordSys2D
   * @param createSegments
   *          says if the polygon has to creates its edges (3D only)
   * @param polyhedron
   *          polyhedron (when segment is part of), used for 3D
   */
  protected AlgoPolygon(Construction cons, String[] labels,
      GeoPointInterface[] points, GeoList geoList, GeoElement cs2D,
      boolean createSegments, GeoElement polyhedron) {
    super(cons);
    this.points = points;
    this.geoList = geoList;
    this.cs2D = cs2D;
    this.polyhedron = polyhedron;

    // poly = new GeoPolygon(cons, points);
    createPolygon(createSegments);

    // compute polygon points
    compute();

    setInputOutput(); // for AlgoElement
    poly.initLabels(labels);
  }

  @Override
  protected final void compute() {
    if (geoList != null)
      updatePointArray(geoList);

    // compute area
    poly.calcArea();

    // update region coord sys
    poly.updateRegionCS();
  }

  /**
   * create the polygon
   * 
   * @param createSegments
   *          says if the polygon has to creates its edges (3D only)
   */
  protected void createPolygon(boolean createSegments) {
    poly = new GeoPolygon(cons, points);
  }

  @Override
  protected String getClassName() {
    return "AlgoPolygon";
  }

  public GeoPoint[] getPoints() {
    return (GeoPoint[]) points;
  }

  public GeoPolygon getPoly() {
    return poly;
  }

  @Override
  public void remove() {
    super.remove();
    // if polygon is part of a polyhedron, remove it
    if (polyhedron != null)
      polyhedron.remove();
  }

  // for AlgoElement
  @Override
  protected void setInputOutput() {
    if (geoList != null) {
      // list as input
      if (polyhedron == null) {
        input = new GeoElement[1];
        input[0] = geoList;
      } else {
        input = new GeoElement[2];
        input[0] = geoList;
        input[1] = polyhedron;
      }
    } else // points as input
    if (polyhedron == null)
      input = (GeoElement[]) points;
    else {
      input = new GeoElement[points.length + 1];
      for (int i = 0; i < points.length; i++)
        input[i] = (GeoElement) points[i];
      input[points.length] = polyhedron;
    }
    // set dependencies
    for (GeoElement element : input)
      element.addAlgorithm(this);

    setOutput();

    // parent of output
    poly.setParentAlgorithm(this);
    cons.addToAlgorithmList(this);
  }
  private void setOutput() {
    GeoSegmentInterface[] segments = poly.getSegments();
    int size = 1;
    if (segments != null)
      size += segments.length;
    output = new GeoElement[size];
    output[0] = poly;
    for (int i = 0; i < size - 1; i++)
      output[i + 1] = (GeoElement) segments[i];

    /*
     * String s="output = "; for (int i=0; i < size-1; i++) {
     * s+=output[i].getLabel()+", "; } Application.debug(s);
     */

  }

  @Override
  final public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append(Plain.Polygon);
    sb.append(' ');
    int last = points.length - 1;
    for (int i = 0; i < last; i++) {
      sb.append(points[i].getLabel());
      sb.append(", ");
    }
    sb.append(points[last].getLabel());

    // TODO use app.getPlain()
    if (polyhedron != null)
      sb.append(" of " + polyhedron.getLabel());

    return sb.toString();
  }

  @Override
  void update() {
    // compute output from input
    compute();
    output[0].update();
  }

  /**
   * Update point array of polygon using the given array list
   * 
   * @param pointList
   */
  private void updatePointArray(GeoList pointList) {
    // check if we have a point list
    if (pointList.getElementType() != GeoElement.GEO_CLASS_POINT) {
      poly.setUndefined();
      return;
    }

    // remember old number of points
    int oldPointsLength = points == null ? 0 : points.length;

    // create new points array
    int size = pointList.size();
    points = new GeoPoint[size];
    for (int i = 0; i < size; i++)
      points[i] = (GeoPoint) pointList.get(i);
    poly.setPoints(points);

    if (oldPointsLength != points.length)
      setOutput();
  }
}
