/* 
  GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * GeoElement.java
 *
 * Created on 30. August 2001, 17:10
 */

package geogebra.kernel;

import geogebra.Plain;
import geogebra.euclidian.EuclidianView;
import geogebra.kernel.arithmetic.ExpressionValue;
import geogebra.kernel.arithmetic.NumberValue;
import geogebra.main.MyError;
import geogebra.util.Util;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Markus
 * @version
 */
public abstract class GeoElement extends ConstructionElement
    implements
      ExpressionValue {

  // min decimals or significant figures to use in editing string
  public static final int MIN_EDITING_PRINT_PRECISION = 5;

  // maximum label offset distance
  private static final int MAX_LABEL_OFFSET = 80;

  // private static int geoElementID = Integer.MIN_VALUE;

  private static final char[] complexLabels = {'z'};

  private static final char[] pointLabels = {'A', 'B', 'C', 'D', 'E', 'F', 'G',
      'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
      'V', 'W', 'Z'};

  private static final char[] functionLabels = {'f', 'g', 'h', 'p', 'q', 'r',
      's', 't'};

  private static final char[] lineLabels = {'a', 'b', 'c', 'd', 'e', 'f', 'g',
      'h', 'i', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't'};

  private static final char[] vectorLabels = {'u', 'v', 'w', 'z', 'a', 'b',
      'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'p', 'q',
      'r', 's', 't'};

  private static final char[] conicLabels = {'c', 'd', 'e', 'f', 'g', 'h', 'k',
      'p', 'q', 'r', 's', 't'};

  private static final char[] lowerCaseLabels = {'a', 'b', 'c', 'd', 'e', 'f',
      'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'z'};

  private static final char[] greekLowerCase = {'\u03b1', '\u03b2', '\u03b3',
      '\u03b4', '\u03b5', '\u03b6', '\u03b7', '\u03b8', '\u03b9', '\u03ba',
      '\u03bb', '\u03bc', '\u03bd', '\u03be', '\u03bf', '\u03c1', '\u03c3',
      '\u03c4', '\u03c5', '\u03c6', '\u03c7', '\u03c8', '\u03c9'};

  /*
   * private static final char[] greekUpperCase = { // Michael Borcherds
   * 2008-02-23 '\u0391', '\u0392', '\u0393', '\u0394', '\u0395', '\u0396',
   * '\u0397', '\u0398', '\u0399', '\u039a', '\u039b', '\u039c', '\u039d',
   * '\u039e', '\u039f', '\u03a0', '\u03a1', '\u03a3', '\u03a4', '\u03a5',
   * '\u03a6', '\u03a7', '\u03a8', '\u03a9'};
   */

  // GeoElement types
  public static final int GEO_CLASS_ANGLE = 10;
  protected static final int GEO_CLASS_AXIS = 20;
  public static final int GEO_CLASS_BOOLEAN = 30;
  public static final int GEO_CLASS_JAVASCRIPT_BUTTON = 31;
  public static final int GEO_CLASS_CONIC = 40;
  public static final int GEO_CLASS_CONICPART = 50;
  public static final int GEO_CLASS_FUNCTION = 60;
  public static final int GEO_CLASS_FUNCTIONCONDITIONAL = 70;
  public static final int GEO_CLASS_IMAGE = 80;
  public static final int GEO_CLASS_LINE = 90;
  public static final int GEO_CLASS_LIST = 100;
  public static final int GEO_CLASS_LOCUS = 110;
  public static final int GEO_CLASS_NUMERIC = 120;
  public static final int GEO_CLASS_POINT = 130;
  public static final int GEO_CLASS_POLYGON = 140;
  public static final int GEO_CLASS_RAY = 150;
  public static final int GEO_CLASS_SEGMENT = 160;
  public static final int GEO_CLASS_TEXT = 170;
  public static final int GEO_CLASS_VECTOR = 180;
  public static final int GEO_CLASS_CURVE_CARTESIAN = 190;
  public static final int GEO_CLASS_CURVE_POLAR = 191;

  protected static final int LABEL_NAME = 0;
  public static final int LABEL_NAME_VALUE = 1;
  public static final int LABEL_VALUE = 2;
  public static final int LABEL_CAPTION = 3; // Michael Borcherds 2008-02-18

  private static StringBuffer sbToolTipDesc = new StringBuffer();
  private static boolean containsOnlyMoveableGeos(ArrayList geos) {
    if (geos == null || geos.size() == 0)
      return false;

    for (int i = 0; i < geos.size(); i++) {
      GeoElement geo = (GeoElement) geos.get(i);
      if (!geo.isMoveable())
        return false;
    }
    return true;
  }
  /**
   * Copies the given points array. The resulting points are part of the given
   * construction.
   */
  protected static GeoPoint[] copyPoints(Construction cons, GeoPoint[] points) {
    GeoPoint[] pointsCopy = new GeoPoint[points.length];
    for (int i = 0; i < points.length; i++) {
      pointsCopy[i] = (GeoPoint) points[i].copyInternal(cons);
      pointsCopy[i].set(points[i]);
    }
    return pointsCopy;
  }
  // Cong Liu
  public static int getSpreadsheetRow(Matcher matcher) {
    if (!matcher.matches())
      return -1;
    String s = matcher.group(2);
    return Integer.parseInt(s) - 1;
  }
  /**
   * Returns long description for all GeoElements in given array.
   */
  final public static String getToolTipDescriptionHTML(ArrayList<Object> geos,
      boolean colored, boolean addHTMLtag) {
    if (geos == null)
      return null;

    sbToolTipDesc.setLength(0);
    if (addHTMLtag)
      sbToolTipDesc.append("<html>");
    int count = 0;
    for (int i = 0; i < geos.size(); ++i) {
      GeoElement geo = (GeoElement) geos.get(i);
      if (geo.showToolTipText()) {
        count++;
        sbToolTipDesc.append(geo.getLongDescriptionHTML(colored, false));
        if (i + 1 < geos.size())
          sbToolTipDesc.append("<br>");
      }
    }
    if (count == 0)
      return null;
    if (addHTMLtag)
      sbToolTipDesc.append("</html>");
    return sbToolTipDesc.toString();
  }
  /**
   * set labels for array of GeoElements with given label prefix. e.g.
   * labelPrefix = "F", geos.length = 2 sets geo[0].setLabel("F_1") and
   * geo[0].setLabel("F_2")
   */
  public static void setLabels(String labelPrefix, GeoElement[] geos) {
    if (geos == null)
      return;

    int visible = 0;
    for (GeoElement geo : geos)
      if (geo.isVisible())
        visible++;

    switch (visible) {
      case 0 : // no visible geos: they all get the labelPrefix as suggestion
        for (GeoElement geo : geos)
          geo.setLabel(labelPrefix);
        break;

      case 1 : // if there is only one visible geo, don't use indices
        geos[0].setLabel(labelPrefix);
        break;

      default :
        // is this a spreadsheet label?
        Matcher matcher = GeoElement.spreadsheetPattern.matcher(labelPrefix);
        if (matcher.matches()) {
          // more than one visible geo and it's a spreadsheet cell
          // use D1, E1, F1, etc as names
          int col = getSpreadsheetColumn(matcher);
          int row = getSpreadsheetRow(matcher);
          for (int i = 0; i < geos.length; i++)
            geos[i].setLabel(geos[i].getFreeLabel(getSpreadsheetCellName(col
                + i, row)));
        } else
          for (GeoElement geo : geos)
            geo.setLabel(geo.getIndexLabel(labelPrefix));
    }
  }
  /**
   * set labels for array of GeoElements pairwise: geos[i].setLabel(labels[i])
   */
  static void setLabels(String[] labels, GeoElement[] geos) {
    setLabels(labels, geos, false);
  }
  private static void setLabels(String[] labels, GeoElement[] geos,
      boolean indexedOnly) {
    int labelLen = labels == null ? 0 : labels.length;

    if (labelLen == 1 && labels[0] != null && !labels[0].equals("")) {
      setLabels(labels[0], geos);
      return;
    }

    String label;
    for (int i = 0; i < geos.length; i++) {
      if (i < labelLen)
        label = labels[i];
      else
        label = null;

      if (indexedOnly)
        label = geos[i].getIndexLabel(label);

      geos[i].setLabel(label);
    }
  }
  /**
   * Updates all GeoElements in the given ArrayList and all algorithms that
   * depend on free GeoElements in that list. Note: this method is more
   * efficient than calling updateCascade() for all individual GeoElements.
   * 
   * @param tempSet
   *          : a temporary set that is used to collect all algorithms that need
   *          to be updated
   */
  final static public synchronized void updateCascade(ArrayList geos,
      TreeSet<AlgoElement> tempSet) {
    // only one geo: call updateCascade()
    if (geos.size() == 1) {
      GeoElement geo = (GeoElement) geos.get(0);
      geo.updateCascade();
      return;
    }

    // build update set of all algorithms in construction element order
    // clear temp set
    tempSet.clear();

    int size = geos.size();
    for (int i = 0; i < size; i++) {
      GeoElement geo = (GeoElement) geos.get(i);
      geo.update();

      if (geo.isIndependent() && geo.algoUpdateSet != null)
        // add all dependent algos of geo to the overall algorithm set
        geo.algoUpdateSet.addAllToCollection(tempSet);
    }

    // now we have one nice algorithm set that we can update
    if (tempSet.size() > 0) {
      Iterator<AlgoElement> it = tempSet.iterator();
      while (it.hasNext()) {
        AlgoElement algo = it.next();
        algo.update();
      }
    }
  }
  protected String label; // should only be used directly in subclasses
  private String oldLabel; // see doRenameLabel
  private String caption; // only used by GeoBoolean for check boxes at the
  // moment
  boolean labelWanted = false;

  boolean labelSet = false;

  private boolean localVarLabelSet = false;
  private boolean euclidianVisible = true;
  private boolean algebraVisible = true;
  private boolean labelVisible = true;
  private boolean isConsProtBreakpoint; // in construction protocol
  private boolean isAlgoMacroOutput; // is an output object of a macro
  // construction
  private boolean fixed = false;
  private int labelMode = LABEL_NAME;

  protected int toStringMode = Kernel.COORD_CARTESIAN; // cartesian or polar
  private Color objColor = Color.black;
  private Color selColor = objColor;
  private Color labelColor = objColor;
  private Color fillColor = objColor;
  private int layer = 0; // Michael Borcherds 2008-02-23

  public double animationIncrement = 0.1;
  private NumberValue animationSpeedObj;
  private boolean animating = false;

  public final static double MAX_ANIMATION_SPEED = 100;
  public final static int ANIMATION_OSCILLATING = 0;

  protected final static int ANIMATION_INCREASING = 1;

  public final static int ANIMATION_DECREASING = 2;

  private int animationType = ANIMATION_OSCILLATING;
  private int animationDirection = 1;
  public float alphaValue = 0.0f;
  public int labelOffsetX = 0, labelOffsetY = 0;
  private boolean auxiliaryObject = false;
  // on change: see setVisualValues()
  // spreadsheet specific properties
  private Point spreadsheetCoords, oldSpreadsheetCoords;
  private int cellRangeUsers = 0; // number of AlgoCellRange using this cell:
  // don't allow renaming when greater 0
  // condition to show object
  private GeoBoolean condShowObject;
  // function to determine color
  private GeoList colFunction; // { GeoNumeric red, GeoNumeric Green, GeoNumeric
  // Blue }
  private boolean useVisualDefaults = true;

  private boolean isColorSet = false;
  private boolean highlighted = false;

  private boolean selected = false;

  private String strAlgebraDescription, strAlgebraDescTextOrHTML,
      strAlgebraDescriptionHTML, strLabelTextOrHTML, strLaTeX;
  private boolean strAlgebraDescriptionNeedsUpdate = true;
  private boolean strAlgebraDescTextOrHTMLneedsUpdate = true;
  private boolean strAlgebraDescriptionHTMLneedsUpdate = true;
  private boolean strLabelTextOrHTMLUpdate = true;
  private boolean strLaTeXneedsUpdate = true;
  // line thickness and line type: s
  // note: line thickness in Drawable is calculated as lineThickness / 2.0f
  public int lineThickness = EuclidianView.DEFAULT_LINE_THICKNESS;
  public int lineType = EuclidianView.DEFAULT_LINE_TYPE;
  // decoration types
  public int decorationType = DECORATION_NONE;
  // DECORATION
  public static final int DECORATION_NONE = 0;
  // segment decorations
  public static final int DECORATION_SEGMENT_ONE_TICK = 1;
  public static final int DECORATION_SEGMENT_TWO_TICKS = 2;
  public static final int DECORATION_SEGMENT_THREE_TICKS = 3;
  // Michael Borcherds 2007-10-06
  public static final int DECORATION_SEGMENT_ONE_ARROW = 4;

  public static final int DECORATION_SEGMENT_TWO_ARROWS = 5;
  public static final int DECORATION_SEGMENT_THREE_ARROWS = 6;

  // Michael Borcherds 2007-10-06
  // angle decorations
  public static final int DECORATION_ANGLE_TWO_ARCS = 1;

  /********************************************************/

  public static final int DECORATION_ANGLE_THREE_ARCS = 2;

  /* ****************************************************** */

  public static final int DECORATION_ANGLE_ONE_TICK = 3;

  public static final int DECORATION_ANGLE_TWO_TICKS = 4;

  public static final int DECORATION_ANGLE_THREE_TICKS = 5;

  // Michael Borcherds START 2007-11-19
  public static final int DECORATION_ANGLE_ARROW_ANTICLOCKWISE = 6; // Michael
  // Borcherds
  // 2007-10-22

  public static final int DECORATION_ANGLE_ARROW_CLOCKWISE = 7; // Michael
  // Borcherds
  // 2007-10-22
  // Michael Borcherds END 2007-11-19

  // public int geoID;
  // static private int geoCounter = 0;
  private AlgoElement algoParent = null; // Parent Algorithm

  private ArrayList<AlgoElement> algorithmList; // directly dependent algos

  // set of all dependent algos sorted in topological order
  private AlgorithmSet algoUpdateSet;

  private StringBuffer captionSB = null;

  // Cong Liu
  public static final Pattern spreadsheetPattern = Pattern
      .compile("\\$?([A-Z]+)\\$?([0-9]+)");

  // Cong Liu
  public static String getSpreadsheetCellName(int column, int row) {
    ++row;
    return getSpreadsheetColumnName(column) + row;
  }

  // Cong Liu
  public static int getSpreadsheetColumn(Matcher matcher) {
    if (!matcher.matches())
      return -1;

    String s = matcher.group(1);
    int column = 0;
    while (s.length() > 0) {
      column *= 26;
      column += s.charAt(0) - 'A' + 1;
      s = s.substring(1);
    }
    // Application.debug(column);
    return column - 1;
  }
  public static String getSpreadsheetColumnName(int i) {
    ++i;
    String col = "";
    while (i > 0) {
      col = (char) ('A' + i % 26 - 1) + col;
      i /= 26;
    }
    return col;
  }
  protected static String getSpreadsheetColumnName(String label) {
    Matcher matcher = spreadsheetPattern.matcher(label);
    if (!matcher.matches())
      return null;
    return matcher.group(1);
  }

  /**
   * Returns a point with the spreadsheet coordinates of the given inputLabel.
   * Note that this can also be used for names that include $ signs like "$A1".
   * 
   * @return null for non-spreadsheet names
   */
  protected static Point getSpreadsheetCoordsForLabel(String inputLabel) {
    // we need to also support wrapped GeoElements like
    // $A4 that are implemented as dependent geos (using ExpressionNode)
    Matcher matcher = GeoElement.spreadsheetPattern.matcher(inputLabel);
    int column = getSpreadsheetColumn(matcher);
    int row = getSpreadsheetRow(matcher);

    // System.out.println("match: " + inputLabel);
    // for (int i=0; i < matcher.groupCount(); i++) {
    // System.out.println("    group: " + i + ": " + matcher.group(i));
    // }

    if (column >= 0 && row >= 0)
      return new Point(column, row);
    else
      return null;
  }

  // Michael Borcherds
  public static boolean isSpreadsheetLabel(String str) {
    Matcher matcher = spreadsheetPattern.matcher(str);
    if (matcher.matches())
      return true;
    else
      return false;
  }

  private final StringBuffer sbDefaultLabel = new StringBuffer();

  private final StringBuffer sbIndexLabel = new StringBuffer();

  private final GeoElement[] myGeoElements = new GeoElement[]{this};

  private final StringBuffer sbLongDesc = new StringBuffer();

  private final StringBuffer sbLongDescHTML = new StringBuffer();

  private final StringBuffer sbAlgebraDesc = new StringBuffer();

  /*
   * replaces all indices (_ and _{}) in str by <sub> tags, all and converts all
   * special characters in str to HTML examples: "a_1" becomes "a<sub>1</sub>"
   * "s_{AB}" becomes "s<sub>AB</sub>"
   */
  private static String subBegin = "<sub><font size=\"-1\">";

  private static String subEnd = "</font></sub>";

  private static StringBuffer sbIndicesToHTML = new StringBuffer();

  private static String indicesToHTML(String str, boolean addHTMLtag) {
    sbIndicesToHTML.setLength(0);
    if (addHTMLtag)
      sbIndicesToHTML.append("<html>");

    int depth = 0;
    int startPos = 0;
    int length = str.length();
    for (int i = 0; i < length; i++)
      switch (str.charAt(i)) {
        case '_' :
          // write everything before _
          if (i > startPos)
            sbIndicesToHTML.append(Util
                .toHTMLString(str.substring(startPos, i)));
          startPos = i + 1;
          depth++;

          // check if next character is a '{' (beginning of index with several
          // chars)
          if (startPos < length && str.charAt(startPos) != '{') {
            sbIndicesToHTML.append(subBegin);
            sbIndicesToHTML.append(Util.toHTMLString(str.substring(startPos,
                startPos + 1)));
            sbIndicesToHTML.append(subEnd);
            depth--;
          } else
            sbIndicesToHTML.append(subBegin);
          i++;
          startPos++;
          break;

        case '}' :
          if (depth > 0) {
            if (i > startPos)
              sbIndicesToHTML.append(Util.toHTMLString(str.substring(startPos,
                  i)));
            sbIndicesToHTML.append(subEnd);
            startPos = i + 1;
            depth--;
          }
          break;
      }

    if (startPos < length)
      sbIndicesToHTML.append(Util.toHTMLString(str.substring(startPos)));
    if (addHTMLtag)
      sbIndicesToHTML.append("</html>");
    return sbIndicesToHTML.toString();
  }

  private final StringBuffer sbNameDescription = new StringBuffer();

  private String strHasIndexLabel;

  private boolean hasIndexLabel = false;

  private final StringBuffer sbNameDescriptionHTML = new StringBuffer();

  private static ArrayList moveObjectsUpdateList;

  private static TreeSet<AlgoElement> tempSet;

  private static TreeSet<AlgoElement> getTempSet() {
    if (tempSet == null)
      tempSet = new TreeSet<AlgoElement>();
    return tempSet;
  }

  /**
   * Translates all GeoElement objects in geos by a vector in real world
   * coordinates or by (xPixel, yPixel) in screen coordinates.
   * 
   * @param endPosition
   *          may be null
   */
  public static boolean moveObjects(ArrayList geos, GeoVector rwTransVec,
      Point2D.Double endPosition) {
    if (moveObjectsUpdateList == null)
      moveObjectsUpdateList = new ArrayList();

    boolean moved = false;
    int size = geos.size();
    moveObjectsUpdateList.clear();
    moveObjectsUpdateList.ensureCapacity(size);

    // only use end position for a single point
    Point2D.Double position = size == 1 ? endPosition : null;

    for (int i = 0; i < size; i++) {
      GeoElement geo = (GeoElement) geos.get(i);

      moved = geo.moveObject(rwTransVec, position, moveObjectsUpdateList)
          || moved;
    }

    // take all independent input objects and build a common updateSet
    // then update all their algos.
    // (don't do updateCascade() on them individually as this could cause
    // multiple updates of the same algorithm)
    updateCascade(moveObjectsUpdateList, getTempSet());

    return moved;
  }

  private ArrayList tempMoveObjectList;

  private int traceColumn1 = -1;

  private double lastTrace1 = Math.random();

  private double lastTrace2 = Math.random();

  private boolean inTree = false;

  /** Creates new GeoElement for given construction */
  public GeoElement(Construction c) {
    super(c);
    // this.geoID = geoCounter++;
    setConstructionDefaults(); // init visual settings

    // new elements become breakpoints if only breakpoints are shown
    // isConsProtBreakpoint = cons.showOnlyBreakpoints();

    // ensure all new objects are in the top layer
    EuclidianView ev = c.getApplication().getEuclidianView();
    if (ev != null)
      layer = ev.getMaxLayerUsed();
  }

  /**
   * add algorithm to dependency list of this GeoElement
   */
  public final void addAlgorithm(AlgoElement algorithm) {
    if (!getAlgorithmList().contains(algorithm))
      algorithmList.add(algorithm);
    addToUpdateSets(algorithm);
  }

  protected void addCellRangeUser() {
    ++cellRangeUsers;
  }

  /**
   * @return
   * 
   *         private static Color getInverseColor(Color c) { float[] hsb =
   *         Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null); hsb[0]
   *         += 0.40; if (hsb[0] > 1) hsb[0]--; hsb[1] = 1; hsb[2] = 0.7f;
   *         return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
   * 
   *         }
   */

  // adds all predecessors of this object to the given set
  // the set is topologically sorted
  // @param onlyIndependent: whether only indpendent geos should be added
  final public void addPredecessorsToSet(TreeSet set, boolean onlyIndependent) {
    if (algoParent == null)
      set.add(this);
    else
      algoParent.addPredecessorsToSet(set, onlyIndependent);
  }

  /**
   * Adds the given algorithm to the dependency list of this GeoElement. The
   * algorithm is NOT added to the updateSet of this GeoElement! I.e. when
   * updateCascade() is called the given algorithm will not be updated.
   */
  final void addToAlgorithmListOnly(AlgoElement algorithm) {
    if (!getAlgorithmList().contains(algorithm))
      algorithmList.add(algorithm);
  }

  /**
   * Adds the given algorithm to the update set this GeoElement. Note: the
   * algorithm is NOT added to the algorithm list, i.e. the dependency graph of
   * the construction.
   */
  final void addToUpdateSetOnly(AlgoElement algorithm) {
    addToUpdateSets(algorithm);
  }

  /**
   * add algorithm to update sets up the construction graph
   */
  private void addToUpdateSets(AlgoElement algorithm) {
    boolean added = getAlgoUpdateSet().add(algorithm);

    if (added)
      // propagate up the graph if we didn't do this before
      if (algoParent != null) {
        GeoElement[] input = algoParent.getInputForUpdateSetPropagation();
        for (GeoElement element : input)
          element.addToUpdateSets(algorithm);
      }
  }

  private void algebraStringsNeedUpdate() {
    strAlgebraDescriptionNeedsUpdate = true;
    strAlgebraDescTextOrHTMLneedsUpdate = true;
    strAlgebraDescriptionHTMLneedsUpdate = true;
    strLabelTextOrHTMLUpdate = true;
    strLaTeXneedsUpdate = true;
  }

  protected void changeAnimationDirection() {
    animationDirection = -animationDirection;
  }

  final public boolean contains(ExpressionValue ev) {
    return ev == this;
  }

  /**
   * every subclass implements it's own copy method this is needed for
   * assignment copies like: a = 2.7 b = a (here copy() is needed)
   * */
  public abstract GeoElement copy();

  /**
   * This method always returns a GeoElement of the SAME CLASS as this
   * GeoElement. Furthermore the resulting geo is in construction cons.
   */
  public GeoElement copyInternal(Construction cons) {
    // default implementation: changed in some subclasses
    GeoElement geoCopy = copy();
    geoCopy.setConstruction(cons);
    return geoCopy;
  }

  public void copyLabel(GeoElement c) {
    label = c.label;
  }

  /**
   * Copies the given segments array. The resulting segments are part of the
   * given construction.
   * 
   * public static GeoSegment [] copySegments(Construction cons, GeoSegment []
   * segments) { GeoSegment [] segmentsCopy = new GeoSegment[segments.length];
   * for (int i=0; i < segments.length; i++) { segmentsCopy[i] = (GeoSegment)
   * segments[i].copyInternal(cons);
   * 
   * } return segmentsCopy; }
   */

  public ExpressionValue deepCopy(Kernel kernel) {
    // default implementation: changed in some subclasses
    return copy();
  }

  final public boolean doHighlighting() {
    return highlighted || selected;
  }

  // removes this GeoElement and all its dependents
  protected void doRemove() {
    // stop animation of this geo
    setAnimating(false);

    // remove this object from List
    if (isIndependent())
      cons.removeFromConstructionList(this);

    // remove all dependent algorithms
    if (algorithmList != null) {
      AlgoElement algo;
      Object[] algos = algorithmList.toArray();
      for (Object algo2 : algos) {
        algo = (AlgoElement) algo2;
        algo.remove(this);
      }
    }

    // remove this object from table
    if (isLabelSet())
      cons.removeLabel(this);

    // remove from selection
    app.removeSelectedGeo(this, false);

    // notify views before we change labelSet
    notifyRemove();

    labelSet = false;
    labelWanted = false;
  }

  private void doRenameLabel(String newLabel) {
    if (newLabel == null || newLabel.equals(label))
      return;

    /*
     * if (!cons.isFreeLabel(newLabel)) { try { throw new
     * Exception("RENAME ERROR: old: " + label + ", new: " + newLabel +
     * ", type: " + this.getTypeString()); } catch (Exception e) {
     * e.printStackTrace(); } } else { Application.debug("RENAME: old: " + label
     * + ", new: " + newLabel + ", type: " + this.getTypeString()); }
     */

    // UPDATE KERNEL
    cons.removeLabel(this); // remove old table entry
    oldLabel = label; // remember old label (for applet to javascript rename)
    label = newLabel; // set new label
    cons.putLabel(this); // add new table entry

    algebraStringsNeedUpdate();
    updateSpreadsheetCoordinates();

    kernel.notifyRename(this); // tell views
    updateCascade();
  }

  private void doSetLabel(String label) {
    // UPDATE KERNEL
    if (!labelSet && isIndependent())
      // add independent object to list of all Construction Elements
      // dependent objects are represented by their parent algorithm
      cons.addToConstructionList(this, true);

    /*
     * if (!cons.isFreeLabel(label)) { try { throw new
     * Exception("SET LABEL: label: " + label + ", type: " +
     * this.getTypeString()); } catch (Exception e) { e.printStackTrace(); } }
     * else { Application.debug("SET LABEL: " + label + ", type: " +
     * this.getTypeString()); }
     */

    this.label = label; // set new label
    labelSet = true;
    labelWanted = false; // got a label, no longer wanted

    cons.putLabel(this); // add new table entry
    algebraStringsNeedUpdate();
    updateSpreadsheetCoordinates();

    notifyAdd();
  }

  public ExpressionValue evaluate() {
    return this;
  }

  /*
   * return black if the color is white, so it can be seen
   */
  public Color getAlgebraColor() {
    Color col = getLabelColor();
    return col.equals(Color.white) ? Color.black : col;
  }

  /**
   * Returns algebraic representation of this GeoElement.
   */
  @Override
  final public String getAlgebraDescription() {
    if (strAlgebraDescriptionNeedsUpdate) {
      if (isDefined())
        strAlgebraDescription = toString();
      else {
        sbAlgebraDesc.setLength(0);
        sbAlgebraDesc.append(label);
        sbAlgebraDesc.append(' ');
        sbAlgebraDesc.append(Plain.undefined);
        strAlgebraDescription = sbAlgebraDesc.toString();
      }

      strAlgebraDescriptionNeedsUpdate = false;
    }

    return strAlgebraDescription;
  }

  final public String getAlgebraDescriptionHTML(boolean addHTMLtag) {
    if (strAlgebraDescriptionHTMLneedsUpdate) {
      strAlgebraDescriptionHTML = indicesToHTML(getAlgebraDescription(), false);

      strAlgebraDescriptionHTMLneedsUpdate = false;
    }

    return strAlgebraDescriptionHTML;
  }

  /**
   * Returns algebraic representation of this GeoElement as Text. If this is not
   * possible (because there are indices in the representation) a HTML string is
   * returned.
   */
  final public String getAlgebraDescriptionTextOrHTML() {
    if (strAlgebraDescTextOrHTMLneedsUpdate) {
      String algDesc = getAlgebraDescription();
      // convertion to html is only needed if indices are found
      if (hasIndexLabel())
        strAlgebraDescTextOrHTML = indicesToHTML(algDesc, true);
      else
        strAlgebraDescTextOrHTML = algDesc;

      strAlgebraDescTextOrHTMLneedsUpdate = false;
    }

    return strAlgebraDescTextOrHTML;
  }

  final public ArrayList<AlgoElement> getAlgorithmList() {
    if (algorithmList == null)
      algorithmList = new ArrayList<AlgoElement>();
    return algorithmList;
  }

  protected AlgorithmSet getAlgoUpdateSet() {
    if (algoUpdateSet == null)
      algoUpdateSet = new AlgorithmSet();

    return algoUpdateSet;
  }

  /**
   * Returns all children (of type GeoElement) that depend on this object.
   */
  public TreeSet getAllChildren() {
    TreeSet set = new TreeSet();
    if (algoUpdateSet != null) {
      Iterator it = algoUpdateSet.getIterator();
      while (it.hasNext()) {
        AlgoElement algo = (AlgoElement) it.next();
        for (GeoElement element : algo.output)
          set.add(element);
      }
    }
    return set;
  }

  /**
   * Returns all independent predecessors (of type GeoElement) that this object
   * depends on. The predecessors are sorted topologically. Note: when this
   * method is called on an independent geo that geo is included in the TreeSet.
   */
  @Override
  public TreeSet getAllIndependentPredecessors() {
    TreeSet set = new TreeSet();
    addPredecessorsToSet(set, true);
    return set;
  }

  /**
   * Returns all predecessors (of type GeoElement) that this object depends on.
   * The predecessors are sorted topologically.
   */
  public TreeSet getAllPredecessors() {
    TreeSet set = new TreeSet();
    addPredecessorsToSet(set, false);
    set.remove(this);
    return set;
  }

  public float getAlphaValue() {
    return alphaValue;
  }

  protected int getAnimationDirection() {
    return animationDirection;
  }
  /**
   * Returns the current animation speed of this slider. Note that the speed can
   * be negative which will change the direction of the animation.
   */
  final public double getAnimationSpeed() {
    if (animationSpeedObj == null)
      initAnimationSpeedObject();

    // get speed
    double speed = animationSpeedObj.getDouble();
    if (Double.isNaN(speed))
      speed = 0;
    else if (speed > MAX_ANIMATION_SPEED)
      speed = MAX_ANIMATION_SPEED;
    else if (speed < -MAX_ANIMATION_SPEED)
      speed = -MAX_ANIMATION_SPEED;

    return speed;
  }

  public GeoElement getAnimationSpeedObject() {
    if (animationSpeedObj == null)
      return null;
    else
      return animationSpeedObj.toGeoElement();
  }

  public double getAnimationStep() {
    return animationIncrement;
  }

  final public int getAnimationType() {
    return animationType;
  }

  final String getAuxiliaryXML() {
    if (auxiliaryObject) {
      StringBuffer sb = new StringBuffer();
      sb.append("\t<auxiliary val=\"");
      sb.append(auxiliaryObject);
      sb.append("\"/>\n");
      return sb.toString();
    } else
      return "";
  }

  /**
   * Returns line type and line thickness as xml string.
   * 
   * @see getXMLtags() of GeoConic, GeoLine and GeoVector
   */
  String getBreakpointXML() {
    if (isConsProtBreakpoint) {
      StringBuffer sb = new StringBuffer();
      sb.append("\t<breakpoint val=\"");
      sb.append(isConsProtBreakpoint);
      sb.append("\"/>\n");
      return sb.toString();
    } else
      return "";
  }

  public String getCaption() {
    if (caption == null)
      return getLabel();

    // for speed, check first for a %
    if (caption.indexOf("%") < 0)
      return caption;

    if (captionSB == null)
      captionSB = new StringBuffer();

    captionSB.setLength(0);

    // replace %v with value and %n with name
    for (int i = 0; i < caption.length(); i++) {
      char ch = caption.charAt(i);
      if (ch == '%' && i < caption.length() - 1) {
        // get number after %
        i++;
        ch = caption.charAt(i);
        switch (ch) {
          case 'v' :
            captionSB.append(toValueString());
            break;
          case 'n' :
            captionSB.append(getLabel());
            break;
          default :
            captionSB.append(ch);
        }
      } else
        captionSB.append(ch);
    }

    return captionSB.toString();
  }

  public String getCaptionXML() {
    StringBuffer sb = new StringBuffer();
    // caption text
    if (caption != null && caption.length() > 0 && !caption.equals(label)) {
      sb.append("\t<caption val=\"");
      sb.append(Util.encodeXML(caption));
      sb.append("\"/>\n");
    }
    return sb.toString();
  }

  public final GeoList getColorFunction() {
    return colFunction;
  }

  @Override
  public String getCommandDescription() {
    if (algoParent == null)
      return "";
    else
      return algoParent.getCommandDescription();
  }

  public String getCommandDescriptionHTML(boolean addHTMLtag) {
    if (algoParent == null)
      return "";
    else
      return indicesToHTML(algoParent.getCommandDescription(), addHTMLtag);
  }

  /**
   * Returns construction index in current construction. For a dependent object
   * the construction index of its parent algorithm is returned.
   */
  @Override
  public int getConstructionIndex() {
    if (algoParent == null)
      return super.getConstructionIndex();
    else
      return algoParent.getConstructionIndex();
  }

  public String getDefaultLabel() {
    char[] chars;

    if (isGeoPoint()) {
      // Michael Borcherds 2008-02-23
      // use Greek upper case for labeling points if lenguage is Greek (el)
      // TODO decide if we want this as an option, or:
      // if (app.languageIs(app.getLocale(), "el")) chars=greekUpperCase; else
      // if (app.languageIs(app.getLocale(), "ar")) chars=arabic; else
      chars = pointLabels;

      GeoPointInterface point = (GeoPointInterface) this;
      if (point.getMode() == Kernel.COORD_COMPLEX)
        chars = complexLabels;

    } else if (isGeoFunction())
      chars = functionLabels;
    else if (isGeoLine())
      chars = lineLabels;
    else if (isGeoConic())
      chars = conicLabels;
    else if (isGeoVector())
      chars = vectorLabels;
    else if (isGeoAngle())
      chars = greekLowerCase;
    else if (isGeoPolygon()) {
      int counter = 0;
      String str;
      do {
        counter++;
        str = Plain.Name_polygon + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else if (isGeoText()) {
      int counter = 0;
      String str;
      do {
        counter++;
        str = Plain.Name_text + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else if (isGeoImage()) {
      int counter = 0;
      String str;
      do {
        counter++;
        str = Plain.Name_picture + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else if (isGeoLocus()) {
      int counter = 0;
      String str;
      do {
        counter++;
        str = Plain.Name_locus + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else if (isGeoJavaScriptButton()) {
      int counter = 0;
      String str;
      do {
        counter++;
        str = Plain.Name_button + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else if (isGeoList()) {
      GeoList list = (GeoList) this;
      int counter = 0;
      String str;
      do {
        counter++;
        str = list.isMatrix() ? Plain.Name_matrix + counter : Plain.Name_list
            + counter;
      } while (!cons.isFreeLabel(str));
      return str;
    } else
      chars = lowerCaseLabels;

    int counter = 0, q, r;
    sbDefaultLabel.setLength(0);
    sbDefaultLabel.append(chars[0]);
    while (!cons.isFreeLabel(sbDefaultLabel.toString())) {
      sbDefaultLabel.setLength(0);
      q = counter / chars.length; // quotient
      r = counter % chars.length; // remainder

      char ch = chars[r];
      sbDefaultLabel.append(ch);

      // arabic letter is two unicode chars
      if (ch == '\u0647')
        sbDefaultLabel.append('\u0640');

      if (q > 0)
        // q as index
        if (q < 10) {
          sbDefaultLabel.append('_');
          sbDefaultLabel.append(q);
        } else {
          sbDefaultLabel.append("_{");
          sbDefaultLabel.append(q);
          sbDefaultLabel.append('}');
        }
      counter++;
    }
    return sbDefaultLabel.toString();
  }

  /**
   * Returns the label for a free geo and the definition description for a
   * dependent geo.
   * 
   * @return
   */
  // public String getLabelOrCommandDescription() {
  // if (algoParent == null)
  // return getLabel();
  // else
  // return algoParent.getCommandDescription();
  // }

  @Override
  public String getDefinitionDescription() {
    if (algoParent == null)
      return "";
    else
      return algoParent.toString();
  }

  public String getDefinitionDescriptionHTML(boolean addHTMLtag) {
    if (algoParent == null)
      return "";
    else
      return indicesToHTML(algoParent.toString(), addHTMLtag);
  }

  /**
   * Returns the definition of this GeoElement for the input field, e.g. A1 = 5,
   * B1 = A1 + 2
   */
  public String getDefinitionForInputBar() {
    // for expressions like "3 = 2 A2 - A1"
    // getAlgebraDescription() returns "3 = 5"
    // so we need to use getCommandDescription() in those cases
    boolean increasePrecision = kernel
        .ensureTemporaryPrintAccuracy(MIN_EDITING_PRINT_PRECISION);

    String inputBarStr = getCommandDescription();
    if (!inputBarStr.equals("")) {

      // check needed for eg f(x) = g(x) + h(x), f(x) = sin(x)
      if (inputBarStr.indexOf('=') < 0)
        inputBarStr = getLabel() + " = " + inputBarStr;
    } else
      inputBarStr = getAlgebraDescription();

    if (increasePrecision)
      kernel.restorePrintAccuracy();

    return inputBarStr;
  }

  // Michael Borcherds 2008-02-23
  public long getDrawingPriority() {

    long typePriority;

    switch (getGeoClassType()) {
      case GEO_CLASS_AXIS :
        typePriority = 10;
        break;
      case GEO_CLASS_IMAGE :
      case GEO_CLASS_BOOLEAN :
        typePriority = 20;
        break;
      case GEO_CLASS_TEXT :
        typePriority = 30;
        break;
      case GEO_CLASS_LIST :
        typePriority = 40;
        break;
      case GEO_CLASS_POLYGON :
        typePriority = 50;
        break;
      case GEO_CLASS_CONIC :
      case GEO_CLASS_CONICPART :
        typePriority = 70;
        break;
      case GEO_CLASS_ANGLE :
      case GEO_CLASS_NUMERIC :
        typePriority = 80;
        break;
      case GEO_CLASS_FUNCTION :
      case GEO_CLASS_FUNCTIONCONDITIONAL :
      case GEO_CLASS_CURVE_CARTESIAN :
      case GEO_CLASS_CURVE_POLAR :
        typePriority = 90;
        break;
      case GEO_CLASS_LINE :
        typePriority = 100;
        break;
      case GEO_CLASS_RAY :
      case GEO_CLASS_SEGMENT :
        typePriority = 110;
        break;
      case GEO_CLASS_VECTOR :
        typePriority = 120;
        break;
      case GEO_CLASS_LOCUS :
        typePriority = 130;
        break;
      case GEO_CLASS_POINT :
        typePriority = 140;
        break;
      default : // shouldn't occur
        typePriority = 150;
    }

    // priority = 100 000 000
    long ret = (long) (typePriority * 10E9 + getConstructionIndex());

    // Application.debug("priority: " + ret + ", " + this);
    return ret;
  }

  // Michael Borcherds 2008-04-02
  public Color getFillColor() {
    if (colFunction == null)
      return fillColor;
    // else return RGBtoColor((int)colFunction.getValue(),alphaValue);
    else
      return getRGBFromList(alphaValue);
  }

  /**
   * String getFormulaString(int, boolean substituteNumbers) substituteNumbers
   * determines (for a function) whether you want "2*x^2" or "a*x^2" returns a
   * string representing the formula of the GeoElement in the following formats:
   * getFormulaString(ExpressionNode.STRING_TYPE_MathPiper) eg Sqrt(x)
   * getFormulaString(ExpressionNode.STRING_TYPE_LATEX) eg \sqrt(x)
   * getFormulaString(ExpressionNode.STRING_TYPE_GEOGEBRA) eg sqrt(x)
   * getFormulaString(ExpressionNode.STRING_TYPE_GEOGEBRA_XML)
   * getFormulaString(ExpressionNode.STRING_TYPE_JASYMCA)
   */
  protected String getFormulaString(int ExpressionNodeType,
      boolean substituteNumbers) {

    /*
     * maybe use this doesn't work on f=Factor[x^2-1] Expand[f] if
     * (ExpressionNodeType == ExpressionNode.STRING_TYPE_MathPiper ||
     * ExpressionNodeType == ExpressionNode.STRING_TYPE_JASYMCA) {
     * 
     * ExpressionValue ev; if (!this.isExpressionNode()) ev = new
     * ExpressionNode(kernel, this); else ev = this;
     * 
     * String ret = ((ExpressionNode) ev).getCASstring(ExpressionNodeType,
     * !substituteNumbers); Application.debug(ret); return ret; }
     */

    int tempCASPrintForm = kernel.getCASPrintForm();
    kernel.setCASPrintForm(ExpressionNodeType);

    String ret = "";
    if (isGeoFunction()) {
      GeoFunction geoFun = (GeoFunction) this;

      if (geoFun.isIndependent())
        ret = geoFun.toValueString();
      else
        ret = substituteNumbers ? geoFun.getFunction().toValueString() : geoFun
            .getFunction().toString();
    } else
      ret = substituteNumbers ? toValueString() : getCommandDescription();

    if (ret.equals("") && !isGeoText())
      // eg Text[ (1,2), false]
      ret = toOutputValueString();

    kernel.setCASPrintForm(tempCASPrintForm);
    return ret;
  }

  /**
   * Returns all free parent points of this GeoElement.
   */
  public ArrayList getFreeInputPoints() {
    if (algoParent == null)
      return null;
    else
      return algoParent.getFreeInputPoints();
  }

  /** Get a free label. Try the suggestedLabel first */
  private String getFreeLabel(String suggestedLabel) {
    if (suggestedLabel != null) {
      if ("x".equals(suggestedLabel) || "y".equals(suggestedLabel))
        return getDefaultLabel();

      if (cons.isFreeLabel(suggestedLabel))
        return suggestedLabel;
      else if (suggestedLabel.length() > 0)
        return getIndexLabel(suggestedLabel);
    }

    // standard case: get default label
    return getDefaultLabel();
  }

  /**
   * Returns the GEO_CLASS_ type integer
   */
  public abstract int getGeoClassType();

  /*
   * over-ridden in GeoList
   */
  public GeoElement getGeoElementForPropertiesDialog() {
    return this;
  }

  /*
   * implementation of abstract methods from ConstructionElement
   */
  @Override
  public GeoElement[] getGeoElements() {
    return myGeoElements;
  }

  /**
   * save object in i2g format Intergeo File Format (Yves Kreis)
   */
  @Override
  public String getI2G(int mode) {
    boolean oldValue = kernel.isTranslateCommandName();
    kernel.setTranslateCommandName(false);

    String type = getXMLtypeString();

    StringBuffer sb = new StringBuffer();

    if (mode == CONSTRAINTS) {
      if (isIndependent() || isPointOnPath()) {
        sb.append("\t\t<free_");
        sb.append(type);
        sb.append(">\n");

        sb.append("\t\t\t<");
        sb.append(type);
        sb.append(" out=\"true\">");
        sb.append(Util.encodeXML(label));
        sb.append("</");
        sb.append(type);
        sb.append(">\n");

        sb.append("\t\t</free_");
        sb.append(type);
        sb.append(">\n");
      }
    } else {
      sb.append("\t\t<");
      sb.append(type);
      sb.append(" id=\"");
      sb.append(Util.encodeXML(label));
      sb.append("\">\n");

      if (mode == ELEMENTS)
        sb.append(getI2Gtags());
      else if (mode == DISPLAY)
        // caption text
        if (caption != null && caption.length() > 0 && !caption.equals(label)) {
          sb.append("\t\t\t<label>");
          sb.append(Util.encodeXML(caption));
          sb.append("</label>\n");
        } else
          return "";

      sb.append("\t\t</");
      sb.append(type);
      sb.append(">\n");
    }

    kernel.setTranslateCommandName(oldValue);
    return sb.toString();
  }

  /**
   * returns all class-specific i2g tags for getI2G Intergeo File Format (Yves
   * Kreis)
   */
  protected String getI2Gtags() {
    return "";
  }

  public String getIndexLabel(String prefix) {
    if (prefix == null)
      return getFreeLabel(null) + "_1";

    // start numbering with indices using suggestedLabel
    // as prefix
    String pref;
    int pos = prefix.indexOf('_');
    if (pos == -1)
      pref = prefix;
    else
      pref = prefix.substring(0, pos);

    sbIndexLabel.setLength(0);
    int n = 0; // index
    do {
      sbIndexLabel.setLength(0);
      sbIndexLabel.append(pref);
      // n as index
      n++;
      if (n < 10) {
        sbIndexLabel.append('_');
        sbIndexLabel.append(n);
      } else {
        sbIndexLabel.append("_{");
        sbIndexLabel.append(n);
        sbIndexLabel.append('}');
      }
    } while (!cons.isFreeLabel(sbIndexLabel.toString()));
    return sbIndexLabel.toString();
  }

  /**
   * Returns label of GeoElement. If the label is null then
   * algoParent.getCommandDescription() or toValueString() is returned.
   */
  public String getLabel() {
    if (!labelSet && !localVarLabelSet) {
      if (algoParent == null)
        return toOutputValueString();
      else
        return algoParent.getCommandDescription();
    } else
      return label;
  }

  // Michael Borcherds 2008-04-01
  public Color getLabelColor() {
    if (colFunction == null)
      return labelColor;
    else
      return getObjectColor();
  }

  /**
   * Returns the label and/or value of this object for showing in EuclidianView.
   * This depends on the current setting of labelMode: LABEL_NAME : only label
   * LABEL_NAME_VALUE : label and value
   */
  public String getLabelDescription() {
    switch (labelMode) {
      case LABEL_NAME_VALUE :
        return getAlgebraDescription();

      case LABEL_VALUE :
        return toDefinedValueString();

      case LABEL_CAPTION : // Michael Borcherds 2008-02-18
        return getCaption();

      default : // case LABEL_NAME:
        // return label;
        // Mathieu Blossier - 2009-06-30
        return getLabel();
    }
  }

  public int getLabelMode() {
    return labelMode;
  }

  /**
   * returns type and label of a GeoElement (for tooltips and error messages)
   */
  final public String getLabelTextOrHTML() {
    if (strLabelTextOrHTMLUpdate)
      if (hasIndexLabel())
        strLabelTextOrHTML = indicesToHTML(getLabel(), true);
      else
        strLabelTextOrHTML = getLabel();

    return strLabelTextOrHTML;
  }

  public double getLastTrace1() {
    return lastTrace1;
  }

  /* *******************************************************
   * GeoElementTable Management Hashtable: String (label) -> GeoElement
   * ******************************************************
   */

  public double getLastTrace2() {
    return lastTrace2;
  }

  final public String getLaTeXdescription() {
    if (strLaTeXneedsUpdate)
      if (isDefined())
        strLaTeX = toLaTeXString(false);
      else
        strLaTeX = getAlgebraDescription();

    return strLaTeX;
  }

  // Michael Borcherds 2008-02-23
  public int getLayer() {
    return layer;
  }

  /**
   * Returns line type and line thickness as xml string.
   * 
   * @see getXMLtags() of GeoConic, GeoLine and GeoVector
   */
  String getLineStyleXML() {
    if (isGeoPoint())
      return "";

    StringBuffer sb = new StringBuffer();
    sb.append("\t<lineStyle");
    sb.append(" thickness=\"");
    sb.append(lineThickness);
    sb.append("\"");
    sb.append(" type=\"");
    sb.append(lineType);
    sb.append("\"");
    sb.append("/>\n");
    return sb.toString();
  }

  /**
   * @return
   */
  public int getLineThickness() {
    return lineThickness;
  }

  /**
   * @return
   */
  public int getLineType() {
    return lineType;
  }

  // private StringBuffer sb;
  //
  // private String removeDollars(String s) {
  // if (sb == null)
  // sb = new StringBuffer();
  // sb.setLength(0);
  //
  // for (int i = 0; i < s.length(); i++) {
  // char c = s.charAt(i);
  // if (c != '$')
  // sb.append(c);
  // }
  //
  // return sb.toString();
  // }

  /**
   * returns Type, label and definition information about this GeoElement (for
   * tooltips and error messages)
   */
  final public String getLongDescription() {
    if (algoParent == null)
      return getNameDescription();
    else {
      sbLongDesc.setLength(0);
      sbLongDesc.append(getNameDescription());
      // add dependency information
      sbLongDesc.append(": ");
      sbLongDesc.append(algoParent.toString());
      return sbLongDesc.toString();
    }
  }

  /**
   * returns Type, label and definition information about this GeoElement as
   * html string. (for tooltips and error messages)
   */
  final public String getLongDescriptionHTML(boolean colored, boolean addHTMLtag) {
    if (algoParent == null || isTextValue())
      return getNameDescriptionHTML(colored, addHTMLtag);
    else {
      sbLongDescHTML.setLength(0);

      String label = getLabel();
      String typeString = translatedTypeString();

      // html string
      if (addHTMLtag)
        sbLongDescHTML.append("<html>");

      boolean reverseOrder = app.isReverseNameDescriptionLanguage();
      if (!reverseOrder) {
        // standard order: "point A"
        sbLongDescHTML.append(typeString);
        sbLongDescHTML.append(' ');
      }

      if (colored) {
        sbLongDescHTML.append("<b><font color=\"#");
        sbLongDescHTML.append(Util.toHexString(getAlgebraColor()));
        sbLongDescHTML.append("\">");
      }
      sbLongDescHTML.append(indicesToHTML(label, false));
      if (colored)
        sbLongDescHTML.append("</font></b>");

      if (reverseOrder) {
        // reverse order: "A point"
        sbLongDescHTML.append(' ');
        sbLongDescHTML.append(typeString);
      }

      // add dependency information
      if (algoParent != null) {
        // Guy Hed, 25.8.2008
        // In order to present the text cottectly in Hebrew and Arabic:
        boolean rightToLeft = app.isRightToLeftReadingOrder();
        if (rightToLeft)
          sbLongDescHTML.append("\u200e\u200f: \u200e");
        else
          sbLongDescHTML.append(": ");
        sbLongDescHTML.append(indicesToHTML(algoParent.toString(), false));
        if (rightToLeft)
          sbLongDescHTML.append("\u200e");
      }
      if (addHTMLtag)
        sbLongDescHTML.append("</html>");
      return sbLongDescHTML.toString();
    }
  }

  /**
   * Returns the largest possible construction index for this object in its
   * construction.
   */
  @Override
  public int getMaxConstructionIndex() {
    if (algoParent == null) {
      // independent object:
      // index must be less than every dependent algorithm's index
      int min = cons.steps();
      int size = algorithmList == null ? 0 : algorithmList.size();
      for (int i = 0; i < size; ++i) {
        int index = algorithmList.get(i).getConstructionIndex();
        if (index < min)
          min = index;
      }
      return min - 1;
    } else
      // dependent object
      return algoParent.getMaxConstructionIndex();
  }

  /**
   * Returns the smallest possible construction index for this object in its
   * construction. For an independent object 0 is returned.
   */
  @Override
  public int getMinConstructionIndex() {
    if (algoParent == null)
      return 0;
    else
      return algoParent.getMinConstructionIndex();
  }

  /**
   * returns type and label of a GeoElement (for tooltips and error messages)
   */
  @Override
  public String getNameDescription() {
    sbNameDescription.setLength(0);

    String label = getLabel();
    String typeString = translatedTypeString();

    if (app.isReverseNameDescriptionLanguage()) {
      // reverse order: "A point"
      sbNameDescription.append(label);
      sbNameDescription.append(' ');
      sbNameDescription.append(typeString);
    } else {
      // standard order: "point A"
      sbNameDescription.append(typeString);
      sbNameDescription.append(' ');
      sbNameDescription.append(label);
    }

    return sbNameDescription.toString();
  }

  /**
   * returns type and label of a GeoElement as html string (for tooltips and
   * error messages)
   */
  public String getNameDescriptionHTML(boolean colored, boolean addHTMLtag) {
    sbNameDescriptionHTML.setLength(0);
    if (addHTMLtag)
      sbNameDescriptionHTML.append("<html>");

    String label = getLabel();
    String typeString = translatedTypeString();

    boolean reverseOrder = app.isReverseNameDescriptionLanguage();
    if (!reverseOrder) {
      // standard order: "point A"
      sbNameDescriptionHTML.append(typeString);
      sbNameDescriptionHTML.append(' ');
    }

    if (colored) {
      sbNameDescriptionHTML.append(" <b><font color=\"#");
      sbNameDescriptionHTML.append(Util.toHexString(getAlgebraColor()));
      sbNameDescriptionHTML.append("\">");
    }
    sbNameDescriptionHTML.append(indicesToHTML(label, false));
    if (colored)
      sbNameDescriptionHTML.append("</font></b>");

    if (reverseOrder) {
      // reverse order: "A point"
      sbNameDescriptionHTML.append(' ');
      sbNameDescriptionHTML.append(typeString);
    }

    if (addHTMLtag)
      sbNameDescriptionHTML.append("</html>");
    return sbNameDescriptionHTML.toString();
  }

  /**
   * returns type and label of a GeoElement (for tooltips and error messages)
   */
  final public String getNameDescriptionTextOrHTML() {
    if (hasIndexLabel())
      return getNameDescriptionHTML(false, true);
    else
      return getNameDescription();
  }

  // Michael Borcherds 2008-04-02
  public Color getObjectColor() {
    Color col = objColor;

    try {
      if (colFunction != null)
        col = getRGBFromList(255);
    } catch (Exception e) {
      removeColorFunction();
    }

    return col;
  }

  final public String getObjectType() {
    return getTypeString();
  }

  /**
   * Returns the label of this object before rename() was called.
   */
  final public String getOldLabel() {
    return oldLabel;
  }

  public Point getOldSpreadsheetCoords() {
    return oldSpreadsheetCoords;
  }

  final public AlgoElement getParentAlgorithm() {
    return algoParent;
  }

  /**
   * Returns all predecessors of this GeoElement that are random numbers and
   * don't have labels.
   */
  public ArrayList<GeoNumeric> getRandomNumberPredecessorsWithoutLabels() {
    if (isIndependent())
      return null;
    else {
      ArrayList<GeoNumeric> randNumbers = null;

      TreeSet pred = getAllPredecessors();
      Iterator it = pred.iterator();
      while (it.hasNext()) {
        GeoElement geo = (GeoElement) it.next();
        if (geo.isGeoNumeric()) {
          GeoNumeric num = (GeoNumeric) geo;
          if (num.isRandomNumber() && !num.isLabelSet()) {
            if (randNumbers == null)
              randNumbers = new ArrayList<GeoNumeric>();
            randNumbers.add(num);
          }
        }
      }

      return randNumbers;
    }
  }

  public String getRawCaption() {
    if (caption == null)
      return getLabel();
    else
      return caption;
  }

  /**
   * Returns definition or value string of this object. Automatically increases
   * decimals to at least 5, e.g. FractionText[4/3] ->
   * FractionText[1.333333333333333]
   */
  public String getRedefineString(boolean useChangeable,
      boolean useOutputValueString) {
    boolean increasePrecision = kernel
        .ensureTemporaryPrintAccuracy(MIN_EDITING_PRINT_PRECISION);

    String ret = null;
    boolean isIndependent = useChangeable ? isChangeable() : isIndependent();
    if (isIndependent)
      ret = useOutputValueString ? toOutputValueString() : toValueString();
    else
      ret = getCommandDescription();

    if (increasePrecision)
      kernel.restorePrintAccuracy();
    return ret;
  }

  // Michael Borcherds 2008-04-02
  private Color getRGBFromList(float alpha2) {
    if (alpha2 > 1f)
      alpha2 = 1f;
    if (alpha2 < 0f)
      alpha2 = 0f;

    int alpha = (int) (alpha2 * 255f);
    return getRGBFromList(alpha);
  }

  // Michael Borcherds 2008-04-02
  private Color getRGBFromList(int alpha) {
    if (alpha > 255)
      alpha = 255;
    else if (alpha < 0)
      alpha = 0;

    // get rgb values from color list
    double redD = 0, greenD = 0, blueD = 0;
    for (int i = 0; i < 3; i++) {
      GeoElement geo = colFunction.get(i);
      if (geo.isDefined()) {
        double val = ((NumberValue) geo).getDouble();
        switch (i) {
          case 0 :
            redD = val;
            break;
          case 1 :
            greenD = val;
            break;
          case 2 :
            blueD = val;
            break;
        }
      }
    }

    // double epsilon = 0.000001; // 1 - floor(1) = 0 but we want 1.

    // make sure the colors are between 0 and 1
    redD = redD / 2 - Math.floor(redD / 2);
    greenD = greenD / 2 - Math.floor(greenD / 2);
    blueD = blueD / 2 - Math.floor(blueD / 2);

    // step function so
    // [0,1] -> [0,1]
    // [1,2] -> [1,0]
    // [2,3] -> [0,1]
    // [3,4] -> [1,0]
    // [4,5] -> [0,1] etc
    if (redD > 0.5)
      redD = 2 * (1 - redD);
    else
      redD = 2 * redD;
    if (greenD > 0.5)
      greenD = 2 * (1 - greenD);
    else
      greenD = 2 * greenD;
    if (blueD > 0.5)
      blueD = 2 * (1 - blueD);
    else
      blueD = 2 * blueD;

    // Application.debug("red"+redD+"green"+greenD+"blue"+blueD);

    return new Color((int) (redD * 255.0), (int) (greenD * 255.0),
        (int) (blueD * 255.0), alpha);

    /*
     * if (red < 0) red = 0; if (red > 255) red = 255;
     * 
     * if (green < 0) green = 0; if (green > 255) green = 255;
     * 
     * if (blue < 0) blue = 0; if (blue > 255) blue = 255;
     * 
     * return new Color(red, green, blue, alpha);
     */
  }

  // Michael Borcherds 2008-04-02
  public Color getSelColor() {
    if (colFunction == null)
      return selColor;
    // else return RGBtoColor((int)colFunction.getValue(),100);
    else
      return getRGBFromList(100);
  }

  public final GeoBoolean getShowObjectCondition() {
    return condShowObject;
  }

  private String getShowObjectConditionXML() {
    if (condShowObject != null) {
      StringBuffer sb = new StringBuffer();
      sb.append("\t<condition showObject=\"");
      sb.append(Util.encodeXML(condShowObject.getLabel()));
      sb.append("\"/>\n");
      return sb.toString();
    }
    return "";
  }

  /**
   * Returns the children of the parent algorithm or null.
   */
  public GeoElement[] getSiblings() {
    if (algoParent != null)
      return algoParent.getOutput();
    else
      return null;
  }

  /**
   * Returns the position of this GeoElement in GeoGebra's spreadsheet view. The
   * x-coordinate of the returned point specifies its column and the
   * y-coordinate specifies its row location. Note that this method may return
   * null if no position was specified so far.
   */
  public Point getSpreadsheetCoords() {
    if (spreadsheetCoords == null)
      updateSpreadsheetCoordinates();
    return spreadsheetCoords;
  }

  /**
   * Returns the spreadsheet reference name of this GeoElement using $ signs for
   * absolute spreadsheet reference names like A$1 or $A$1.
   */
  public String getSpreadsheetLabelWithDollars(boolean col$, boolean row$) {
    String colName = getSpreadsheetColumnName(spreadsheetCoords.x);
    String rowName = Integer.toString(spreadsheetCoords.y + 1);

    StringBuffer sb = new StringBuffer(label.length() + 2);
    if (col$)
      sb.append('$');
    sb.append(colName);
    if (row$)
      sb.append('$');
    sb.append(rowName);
    return sb.toString();
  }

  public String getTraceColumn1() {
    if (app.isUsingLayout() && app.getGuiManager().showSpreadsheetView()
        && traceColumn1 == -1)
      traceColumn1 = app.getGuiManager().getHighestUsedSpreadsheetColumn() + 1;
    return GeoElement.getSpreadsheetColumnName(traceColumn1);
  }
  public String getTraceColumn2() {
    if (app.isUsingLayout() && app.getGuiManager().showSpreadsheetView()
        && traceColumn1 == -1)
      traceColumn1 = app.getGuiManager().getHighestUsedSpreadsheetColumn() + 1;
    return GeoElement.getSpreadsheetColumnName(traceColumn1 + 1);
  }

  public int getTraceRow() {
    if (traceColumn1 == -1)
      return -1;

    if (!(app.isUsingLayout() && app.getGuiManager().showSpreadsheetView()))
      return -1;

    return app.getGuiManager().getSpreadsheetTraceRow(traceColumn1);
  }
  /**
   * Returns type string of GeoElement. Note: this is equal to
   * getClassName().substring(3), but faster
   */
  abstract protected String getTypeString();
  /*
   * { // e.g. GeoPoint -> type = Point //return getClassName().substring(3); }
   */

  /**
   * Returns the value of this GeoElement for the input field, e.g. A1 = 5, B1 =
   * A1 + 2
   */
  public String getValueForInputBar() {
    boolean increasePrecision = kernel
        .ensureTemporaryPrintAccuracy(MIN_EDITING_PRINT_PRECISION);

    // copy into text field
    String ret = toOutputValueString();

    if (increasePrecision)
      kernel.restorePrintAccuracy();

    return ret;
  }

  public HashSet getVariables() {
    HashSet ret = new HashSet();
    ret.add(this);
    return ret;
  }

  /**
   * save object in xml format GeoGebra File Format
   */
  @Override
  public String getXML() {
    boolean oldValue = kernel.isTranslateCommandName();
    kernel.setTranslateCommandName(false);

    String type = getXMLtypeString();

    StringBuffer sb = new StringBuffer();
    sb.append("<element");
    sb.append(" type=\"");
    sb.append(type);
    sb.append("\" label=\"");
    sb.append(Util.encodeXML(label));
    sb.append("\">\n");
    sb.append(getXMLtags());
    sb.append(getCaptionXML());

    sb.append("</element>\n");

    kernel.setTranslateCommandName(oldValue);
    return sb.toString();
  }

  String getXMLanimationTags() {
    // animation step width
    if (isChangeable()) {
      StringBuffer sb = new StringBuffer();
      sb.append("\t<animation");
      sb.append(" step=\"" + animationIncrement + "\"");
      String animSpeed = animationSpeedObj == null ? "1" : animationSpeedObj
          .toGeoElement().getLabel();
      sb.append(" speed=\"" + Util.encodeXML(animSpeed) + "\"");
      sb.append(" type=\"" + animationType + "\"");
      sb.append(" playing=\"");
      sb.append((isAnimating() ? "true" : "false"));
      sb.append("\"");
      sb.append("/>\n");
      return sb.toString();
    }
    return "";
  }

  String getXMLfixedTag() {
    // is object fixed
    if (fixed && isFixable()) {
      StringBuffer sb = new StringBuffer();
      sb.append("\t<fixed val=\"");
      sb.append(fixed);
      sb.append("\"/>\n");
      return sb.toString();
    }
    return "";
  }

  /**
   * returns all class-specific xml tags for getXML GeoGebra File Format
   */
  protected String getXMLtags() {
    StringBuffer sb = new StringBuffer();
    sb.append(getLineStyleXML());
    sb.append(getXMLvisualTags());
    sb.append(getXMLanimationTags());
    sb.append(getXMLfixedTag());
    sb.append(getAuxiliaryXML());
    sb.append(getBreakpointXML());
    return sb.toString();
  }

  /*
   * private void printUpdateSets() { Iterator itList =
   * cons.getAllGeoElementsIterator(); while (itList.hasNext()) { GeoElement geo
   * = (GeoElement) itList.next(); Application.debug(geo.label + ": " +
   * geo.algoUpdateSet.toString()); } }
   */

  /* *******************************************************
   * AlgorithmList Management each GeoElement has a list of dependent algorithms
   * ******************************************************
   */

  /*******************************************************
   * SAVING
   *******************************************************/

  final public String getXMLtypeString() {
    return getClassName().substring(3).toLowerCase(Locale.US);
  }

  /**
   * returns all visual xml tags (like show, objColor, labelOffset, ...)
   */
  String getXMLvisualTags() {
    return getXMLvisualTags(true);
  }

  String getXMLvisualTags(boolean withLabelOffset) {
    StringBuffer sb = new StringBuffer();
    boolean isDrawable = isDrawable();

    // show object and/or label in EuclidianView
    // don't save this for simple dependent numbers (e.g. in spreadsheet)
    if (isDrawable) {
      sb.append("\t<show");
      sb.append(" object=\"");
      sb.append(euclidianVisible);
      sb.append("\"");
      sb.append(" label=\"");
      sb.append(labelVisible);
      sb.append("\"");
      sb.append("/>\n");
    }

    // conditional visibility
    sb.append(getShowObjectConditionXML());

    // if (isDrawable) removed - want to be able to color objects in AlgebraView
    {
      sb.append("\t<objColor");
      sb.append(" r=\"");
      sb.append(objColor.getRed());
      sb.append("\"");
      sb.append(" g=\"");
      sb.append(objColor.getGreen());
      sb.append("\"");
      sb.append(" b=\"");
      sb.append(objColor.getBlue());
      sb.append("\"");
      sb.append(" alpha=\"");
      sb.append(alphaValue);
      sb.append("\"");

      if (colFunction != null) {
        sb.append(" dynamicr=\"");
        sb.append(Util.encodeXML(colFunction.get(0).getLabel()));
        sb.append("\"");
        sb.append(" dynamicg=\"");
        sb.append(Util.encodeXML(colFunction.get(1).getLabel()));
        sb.append("\"");
        sb.append(" dynamicb=\"");
        sb.append(Util.encodeXML(colFunction.get(2).getLabel()));
        sb.append("\"");

      }
      sb.append("/>\n");
    }

    // don't remove layer 0 information
    // we always need it in case an earlier element has higher layer eg 1
    if (isDrawable) {
      sb.append("\t<layer ");
      sb.append("val=\"" + layer + "\"");
      sb.append("/>\n");
    }

    if (withLabelOffset && (labelOffsetX != 0 || labelOffsetY != 0)) {
      sb.append("\t<labelOffset");
      sb.append(" x=\"");
      sb.append(labelOffsetX);
      sb.append("\"");
      sb.append(" y=\"");
      sb.append(labelOffsetY);
      sb.append("\"");
      sb.append("/>\n");
    }

    if (isDrawable()) {
      sb.append("\t<labelMode");
      sb.append(" val=\"");
      sb.append(labelMode);
      sb.append("\"");
      sb.append("/>\n");
    }

    // trace on or off
    if (isTraceable()) {
      Traceable t = (Traceable) this;
      if (t.getTrace())
        sb.append("\t<trace val=\"true\"/>\n");
    }

    // trace to spreadsheet on or off
    if (isGeoPoint()) {
      GeoPointInterface p = (GeoPointInterface) this;
      if (p.getSpreadsheetTrace())
        sb.append("\t<spreadsheetTrace val=\"true\"/>\n");
    }

    // decoration type
    if (decorationType != DECORATION_NONE) {
      sb.append("\t<decoration");
      sb.append(" type=\"");
      sb.append(decorationType);
      sb.append("\"/>\n");
    }

    return sb.toString();
  }

  /**
   * Returns whether this object is parent of other geos.
   */
  public boolean hasChildren() {
    return algorithmList != null && algorithmList.size() > 0;
  }

  /**
   * Returns whether the str contains any indices (i.e. '_' chars).
   */
  final protected boolean hasIndexLabel() {
    if (strHasIndexLabel != label) {
      hasIndexLabel = label == null || label.indexOf('_') > -1;
      strHasIndexLabel = label;
    }

    return hasIndexLabel;
  }

  /**
   * Returns whether this (dependent) GeoElement has input points that can be
   * moved in Euclidian View.
   * 
   * @return
   */
  public boolean hasMoveableInputPoints() {
    // allow only moving of certain object types
    switch (getGeoClassType()) {
      case GEO_CLASS_CONIC :
      case GEO_CLASS_CONICPART :
      case GEO_CLASS_IMAGE :
      case GEO_CLASS_LINE :
      case GEO_CLASS_RAY :
      case GEO_CLASS_SEGMENT :
      case GEO_CLASS_TEXT :
        return hasOnlyFreeInputPoints()
            && containsOnlyMoveableGeos(getFreeInputPoints());

      case GEO_CLASS_POLYGON :
        return containsOnlyMoveableGeos(getFreeInputPoints());

      case GEO_CLASS_VECTOR :
        if (hasOnlyFreeInputPoints()
            && containsOnlyMoveableGeos(getFreeInputPoints())) {
          // check if first free input point is start point of vector
          ArrayList freeInputPoints = getFreeInputPoints();
          if (freeInputPoints.size() > 0) {
            GeoPoint firstInputPoint = (GeoPoint) freeInputPoints.get(0);
            GeoPoint startPoint = ((GeoVector) this).getStartPoint();
            return firstInputPoint == startPoint;
          }
        }
        break;
    }

    return false;
  }

  final private boolean hasOnlyFreeInputPoints() {
    if (algoParent == null)
      return false;
    else
      return algoParent.getFreeInputPoints().size() == algoParent.input.length;
  }

  /**
   * Returns whether this GeoElement has properties that can be edited in a
   * properties dialog.
   */
  public boolean hasProperties() {
    // return isDrawable() || isChangeable();
    return true;
  }

  private void initAnimationSpeedObject() {
    if (animationSpeedObj == null) {
      GeoNumeric num = new GeoNumeric(cons);
      num.setValue(1);
      animationSpeedObj = num;
    }
  }

  public boolean isAbsoluteScreenLocateable() {
    return false;
  }

  public boolean isAlgebraShowable() {
    return showInAlgebraView();
  }

  /**
   * object should be printed in algebra view
   */
  final public boolean isAlgebraVisible() {
    return algebraVisible && showInAlgebraView();
  }

  final public boolean isAlgoElement() {
    return false;
  }

  final boolean isAlgoMacroOutput() {
    return isAlgoMacroOutput;
  }

  public boolean isAngle() {
    return false;
  }

  public boolean isAnimatable() {
    // over ridden by types that implement Animateable
    return false;
  }

  final public boolean isAnimating() {
    return animating;
  }

  final public boolean isAuxiliaryObject() {
    return auxiliaryObject;
  }

  public boolean isBooleanValue() {
    return false;
  }

  /**
   * Returns whether this GeoElement can be changed directly. Note: for points
   * on lines this is different than isIndependent()
   */
  public boolean isChangeable() {
    return !fixed && isIndependent();
  }

  /**
   * Returns whether this object is dependent on geo.
   */
  public boolean isChildOf(GeoElement geo) {
    if (geo == null || isIndependent())
      return false;
    else
      return geo.isParentOf(this);

    // GeoElement [] input = algoParent.getInput();
    // for (int i = 0; i < input.length; i++) {
    // if (geo == input[i])
    // return true;
    // if (input[i].isChildOf(geo))
    // return true;
    // }
    // return false;
  }

  public boolean isColorSet() {
    return isColorSet;
  }

  /**
   * Returns whether this GeoElement is visible in the construction protocol
   */
  final public boolean isConsProtocolBreakpoint() {
    return isConsProtBreakpoint;
  }

  /*
   * implementation of interface ExpressionValue
   */
  public boolean isConstant() {
    return false;
  }

  public abstract boolean isDefined();

  public boolean isDrawable() {
    return true;
  }

  // Michael Borcherds 2008-04-30
  public abstract boolean isEqual(GeoElement Geo);
  final public boolean isEuclidianShowable() {
    return showInEuclidianView();
  }

  /**
   * object should be drawn in euclidian view
   */
  final public boolean isEuclidianVisible() {
    if (!showInEuclidianView())
      return false;

    if (condShowObject == null)
      return euclidianVisible;
    else
      return condShowObject.getBoolean();
  }

  final public boolean isExpressionNode() {
    return false;
  }

  public boolean isFillable() {
    return false;
  }

  public boolean isFixable() {
    return true; // deleting objects with fixed descendents makes them undefined
    // return isIndependent();
  }

  public boolean isFixed() {
    return fixed;
  }

  public boolean isGeoAngle() {
    return false;
  }

  public boolean isGeoBoolean() {
    return false;
  }

  public boolean isGeoConic() {
    return false;
  }

  public boolean isGeoConicPart() {
    return false;
  }

  public boolean isGeoCurveable() {
    return false;
  }

  public boolean isGeoCurveCartesian() {
    return false;
  }

  public boolean isGeoDeriveable() {
    return false;
  }

  final public boolean isGeoElement() {
    return true;
  }
  public boolean isGeoElement3D() {
    return false;
  }

  public boolean isGeoFunction() {
    return false;
  }
  public boolean isGeoFunctionable() {
    return false;
  }

  public boolean isGeoFunctionConditional() {
    return false;
  }
  public boolean isGeoImage() {
    return false;
  }

  public boolean isGeoJavaScriptButton() {
    return false;
  }

  public boolean isGeoLine() {
    return false;
  }

  public boolean isGeoList() {
    return false;
  }

  public boolean isGeoLocus() {
    return false;
  }

  public boolean isGeoNumeric() {
    return false;
  }

  public boolean isGeoPoint() {
    return false;
  }
  public boolean isGeoPolygon() {
    return false;
  }

  public boolean isGeoRay() {
    return false;
  }

  /*
   * final public Image getAlgebraImage(Image tempImage) { Graphics2D g2 =
   * (Graphics2D) g; GraphicsConfiguration gc = app.getGraphicsConfiguration();
   * if (gc != null) { bgImage = gc.createCompatibleImage(width, height); Point
   * p = drawIndexedString(g2, labelDesc, xLabel, yLabel);
   * 
   * setSize(fontSize, p.x, fontSize + p.y); }
   */

  public boolean isGeoSegment() {
    return false;
  }
  public boolean isGeoText() {
    return false;
  }
  public boolean isGeoVector() {
    return false;
  }
  public boolean isIndependent() {
    return algoParent == null;
  }

  final public boolean isInTree() {
    return inTree;
  }
  /**
   * Returns whether this object's label has been set and is valid now. (this is
   * needed for saving: only object's with isLabelSet() == true should be saved)
   */
  final public boolean isLabelSet() {
    return labelSet;
  }

  /**
   * Returns whether the label can be shown in Euclidian view.
   */
  final public boolean isLabelShowable() {
    return isDrawable()
        && !(isTextValue() || isGeoImage() || isGeoList() || isGeoBoolean()
            && !isIndependent());
  }

  /**
   * Returns whether the value (e.g. equation) should be shown as part of the
   * label description
   */
  final public boolean isLabelValueShowable() {
    return !(isGeoLocus() || isGeoBoolean());
  }
  /**
   * Returns whether the label should be shown in Euclidian view.
   */
  public boolean isLabelVisible() {
    return labelVisible;
  }
  public boolean isLeaf() {
    return true;
  }

  public boolean isLimitedPath() {
    return false;
  }
  public boolean isListValue() {
    return false;
  }

  /**
   * Returns whether this GeoElement can be moved in Euclidian View. Note: this
   * is needed for texts
   */
  public boolean isMoveable() {
    return isChangeable();
  }

  public boolean isNumberValue() {
    return false;
  }

  /**
   * Returns whether geo depends on this object.
   */
  public boolean isParentOf(GeoElement geo) {
    if (algoUpdateSet != null) {
      Iterator it = algoUpdateSet.getIterator();
      while (it.hasNext()) {
        AlgoElement algo = (AlgoElement) it.next();
        for (GeoElement element : algo.output)
          if (geo == element) // child found
            return true;
      }
    }

    return false;
  }

  public boolean isPath() {
    return false;
  }

  /**
   * Returns whether this GeoElement is a point on a path.
   */
  public boolean isPointOnPath() {
    return false;
  }

  public boolean isPolynomialInstance() {
    return false;
  }

  /**
   * Returns whether this object may be redefined
   */
  public boolean isRedefineable() {
    return !fixed && app.letRedefine() && !(isTextValue() || isGeoImage())
        && (isChangeable() || // redefine changeable (independent and not fixed)
        !isIndependent()); // redefine dependent object
  }

  public boolean isRegion() {
    return false;
  }

  public boolean isRenameable() {
    // don't allow renaming when this object is used in
    // cell ranges, see AlgoCellRange
    return cellRangeUsers == 0;
  }

  /**
   * Returns whether this GeoElement can be rotated in Euclidian View. Note:
   * this is needed for images
   */
  public boolean isRotateMoveable() {
    return isChangeable() && this instanceof PointRotateable;
  }

  public boolean isSetAlgebraVisible() {
    return algebraVisible;
  }

  public boolean isSetEuclidianVisible() {
    return euclidianVisible;
  }

  /*
   * over-ridden in GeoText
   */
  public boolean isTextCommand() {
    return false;
  }

  public boolean isTextValue() {
    return false;
  }

  public boolean isTraceable() {
    return false;
  }

  /**
   * Returns whether this object's class implements the interface Translateable.
   */
  public boolean isTranslateable() {
    return false;
  }

  public boolean isUseVisualDefaults() {
    return useVisualDefaults;
  }

  final public boolean isVariable() {
    return false;
  }

  public boolean isVectorValue() {
    return false;
  }

  /*
   * NOTE: change in GeoElementWrapper too!
   */

  /**
   * object should be visible in at least one view
   */
  final public boolean isVisible() {
    return isEuclidianVisible() || isAlgebraVisible();
  }

  /**
   * Moves geo by a vector in real world coordinates.
   * 
   * @return whether actual moving occurred
   */
  private boolean moveObject(GeoVector rwTransVec, Point2D.Double endPosition,
      ArrayList updateGeos) {
    boolean movedGeo = false;

    // moveable geo
    if (isMoveable()) {
      // point
      if (isGeoPoint()) {
        GeoPoint point = (GeoPoint) this;
        if (endPosition != null) {
          point.setCoords(endPosition.x, endPosition.y, 1);
          movedGeo = true;
        }

        // translate point
        else {
          double x = point.inhomX + rwTransVec.x;
          double y = point.inhomY + rwTransVec.y;

          // round to decimal fraction, e.g. 2.800000000001 to 2.8
          if (Math.abs(rwTransVec.x) > Kernel.MIN_PRECISION)
            x = kernel.checkDecimalFraction(x);
          if (Math.abs(rwTransVec.y) > Kernel.MIN_PRECISION)
            y = kernel.checkDecimalFraction(y);

          // set translated point coords
          point.setCoords(x, y, 1);
          movedGeo = true;
        }
      }

      // translateable
      else if (isTranslateable()) {
        Translateable trans = (Translateable) this;
        trans.translate(rwTransVec);
        movedGeo = true;
      }

      // absolute position on screen
      else if (isAbsoluteScreenLocateable()) {
        AbsoluteScreenLocateable screenLoc = (AbsoluteScreenLocateable) this;
        if (screenLoc.isAbsoluteScreenLocActive()) {
          int vxPixel = (int) Math.round(kernel.getXscale() * rwTransVec.x);
          int vyPixel = -(int) Math.round(kernel.getYscale() * rwTransVec.y);
          int x = screenLoc.getAbsoluteScreenLocX() + vxPixel;
          int y = screenLoc.getAbsoluteScreenLocY() + vyPixel;
          screenLoc.setAbsoluteScreenLoc(x, y);
          movedGeo = true;
        } else if (isGeoText()) {
          // check for GeoText with unlabeled start point
          GeoText movedGeoText = (GeoText) this;
          if (movedGeoText.hasAbsoluteLocation()) {
            // absolute location: change location
            GeoPoint loc = movedGeoText.getStartPoint();
            loc.translate(rwTransVec);
            movedGeo = true;
          }
        }
      }

      if (movedGeo)
        if (updateGeos != null)
          updateGeos.add(this);
        else
          updateCascade();
    } else // point with changeable parent coordinates
    if (isGeoPoint()) {
      GeoPoint point = (GeoPoint) this;
      if (point.hasChangeableCoordParentNumbers()) {
        // translate x and y coordinates by changing the parent coords
        // accordingly
        ArrayList<GeoNumeric> changeableCoordNumbers = point
            .getCoordParentNumbers();
        GeoNumeric xvar = changeableCoordNumbers.get(0);
        GeoNumeric yvar = changeableCoordNumbers.get(1);

        // polar coords (r; phi)
        if (point.hasPolarParentNumbers()) {
          // radius
          double radius = GeoVec2D.length(endPosition.x, endPosition.y);
          xvar.setValue(radius);

          // angle
          double angle = kernel.convertToAngleValue(Math.atan2(endPosition.y,
              endPosition.x));
          // angle outsid of slider range
          if (yvar.isIntervalMinActive()
              && yvar.isIntervalMaxActive()
              && (angle < yvar.getIntervalMin() || angle > yvar
                  .getIntervalMax())) {
            // use angle value closest to closest border
            double minDiff = Math.abs((angle - yvar.getIntervalMin()));
            if (minDiff > Math.PI)
              minDiff = Kernel.PI_2 - minDiff;
            double maxDiff = Math.abs((angle - yvar.getIntervalMax()));
            if (maxDiff > Math.PI)
              maxDiff = Kernel.PI_2 - maxDiff;

            if (minDiff < maxDiff)
              angle = angle - Kernel.PI_2;
            else
              angle = angle + Kernel.PI_2;
          }
          yvar.setValue(angle);
        }

        // cartesian coords (xvar + constant, yvar + constant)
        else {
          xvar.setValue(xvar.getValue() - point.inhomX + endPosition.x);
          yvar.setValue(yvar.getValue() - point.inhomY + endPosition.y);
        }

        if (updateGeos != null) {
          // add both variables to update list
          updateGeos.add(xvar);
          updateGeos.add(yvar);
        } else {
          // update both variables right now
          if (tempMoveObjectList == null)
            tempMoveObjectList = new ArrayList();
          tempMoveObjectList.add(xvar);
          tempMoveObjectList.add(yvar);
          updateCascade(tempMoveObjectList, getTempSet());
        }

        movedGeo = true;
      }
    }

    return movedGeo;
  }

  final public void notifyAdd() {
    kernel.notifyAdd(this);

    // Application.debug("add " + label);
    // printUpdateSets();
  }

  final public void notifyRemove() {
    kernel.notifyRemove(this);

    // Application.debug("remove " + label);
    // printUpdateSets();
  }

  final protected void notifyUpdate() {
    kernel.notifyUpdate(this);

    // Application.debug("update " + label);
    // printUpdateSets();
  }

  final private void notifyUpdateAuxiliaryObject() {
    kernel.notifyUpdateAuxiliaryObject(this);

    // Application.debug("add " + label);
    // printUpdateSets();
  }

  /**
   * Removes this object and all dependent objects from the Kernel. If this
   * object is not independent, it's parent algorithm is removed too.
   */
  final public void remove() {
    // dependent object: remove parent algorithm
    if (algoParent != null)
      algoParent.remove(this);
    else
      doRemove();
  }

  /**
   * remove algorithm from dependency list of this GeoElement
   */
  final void removeAlgorithm(AlgoElement algorithm) {
    algorithmList.remove(algorithm);
    removeFromUpdateSets(algorithm);
  }

  protected void removeCellRangeUser() {
    if (cellRangeUsers > 0)
      --cellRangeUsers;
  }

  public final void removeColorFunction() {
    // unregister old condition
    if (colFunction != null)
      colFunction.unregisterColorFunctionListener(this);
    // Application.debug("removeColorFunction");
    // if (colFunction == col)
    colFunction = null;
  }

  protected final void removeCondition(GeoBoolean bool) {
    if (condShowObject == bool)
      condShowObject = null;
  }

  /**
   * remove algorithm from update sets up the construction graph
   */
  final protected void removeFromUpdateSets(AlgoElement algorithm) {
    boolean removed = algoUpdateSet != null && algoUpdateSet.remove(algorithm);

    if (removed)
      // propagate up the graph
      if (algoParent != null) {
        GeoElement[] input = algoParent.getInputForUpdateSetPropagation();
        for (GeoElement element : input)
          element.removeFromUpdateSets(algorithm);
      }
  }

  /*
   * if an object has a fixed descendent, we want to set it undefined
   */
  final public void removeOrSetUndefinedIfHasFixedDescendent() {

    // can't delete a fixed object at all
    if (isFixed())
      return;

    boolean hasFixedDescendent = false;

    Set tree = getAllChildren();
    Iterator it = tree.iterator();
    while (it.hasNext() && hasFixedDescendent == false)
      if (((GeoElement) it.next()).isFixed())
        hasFixedDescendent = true;

    if (hasFixedDescendent) {
      // Application.debug("hasFixedDescendent, not deleting");
      setUndefined();
      updateRepaint();
    } else
      remove();

  }

  /*
   * public boolean isGeoPoint3D() { return false; }
   */

  /**
   * renames this GeoElement to newLabel.
   * 
   * @param newLabel
   * @return true if label was changed
   * @throws MyError
   *           : if new label is already in use
   */
  public boolean rename(String newLabel) {
    if (!isRenameable())
      return false;

    if (newLabel == null)
      return false;
    newLabel = newLabel.trim();
    if (newLabel.length() == 0)
      return false;
    String oldLabel = label;

    if (newLabel.equals(oldLabel))
      return false;
    else if (cons.isFreeLabel(newLabel)) {
      setLabel(newLabel); // now we rename
      return true;
    } else {
      String str[] = {"NameUsed", newLabel};
      throw new MyError(app, str);
    }
  }

  public void resetTraceColumns() {
    traceColumn1 = -1;
  }

  public void resolveVariables() {
  }

  /** every subclass implements it's own set method */
  public abstract void set(GeoElement geo);

  /**
   * Also copy advanced settings of this object.
   * 
   * @param geo
   */
  public void setAdvancedVisualStyle(GeoElement geo) {
    setVisualStyle(geo);

    // set layer
    setLayer(geo.getLayer());

    // copy color function
    setColorFunction(geo.getColorFunction());

    // copy ShowObjectCondition, unless it generates a
    // CirclularDefinitionException
    try {
      setShowObjectCondition(geo.getShowObjectCondition());
    } catch (Exception e) {
    }
  }

  public void setAlgebraVisible(boolean visible) {
    algebraVisible = visible;
  }

  void setAlgoMacroOutput(boolean isAlgoMacroOutput) {
    this.isAlgoMacroOutput = isAlgoMacroOutput;
  }

  /**
   * Sets all visual values from given GeoElement. This will also affect
   * tracing, label location and the location of texts for example.
   */
  public void setAllVisualProperties(GeoElement geo, boolean keepAdvanced) {
    if (keepAdvanced)
      setVisualStyle(geo);
    else
      setAdvancedVisualStyle(geo);

    euclidianVisible = geo.euclidianVisible;
    algebraVisible = geo.algebraVisible;
    labelOffsetX = geo.labelOffsetX;
    labelOffsetY = geo.labelOffsetY;
    caption = geo.caption;

    if (isTraceable() && geo.isTraceable())
      ((Traceable) this).setTrace(((Traceable) geo).getTrace());

    // if (isGeoPoint() && geo.isGeoPoint()) {
    if (getGeoClassType() == GeoElement.GEO_CLASS_POINT
        && geo.getGeoClassType() == GeoElement.GEO_CLASS_POINT)
      ((GeoPoint) this).setSpreadsheetTrace(((GeoPoint) geo)
          .getSpreadsheetTrace());

    // copy color function
    if (geo.colFunction != null)
      setColorFunction(geo.colFunction);

    // copy ShowObjectCondition, unless it generates a
    // CirclularDefinitionException
    if (geo.condShowObject != null)
      try {
        setShowObjectCondition(geo.getShowObjectCondition());
      } catch (Exception e) {
      }
  }

  public void setAlphaValue(float alpha) {
    if (fillColor == null || alpha < 0.0f || alpha > 1.0f)
      return;
    alphaValue = alpha;

    float[] rgb = new float[3];
    fillColor.getRGBColorComponents(rgb);
    fillColor = new Color(rgb[0], rgb[1], rgb[2], alpha);
  }

  /**
   * Sets the state of this object to animating on or off. Note that this
   * 
   * @see Animatable interface
   */
  public synchronized void setAnimating(boolean flag) {
    boolean oldValue = animating;
    animating = flag && isAnimatable();

    // tell animation manager
    if (oldValue != animating) {
      AnimationManager am = kernel.getAnimatonManager();
      if (animating)
        am.addAnimatedGeo(this);
      else
        am.removeAnimatedGeo(this);
    }
  }

  public void setAnimationSpeed(double speed) {
    initAnimationSpeedObject();

    GeoElement speedObj = animationSpeedObj.toGeoElement();
    if (speedObj.isGeoNumeric() && speedObj.isIndependent())
      ((GeoNumeric) speedObj).setValue(speed);
  }

  /*
   * ** hightlighting and selecting only for internal purpouses, i.e. this is
   * not saved
   */

  public void setAnimationSpeedObject(NumberValue speed) {
    animationSpeedObj = speed;
  }

  public void setAnimationStep(double s) {
    if (s > 0 && s < 1000)
      animationIncrement = s;
  }

  final public void setAnimationType(int type) {
    switch (type) {
      case ANIMATION_INCREASING :
      case ANIMATION_OSCILLATING :
        animationType = type;
        animationDirection = 1;
        break;

      case ANIMATION_DECREASING :
        animationType = type;
        animationDirection = -1;
        break;
    }
  }

  public void setAuxiliaryObject(boolean flag) {
    if (auxiliaryObject != flag) {
      auxiliaryObject = flag;
      if (labelSet)
        notifyUpdateAuxiliaryObject();
    }
  }

  public boolean setCaption(String caption) {
    if (caption == null || caption.equals(label)) {
      caption = null;
      return false;
    }

    caption = caption.trim();

    if (caption.trim().length() == 0) {
      this.caption = null;
      return true;
    }

    this.caption = caption.trim();
    return true;
  }

  public void setColorFunction(GeoList col)
  // throws CircularDefinitionException
  {
    // Application.debug("setColorFunction"+col.getValue());

    // check for circular definition (not needed)
    // if (this == col || isParentOf(col))
    // throw new CircularDefinitionException();

    // unregister old condition
    if (colFunction != null)
      colFunction.unregisterColorFunctionListener(this);

    // set new condition
    colFunction = col;

    // register new condition
    if (colFunction != null)
      colFunction.registerColorFunctionListener(this);
  }

  public void setConsProtocolBreakpoint(boolean flag) {
    /*
     * // all siblings need to have same breakpoint information GeoElement []
     * siblings = getSiblings(); if (siblings != null) { for (int i=0; i <
     * siblings.length; i++) { siblings[i].isConsProtBreakpoint = flag; } }
     */

    isConsProtBreakpoint = flag;
  }

  public void setConstructionDefaults() {
    if (useVisualDefaults) {
      ConstructionDefaults consDef = cons.getConstructionDefaults();
      if (consDef != null)
        consDef.setDefaultVisualStyles(this, false);
    }
  }

  public void setDecorationType(int type) {
    decorationType = type;
  }

  public void setEuclidianVisible(boolean visible) {
    euclidianVisible = visible;
  }

  public void setFixed(boolean flag) {
    if (!flag)
      fixed = flag;
    else if (isFixable())
      fixed = flag;
  }

  final public void setHighlighted(boolean flag) {
    highlighted = flag;
  }

  final public void setInTree(boolean flag) {
    inTree = flag;
  }

  /**
   * Sets label of a GeoElement and updates Construction list and GeoElement
   * tabel (String label, GeoElement geo) in Kernel. If the old label was null,
   * a new free label is assigned starting with label as a prefix. If newLabel
   * is not already used, this object is renamed to newLabel. Otherwise nothing
   * is done.
   */
  public void setLabel(String newLabel) {
    if (cons.isSuppressLabelsActive())
      return;

    labelWanted = true;

    // had no label: try to set it
    if (!labelSet) {
      // to avoid wasting of labels, new elements must wait
      // until they are shown in one of the views to get a label
      if (isVisible()) {
        // newLabel is used already: rename the using geo
        GeoElement geo = kernel.lookupLabel(newLabel);
        if (geo != null)
          geo.doRenameLabel(getFreeLabel(newLabel));

        // set newLabel for this geo
        doSetLabel(getFreeLabel(newLabel));
      } else
        // remember desired label
        label = newLabel;
    }
    // try to rename
    else if (isRenameable())
      if (cons.isFreeLabel(newLabel))
        doRenameLabel(newLabel);
      else {
        System.out.println("setLabel DID NOT RENAME:");
        if (cons.lookupLabel(newLabel) != null)
          System.out.println(label + " to " + newLabel
              + ", new label is not free: "
              + cons.lookupLabel(newLabel).getLongDescription());
      }
  }

  // Michael Borcherds 2008-04-01
  public void setLabelColor(Color color) {
    labelColor = color;
  }

  public void setLabelMode(int mode) {
    switch (mode) {
      case LABEL_NAME_VALUE :
        labelMode = LABEL_NAME_VALUE;
        break;

      case LABEL_VALUE :
        labelMode = LABEL_VALUE;
        break;

      case LABEL_CAPTION : // Michael Borcherds 2008-02-18
        labelMode = LABEL_CAPTION;
        break;

      default :
        labelMode = LABEL_NAME;
    }
  }

  /**
   * Moves label by updating label offset
   */
  public void setLabelOffset(int x, int y) {
    double len = GeoVec2D.length(x, y);
    if (len > MAX_LABEL_OFFSET) {
      double factor = MAX_LABEL_OFFSET / len;
      x = (int) Math.round(factor * x);
      y = (int) Math.round(factor * y);
    }

    labelOffsetX = x;
    labelOffsetY = y;
  }

  /**
   * sets wheter the object's label should be drawn in an EuclidianView
   * 
   * @param visible
   */
  public void setLabelVisible(boolean visible) {
    labelVisible = visible;
  }

  public void setLastTrace1(double val) {
    lastTrace1 = val;
  }

  public void setLastTrace2(double val) {
    lastTrace2 = val;
  }

  // Michael Borcherds 2008-03-01
  public void setLayer(int layer) {
    if (layer == this.layer
    // layer valid only for Drawable objects
        || !isDrawable())
      return;
    if (layer > EuclidianView.MAX_LAYERS)
      layer = EuclidianView.MAX_LAYERS;
    else if (layer < 0)
      layer = 0;
    EuclidianView ev = app.getEuclidianView();
    if (ev != null)
      ev.changeLayer(this, this.layer, layer);
    this.layer = layer;
  }
  /**
   * @param f
   */
  public void setLineThickness(int th) {
    lineThickness = th;
  }
  /**
   * @param i
   */
  public void setLineType(int i) {
    lineType = i;
  }

  /**
   * Sets label of a GeoElement and updates GeoElement table (label,
   * GeoElement). This method should only be used by MyXMLHandler.
   */
  public void setLoadedLabel(String label) {
    if (labelSet)
      doRenameLabel(label);
    else
      doSetLabel(getFreeLabel(label));
  }

  // /**
  // * Moves geo by a vector in real world coordinates.
  // * @return whether actual moving occurred
  // */
  // final public boolean moveObject(GeoVector rwTransVec, Point2D.Double
  // endPosition) {
  // return moveObject(rwTransVec, endPosition, null);
  // }

  /**
   * Sets label of a local variable object. This method should only be used by
   * Construction.
   */
  public void setLocalVariableLabel(String label) {
    this.label = label;
    localVarLabelSet = true;
  }
  public void setObjColor(Color color) {
    isColorSet = true;

    objColor = color;
    labelColor = color;
    fillColor = color;
    setAlphaValue(alphaValue);

    // selColor = getInverseColor(objColor);
    selColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
  }

  public void setParentAlgorithm(AlgoElement algorithm) {
    algoParent = algorithm;
    if (algorithm != null)
      setConstructionDefaults(); // set colors to dependent colors
  }

  final public void setSelected(boolean flag) {
    selected = flag;
  }

  public void setShowObjectCondition(GeoBoolean cond)
      throws CircularDefinitionException {
    // check for circular definition
    // if (this == cond || isParentOf(cond))
    // I relaxed this to allow (a parallel b) for a and b
    if (this == cond)
      throw new CircularDefinitionException();

    // unregister old condition
    if (condShowObject != null)
      condShowObject.unregisterConditionListener(this);

    // set new condition
    condShowObject = cond;

    // register new condition
    if (condShowObject != null)
      condShowObject.registerConditionListener(this);
  }

  /**
   * Sets the position of this GeoElement in GeoGebra's spreadsheet. The
   * x-coordinate specifies its column and the y-coordinate specifies its row
   * location.
   */
  public void setSpreadsheetCoords(Point spreadsheetCoords) {
    this.spreadsheetCoords = spreadsheetCoords;
  }

  public abstract void setUndefined();

  public void setUseVisualDefaults(boolean useVisualDefaults) {
    this.useVisualDefaults = useVisualDefaults;
  }

  /**
   * Just changes the basic visual styles. If the style of a geo is reset this
   * is required as we don't want to overwrite advanced settings in that case.
   * 
   * @param geo
   */
  public void setVisualStyle(GeoElement geo) {
    // label style
    labelVisible = geo.labelVisible;
    labelMode = geo.labelMode;

    // style of equation, coordinates, ...
    if (getGeoClassType() == geo.getGeoClassType())
      toStringMode = geo.toStringMode;

    // colors
    objColor = geo.objColor;
    selColor = geo.selColor;
    labelColor = geo.labelColor;
    fillColor = geo.fillColor;
    alphaValue = geo.alphaValue;

    // line thickness and line type:
    // note: line thickness in Drawable is calculated as lineThickness / 2.0f
    lineThickness = geo.lineThickness;
    lineType = geo.lineType;
    decorationType = geo.decorationType;

    // set whether it's an auxilliary object
    setAuxiliaryObject(geo.isAuxiliaryObject());

    // if layer is not zero (eg a new object has layer set to
    // ev.getMaxLayerUsed())
    // we don't want to set it
    if (layer == 0)
      setLayer(geo.getLayer());

  }

  /**
   * Sets this object to zero (number = 0, points = (0,0), etc.)
   */
  public void setZero() {

  }
  public abstract boolean showInAlgebraView();
  protected abstract boolean showInEuclidianView();

  public boolean showToolTipText() {
    return isAlgebraVisible();
  }

  /**
   * Returns toValueString() if isDefined() ist true, else the translation of
   * "undefined" is returne
   */
  final public String toDefinedValueString() {
    if (isDefined())
      return toValueString();
    else
      return Plain.undefined;
  }

  public GeoElement toGeoElement() {
    return this;
  }

  public String toLaTeXString(boolean symbolic) {
    if (symbolic)
      return toString();
    else
      return toDefinedValueString();
  }

  /**
   * Returns a value string that is saveable in an XML file. Note: this is
   * needed for texts that need to be quoted in lists and as command arguments.
   */
  public String toOutputValueString() {
    return toValueString();
  }

  public String toString() {
    return label;
  }

  public abstract String toValueString();

  public String translatedTypeString() {
    return app.getPlain1(getTypeString());
  }

  /**
   * updates this object and notifies kernel. Note: no dependent objects are
   * updated.
   * 
   * @see updateRepaint()
   */
  public void update() {
    if (labelWanted && !labelSet)
      // check if this object's label needs to be set
      if (isVisible())
        setLabel(label);

    // texts need updates
    algebraStringsNeedUpdate();

    kernel.notifyUpdate(this);
  }

  /**
   * Updates this object and all dependent ones. Note: no repainting is done
   * afterwards! synchronized for animation
   */
  final public void updateCascade() {
    update();

    // update all algorithms in the algorithm set of this GeoElement
    if (algoUpdateSet != null)
      algoUpdateSet.updateAll();
  }

  final void updateCascadeParentAlgo() {
    if (algoParent != null) {
      algoParent.compute();
      for (GeoElement element : algoParent.output)
        element.updateCascade();
    }
  }

  /**
   * Updates this object and all dependent ones. Notifies kernel to repaint
   * views.
   */
  final public void updateRepaint() {
    updateCascade();
    kernel.notifyRepaint();
  }

  private void updateSpreadsheetCoordinates() {
    if (labelSet && Character.isLetter(label.charAt(0)) // starts with letter
        && Character.isDigit(label.charAt(label.length() - 1))) // ends with
    // digit
    {

      // init old and current spreadsheet coords
      if (spreadsheetCoords == null) {
        oldSpreadsheetCoords = null;
        spreadsheetCoords = new Point();
      } else {
        if (oldSpreadsheetCoords == null)
          oldSpreadsheetCoords = new Point();
        oldSpreadsheetCoords.setLocation(spreadsheetCoords);
      }

      // we need to also support wrapped GeoElements like
      // $A4 that are implemented as dependent geos (using ExpressionNode)
      Matcher matcher = GeoElement.spreadsheetPattern.matcher(getLabel());
      int column = getSpreadsheetColumn(matcher);
      int row = getSpreadsheetRow(matcher);
      if (column >= 0 && row >= 0)
        spreadsheetCoords.setLocation(column, row);
      else
        spreadsheetCoords = null;
    } else {
      oldSpreadsheetCoords = spreadsheetCoords;
      spreadsheetCoords = null;
    }

    // Application.debug("update spread sheet coords: " + this + ", " +
    // spreadsheetCoords + ", old: " + oldSpreadsheetCoords);
  }

}