/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * GeoPoint.java
 *
 * The point (x,y) has homogenous coordinates (x,y,1)
 *
 * Created on 30. August 2001, 17:39
 */

package geogebra.kernel;

import geogebra.Plain;
import geogebra.kernel.arithmetic.*;
import geogebra.util.Util;

import java.util.*;

/**
 * 2D Point
 * 
 * @author Markus
 * @version
 */
final public class GeoPoint extends GeoVec3D
    implements
      VectorValue,
      Translateable,
      PointRotateable,
      Mirrorable,
      Dilateable,
      PointProperties,
      GeoPointInterface {

  private static final long serialVersionUID = 1L;

  // don't set point size here as this would overwrite setConstructionDefaults()
  // in GeoElement constructor
  // public int pointSize = EuclidianView.DEFAULT_POINT_SIZE;
  public int pointSize;
  private int pointStyle;

  private Path path;
  private PathParameter pathParameter;

  private Region region;
  private RegionParameters regionParameters;
  /** equals x/z when updated */
  private double x2D = 0;
  /** equals y/z when updated */
  private double y2D = 0;

  // temp
  public double inhomX, inhomY;
  private boolean isInfinite, isDefined;
  private boolean showUndefinedInAlgebraView = true;

  // list of Locateables (GeoElements) that this point is start point of
  // if this point is removed, the Locateables have to be notified
  private ArrayList locateableList;

  private ArrayList<GeoNumeric> changeableCoordNumbers = null;

  private boolean hasPolarParentNumbers = false;

  private PathParameter tempPathParameter;

  private static TreeSet<AlgoElement> tempSet;

  private static Comparator<Object> comparatorX;

  /**
   * Returns the affine ratio for three collinear points A, B and C. The ratio
   * is lambda with C = A + lambda * AB, i.e. lambda = AC/AB. Note: the
   * collinearity is not checked in this method.
   */
  protected static final double affineRatio(GeoPoint A, GeoPoint B, GeoPoint C) {
    double ABx = B.inhomX - A.inhomX;
    double ABy = B.inhomY - A.inhomY;

    // avoid division by a number close to zero
    if (Math.abs(ABx) > Math.abs(ABy))
      return (C.inhomX - A.inhomX) / ABx;
    else
      return (C.inhomY - A.inhomY) / ABy;
  }

  /**
   * Returns whether the three points A, B and C are collinear.
   */
  protected static boolean collinear(GeoPoint A, GeoPoint B, GeoPoint C) {
    // A, B, C are collinear iff det(ABC) == 0

    // calculate the determinante of ABC
    // det(ABC) = sum1 - sum2
    double sum1 = A.x * B.y * C.z + B.x * C.y * A.z + C.x * A.y * B.z;
    double sum2 = A.z * B.y * C.x + B.z * C.y * A.x + C.z * A.y * B.x;

    // det(ABC) == 0 <=> sum1 == sum2
    return Kernel.isEqual(sum1, sum2, Kernel.MIN_PRECISION);
  }

  /**
   * Calcs determinant of P and Q. Note: no test for defined or infinite is done
   * here.
   */
  protected static final double det(GeoPoint P, GeoPoint Q) {
    return (P.x * Q.y - Q.x * P.y) / (P.z * Q.z);
  }

  /**
   * Returns a comparator for GeoPoint objects. (sorts on X coordinate) If
   * equal, doesn't return zero (otherwise TreeSet deletes duplicates)
   */
  public static Comparator<Object> getComparatorX() {
    if (comparatorX == null)
      comparatorX = new Comparator() {
        public int compare(Object a, Object b) {
          GeoPoint itemA = (GeoPoint) a;
          GeoPoint itemB = (GeoPoint) b;

          double compX = itemA.inhomX - itemB.inhomX;

          if (itemA.getKernel().isZero(compX)) {
            double compY = itemA.inhomY - itemB.inhomY;

            // if x-coords equal, sort on y-coords
            if (!itemA.getKernel().isZero(compY))
              return compY < 0 ? -1 : +1;

            // don't return 0 for equal objects, otherwise the TreeSet deletes
            // duplicates
            return itemA.getConstructionIndex() > itemB.getConstructionIndex()
                ? -1
                : 1;
          } else
            return compX < 0 ? -1 : +1;
        }
      };

    return comparatorX;
  }

  private static TreeSet<AlgoElement> getTempSet() {
    if (tempSet == null)
      tempSet = new TreeSet<AlgoElement>();
    return tempSet;
  }

  private final StringBuffer sbToString = new StringBuffer(50);

  private final StringBuffer sbBuildValueString = new StringBuffer(50);

  public GeoPoint(Construction c) {
    super(c);
    setUndefined();
  }

  public GeoPoint(Construction c, Path path) {
    super(c);
    this.path = path;
  }

  /*
   * void initSetLabelVisible() { setLabelVisible(true); }
   */

  protected GeoPoint(Construction c, Region region) {
    super(c);
    this.region = region;
  }

  /**
   * Creates new GeoPoint
   */
  public GeoPoint(Construction c, String label, double x, double y, double z) {
    super(c, x, y, z); // GeoVec3D constructor
    setLabel(label);
  }

  protected GeoPoint(GeoPoint point) {
    super(point.cons);
    set(point);
  }

  public void addToPathParameter(double a) {
    PathParameter pathParameter = getPathParameter();
    pathParameter.t += a;

    // update point relative to path
    path.pathChanged(this);
    updateCoords();
  }

  private StringBuffer buildValueString() {
    sbBuildValueString.setLength(0);
    if (isInfinite()) {
      sbBuildValueString.append(Plain.undefined);
      return sbBuildValueString;
    }

    switch (toStringMode) {
      case Kernel.COORD_POLAR :
        sbBuildValueString.append('(');
        sbBuildValueString.append(kernel.format(GeoVec2D.length(getInhomX(),
            getInhomY())));
        sbBuildValueString.append("; ");
        sbBuildValueString.append(kernel.formatAngle(Math.atan2(getInhomY(),
            getInhomX())));
        sbBuildValueString.append(')');
        break;

      case Kernel.COORD_COMPLEX :
        sbBuildValueString.append(kernel.format(getInhomX()));
        sbBuildValueString.append(" ");
        sbBuildValueString.append(kernel.formatSigned(getInhomY()));
        sbBuildValueString.append("i");
        break;

      default : // CARTESIAN
        sbBuildValueString.append('(');
        sbBuildValueString.append(kernel.format(getInhomX()));
        switch (kernel.getCoordStyle()) {
          case Kernel.COORD_STYLE_AUSTRIAN :
            sbBuildValueString.append(" | ");
            break;

          default :
            sbBuildValueString.append(", ");
        }
        sbBuildValueString.append(kernel.format(getInhomY()));
        sbBuildValueString.append(')');
    }
    return sbBuildValueString;
  }

  final public void clearPathParameter() {
    pathParameter = null;
  }

  @Override
  public GeoElement copy() {
    return new GeoPoint(this);
  }
  /**
   * dilate from S by r
   */
  final public void dilate(NumberValue rval, GeoPoint S) {
    double r = rval.getDouble();
    double temp = 1 - r;
    setCoords(r * x + temp * S.inhomX * z, r * y + temp * S.inhomY * z, z);
  }
  // euclidian distance between this GeoPoint and P
  final public double distance(GeoPoint P) {
    return GeoVec2D.length(P.inhomX - inhomX, P.inhomY - inhomY);
  }

  /**
   * returns the square distance of this point and P (may return infinty or
   * NaN).
   */
  final protected double distanceSqr(GeoPoint P) {
    double vx = P.inhomX - inhomX;
    double vy = P.inhomY - inhomY;
    return vx * vx + vy * vy;
  }

  /**
   * Tells Locateables that their start point is removed and calls
   * super.remove()
   */
  @Override
  protected void doRemove() {
    if (locateableList != null) {
      // copy locateableList into array
      Object[] locs = locateableList.toArray();
      locateableList.clear();

      // tell all locateables
      for (Object loc2 : locs) {
        Locateable loc = (Locateable) loc2;
        loc.removeStartPoint(this);
        loc.toGeoElement().updateCascade();
      }
    }

    if (path != null) {
      GeoElement geo = path.toGeoElement();
      if (geo.isGeoConic())
        ((GeoConic) geo).removePointOnConic(this);
    }

    super.doRemove();
  }

  /** POLAR or CARTESIAN */

  @Override
  public ExpressionValue evaluate() {
    return this;
  }

  @Override
  protected String getClassName() {
    return "GeoPoint";
  }

  /**
   * Returns the single free GeoNumeric expression wrapped in this
   * ExpressionValue. For "a + x(A)" this returns a, for "x(A)" this returns
   * null where A is a free point. If A is a dependent point, "a + x(A)" throws
   * an Exception.
   */
  private GeoNumeric getCoordNumber(ExpressionValue ev, boolean allowPlusNode)
      throws Throwable {
    // simple variable "a"
    if (ev.isLeaf())
      return (GeoNumeric) kernel.lookupLabel(ev.toString(), false);

    // are expressions like "a + x(A)" allowed?
    if (!allowPlusNode)
      return null;

    // return value
    GeoNumeric coordNumeric = null;

    // expression + expression
    ExpressionNode en = (ExpressionNode) ev;
    if (en.getOperation() == ExpressionNode.PLUS
        && en.getLeft() instanceof GeoNumeric) {

      // left branch needs to be a single number variable: get it
      // e.g. a + x(D)
      coordNumeric = (GeoNumeric) en.getLeft();

      // check that variables in right branch are all independent to avoid
      // circular definitions
      HashSet rightVars = en.getRight().getVariables();
      if (rightVars != null) {
        Iterator it = rightVars.iterator();
        while (it.hasNext()) {
          GeoElement var = (GeoElement) it.next();
          if (!var.isIndependent())
            throw new Exception("dependent var: " + var);
        }
      }
    }

    return coordNumeric;
  }

  /**
   * Returns an array of GeoNumeric objects that directly control this point's
   * coordinates. For point P = (a, b) the array [a, b] is returned, for P =
   * (x(A) + c, d + y(A)) the array [c, d] is returned, for P = (x(A) + c, y(A))
   * the array [c, null] is returned.
   * 
   * @return null if this point is not defined using two GeoNumeric objects
   */
  final public ArrayList<GeoNumeric> getCoordParentNumbers() {
    // init changeableCoordNumbers
    if (changeableCoordNumbers == null) {
      changeableCoordNumbers = new ArrayList<GeoNumeric>(2);
      AlgoElement parentAlgo = getParentAlgorithm();

      // dependent point of form P = (a, b)
      if (parentAlgo instanceof AlgoDependentPoint) {
        AlgoDependentPoint algo = (AlgoDependentPoint) parentAlgo;
        ExpressionNode en = algo.getExpressionNode();

        // (xExpression, yExpression)
        if (en.isLeaf() && en.getLeft() instanceof MyVecNode) {
          // (xExpression, yExpression)
          MyVecNode vn = (MyVecNode) en.getLeft();
          hasPolarParentNumbers = vn.hasPolarCoords();

          try {
            // try to get free number variables used in coords for this point
            // don't allow expressions like "a + x(A)" for polar coords (r; phi)
            GeoNumeric xvar = getCoordNumber(vn.getX(), !hasPolarParentNumbers);
            GeoNumeric yvar = getCoordNumber(vn.getY(), !hasPolarParentNumbers);
            if (xvar != yvar) { // avoid (a,a)
              changeableCoordNumbers.add(xvar);
              changeableCoordNumbers.add(yvar);
            }
          } catch (Throwable e) {
            changeableCoordNumbers.clear();
            e.printStackTrace();
          }
        }
      }
    }

    return changeableCoordNumbers;
  }

  @Override
  public int getGeoClassType() {
    return GEO_CLASS_POINT;
  }

  /**
   * Writes (x/z, y/z) to res.
   */
  @Override
  final public void getInhomCoords(double[] res) {
    res[0] = inhomX;
    res[1] = inhomY;
  }

  final public double getInhomX() {
    return inhomX;
  }

  final public double getInhomY() {
    return inhomY;
  }

  final public Path getPath() {
    return path;
  }

  final public PathParameter getPathParameter() {
    if (pathParameter == null)
      pathParameter = new PathParameter();
    return pathParameter;
  }

  /**
   * @return
   */
  public int getPointSize() {
    return pointSize;
  }

  /**
   * @author Florian Sonner
   * @version 2008-07-17
   */
  final public int getPointStyle() {
    return pointStyle;
  }

  final public void getPolarCoords(double[] res) {
    res[0] = GeoVec2D.length(inhomX, inhomY);
    res[1] = Math.atan2(inhomY, inhomX);
  }
  public Region getRegion() {
    return region;
  }

  final public RegionParameters getRegionParameters() {
    if (regionParameters == null)
      regionParameters = new RegionParameters();
    return regionParameters;
  }

  public String getStartPointXML() {
    StringBuffer sb = new StringBuffer();
    sb.append("\t<startPoint ");

    if (isAbsoluteStartPoint()) {
      sb.append(" x=\"" + x + "\"");
      sb.append(" y=\"" + y + "\"");
      sb.append(" z=\"" + z + "\"");
    } else {
      sb.append("exp=\"");
      boolean oldValue = kernel.isTranslateCommandName();
      kernel.setTranslateCommandName(false);
      sb.append(Util.encodeXML(getLabel()));
      kernel.setTranslateCommandName(oldValue);
      sb.append("\"");
    }
    sb.append("/>\n");
    return sb.toString();
  }

  private PathParameter getTempPathparameter() {
    if (tempPathParameter == null)
      tempPathParameter = new PathParameter();
    return tempPathParameter;
  }

  @Override
  protected String getTypeString() {
    if (toStringMode == Kernel.COORD_COMPLEX)
      return "ComplexNumber";
    else
      return "Point";
  }

  @Override
  public HashSet getVariables() {
    HashSet varset = new HashSet();
    varset.add(this);
    return varset;
  }

  /**
   * interface VectorValue implementation
   */
  public GeoVec2D getVector() {
    GeoVec2D ret = new GeoVec2D(kernel, inhomX, inhomY);
    ret.setMode(toStringMode);
    return ret;
  }

  public double getX2D() {
    return x2D;
  }

  /**
   * returns all class-specific xml tags for saveXML GeoGebra File Format
   */
  @Override
  protected String getXMLtags() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.getXMLtags());

    /*
     * should not be needed if (path != null) { pathParameter.appendXML(sb); }
     */

    // polar or cartesian coords
    switch (toStringMode) {
      case Kernel.COORD_POLAR :
        sb.append("\t<coordStyle style=\"polar\"/>\n");
        break;

      case Kernel.COORD_COMPLEX :
        sb.append("\t<coordStyle style=\"complex\"/>\n");
        break;

      default :
        // don't save default
        // sb.append("\t<coordStyle style=\"cartesian\"/>\n");
    }

    // point size
    sb.append("\t<pointSize val=\"");
    sb.append(pointSize);
    sb.append("\"/>\n");

    // point style, Florian Sonner 2008-07-17
    if (pointStyle >= 0) {
      sb.append("\t<pointStyle val=\"");
      sb.append(pointStyle);
      sb.append("\"/>\n");
    }

    return sb.toString();
  }

  public double getY2D() {
    return y2D;
  }

  /**
   * Returns whether this point has two changeable numbers as coordinates, e.g.
   * point A = (a, b) where a and b are free GeoNumeric objects.
   */
  final public boolean hasChangeableCoordParentNumbers() {

    if (isFixed())
      return false;

    ArrayList<GeoNumeric> coords = getCoordParentNumbers();
    if (coords.size() == 0)
      return false;

    Object num1 = coords.get(0);
    Object num2 = coords.get(1);

    if (num1 == null || num2 == null)
      return false;

    boolean ret = ((GeoNumeric) num1).isChangeable()
        && ((GeoNumeric) num2).isChangeable();

    return ret;
  }

  public boolean hasPath() {
    return path != null;
  }

  /**
   * Returns whether getCoordParentNumbers() returns polar variables (r; phi).
   */
  protected boolean hasPolarParentNumbers() {
    return hasPolarParentNumbers;
  }

  protected boolean hasRegion() {
    return region != null;
  }

  final public boolean isAbsoluteStartPoint() {
    return isIndependent() && !isLabelSet();
  }

  /***********************************************************
   * MOVEMENTS
   ***********************************************************/

  @Override
  public boolean isChangeable() {

    // if we drag a AlgoDynamicCoordinates, we want its point to be dragged
    AlgoElement algo = getParentAlgorithm();
    if (algo != null && algo instanceof AlgoDynamicCoordinates)
      return true;

    return !isFixed()
        && (isIndependent() || isPointOnPath() || isPointInRegion());
  }

  @Override
  public boolean isConstant() {
    return false;
  }

  @Override
  final public boolean isDefined() {
    return isDefined;
  }

  /**
   * Yields true if the inhomogenous coordinates of this point are equal to
   * those of point P. Infinite points are checked for linear dependency.
   */
  // Michael Borcherds 2008-04-30
  @Override
  final public boolean isEqual(GeoElement geo) {

    if (!geo.isGeoPoint())
      return false;

    GeoPoint P = (GeoPoint) geo;

    if (!(isDefined() && P.isDefined()))
      return false;

    // both finite
    if (isFinite() && P.isFinite())
      return kernel.isEqual(inhomX, P.inhomX)
          && kernel.isEqual(inhomY, P.inhomY);
    else if (isInfinite() && P.isInfinite())
      return linDep(P);
    else
      return false;
  }

  final public boolean isFinite() {
    return isDefined && !isInfinite;
  }

  @Override
  final public boolean isFixable() {
    return path != null || super.isFixable();
  }

  @Override
  final public boolean isGeoPoint() {
    return true;
  }

  /**
   * Returns true if this point's path is a circle or ellipse
   * 
   * public boolean hasAnglePathParameter() { return (path != null) && (path
   * instanceof GeoConic) && (((GeoConic)path).isElliptic()); }
   */

  final public boolean isInfinite() {
    return isInfinite;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  @Override
  public boolean isNumberValue() {
    return false;
  }
  final public boolean isPointInRegion() {
    return region != null;
  }

  @Override
  final public boolean isPointOnPath() {
    return path != null;
  }

  @Override
  public boolean isPolynomialInstance() {
    return false;
  }
  @Override
  public boolean isTextValue() {
    return false;
  }

  @Override
  final public boolean isTranslateable() {
    return true;
  }

  public boolean isVector3DValue() {
    return false;
  }

  @Override
  public boolean isVectorValue() {
    return true;
  }

  /**
   * mirror transform with angle phi [ cos(phi) sin(phi) ] [ sin(phi) -cos(phi)
   * ]
   */
  final private void mirror(double phi) {
    double cos = Math.cos(phi);
    double sin = Math.sin(phi);

    double x0 = x * cos + y * sin;
    y = x * sin - y * cos;
    x = x0;
  }

  // Michael Borcherds 2008-02-10
  final protected void mirror(GeoConic c) {
    if (c.getType() == GeoConic.CONIC_CIRCLE) { // Mirror point in circle
      double r = c.getHalfAxes()[0];
      GeoVec2D midpoint = c.getTranslationVector();
      double a = midpoint.x;
      double b = midpoint.y;
      double sf = r * r
          / ((inhomX - a) * (inhomX - a) + (inhomY - b) * (inhomY - b));
      setCoords(a + sf * (inhomX - a), b + sf * (inhomY - b), 1.0);
    } else
      setUndefined();
  }

  /**
   * mirror this point at line g
   */
  final public void mirror(GeoLine g) {
    // Y = S(phi).(X - Q) + Q
    // where Q is a point on g, S(phi) is the mirrorTransform(phi)
    // and phi/2 is the line's slope angle

    // get arbitrary point of line
    double qx, qy;
    if (Math.abs(g.x) > Math.abs(g.y)) {
      qx = -z * g.z / g.x;
      qy = 0.0d;
    } else {
      qx = 0.0d;
      qy = -z * g.z / g.y;
    }

    // translate -Q
    x -= qx;
    y -= qy;

    // S(phi)
    mirror(2.0 * Math.atan2(-g.x, g.y));

    // translate back +Q
    x += qx;
    y += qy;

    // update inhom coords
    updateCoords();
  }

  /**
   * mirror this point at point Q
   */
  final public void mirror(GeoPoint Q) {
    double qx = z * Q.inhomX;
    double qy = z * Q.inhomY;

    setCoords(2.0 * qx - x, 2.0 * qy - y, z);
  }

  /**
   * Tells this point that the given Locateable has this point as start point.
   */
  protected void registerLocateable(Locateable l) {
    if (locateableList == null)
      locateableList = new ArrayList();
    if (locateableList.contains(l))
      return;

    // add only locateables that are not already
    // part of the updateSet of this point
    AlgoElement parentAlgo = l.toGeoElement().getParentAlgorithm();
    if (parentAlgo == null || !getAlgoUpdateSet().contains(parentAlgo))
      // add the locatable
      locateableList.add(l);
  }

  /**
   * rotate this point by angle phi around (0,0)
   */
  final public void rotate(NumberValue phiValue) {
    double phi = phiValue.getDouble();
    double cos = Math.cos(phi);
    double sin = Math.sin(phi);

    setCoords(x * cos - y * sin, x * sin + y * cos, z);
  }

  /**
   * rotate this point by angle phi around Q
   */
  final public void rotate(NumberValue phiValue, GeoPoint Q) {
    double phi = phiValue.getDouble();
    double cos = Math.cos(phi);
    double sin = Math.sin(phi);
    double qx = z * Q.inhomX;
    double qy = z * Q.inhomY;

    setCoords((x - qx) * cos + (qy - y) * sin + qx, (x - qx) * sin + (y - qy)
        * cos + qy, z);
  }

  @Override
  public void set(GeoElement geo) {
    if (geo.isGeoPoint()) {
      GeoPoint p = (GeoPoint) geo;
      if (p.pathParameter != null) {
        pathParameter = getPathParameter();
        pathParameter.set(p.pathParameter);
      }
      setCoords(p.x, p.y, p.z);
      setMode(p.toStringMode); // complex etc
    } else if (geo.isGeoVector()) {
      GeoVector v = (GeoVector) geo;
      setCoords(v.x, v.y, 1d);
      setMode(v.toStringMode); // complex etc
    }

  }

  /**
   * Sets homogeneous coordinates and updates inhomogeneous coordinates
   */
  @Override
  final public void setCoords(double x, double y, double z) {
    // set coordinates
    this.x = x;
    this.y = y;
    this.z = z;

    // update point on path: this may change coords
    // so updateCoords() is called afterwards
    if (path != null) {
      // remember path parameter for undefined case
      PathParameter tempPathParameter = getTempPathparameter();
      tempPathParameter.set(getPathParameter());
      path.pointChanged(this);
    }

    // region
    if (hasRegion())
      region.pointChangedForRegion(this);

    // this avoids multiple computations of inhomogeneous coords;
    // see for example distance()
    updateCoords();

    // undefined and on path: remember old path parameter
    if (!isDefined && path != null) {
      PathParameter pathParameter = getPathParameter();
      PathParameter tempPathParameter = getTempPathparameter();
      pathParameter.set(tempPathParameter);
    }
  }

  final public void setCoords(GeoVec2D v) {
    setCoords(v.x, v.y, 1.0);
  }

  @Override
  final public void setCoords(GeoVec3D v) {
    setCoords(v.x, v.y, v.z);
  }
  void setPath(Path p) {
    path = p;

    // tell conic that this point is on it, that's needed to handle reflections
    // of concis correctly for path parameter calculation of point P
    GeoElement geo = path.toGeoElement();
    if (geo.isGeoConic())
      ((GeoConic) geo).addPointOnConic(this);
  }

  /**
   * @param i
   */
  public void setPointSize(int i) {
    pointSize = i;
  }

  /**
   * @author Florian Sonner
   * @version 2008-07-17
   * @param int style the new style to use
   */
  public void setPointStyle(int style) {
    switch (style) {
      // TODO: Remove violation of the DRY principle
      // (EucledianView.setPointStyle)

      case 0 :
      case 1 :
      case 2 :
        pointStyle = style;
        break;
    }
  }

  final public void setPolarCoords(double r, double phi) {
    setCoords(r * Math.cos(phi), r * Math.sin(phi), 1.0d);
  }

  public void setRegion(Region a_region) {
    region = a_region;
  }

  @Override
  public void setVisualStyle(GeoElement geo) {
    super.setVisualStyle(geo);

    if (geo.isGeoPoint()) {
      pointSize = ((GeoPoint) geo).pointSize;
      pointStyle = ((GeoPoint) geo).pointStyle;
    } else if (geo instanceof PointProperties) {
      setPointSize(((PointProperties) geo).getPointSize());
      setPointStyle(((PointProperties) geo).getPointStyle());
    }
  }

  @Override
  public void setZero() {
    setCoords(0, 0, 1);
  }

  @Override
  public final boolean showInAlgebraView() {
    // intersection points
    return isDefined || showUndefinedInAlgebraView;
  }
  @Override
  final public boolean showInEuclidianView() {
    return isDefined && !isInfinite;
  }

  // ///////////////////////////////////////////
  // REGION

  void showUndefinedInAlgebraView(boolean flag) {
    showUndefinedInAlgebraView = flag;
  }

  /***********************************************************/

  @Override
  final public String toString() {
    sbToString.setLength(0);
    sbToString.append(label);
    if (kernel.getCoordStyle() != Kernel.COORD_STYLE_AUSTRIAN)
      sbToString.append(" = ");
    sbToString.append(buildValueString());
    return sbToString.toString();
  }

  @Override
  final public String toValueString() {
    return buildValueString().toString();
  }

  /**
   * translate by vector v
   */
  final public void translate(GeoVector v) {
    setCoords(x + v.x * z, y + v.y * z, z);
  }

  /**
   * Tells this point that the given Locatable no longer has this point as start
   * point.
   */
  protected void unregisterLocateable(Locateable l) {
    if (locateableList != null)
      locateableList.remove(l);
  }

  /**
   * Calls super.update() and updateCascade() for all registered locateables.
   */
  @Override
  public void update() {
    super.update();

    // update all registered locatables (they have this point as start point)
    if (locateableList != null)
      GeoElement.updateCascade(locateableList, getTempSet());
  }

  final protected void updateCoords() {
    // infinite point
    if (kernel.isZero(z)) {
      isInfinite = true;
      isDefined = !(Double.isNaN(x) || Double.isNaN(y));
      inhomX = Double.NaN;
      inhomY = Double.NaN;
    }
    // finite point
    else {
      isInfinite = false;
      isDefined = !(Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z));

      if (isDefined) {
        // make sure the z coordinate is always positive
        // this is important for the orientation of a line or ray
        // computed using two points P, Q with cross(P, Q)
        if (z < 0) {
          x = -x;
          y = -y;
          z = -z;
        }

        // update inhomogeneous coords
        if (z == 1.0) {
          inhomX = x;
          inhomY = y;
        } else {
          inhomX = x / z;
          inhomY = y / z;
        }
      } else {
        inhomX = Double.NaN;
        inhomY = Double.NaN;
      }
    }
  }

  public void updateCoords2D() {
    x2D = x / z;
    y2D = y / z;
  }

  // only used for 3D stuff
  public void updateCoordsFrom2D(boolean doPathOrRegion) {

  }

}
