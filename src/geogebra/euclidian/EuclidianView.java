/* 
 GeoGebra - Dynamic Mathematics for Schools
 Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

 This file is part of GeoGebra.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by 
 the Free Software Foundation.
 
 */

package geogebra.euclidian;

import geogebra.Plain;
import geogebra.euclidian.DrawableList.DrawableIterator;
import geogebra.kernel.AlgoElement;
import geogebra.kernel.AlgoFunctionAreaSums;
import geogebra.kernel.AlgoIntegralDefinite;
import geogebra.kernel.AlgoIntegralFunctions;
import geogebra.kernel.AlgoSlope;
import geogebra.kernel.Construction;
import geogebra.kernel.ConstructionDefaults;
import geogebra.kernel.GeoAngle;
import geogebra.kernel.GeoBoolean;
import geogebra.kernel.GeoConic;
import geogebra.kernel.GeoConicPart;
import geogebra.kernel.GeoCurveCartesian;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoFunction;
import geogebra.kernel.GeoImage;
import geogebra.kernel.GeoJavaScriptButton;
import geogebra.kernel.GeoLine;
import geogebra.kernel.GeoList;
import geogebra.kernel.GeoLocus;
import geogebra.kernel.GeoNumeric;
import geogebra.kernel.GeoPoint;
import geogebra.kernel.GeoPolygon;
import geogebra.kernel.GeoRay;
import geogebra.kernel.GeoSegment;
import geogebra.kernel.GeoSegmentInterface;
import geogebra.kernel.GeoText;
import geogebra.kernel.GeoVector;
import geogebra.kernel.Kernel;
import geogebra.kernel.ParametricCurve;
import geogebra.main.Application;
import geogebra.main.View;
import geogebra.util.FastHashMapKeyless;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * 
 * @author Markus Hohenwarter
 * @version
 */
public class EuclidianView extends JPanel
    implements
      View,
      EuclidianViewInterface,
      Printable,
      EuclidianConstants {

  // changes the scale of the y-Axis continously to reach
  // the given scale ratio yscale / xscale
  protected class MyAxesRatioZoomer implements ActionListener {

    protected Timer timer; // for animation

    protected double factor;

    protected int counter;

    protected double oldScale, newScale, add;

    protected long startTime;

    protected boolean storeUndo;

    public MyAxesRatioZoomer() {
      timer = new Timer(MyZoomer.DELAY, this);
    }

    public synchronized void actionPerformed(ActionEvent e) {
      counter++;
      long time = System.currentTimeMillis() - startTime;
      if (counter == MyZoomer.MAX_STEPS || time > MyZoomer.MAX_TIME)
        // of
        // animation
        stopAnimation();
      else {
        factor = 1.0 + counter * add / oldScale;
        setCoordSystem(xZero, yZero, xscale, oldScale * factor);
      }
    }

    protected void init(double ratio, boolean storeUndo) {
      // this.ratio = ratio;
      this.storeUndo = storeUndo;

      // zoomFactor = ratio / scaleRatio;
      oldScale = yscale;
      newScale = xscale * ratio; // new yscale
    }

    final synchronized boolean isRunning() {
      return timer.isRunning();
    }

    protected synchronized void startAnimation() {
      if (timer == null)
        return;
      // setDrawMode(DRAW_MODE_DIRECT_DRAW);
      add = (newScale - oldScale) / MyZoomer.MAX_STEPS;
      counter = 0;

      startTime = System.currentTimeMillis();
      timer.start();
    }

    protected synchronized void stopAnimation() {
      timer.stop();
      // setDrawMode(DRAW_MODE_BACKGROUND_IMAGE);
      setCoordSystem(xZero, yZero, xscale, newScale);
      if (storeUndo)
        app.storeUndoInfo();
    }
  }

  // used for animated moving of euclidian view to standard origin
  protected class MyMover implements ActionListener {
    protected double dx, dy, add;

    protected int counter;

    protected double ox, oy; // new origin

    protected Timer timer;

    protected long startTime;

    protected boolean storeUndo;

    public MyMover() {
      timer = new Timer(MyZoomer.DELAY, this);
    }

    public synchronized void actionPerformed(ActionEvent e) {
      counter++;
      long time = System.currentTimeMillis() - startTime;
      if (counter == MyZoomer.MAX_STEPS || time > MyZoomer.MAX_TIME)
        // of
        // animation
        stopAnimation();
      else {
        double factor = 1.0 - counter * add;
        setCoordSystem(ox + dx * factor, oy + dy * factor, xscale, yscale);
      }
    }

    protected void init(double ox, double oy, boolean storeUndo) {
      this.ox = ox;
      this.oy = oy;
      this.storeUndo = storeUndo;
    }

    protected synchronized void startAnimation() {
      dx = xZero - ox;
      dy = yZero - oy;
      if (kernel.isZero(dx) && kernel.isZero(dy))
        return;

      // setDrawMode(DRAW_MODE_DIRECT_DRAW);
      add = 1.0 / MyZoomer.MAX_STEPS;
      counter = 0;

      startTime = System.currentTimeMillis();
      timer.start();
    }

    protected synchronized void stopAnimation() {
      timer.stop();
      // setDrawMode(DRAW_MODE_BACKGROUND_IMAGE);
      setCoordSystem(ox, oy, xscale, yscale);
      if (storeUndo)
        app.storeUndoInfo();
    }
  }
  protected class MyZoomer implements ActionListener {
    static final int MAX_STEPS = 15; // frames

    static final int DELAY = 10;

    static final int MAX_TIME = 400; // millis

    protected Timer timer; // for animation

    protected double px, py; // zoom point

    protected double factor;

    protected int counter, steps;

    protected double oldScale, newScale, add, dx, dy;

    protected long startTime;

    protected boolean storeUndo;

    public MyZoomer() {
      timer = new Timer(DELAY, this);
    }

    public synchronized void actionPerformed(ActionEvent e) {
      counter++;
      long time = System.currentTimeMillis() - startTime;
      if (counter == steps || time > MAX_TIME)
        stopAnimation();
      else {
        factor = 1.0 + counter * add / oldScale;
        setCoordSystem(px + dx * factor, py + dy * factor, oldScale * factor,
            oldScale * factor * scaleRatio);
      }
    }

    protected void init(double px, double py, double zoomFactor, int steps,
        boolean storeUndo) {
      this.px = px;
      this.py = py;
      // this.zoomFactor = zoomFactor;
      this.storeUndo = storeUndo;

      oldScale = xscale;
      newScale = xscale * zoomFactor;
      this.steps = Math.min(MAX_STEPS, steps);
    }

    protected synchronized void startAnimation() {
      if (timer == null)
        return;
      // setDrawMode(DRAW_MODE_DIRECT_DRAW);
      add = (newScale - oldScale) / steps;
      dx = xZero - px;
      dy = yZero - py;
      counter = 0;

      startTime = System.currentTimeMillis();
      timer.start();
    }

    protected synchronized void stopAnimation() {
      timer.stop();
      // setDrawMode(DRAW_MODE_BACKGROUND_IMAGE);
      factor = newScale / oldScale;
      setCoordSystem(px + dx * factor, py + dy * factor, newScale, newScale
          * scaleRatio);

      if (storeUndo)
        app.storeUndoInfo();
    }
  }

  protected class MyZoomerRW implements ActionListener {
    static final int MAX_STEPS = 15; // frames

    static final int DELAY = 10;

    static final int MAX_TIME = 400; // millis

    protected Timer timer; // for animation

    protected int counter, steps;

    protected long startTime;

    protected boolean storeUndo;

    protected double x0, x1, y0, y1, xminOld, xmaxOld, yminOld, ymaxOld;

    public MyZoomerRW() {
      timer = new Timer(DELAY, this);
    }

    public synchronized void actionPerformed(ActionEvent e) {
      counter++;
      long time = System.currentTimeMillis() - startTime;
      if (counter == steps || time > MAX_TIME)
        stopAnimation();
      else {
        double i = counter;
        double j = steps - counter;
        setRealWorldCoordSystem((x0 * i + xminOld * j) / steps,
            (x1 * i + xmaxOld * j) / steps, (y0 * i + yminOld * j) / steps, (y1
                * i + ymaxOld * j)
                / steps);
      }
    }

    protected void init(double x0, double x1, double y0, double y1, int steps,
        boolean storeUndo) {
      this.x0 = x0;
      this.x1 = x1;
      this.y0 = y0;
      this.y1 = y1;

      xminOld = xmin;
      xmaxOld = xmax;
      yminOld = ymin;
      ymaxOld = ymax;
      // this.zoomFactor = zoomFactor;
      this.storeUndo = storeUndo;

      this.steps = Math.min(MAX_STEPS, steps);
    }

    protected synchronized void startAnimation() {
      if (timer == null)
        return;
      counter = 0;

      startTime = System.currentTimeMillis();
      timer.start();
    }

    protected synchronized void stopAnimation() {
      timer.stop();
      setRealWorldCoordSystem(x0, x1, y0, y1);

      if (storeUndo)
        app.storeUndoInfo();
    }
  }

  protected static final long serialVersionUID = 1L;
  protected static final int MIN_WIDTH = 50;

  protected static final int MIN_HEIGHT = 50;

  protected static final String PI_STRING = "\u03c0";

  private static final String EXPORT1 = "Export_1"; // Points used to define
  // corners for export (if
  // they exist)

  private static final String EXPORT2 = "Export_2";

  // pixel per centimeter (at 72dpi)
  protected static final double PRINTER_PIXEL_PER_CM = 72.0 / 2.54;

  protected static final double MODE_ZOOM_FACTOR = 1.5;

  protected static final double MOUSE_WHEEL_ZOOM_FACTOR = 1.1;

  protected static final double SCALE_STANDARD = 50;

  // public static final double SCALE_MAX = 10000;
  // public static final double SCALE_MIN = 0.1;
  public static final double XZERO_STANDARD = 215;

  public static final double YZERO_STANDARD = 315;

  protected static final int LINE_TYPE_FULL = 0;

  public static final int LINE_TYPE_DASHED_SHORT = 10;

  public static final int LINE_TYPE_DASHED_LONG = 15;

  public static final int LINE_TYPE_DOTTED = 20;

  public static final int LINE_TYPE_DASHED_DOTTED = 30;

  public static final int AXES_LINE_TYPE_FULL = 0;

  public static final int AXES_LINE_TYPE_ARROW = 1;

  public static final int AXES_LINE_TYPE_FULL_BOLD = 2;

  public static final int AXES_LINE_TYPE_ARROW_BOLD = 3;

  public static final int AXES_TICK_STYLE_MAJOR_MINOR = 0;

  public static final int AXES_TICK_STYLE_MAJOR = 1;

  public static final int AXES_TICK_STYLE_NONE = 2;

  public static final int POINT_STYLE_DOT = 0;

  public static final int POINT_STYLE_CROSS = 1;

  public static final int POINT_STYLE_CIRCLE = 2;

  public static final int RIGHT_ANGLE_STYLE_NONE = 0;

  public static final int RIGHT_ANGLE_STYLE_SQUARE = 1;

  public static final int RIGHT_ANGLE_STYLE_DOT = 2;

  public static final int DEFAULT_POINT_SIZE = 3;

  public static final int DEFAULT_LINE_THICKNESS = 2;

  // ggb3D 2008-10-27 : mode constants moved to EuclidianConstants.java

  public static final int DEFAULT_ANGLE_SIZE = 30;
  public static final int DEFAULT_LINE_TYPE = LINE_TYPE_FULL;
  protected static final float SELECTION_ADD = 2.0f;
  public static final int POINT_CAPTURING_OFF = 0;

  protected static final int POINT_CAPTURING_ON = 1;
  protected static final int POINT_CAPTURING_ON_GRID = 2;
  protected static final int POINT_CAPTURING_AUTOMATIC = 3;

  // Michael Borcherds 2008-04-28
  public static final int GRID_CARTESIAN = 0;
  public static final int GRID_ISOMETRIC = 1;

  private static int SCREEN_BORDER = 10;

  public static final Integer[] getLineTypes() {
    Integer[] ret = {new Integer(LINE_TYPE_FULL),
        new Integer(LINE_TYPE_DASHED_LONG),
        new Integer(LINE_TYPE_DASHED_SHORT), new Integer(LINE_TYPE_DOTTED),
        new Integer(LINE_TYPE_DASHED_DOTTED)};
    return ret;
  }

  public static String getModeText(int mode) {
    switch (mode) {
      case EuclidianView.MODE_SELECTION_LISTENER :
        return "Select";

      case EuclidianView.MODE_MOVE :
        return "Move";

      case EuclidianView.MODE_POINT :
        return "Point";

      case EuclidianView.MODE_POINT_IN_REGION :
        return "PointInRegion";

      case EuclidianView.MODE_JOIN :
        return "Join";

      case EuclidianView.MODE_SEGMENT :
        return "Segment";

      case EuclidianView.MODE_SEGMENT_FIXED :
        return "SegmentFixed";

      case EuclidianView.MODE_RAY :
        return "Ray";

      case EuclidianView.MODE_POLYGON :
        return "Polygon";

      case EuclidianView.MODE_PARALLEL :
        return "Parallel";

      case EuclidianView.MODE_ORTHOGONAL :
        return "Orthogonal";

      case EuclidianView.MODE_INTERSECT :
        return "Intersect";

      case EuclidianView.MODE_LINE_BISECTOR :
        return "LineBisector";

      case EuclidianView.MODE_ANGULAR_BISECTOR :
        return "AngularBisector";

      case EuclidianView.MODE_TANGENTS :
        return "Tangent";

      case EuclidianView.MODE_POLAR_DIAMETER :
        return "PolarDiameter";

      case EuclidianView.MODE_CIRCLE_TWO_POINTS :
        return "Circle2";

      case EuclidianView.MODE_CIRCLE_THREE_POINTS :
        return "Circle3";

      case EuclidianView.MODE_ELLIPSE_THREE_POINTS :
        return "Ellipse3";

      case EuclidianView.MODE_PARABOLA :
        return "Parabola";

      case EuclidianView.MODE_HYPERBOLA_THREE_POINTS :
        return "Hyperbola3";

        // Michael Borcherds 2008-03-13
      case EuclidianView.MODE_COMPASSES :
        return "Compasses";

      case EuclidianView.MODE_CONIC_FIVE_POINTS :
        return "Conic5";

      case EuclidianView.MODE_RELATION :
        return "Relation";

      case EuclidianView.MODE_TRANSLATEVIEW :
        return "TranslateView";

      case EuclidianView.MODE_SHOW_HIDE_OBJECT :
        return "ShowHideObject";

      case EuclidianView.MODE_SHOW_HIDE_LABEL :
        return "ShowHideLabel";

      case EuclidianView.MODE_COPY_VISUAL_STYLE :
        return "CopyVisualStyle";

      case EuclidianView.MODE_DELETE :
        return "Delete";

      case EuclidianView.MODE_VECTOR :
        return "Vector";

      case EuclidianView.MODE_TEXT :
        return "Text";

      case EuclidianView.MODE_IMAGE :
        return "Image";

      case EuclidianView.MODE_MIDPOINT :
        return "Midpoint";

      case EuclidianView.MODE_SEMICIRCLE :
        return "Semicircle";

      case EuclidianView.MODE_CIRCLE_ARC_THREE_POINTS :
        return "CircleArc3";

      case EuclidianView.MODE_CIRCLE_SECTOR_THREE_POINTS :
        return "CircleSector3";

      case EuclidianView.MODE_CIRCUMCIRCLE_ARC_THREE_POINTS :
        return "CircumcircleArc3";

      case EuclidianView.MODE_CIRCUMCIRCLE_SECTOR_THREE_POINTS :
        return "CircumcircleSector3";

      case EuclidianView.MODE_SLIDER :
        return "Slider";

      case EuclidianView.MODE_MIRROR_AT_POINT :
        return "MirrorAtPoint";

      case EuclidianView.MODE_MIRROR_AT_LINE :
        return "MirrorAtLine";

      case EuclidianView.MODE_MIRROR_AT_CIRCLE :
        return "MirrorAtCircle";

      case EuclidianView.MODE_TRANSLATE_BY_VECTOR :
        return "TranslateByVector";

      case EuclidianView.MODE_ROTATE_BY_ANGLE :
        return "RotateByAngle";

      case EuclidianView.MODE_DILATE_FROM_POINT :
        return "DilateFromPoint";

      case EuclidianView.MODE_CIRCLE_POINT_RADIUS :
        return "CirclePointRadius";

      case EuclidianView.MODE_ANGLE :
        return "Angle";

      case EuclidianView.MODE_ANGLE_FIXED :
        return "AngleFixed";

      case EuclidianView.MODE_VECTOR_FROM_POINT :
        return "VectorFromPoint";

      case EuclidianView.MODE_DISTANCE :
        return "Distance";

      case EuclidianView.MODE_MOVE_ROTATE :
        return "MoveRotate";

      case EuclidianView.MODE_ZOOM_IN :
        return "ZoomIn";

      case EuclidianView.MODE_ZOOM_OUT :
        return "ZoomOut";

      case EuclidianView.MODE_LOCUS :
        return "Locus";

      case MODE_AREA :
        return "Area";

      case MODE_SLOPE :
        return "Slope";

      case MODE_REGULAR_POLYGON :
        return "RegularPolygon";

      case MODE_SHOW_HIDE_CHECKBOX :
        return "ShowCheckBox";

      case MODE_JAVASCRIPT_ACTION :
        return "JavaScriptAction";

      case MODE_SCRIPT_ACTION :
        return "ScriptAction";

      case MODE_WHITEBOARD :
        return "Whiteboard";

      case MODE_FITLINE :
        return "FitLine";

      case MODE_RECORD_TO_SPREADSHEET :
        return "RecordToSpreadsheet";

      default :
        return "";
    }
  }

  final protected static boolean usesSelectionRectangleAsInput(int mode) {
    switch (mode) {
      case MODE_FITLINE :
        return true;
      default :
        return false;
    }
  }

  private int gridType = GRID_CARTESIAN;

  // zoom rectangle colors
  protected static final Color colZoomRectangle = new Color(200, 200, 230);

  protected static final Color colZoomRectangleFill = new Color(200, 200, 230,
      50);
  // STROKES
  private static MyBasicStroke standardStroke = new MyBasicStroke(1.0f);

  private static MyBasicStroke selStroke = new MyBasicStroke(
      1.0f + SELECTION_ADD);

  // axes strokes
  private static BasicStroke defAxesStroke = new BasicStroke(1.0f,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

  private static BasicStroke boldAxesStroke = new BasicStroke(2.0f, // changed
      // from 1.8f
      // (same as
      // bold
      // grid)
      // Michael
      // Borcherds
      // 2008-04-12
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

  // axes and grid stroke
  private BasicStroke axesStroke, tickStroke, gridStroke;

  private final Line2D.Double tempLine = new Line2D.Double();

  private static RenderingHints defRenderingHints = new RenderingHints(null);

  {
    defRenderingHints.put(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_SPEED);
    defRenderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    defRenderingHints.put(RenderingHints.KEY_COLOR_RENDERING,
        RenderingHints.VALUE_COLOR_RENDER_SPEED);

    // This ensures fast image drawing. Note that DrawImage changes
    // this hint for scaled and sheared images to improve their quality
    defRenderingHints.put(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
  }

  // FONTS
  public Font fontPoint, fontLine, fontVector, fontConic, fontCoords, fontAxes,
      fontAngle;

  int fontSize;

  // member variables
  protected Application app;
  protected Kernel kernel;

  private final EuclidianController euclidianController;

  // axesNumberingDistances /
  // 2

  AffineTransform coordTransform = new AffineTransform();

  int width, height;

  private final NumberFormat[] axesNumberFormat;
  private final NumberFormat printScaleNF;
  double xmin, xmax, ymin, ymax, invXscale, invYscale, xZero, yZero, xscale,
      yscale, scaleRatio = 1.0; // ratio yscale / xscale

  private double xZeroOld, yZeroOld;

  private final double[] AxesTickInterval = {1, 1}; // for axes =

  // END

  private boolean showGrid = false;

  private boolean antiAliasing = true;

  private boolean showMouseCoords = false;

  private boolean showAxesRatio = false;

  private boolean highlightAnimationButtons = false;

  private int pointCapturingMode; // snap to grid points

  // added by Lo�c BEGIN
  // right angle
  int rightAngleStyle = EuclidianView.RIGHT_ANGLE_STYLE_SQUARE;

  private int pointStyle = POINT_STYLE_DOT;

  private int booleanSize = 13;

  private int mode = MODE_MOVE;

  private final boolean[] showAxes = {true, true};

  private boolean[] showAxesNumbers = {true, true};
  private String[] axesLabels = {null, null};
  private String[] axesUnitLabels = {null, null};

  private final boolean[] piAxisUnit = {false, false};

  private int[] axesTickStyles = {AXES_TICK_STYLE_MAJOR, AXES_TICK_STYLE_MAJOR};

  // for axes labeling with numbers
  private final boolean[] automaticAxesNumberingDistances = {true, true};

  private final double[] axesNumberingDistances = {2, 2};

  // distances between grid lines
  private boolean automaticGridDistance = true;

  // since V3.0 this factor is 1, before it was 0.5
  final public static double DEFAULT_GRID_DIST_FACTOR = 1;

  public static double automaticGridDistanceFactor = DEFAULT_GRID_DIST_FACTOR;

  private double[] gridDistances = {2, 2};
  private int gridLineStyle, axesLineType;
  private boolean gridIsBold = false; // Michael Borcherds 2008-04-11

  // on add: change resetLists()

  // colors: axes, grid, background
  private Color axesColor, gridColor, bgColor;

  private double printingScale;

  // Map (geo, drawable) for GeoElements and Drawables
  private final FastHashMapKeyless DrawableMap = new FastHashMapKeyless(500);

  // temp
  // public static final int DRAW_MODE_DIRECT_DRAW = 0;
  // public static final int DRAW_MODE_BACKGROUND_IMAGE = 1;

  private final DrawableList allDrawableList = new DrawableList();
  // Michael Borcherds 2008-03-01
  public static final int MAX_LAYERS = 9;
  private int MAX_LAYER_USED = 0;
  public DrawableList drawLayers[];

  private final DrawableList bgImageList = new DrawableList();

  private Previewable previewDrawable;

  private Rectangle selectionRectangle;

  // or use volatile image
  // protected int drawMode = DRAW_MODE_BACKGROUND_IMAGE;
  private BufferedImage bgImage;

  private Graphics2D bgGraphics; // g2d of bgImage

  private Image resetImage, playImage, pauseImage;

  private boolean firstPaint = true;

  // temp image
  protected Graphics2D g2Dtemp = new BufferedImage(5, 5,
      BufferedImage.TYPE_INT_RGB).createGraphics();
  // public Graphics2D lastGraphics2D;

  private final StringBuffer sb = new StringBuffer();

  private Cursor defaultCursor;

  // ggb3D 2009-02-05
  private final Hits hits;

  public static final double MAX_SCREEN_COORD_VAL = 1E6;

  static protected MyBasicStroke getDefaultSelectionStroke() {
    return selStroke;
  }

  /*
   * public void detachView() { kernel.detach(this); clearView();
   * //kernel.notifyRemoveAll(this); }
   */

  static protected MyBasicStroke getDefaultStroke() {
    return standardStroke;
  }

  /**
   * Creates a stroke with thickness width, dashed according to line style type.
   */
  public static BasicStroke getStroke(float width, int type) {
    float[] dash;

    switch (type) {
      case EuclidianView.LINE_TYPE_DOTTED :
        dash = new float[2];
        dash[0] = width; // dot
        dash[1] = 3.0f; // space
        break;

      case EuclidianView.LINE_TYPE_DASHED_SHORT :
        dash = new float[2];
        dash[0] = 4.0f + width;
        // short dash
        dash[1] = 4.0f; // space
        break;

      case EuclidianView.LINE_TYPE_DASHED_LONG :
        dash = new float[2];
        dash[0] = 8.0f + width; // long dash
        dash[1] = 8.0f; // space
        break;

      case EuclidianView.LINE_TYPE_DASHED_DOTTED :
        dash = new float[4];
        dash[0] = 8.0f + width; // dash
        dash[1] = 4.0f; // space before dot
        dash[2] = width; // dot
        dash[3] = dash[1]; // space after dot
        break;

      default : // EuclidianView.LINE_TYPE_FULL
        dash = null;
    }

    int endCap = dash != null ? BasicStroke.CAP_BUTT : standardStroke
        .getEndCap();

    return new BasicStroke(width, endCap, standardStroke.getLineJoin(),
        standardStroke.getMiterLimit(), dash, 0.0f);
  }

  private MyZoomerRW zoomerRW;

  private final Hits tempArrayList = new Hits();

  private final ArrayList<GeoElement> foundHits = new ArrayList<GeoElement>();

  private final int TEST_MOVEABLE = 1;

  private final int TEST_ROTATEMOVEABLE = 2;

  private final ArrayList<GeoElement> moveableList = new ArrayList<GeoElement>();

  private final ArrayList<GeoElement> topHitsList = new ArrayList<GeoElement>();

  private MyZoomer zoomer;

  protected MyAxesRatioZoomer axesRatioZoomer;

  private MyMover mover;

  /**
   * Creates EuclidianView
   */
  public EuclidianView(EuclidianController ec, boolean[] showAxes,
      boolean showGrid) {

    // Michael Borcherds 2008-03-01
    drawLayers = new DrawableList[MAX_LAYERS + 1];
    for (int k = 0; k <= MAX_LAYERS; k++)
      drawLayers[k] = new DrawableList();

    euclidianController = ec;
    kernel = ec.getKernel();
    app = ec.getApplication();

    this.showAxes[0] = showAxes[0];
    this.showAxes[1] = showAxes[1];
    this.showGrid = showGrid;

    axesNumberFormat = new NumberFormat[2];
    axesNumberFormat[0] = NumberFormat.getInstance(Locale.ENGLISH);
    axesNumberFormat[1] = NumberFormat.getInstance(Locale.ENGLISH);
    axesNumberFormat[0].setGroupingUsed(false);
    axesNumberFormat[1].setGroupingUsed(false);

    printScaleNF = NumberFormat.getInstance(Locale.ENGLISH);
    printScaleNF.setGroupingUsed(false);
    printScaleNF.setMaximumFractionDigits(5);

    // algebra controller will take care of our key events
    setFocusable(true);

    setLayout(null);
    setMinimumSize(new Dimension(20, 20));
    euclidianController.setView(this);

    attachView();

    // register Listener
    addMouseMotionListener(euclidianController);
    addMouseListener(euclidianController);
    addMouseWheelListener(euclidianController);
    addComponentListener(euclidianController);

    // no repaint
    initView(false);

    updateRightAngleStyle(app.getLocale());

    // ggb3D 2009-02-05
    hits = new Hits();
  }

  /**
   * adds a GeoElement to this view
   */
  public void add(GeoElement geo) {
    // check if there is already a drawable for geo
    Drawable d = getDrawable(geo);
    if (d != null)
      return;

    d = createDrawable(geo);
    if (d != null) {
      addToDrawableLists(d);
      repaint();
    }
  }

  // END
  final void addBackgroundImage(DrawImage img) {
    bgImageList.addUnique(img);
    // drawImageList.remove(img);

    // Michael Borcherds 2008-02-29
    int layer = img.getGeoElement().getLayer();
    drawLayers[layer].remove(img);
  }

  /**
   * adds a GeoElement to this view
   */
  private void addToDrawableLists(Drawable d) {
    if (d == null)
      return;

    GeoElement geo = d.getGeoElement();
    int layer = geo.getLayer();

    switch (geo.getGeoClassType()) {
      case GeoElement.GEO_CLASS_BOOLEAN :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_JAVASCRIPT_BUTTON :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_POINT :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_SEGMENT :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_RAY :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_LINE :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_POLYGON :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_ANGLE :
        if (geo.isIndependent())
          drawLayers[layer].add(d);
        else if (geo.isDrawable())
          drawLayers[layer].add(d);
        else
          d = null;
        break;

      case GeoElement.GEO_CLASS_NUMERIC :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_VECTOR :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_CONICPART :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_CONIC :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_FUNCTION :
      case GeoElement.GEO_CLASS_FUNCTIONCONDITIONAL :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_TEXT :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_IMAGE :
        if (!bgImageList.contains(d))
          drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_LOCUS :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_CURVE_CARTESIAN :
        drawLayers[layer].add(d);
        break;

      case GeoElement.GEO_CLASS_LIST :
        drawLayers[layer].add(d);
        break;
    }

    if (d != null)
      allDrawableList.add(d);
  }

  private void attachView() {
    kernel.notifyAddAll(this);
    kernel.attach(this);
  }

  private void calcPrintingScale() {
    double unitPerCM = PRINTER_PIXEL_PER_CM / xscale;
    int exp = (int) Math.round(Math.log(unitPerCM) / Math.log(10));
    printingScale = Math.pow(10, -exp);
  }

  // Michael Borcherds 2008-02-29
  public void changeLayer(GeoElement geo, int oldlayer, int newlayer) {
    updateMaxLayerUsed(newlayer);
    // Application.debug(drawLayers[oldlayer].size());
    drawLayers[oldlayer].remove((Drawable) DrawableMap.get(geo));
    // Application.debug(drawLayers[oldlayer].size());
    drawLayers[newlayer].add((Drawable) DrawableMap.get(geo));

  }

  final private void clearBackground(Graphics2D g) {
    g.setColor(bgColor);
    g.fillRect(0, 0, width, height);
  }

  public void clearView() {
    removeAll(); // remove hotEqns
    resetLists();
    initView(false);
    updateBackgroundImage(); // clear traces and images
    // resetMode();
  }

  // for use in AlgebraController
  final public void clickedGeo(GeoElement geo, MouseEvent e) {
    if (geo == null)
      return;

    tempArrayList.clear();
    tempArrayList.add(geo);
    boolean changedKernel = euclidianController.processMode(tempArrayList, e);
    if (changedKernel)
      app.storeUndoInfo();
    kernel.notifyRepaint();
  }

  final private boolean containsGeoPoint(ArrayList<GeoElement> hits) {
    if (hits == null)
      return false;

    for (int i = 0; i < hits.size(); i++)
      if (hits.get(i).isGeoPoint())
        return true;
    return false;
  }

  protected BufferedImage createBufferedImage(int width, int height) {
    return createBufferedImage(width, height, false);
  }

  private BufferedImage createBufferedImage(int width, int height,
      boolean transparency) throws OutOfMemoryError {
    // this image might be too big for our memory
    BufferedImage img = null;
    try {
      System.gc();
      img = new BufferedImage(width, height, (transparency
          ? BufferedImage.TYPE_INT_ARGB
          : BufferedImage.TYPE_INT_RGB));
    } catch (OutOfMemoryError e) {
      Application.debug(e.getMessage() + ": BufferedImage.TYPE_INT_"
          + (transparency ? "A" : "") + "RGB");
      try {
        System.gc();
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      } catch (OutOfMemoryError e2) {
        Application.debug(e2.getMessage() + ": BufferedImage.TYPE_3BYTE_BGR");
        System.gc();
        img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
      }
    }
    return img;
  }

  /**
   * adds a GeoElement to this view
   */
  protected Drawable createDrawable(GeoElement geo) {
    Drawable d = null;

    switch (geo.getGeoClassType()) {
      case GeoElement.GEO_CLASS_BOOLEAN :
        d = new DrawBoolean(this, (GeoBoolean) geo);
        break;

      case GeoElement.GEO_CLASS_JAVASCRIPT_BUTTON :
        d = new DrawJavaScriptButton(this, (GeoJavaScriptButton) geo);
        break;

      case GeoElement.GEO_CLASS_POINT :
        d = new DrawPoint(this, (GeoPoint) geo);
        break;

      case GeoElement.GEO_CLASS_SEGMENT :
        d = new DrawSegment(this, (GeoSegment) geo);
        break;

      case GeoElement.GEO_CLASS_RAY :
        d = new DrawRay(this, (GeoRay) geo);
        break;

      case GeoElement.GEO_CLASS_LINE :
        d = new DrawLine(this, (GeoLine) geo);
        break;

      case GeoElement.GEO_CLASS_POLYGON :
        d = new DrawPolygon(this, (GeoPolygon) geo);
        break;

      case GeoElement.GEO_CLASS_ANGLE :
        if (geo.isIndependent())
          // independent number may be shown as slider
          d = new DrawSlider(this, (GeoNumeric) geo);
        else {
          d = new DrawAngle(this, (GeoAngle) geo);
          if (geo.isDrawable())
            if (!geo.isColorSet()) {
              Color col = geo.getConstruction().getConstructionDefaults()
                  .getDefaultGeo(ConstructionDefaults.DEFAULT_ANGLE)
                  .getObjectColor();
              geo.setObjColor(col);
            }
        }
        break;

      case GeoElement.GEO_CLASS_NUMERIC :
        AlgoElement algo = geo.getParentAlgorithm();
        if (algo == null)
          // independent number may be shown as slider
          d = new DrawSlider(this, (GeoNumeric) geo);
        else if (algo instanceof AlgoSlope)
          d = new DrawSlope(this, (GeoNumeric) geo);
        else if (algo instanceof AlgoIntegralDefinite)
          d = new DrawIntegral(this, (GeoNumeric) geo);
        else if (algo instanceof AlgoIntegralFunctions)
          d = new DrawIntegralFunctions(this, (GeoNumeric) geo);
        else if (algo instanceof AlgoFunctionAreaSums)
          d = new DrawUpperLowerSum(this, (GeoNumeric) geo);
        if (d != null)
          if (!geo.isColorSet()) {
            ConstructionDefaults consDef = geo.getConstruction()
                .getConstructionDefaults();
            if (geo.isIndependent()) {
              Color col = consDef.getDefaultGeo(
                  ConstructionDefaults.DEFAULT_NUMBER).getObjectColor();
              geo.setObjColor(col);
            } else {
              Color col = consDef.getDefaultGeo(
                  ConstructionDefaults.DEFAULT_POLYGON).getObjectColor();
              geo.setObjColor(col);
            }
          }
        break;

      case GeoElement.GEO_CLASS_VECTOR :
        d = new DrawVector(this, (GeoVector) geo);
        break;

      case GeoElement.GEO_CLASS_CONICPART :
        d = new DrawConicPart(this, (GeoConicPart) geo);
        break;

      case GeoElement.GEO_CLASS_CONIC :
        d = new DrawConic(this, (GeoConic) geo);
        break;

      case GeoElement.GEO_CLASS_FUNCTION :
      case GeoElement.GEO_CLASS_FUNCTIONCONDITIONAL :
        d = new DrawParametricCurve(this, (ParametricCurve) geo);
        break;

      case GeoElement.GEO_CLASS_TEXT :
        GeoText text = (GeoText) geo;
        d = new DrawText(this, text);
        break;

      case GeoElement.GEO_CLASS_IMAGE :
        d = new DrawImage(this, (GeoImage) geo);
        break;

      case GeoElement.GEO_CLASS_LOCUS :
        d = new DrawLocus(this, (GeoLocus) geo);
        break;

      case GeoElement.GEO_CLASS_CURVE_CARTESIAN :
        d = new DrawParametricCurve(this, (GeoCurveCartesian) geo);
        break;

      case GeoElement.GEO_CLASS_LIST :
        d = new DrawList(this, (GeoList) geo);
        break;
    }

    if (d != null)
      DrawableMap.put(geo, d);

    return d;
  }

  private void createImage(GraphicsConfiguration gc) {
    if (gc != null) {
      bgImage = gc.createCompatibleImage(width, height);
      bgGraphics = bgImage.createGraphics();
      if (antiAliasing)
        setAntialiasing(bgGraphics);
    }
  }

  public Previewable createPreviewLine(ArrayList<Object> selectedPoints) {

    // Application.debug("createPreviewLine");

    return new DrawLine(this, selectedPoints);
  }

  public Previewable createPreviewPolygon(ArrayList<Object> selectedPoints) {
    return new DrawPolygon(this, selectedPoints);
  }

  public Previewable createPreviewRay(ArrayList<Object> selectedPoints) {
    return new DrawRay(this, selectedPoints);
  }

  public Previewable createPreviewSegment(ArrayList<Object> selectedPoints) {
    return new DrawSegment(this, selectedPoints);
  }

  final private void drawAnimationButtons(Graphics2D g2) {
    int x = 6;
    int y = height - 22;

    if (highlightAnimationButtons)
      // draw filled circle to highlight button
      g2.setColor(Color.darkGray);
    else
      g2.setColor(Color.lightGray);

    // draw pause or play button
    g2.drawRect(x - 2, y - 2, 18, 18);
    Image img = kernel.isAnimationRunning() ? getPauseImage() : getPlayImage();
    g2.drawImage(img, x, y, null);
  }

  private final void drawAxes(Graphics2D g2) {
    // for axes ticks
    double yZeroTick = yZero;
    double xZeroTick = xZero;
    double yBig = yZero + 4;
    double xBig = xZero - 4;
    double ySmall1 = yZero + 0;
    double ySmall2 = yZero + 2;
    double xSmall1 = xZero - 0;
    double xSmall2 = xZero - 2;
    int xoffset, yoffset;
    boolean bold = axesLineType == AXES_LINE_TYPE_FULL_BOLD
        || axesLineType == AXES_LINE_TYPE_ARROW_BOLD;
    boolean drawArrows = axesLineType == AXES_LINE_TYPE_ARROW
        || axesLineType == AXES_LINE_TYPE_ARROW_BOLD;

    // AXES_TICK_STYLE_MAJOR_MINOR = 0;
    // AXES_TICK_STYLE_MAJOR = 1;
    // AXES_TICK_STYLE_NONE = 2;
    boolean[] drawMajorTicks = {axesTickStyles[0] <= 1, axesTickStyles[1] <= 1};
    boolean[] drawMinorTicks = {axesTickStyles[0] == 0, axesTickStyles[1] == 0};

    FontRenderContext frc = g2.getFontRenderContext();
    g2.setFont(fontAxes);
    int fontsize = fontAxes.getSize();
    int arrowSize = fontsize / 3;
    g2.setPaint(axesColor);

    if (bold) {
      axesStroke = boldAxesStroke;
      tickStroke = boldAxesStroke;
      ySmall2++;
      xSmall2--;
      arrowSize += 1;
    } else {
      axesStroke = defAxesStroke;
      tickStroke = defAxesStroke;
    }

    // turn antialiasing off
    // Object antiAliasValue = g2
    // .getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    // RenderingHints.VALUE_ANTIALIAS_OFF);

    // X - AXIS
    if (showAxes[0] && ymin < 0 && ymax > 0) {
      if (showGrid) {
        yoffset = fontsize + 4;
        xoffset = 10;
      } else {
        yoffset = fontsize + 4;
        xoffset = 1;
      }

      // label of x axis
      if (axesLabels[0] != null) {
        TextLayout layout = new TextLayout(axesLabels[0], fontLine, frc);
        g2.drawString(axesLabels[0], (int) (width - 10 - layout.getAdvance()),
            (int) (yZero - 4));
      }

      // numbers
      double rw = xmin - xmin % axesNumberingDistances[0];
      double pix = xZero + rw * xscale;
      double axesStep = xscale * axesNumberingDistances[0]; // pixelstep
      double smallTickPix;
      double tickStep = axesStep / 2;
      if (pix < SCREEN_BORDER) {
        // big tick
        if (drawMajorTicks[0]) {
          g2.setStroke(tickStroke);
          tempLine.setLine(pix, yZeroTick, pix, yBig);
          g2.draw(tempLine);
        }
        pix += axesStep;
        rw += axesNumberingDistances[0];
      }
      int maxX = width - SCREEN_BORDER;
      int prevTextEnd = -3;
      for (; pix < width; rw += axesNumberingDistances[0], pix += axesStep) {
        if (pix <= maxX) {
          if (showAxesNumbers[0]) {
            String strNum = kernel.formatPiE(rw, axesNumberFormat[0]);
            boolean zero = strNum.equals("0");

            sb.setLength(0);
            sb.append(strNum);
            if (axesUnitLabels[0] != null && !piAxisUnit[0])
              sb.append(axesUnitLabels[0]);

            TextLayout layout = new TextLayout(sb.toString(), fontAxes, frc);
            int x, y = (int) (yZero + yoffset);
            if (zero && showAxes[1])
              x = (int) (pix + 6);
            else
              x = (int) (pix + xoffset - layout.getAdvance() / 2);

            // make sure we don't print one string on top of the other
            if (x > prevTextEnd + 5) {
              prevTextEnd = (int) (x + layout.getAdvance());
              g2.drawString(sb.toString(), x, y);
            }
          }

          // big tick
          if (drawMajorTicks[0]) {
            g2.setStroke(tickStroke);
            tempLine.setLine(pix, yZeroTick, pix, yBig);
            g2.draw(tempLine);
          }
        } else if (drawMajorTicks[0] && !drawArrows) {
          // draw last tick if there is no arrow
          tempLine.setLine(pix, yZeroTick, pix, yBig);
          g2.draw(tempLine);
        }

        // small tick
        smallTickPix = pix - tickStep;
        if (drawMinorTicks[0]) {
          g2.setStroke(tickStroke);
          tempLine.setLine(smallTickPix, ySmall1, smallTickPix, ySmall2);
          g2.draw(tempLine);
        }
      }
      // last small tick
      smallTickPix = pix - tickStep;
      if (drawMinorTicks[0] && (!drawArrows || smallTickPix <= maxX)) {
        g2.setStroke(tickStroke);
        tempLine.setLine(smallTickPix, ySmall1, smallTickPix, ySmall2);
        g2.draw(tempLine);
      }

      // x-Axis
      g2.setStroke(axesStroke);
      tempLine.setLine(0, yZero, width, yZero);
      g2.draw(tempLine);

      if (drawArrows) {
        // tur antialiasing on
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        // antiAliasValue);

        // draw arrow for x-axis
        tempLine.setLine(width - 1, yZero, width - 1 - arrowSize, yZero
            - arrowSize);
        g2.draw(tempLine);
        tempLine.setLine(width - 1, yZero, width - 1 - arrowSize, yZero
            + arrowSize);
        g2.draw(tempLine);

        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        // RenderingHints.VALUE_ANTIALIAS_OFF);
      }
    }

    // Y-AXIS
    if (showAxes[1] && xmin < 0 && xmax > 0) {
      if (showGrid) {
        xoffset = -2 - fontsize / 4;
        yoffset = -2;
      } else {
        xoffset = -4 - fontsize / 4;
        yoffset = fontsize / 2 - 1;
      }

      // label of y axis
      if (axesLabels[1] != null) {
        TextLayout layout = new TextLayout(axesLabels[1], fontLine, frc);
        g2.drawString(axesLabels[1], (int) (xZero + 5), (int) (5 + layout
            .getAscent()));
      }

      // numbers
      double rw = ymax - ymax % axesNumberingDistances[1];
      double pix = yZero - rw * yscale;
      double axesStep = yscale * axesNumberingDistances[1]; // pixelstep
      double tickStep = axesStep / 2;

      // first small tick
      double smallTickPix = pix - tickStep;
      if (drawMinorTicks[1] && (!drawArrows || smallTickPix > SCREEN_BORDER)) {
        g2.setStroke(tickStroke);
        tempLine.setLine(xSmall1, smallTickPix, xSmall2, smallTickPix);
        g2.draw(tempLine);
      }

      // don't get too near to the top of the screen
      if (pix < SCREEN_BORDER) {
        if (drawMajorTicks[1] && !drawArrows) {
          // draw tick if there is no arrow
          g2.setStroke(tickStroke);
          tempLine.setLine(xBig, pix, xZeroTick, pix);
          g2.draw(tempLine);
        }
        smallTickPix = pix + tickStep;
        if (drawMinorTicks[1] && smallTickPix > SCREEN_BORDER) {
          g2.setStroke(tickStroke);
          tempLine.setLine(xSmall1, smallTickPix, xSmall2, smallTickPix);
          g2.draw(tempLine);
        }
        pix += axesStep;
        rw -= axesNumberingDistances[1];
      }
      int maxY = height - SCREEN_BORDER;
      for (; pix <= height; rw -= axesNumberingDistances[1], pix += axesStep) {
        if (pix <= maxY)
          if (showAxesNumbers[1]) {
            String strNum = kernel.formatPiE(rw, axesNumberFormat[1]);
            boolean zero = strNum.equals("0");

            sb.setLength(0);
            sb.append(strNum);
            if (axesUnitLabels[1] != null && !piAxisUnit[1])
              sb.append(axesUnitLabels[1]);

            TextLayout layout = new TextLayout(sb.toString(), fontAxes, frc);
            int x = (int) (xZero + xoffset - layout.getAdvance());
            int y;
            if (zero && showAxes[0])
              y = (int) (yZero - 2);
            else
              y = (int) (pix + yoffset);
            g2.drawString(sb.toString(), x, y);
          }

        // big tick
        if (drawMajorTicks[1]) {
          g2.setStroke(tickStroke);
          tempLine.setLine(xBig, pix, xZeroTick, pix);
          g2.draw(tempLine);
        }

        smallTickPix = pix + tickStep;
        if (drawMinorTicks[1]) {
          g2.setStroke(tickStroke);
          tempLine.setLine(xSmall1, smallTickPix, xSmall2, smallTickPix);
          g2.draw(tempLine);
        }
      }

      // y-Axis
      tempLine.setLine(xZero, 0, xZero, height);
      g2.draw(tempLine);

      if (drawArrows && xmin < 0 && xmax > 0) {
        // draw arrow for y-axis
        tempLine.setLine(xZero, 0, xZero - arrowSize, arrowSize);
        g2.draw(tempLine);
        tempLine.setLine(xZero, 0, xZero + arrowSize, arrowSize);
        g2.draw(tempLine);
      }
    }

    // if one of the axes is not visible, show upper left and lower right corner
    // coords
    if (xmin > 0 || xmax < 0 || ymin > 0 || ymax < 0) {
      // uper left corner
      sb.setLength(0);
      sb.append('(');
      sb.append(kernel.formatPiE(xmin, axesNumberFormat[0]));
      sb.append(", ");
      sb.append(kernel.formatPiE(ymax, axesNumberFormat[1]));
      sb.append(')');

      int textHeight = 2 + fontAxes.getSize();
      g2.setFont(fontAxes);
      g2.drawString(sb.toString(), 5, textHeight);

      // lower right corner
      sb.setLength(0);
      sb.append('(');
      sb.append(kernel.formatPiE(xmax, axesNumberFormat[0]));
      sb.append(", ");
      sb.append(kernel.formatPiE(ymin, axesNumberFormat[1]));
      sb.append(')');

      TextLayout layout = new TextLayout(sb.toString(), fontAxes, frc);
      layout.draw(g2, (int) (width - 5 - layout.getAdvance()), height - 5);
    }
  }

  final private void drawAxesRatio(Graphics2D g2) {
    Point pos = euclidianController.mouseLoc;
    if (pos == null)
      return;

    g2.setColor(Color.darkGray);
    g2.setFont(fontLine);
    g2.drawString(getXYscaleRatioString(), pos.x + 15, pos.y + 30);
  }

  final private void drawBackground(Graphics2D g, boolean clear) {
    if (clear)
      clearBackground(g);

    setAntialiasing(g);
    if (showGrid)
      drawGrid(g);
    if (showAxes[0] || showAxes[1])
      drawAxes(g);

    if (app.showResetIcon())
      g.drawImage(getResetImage(), width - 18, 2, null);
  }

  private void drawBackgroundWithImages(Graphics2D g) {
    drawBackgroundWithImages(g, false);
  }

  private void drawBackgroundWithImages(Graphics2D g, boolean transparency) {
    if (!transparency)
      clearBackground(g);

    bgImageList.drawAll(g);
    drawBackground(g, false);
  }

  // Michael Borcherds 2008-03-01
  private void drawGeometricObjects(Graphics2D g2) {
    // boolean
    // isSVGExtensions=g2.getClass().getName().endsWith("SVGExtensions");
    int layer;

    for (layer = 0; layer <= MAX_LAYER_USED; layer++)
      // if (isSVGExtensions)
      // ((geogebra.export.SVGExtensions)g2).startGroup("layer "+layer);
      drawLayers[layer].drawAll(g2);
    // if (isSVGExtensions)
    // ((geogebra.export.SVGExtensions)g2).endGroup("layer "+layer);
  }
  /*
   * protected void drawObjects(Graphics2D g2, int layer) { // draw images
   * drawImageList.drawAll(g2);
   * 
   * // draw HotEquations // all in layer 0 currently // layer -1 means draw all
   * if (layer == 0 || layer == -1) paintChildren(g2);
   * 
   * // draw Geometric objects drawGeometricObjects(g2, layer); }
   */

  private final void drawGrid(Graphics2D g2) {
    g2.setColor(gridColor);
    g2.setStroke(gridStroke);

    switch (gridType) {
      case GRID_CARTESIAN :
        // vertical grid lines
        double tickStep = xscale * gridDistances[0];
        double start = xZero % tickStep;
        double pix = start;
        for (int i = 0; pix <= width; i++) {
          // int val = (int) Math.round(i);
          // g2.drawLine(val, 0, val, height);
          tempLine.setLine(pix, 0, pix, height);
          g2.draw(tempLine);
          pix = start + i * tickStep;
        }

        // horizontal grid lines
        tickStep = yscale * gridDistances[1];
        start = yZero % tickStep;
        pix = start;
        for (int j = 0; pix <= height; j++) {
          // int val = (int) Math.round(j);
          // g2.drawLine(0, val, width, val);
          tempLine.setLine(0, pix, width, pix);
          g2.draw(tempLine);
          pix = start + j * tickStep;
        }
        break;

      case GRID_ISOMETRIC :
        double tickStepX = xscale * gridDistances[0] * Math.sqrt(3.0);
        double startX = xZero % tickStepX;
        double startX2 = xZero % (tickStepX / 2);
        double tickStepY = yscale * gridDistances[0];
        double startY = yZero % tickStepY;

        // vertical
        pix = startX2;
        for (int j = 0; pix <= width; j++) {
          tempLine.setLine(pix, 0, pix, height);
          g2.draw(tempLine);
          pix = startX2 + j * tickStepX / 2.0;
        }

        // extra lines needed because it's diagonal
        int extra = (int) (height * xscale / yscale * Math.sqrt(3.0) / tickStepX) + 3;

        // positive gradient
        pix = startX + -(extra + 1) * tickStepX;
        for (int j = -extra; pix <= width; j += 1) {
          tempLine.setLine(pix, startY - tickStepY, pix + (height + tickStepY)
              * Math.sqrt(3) * xscale / yscale, startY - tickStepY + height
              + tickStepY);
          g2.draw(tempLine);
          pix = startX + j * tickStepX;
        }

        // negative gradient
        pix = startX;
        for (int j = 0; pix <= width + (height * xscale / yscale + tickStepY)
            * Math.sqrt(3.0); j += 1)
        // for (int j=0; j<=kk; j+=1)
        {
          tempLine.setLine(pix, startY - tickStepY, pix - (height + tickStepY)
              * Math.sqrt(3) * xscale / yscale, startY - tickStepY + height
              + tickStepY);
          g2.draw(tempLine);
          pix = startX + j * tickStepX;
        }

        break;
    }
  }

  final private void drawMouseCoords(Graphics2D g2) {
    Point pos = euclidianController.mouseLoc;
    if (pos == null)
      return;

    sb.setLength(0);
    sb.append('(');
    sb.append(kernel.format(euclidianController.xRW));
    if (kernel.getCoordStyle() == Kernel.COORD_STYLE_AUSTRIAN)
      sb.append(" | ");
    else
      sb.append(", ");
    sb.append(kernel.format(euclidianController.yRW));
    sb.append(')');

    g2.setColor(Color.darkGray);
    g2.setFont(fontCoords);
    g2.drawString(sb.toString(), pos.x + 15, pos.y + 15);
  }

  // Michael Borcherds 2008-03-01
  private void drawObjects(Graphics2D g2) {

    // TODO layers for HotEquations
    // all in layer 0 currently
    paintChildren(g2); // draws HotEquations and Checkboxes (booleans)

    drawGeometricObjects(g2);

    if (previewDrawable != null)
      previewDrawable.drawPreview(g2);
  }

  // Michael Borcherds 2008-03-01
  public void drawObjectsPre(Graphics2D g2) {

    // TODO layers for HotEquations
    // all in layer 0 currently
    paintChildren(g2); // draws HotEquations and Checkboxes (booleans)

  }

  private void drawZoomRectangle(Graphics2D g2) {
    g2.setColor(colZoomRectangleFill);
    g2.setStroke(boldAxesStroke);
    g2.fill(selectionRectangle);
    g2.setColor(colZoomRectangle);
    g2.draw(selectionRectangle);
  }

  /**
   * Scales construction and draws it to g2d.
   * 
   * @param forEPS
   *          : states if export should be optimized for eps. Note: if this is
   *          set to true, no traces are drawn.
   * 
   */
  public void exportPaint(Graphics2D g2d, double scale) {
    exportPaint(g2d, scale, false);
  }

  private void exportPaint(Graphics2D g2d, double scale, boolean transparency) {

    exportPaintPre(g2d, scale, transparency);
    drawObjects(g2d);
  }

  public void exportPaintPre(Graphics2D g2d, double scale) {
    exportPaintPre(g2d, scale, false);
  }

  private void exportPaintPre(Graphics2D g2d, double scale, boolean transparency) {
    g2d.scale(scale, scale);

    // clipping on selection rectangle
    if (selectionRectangle != null) {
      Rectangle rect = selectionRectangle;
      g2d.setClip(0, 0, rect.width, rect.height);
      g2d.translate(-rect.x, -rect.y);
      // Application.debug(rect.x+" "+rect.y+" "+rect.width+" "+rect.height);
    } else
      // use points Export_1 and Export_2 to define corner
      try {
        // Construction cons = kernel.getConstruction();
        GeoPoint export1 = (GeoPoint) kernel.lookupLabel(EXPORT1);
        GeoPoint export2 = (GeoPoint) kernel.lookupLabel(EXPORT2);
        double[] xy1 = new double[2];
        double[] xy2 = new double[2];
        export1.getInhomCoords(xy1);
        export2.getInhomCoords(xy2);
        double x1 = xy1[0];
        double x2 = xy2[0];
        double y1 = xy1[1];
        double y2 = xy2[1];
        x1 = x1 / invXscale + xZero;
        y1 = yZero - y1 / invYscale;
        x2 = x2 / invXscale + xZero;
        y2 = yZero - y2 / invYscale;
        int x = (int) Math.min(x1, x2);
        int y = (int) Math.min(y1, y2);
        int exportWidth = (int) Math.abs(x1 - x2) + 2;
        int exportHeight = (int) Math.abs(y1 - y2) + 2;

        g2d.setClip(0, 0, exportWidth, exportHeight);
        g2d.translate(-x, -y);
      } catch (Exception e) {
        // or take full euclidian view
        g2d.setClip(0, 0, width, height);
      }

    // DRAWING
    if (isTracing() || hasBackgroundImages()) {
      // draw background image to get the traces
      if (bgImage == null)
        drawBackgroundWithImages(g2d, transparency);
      else
        g2d.drawImage(bgImage, 0, 0, this);
    } else
      // just clear the background if transparency is disabled (clear = draw
      // background color)
      drawBackground(g2d, !transparency);

    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY);

    setAntialiasing(g2d);
  }

  public boolean getAntialiasing() {
    return antiAliasing;
  }

  public Application getApplication() {
    return app;
  }

  // move view:
  /*
   * protected void setDrawMode(int mode) { if (mode != drawMode) { drawMode =
   * mode; if (mode == DRAW_MODE_BACKGROUND_IMAGE) updateBackgroundImage(); } }
   */

  public Color getAxesColor() {
    return axesColor;
  }

  public String[] getAxesLabels() {
    return axesLabels;
  }

  public int getAxesLineStyle() {
    return axesLineType;
  }

  public double[] getAxesNumberingDistances() {
    return axesNumberingDistances;
  }

  public int[] getAxesTickStyles() {
    return axesTickStyles;
  }

  public String[] getAxesUnitLabels() {
    return axesUnitLabels;
  }

  @Override
  public Color getBackground() {
    return bgColor;
  }

  final public Graphics2D getBackgroundGraphics() {
    return bgGraphics;
  }

  final public int getBooleanSize() {
    return booleanSize;
  }

  /**
   * Returns the bounding box of all Drawable objects in this view in screen
   * coordinates.
   */
  @Override
  public Rectangle getBounds() {
    Rectangle result = null;

    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      Rectangle bb = d.getBounds();
      if (bb != null) {
        if (result == null)
          result = new Rectangle(bb); // changed () to (bb) bugfix, otherwise
        // top-left of screen is always included
        // add bounding box of list element
        result.add(bb);
      }
    }

    // Cong Liu
    if (result == null)
      result = new Rectangle(0, 0, 0, 0);
    return result;
  }

  private Cursor getCursorForImage(Image image) {
    if (image == null)
      return null;

    // Query for custom cursor support
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension d = tk.getBestCursorSize(32, 32);
    int colors = tk.getMaximumCursorColors();
    if (!d.equals(new Dimension(0, 0)) && colors != 0)
      // load cursor image
      if (image != null)
        try {
          // Create custom cursor from the image
          Cursor cursor = tk.createCustomCursor(image, new Point(16, 16),
              "custom cursor");
          return cursor;
        } catch (Exception exc) {
          // Catch exceptions so that we don't try to set a null
          // cursor
          Application.debug("Unable to create custom cursor.");
        }
    return null;
  }

  /**
   * Returns the drawable for the given GeoElement.
   */
  final Drawable getDrawable(GeoElement geo) {
    return (Drawable) DrawableMap.get(geo);
  }

  final public Drawable getDrawableFor(GeoElement geo) {
    return (Drawable) DrawableMap.get(geo);
  }

  public EuclidianController getEuclidianController() {
    return euclidianController;
  }

  public int getExportHeight() {
    if (selectionRectangle != null)
      return selectionRectangle.height;

    try {
      GeoPoint export1 = (GeoPoint) kernel.lookupLabel(EXPORT1);
      GeoPoint export2 = (GeoPoint) kernel.lookupLabel(EXPORT2);
      double[] xy1 = new double[2];
      double[] xy2 = new double[2];
      export1.getInhomCoords(xy1);
      export2.getInhomCoords(xy2);
      double y1 = xy1[1];
      double y2 = xy2[1];
      y1 = yZero - y1 / invYscale;
      y2 = yZero - y2 / invYscale;

      return (int) Math.abs(y1 - y2) + 2;
    } catch (Exception e) {
      return getHeight();
    }

  }

  /**
   * Returns image of drawing pad sized according to the given scale factor.
   */
  public BufferedImage getExportImage(double scale) throws OutOfMemoryError {
    return getExportImage(scale, false);
  }

  public BufferedImage getExportImage(double scale, boolean transparency)
      throws OutOfMemoryError {
    int width = (int) Math.floor(getExportWidth() * scale);
    int height = (int) Math.floor(getExportHeight() * scale);
    BufferedImage img = createBufferedImage(width, height, transparency);
    exportPaint(img.createGraphics(), scale, transparency);
    img.flush();
    return img;
  }

  public int getExportWidth() {
    if (selectionRectangle != null)
      return selectionRectangle.width;
    try {
      GeoPoint export1 = (GeoPoint) kernel.lookupLabel(EXPORT1);
      GeoPoint export2 = (GeoPoint) kernel.lookupLabel(EXPORT2);
      double[] xy1 = new double[2];
      double[] xy2 = new double[2];
      export1.getInhomCoords(xy1);
      export2.getInhomCoords(xy2);
      double x1 = xy1[0];
      double x2 = xy2[0];
      x1 = x1 / invXscale + xZero;
      x2 = x2 / invXscale + xZero;

      return (int) Math.abs(x1 - x2) + 2;
    } catch (Exception e) {
      return getWidth();
    }

  }

  public Color getGridColor() {
    return gridColor;
  }

  public double[] getGridDistances() {
    return gridDistances;
  }

  public double getGridDistances(int i) {
    return gridDistances[i];
  }
  // Michael Borcherds 2008-04-11
  public boolean getGridIsBold() {
    return gridIsBold;
  }

  public int getGridLineStyle() {
    return gridLineStyle;
  }
  /**
   * Returns grid type.
   */
  final public int getGridType() {
    return gridType;
  }

  /** get the hits recorded */
  public Hits getHits() {
    return hits;
  }

  final public ArrayList getHits(ArrayList<GeoElement> hits, Class geoclass,
      ArrayList<GeoElement> result) {
    return getHits(hits, geoclass, false, result);
  }

  /**
   * Stores all GeoElements of type geoclass to result list.
   * 
   * @param other
   *          == true: returns array of GeoElements NOT of type geoclass out of
   *          hits.
   */
  final private ArrayList getHits(ArrayList<GeoElement> hits, Class geoclass,
      boolean other, ArrayList<GeoElement> result) {
    if (hits == null)
      return null;

    result.clear();
    for (int i = 0; i < hits.size(); ++i) {
      boolean success = geoclass.isInstance(hits.get(i));
      if (other)
        success = !success;
      if (success)
        result.add(hits.get(i));
    }
    return result.size() == 0 ? null : result;
  }

  /**
   * Returns array of GeoElements whose visual representation is at screen
   * coords (x,y). order: points, vectors, lines, conics
   */
  final private ArrayList<GeoElement> getHits(Point p) {
    return getHits(p, false);
  }

  /**
   * returns array of GeoElements whose visual representation is at screen
   * coords (x,y). order: points, vectors, lines, conics
   */
  final private ArrayList<GeoElement> getHits(Point p, boolean includePolygons) {
    foundHits.clear();

    // count lists, images and Polygons
    int listCount = 0;
    int polyCount = 0;
    int imageCount = 0;

    // get anything but a polygon
    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      if (d.hit(p.x, p.y) || d.hitLabel(p.x, p.y)) {
        GeoElement geo = d.getGeoElement();

        if (geo.isEuclidianVisible()) {
          if (geo.isGeoList())
            listCount++;
          else if (geo.isGeoImage())
            imageCount++;
          else if (geo.isGeoPolygon())
            polyCount++;
          foundHits.add(geo);
        }
      }
    }

    // look for axis
    if (imageCount == 0) {
      if (showAxes[0] && Math.abs(yZero - p.y) < 3)
        foundHits.add(kernel.getXAxis());
      if (showAxes[1] && Math.abs(xZero - p.x) < 3)
        foundHits.add(kernel.getYAxis());
    }

    int size = foundHits.size();
    if (size == 0)
      return null;

    // remove all lists, images and polygons if there are other objects too
    if (size - (listCount + imageCount + polyCount) > 0)
      for (int i = 0; i < foundHits.size(); ++i) {
        GeoElement geo = foundHits.get(i);
        if (geo.isGeoList() || geo.isGeoImage() || !includePolygons
            && geo.isGeoPolygon())
          foundHits.remove(i);
      }

    return foundHits;
  }

  /**
   * returns array of GeoElements of type geoclass whose visual representation
   * is at streen coords (x,y). order: points, vectors, lines, conics
   */
  final public ArrayList getHits(Point p, Class geoclass,
      ArrayList<GeoElement> result) {
    return getHits(getHits(p), geoclass, false, result);
  }

  /**
   * Returns array of GeoElements whose visual representation is inside of the
   * given screen rectangle
   */
  final public ArrayList<GeoElement> getHits(Rectangle rect) {
    foundHits.clear();

    if (rect == null)
      return foundHits;

    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      GeoElement geo = d.getGeoElement();
      if (geo.isEuclidianVisible() && d.isInside(rect))
        foundHits.add(geo);
    }
    return foundHits;
  }

  /**
   * Returns hits that are suitable for new point mode. A polygon is only kept
   * if one of its sides is also in hits.
   */
  final public ArrayList getHitsForNewPointMode(ArrayList hits) {
    if (hits == null)
      return null;

    Iterator it = hits.iterator();
    while (it.hasNext()) {
      GeoElement geo = (GeoElement) it.next();
      if (geo.isGeoPolygon()) {
        boolean sidePresent = false;
        GeoSegmentInterface[] sides = ((GeoPolygon) geo).getSegments();
        for (GeoSegmentInterface side : sides)
          if (hits.contains(side)) {
            sidePresent = true;
            break;
          }

        if (!sidePresent)
          it.remove();
      }
    }

    return hits;
  }

  public double getInvXscale() {
    return invXscale;
  }

  public double getInvYscale() {
    return invYscale;
  }

  public Kernel getKernel() {
    return kernel;
  }

  /**
   * returns GeoElement whose label is at screen coords (x,y).
   */
  final public GeoElement getLabelHit(Point p) {
    if (!app.isLabelDragsEnabled())
      return null;
    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      if (d.hitLabel(p.x, p.y)) {
        GeoElement geo = d.getGeoElement();
        if (geo.isEuclidianVisible())
          return geo;
      }
    }
    return null;
  }

  public int getMaxLayerUsed() {
    return MAX_LAYER_USED;
  }

  final public int getMode() {
    return mode;
  }

  /**
   * returns array of changeable GeoElements out of hits
   */
  final private ArrayList<GeoElement> getMoveableHits(ArrayList<GeoElement> hits) {
    return getMoveables(hits, TEST_MOVEABLE, null);
  }

  /**
   * returns array of independent GeoElements whose visual representation is at
   * streen coords (x,y). order: points, vectors, lines, conics
   */
  final public ArrayList<GeoElement> getMoveableHits(Point p) {
    return getMoveableHits(getHits(p));
  }

  private ArrayList<GeoElement> getMoveables(ArrayList<GeoElement> hits,
      int test, GeoPoint rotCenter) {
    if (hits == null)
      return null;

    GeoElement geo;
    moveableList.clear();
    for (int i = 0; i < hits.size(); ++i) {
      geo = hits.get(i);
      switch (test) {
        case TEST_MOVEABLE :
          // moveable object
          if (geo.isMoveable())
            moveableList.add(geo);
          else if (geo.isGeoPoint()) {
            GeoPoint point = (GeoPoint) geo;
            if (point.hasChangeableCoordParentNumbers())
              moveableList.add(point);
          }
          // not a point, but has moveable input points
          else if (geo.hasMoveableInputPoints())
            moveableList.add(geo);
          break;

        case TEST_ROTATEMOVEABLE :
          // check for circular definition
          if (geo.isRotateMoveable())
            if (rotCenter == null || !geo.isParentOf(rotCenter))
              moveableList.add(geo);

          break;
      }
    }
    if (moveableList.size() == 0)
      return null;
    else
      return moveableList;
  }

  /**
   * returns array of GeoElements NOT of type geoclass out of hits
   */
  final public ArrayList getOtherHits(ArrayList<GeoElement> hits,
      Class geoclass, ArrayList<GeoElement> result) {
    return getHits(hits, geoclass, true, result);
  }

  private Image getPauseImage() {
    if (pauseImage == null)
      pauseImage = app.getPauseImage();
    return pauseImage;
  }

  private Image getPlayImage() {
    if (playImage == null)
      playImage = app.getPlayImage();
    return playImage;
  }

  /**
   * Returns point capturing mode.
   */
  final public int getPointCapturingMode() {
    return pointCapturingMode;
  }

  /**
   * returns array of changeable GeoElements out of hits that implement
   * PointRotateable
   */
  final public ArrayList<GeoElement> getPointRotateableHits(
      ArrayList<GeoElement> hits, GeoPoint rotCenter) {
    return getMoveables(hits, TEST_ROTATEMOVEABLE, rotCenter);
  }

  final public int getPointStyle() {
    return pointStyle;
  }

  final public ArrayList<GeoElement> getPointVectorNumericHits(Point p) {
    foundHits.clear();

    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      if (d.hit(p.x, p.y) || d.hitLabel(p.x, p.y)) {
        GeoElement geo = d.getGeoElement();
        if (geo.isEuclidianVisible())
          if (
          // geo.isGeoNumeric() ||
          geo.isGeoVector() || geo.isGeoPoint())
            foundHits.add(geo);
      }
    }

    return foundHits;
  }

  public Previewable getPreviewDrawable() {
    return previewDrawable;
  }

  public final double getPrintingScale() {
    return printingScale;
  }

  /**
   * Stores all GeoElements of type GeoPoint, GeoVector, GeoNumeric to result
   * list.
   * 
   */
  final protected ArrayList getRecordableHits(ArrayList hits, ArrayList result) {
    if (hits == null)
      return null;

    result.clear();
    for (int i = 0; i < hits.size(); ++i) {
      GeoElement hit = (GeoElement) hits.get(i);
      boolean success = hit.isGeoPoint() || hit.isGeoVector()
          || hit.isGeoNumeric();
      if (success)
        result.add(hits.get(i));
    }
    return result.size() == 0 ? null : result;
  }

  private Image getResetImage() {
    if (resetImage == null)
      resetImage = app.getRefreshViewImage();
    return resetImage;
  }

  final public int getRightAngleStyle() {
    return rightAngleStyle;
  }

  /**
   * Returns the ratio yscale / xscale of this view. The scale is the number of
   * pixels in screen space that represent one unit in user space.
   */
  public double getScaleRatio() {
    return yscale / xscale;
  }

  public int getSelectedHeight() {
    if (selectionRectangle == null)
      return getHeight();
    else
      return selectionRectangle.height;
  }

  public int getSelectedWidth() {
    if (selectionRectangle == null)
      return getWidth();
    else
      return selectionRectangle.width;
  }

  public Rectangle getSelectionRectangle() {
    return selectionRectangle;
  }

  // ggb3D 2009-02-05

  public boolean[] getShowAxesNumbers() {
    return showAxesNumbers;
  }

  public boolean getShowGrid() {
    return showGrid;
  }

  public boolean getShowMouseCoords() {
    return showMouseCoords;
  }

  // ggb3D 2009-02-05 (end)

  public boolean getShowXaxis() {
    return showAxes[0];
  }

  public boolean getShowYaxis() {
    return showAxes[1];
  }

  final protected Graphics2D getTempGraphics2D(Font font) {
    g2Dtemp.setFont(font); // Michael Borcherds 2008-06-11 bugfix for
    // Corner[text,n]
    return g2Dtemp;
  }

  /**
   * if there are GeoPoints in hits, all these points are returned. Otherwise
   * hits is returned.
   * 
   * @see EuclidianController: mousePressed(), mouseMoved()
   */
  final private ArrayList<GeoElement> getTopHits(ArrayList<GeoElement> hits) {
    if (hits == null)
      return null;

    // point in there?
    if (containsGeoPoint(hits)) {
      getHits(hits, GeoPoint.class, false, topHitsList);
      return topHitsList;
    } else
      return hits;
  }

  /**
   * returns array of GeoElements whose visual representation is on top of
   * screen coords of Point p. If there are points at location p only the points
   * are returned. Otherwise all GeoElements are returned.
   * 
   * @see EuclidianController: mousePressed(), mouseMoved()
   */
  final public ArrayList<GeoElement> getTopHits(Point p) {
    return getTopHits(getHits(p));
  }

  public int getViewHeight() {
    return height;
  }

  public int getViewWidth() {
    return width;
  }

  /**
   * @return Returns the xmax.
   */
  public double getXmax() {
    return xmax;
  }

  /**
   * @return Returns the xmin.
   */
  public double getXmin() {
    return xmin;
  }

  /**
   * returns settings in XML format
   */
  public String getXML() {
    StringBuffer sb = new StringBuffer();
    sb.append("<euclidianView>\n");

    if (width > MIN_WIDTH && height > MIN_HEIGHT) {
      sb.append("\t<size ");
      sb.append(" width=\"");
      sb.append(width);
      sb.append("\"");
      sb.append(" height=\"");
      sb.append(height);
      sb.append("\"");
      sb.append("/>\n");
    }

    sb.append("\t<coordSystem");
    sb.append(" xZero=\"");
    sb.append(xZero);
    sb.append("\"");
    sb.append(" yZero=\"");
    sb.append(yZero);
    sb.append("\"");
    sb.append(" scale=\"");
    sb.append(xscale);
    sb.append("\"");
    sb.append(" yscale=\"");
    sb.append(yscale);
    sb.append("\"");
    sb.append("/>\n");

    // NOTE: the attribute "axes" for the visibility state of
    // both axes is no longer needed since V3.0.
    // Now there are special axis tags, see below.
    sb.append("\t<evSettings axes=\"");
    sb.append(showAxes[0] || showAxes[1]);
    sb.append("\" grid=\"");
    sb.append(showGrid);
    sb.append("\" gridIsBold=\""); // 
    sb.append(gridIsBold); // Michael Borcherds 2008-04-11
    sb.append("\" pointCapturing=\"");
    sb.append(pointCapturingMode);
    sb.append("\" rightAngleStyle=\"");
    sb.append(rightAngleStyle);

    sb.append("\" checkboxSize=\"");
    sb.append(booleanSize); // Michael Borcherds 2008-05-12

    sb.append("\" gridType=\"");
    sb.append(getGridType()); // cartesian/isometric

    sb.append("\"/>\n");

    // background color
    sb.append("\t<bgColor r=\"");
    sb.append(bgColor.getRed());
    sb.append("\" g=\"");
    sb.append(bgColor.getGreen());
    sb.append("\" b=\"");
    sb.append(bgColor.getBlue());
    sb.append("\"/>\n");

    // axes color
    sb.append("\t<axesColor r=\"");
    sb.append(axesColor.getRed());
    sb.append("\" g=\"");
    sb.append(axesColor.getGreen());
    sb.append("\" b=\"");
    sb.append(axesColor.getBlue());
    sb.append("\"/>\n");

    // grid color
    sb.append("\t<gridColor r=\"");
    sb.append(gridColor.getRed());
    sb.append("\" g=\"");
    sb.append(gridColor.getGreen());
    sb.append("\" b=\"");
    sb.append(gridColor.getBlue());
    sb.append("\"/>\n");

    // axes line style
    sb.append("\t<lineStyle axes=\"");
    sb.append(axesLineType);
    sb.append("\" grid=\"");
    sb.append(gridLineStyle);
    sb.append("\"/>\n");

    // axis settings
    for (int i = 0; i < 2; i++) {
      sb.append("\t<axis id=\"");
      sb.append(i);
      sb.append("\" show=\"");
      sb.append(showAxes[i]);
      sb.append("\" label=\"");
      sb.append(axesLabels[i] == null ? "" : axesLabels[i]);
      sb.append("\" unitLabel=\"");
      sb.append(axesUnitLabels[i] == null ? "" : axesUnitLabels[i]);
      sb.append("\" tickStyle=\"");
      sb.append(axesTickStyles[i]);
      sb.append("\" showNumbers=\"");
      sb.append(showAxesNumbers[i]);

      // the tick distance should only be saved if
      // it isn't calculated automatically
      if (!automaticAxesNumberingDistances[i]) {
        sb.append("\" tickDistance=\"");
        sb.append(axesNumberingDistances[i]);
      }

      sb.append("\"/>\n");
    }

    // grid distances
    if (!automaticGridDistance ||
    // compatibility to v2.7:
        automaticGridDistanceFactor != DEFAULT_GRID_DIST_FACTOR) {
      sb.append("\t<grid distX=\"");
      sb.append(gridDistances[0]);
      sb.append("\" distY=\"");
      sb.append(gridDistances[1]);
      sb.append("\"/>\n");
    }

    sb.append("</euclidianView>\n");
    return sb.toString();
  }

  /**
   * Returns xscale of this view. The scale is the number of pixels in screen
   * space that represent one unit in user space.
   */
  public double getXscale() {
    return xscale;
  }

  private String getXYscaleRatioString() {
    StringBuffer sb = new StringBuffer();
    sb.append("x : y = ");
    if (xscale >= yscale) {
      sb.append("1 : ");
      sb.append(printScaleNF.format(xscale / yscale));
    } else {
      sb.append(printScaleNF.format(yscale / xscale));
      sb.append(" : 1");
    }
    sb.append(' ');
    return sb.toString();
  }

  /**
   * Returns x coordinate of axes origin.
   */
  public double getXZero() {
    return xZero;
  }

  /**
   * @return Returns the ymax.
   */
  public double getYmax() {
    return ymax;
  }

  /**
   * @return Returns the ymin.
   */
  public double getYmin() {
    return ymin;
  }

  /**
   * Returns the yscale of this view. The scale is the number of pixels in
   * screen space that represent one unit in user space.
   */
  public double getYscale() {
    return yscale;
  }

  /**
   * Returns y coordinate of axes origin.
   */
  public double getYZero() {
    return yZero;
  }

  /**
   * Returns array of polygons with n points out of hits.
   * 
   * @return
   * 
   *         final public ArrayList getPolygons(ArrayList hits, int n, ArrayList
   *         polygons) { // search for polygons in hits that exactly have the
   *         needed number of // points polygons.clear(); getHits(hits,
   *         GeoPolygon.class, polygons); for (int k = polygons.size() - 1; k >
   *         0; k--) { GeoPolygon poly = (GeoPolygon) polygons.get(k); // remove
   *         poly with wrong number of points if (n != poly.getPoints().length)
   *         polygons.remove(k); } return polygons; }
   */

  /**
   * Tells if there are any images in the background.
   */
  private boolean hasBackgroundImages() {
    return bgImageList.size() > 0;
  }

  public boolean hasPreferredSize() {
    Dimension prefSize = getPreferredSize();

    return prefSize != null && prefSize.width > MIN_WIDTH
        && prefSize.height > MIN_HEIGHT;
  }

  public final boolean hitAnimationButton(MouseEvent e) {
    return e.getX() <= 20 && e.getY() >= height - 20;
  }

  private void initCursor() {
    defaultCursor = null;

    switch (mode) {
      case EuclidianView.MODE_ZOOM_IN :
        defaultCursor = getCursorForImage(app
            .getInternalImage("cursor_zoomin.gif"));
        break;

      case EuclidianView.MODE_ZOOM_OUT :
        defaultCursor = getCursorForImage(app
            .getInternalImage("cursor_zoomout.gif"));
        break;
    }

    setDefaultCursor();
  }

  private void initView(boolean repaint) {
    // preferred size
    setPreferredSize(null);

    // init grid's line type
    setGridLineStyle(LINE_TYPE_DASHED_SHORT);
    setAxesLineStyle(AXES_LINE_TYPE_ARROW);
    setAxesColor(Color.black); // Michael Borcherds 2008-01-26 was darkgray
    setGridColor(Color.lightGray);
    setBackground(Color.white);

    // showAxes = true;
    // showGrid = false;
    pointCapturingMode = POINT_CAPTURING_AUTOMATIC;
    pointStyle = POINT_STYLE_DOT;

    booleanSize = 13; // Michael Borcherds 2008-05-12

    // added by Lo�c BEGIN
    rightAngleStyle = EuclidianView.RIGHT_ANGLE_STYLE_SQUARE;
    // END

    showAxesNumbers[0] = true;
    showAxesNumbers[1] = true;
    axesLabels[0] = null;
    axesLabels[1] = null;
    axesUnitLabels[0] = null;
    axesUnitLabels[1] = null;
    piAxisUnit[0] = false;
    piAxisUnit[1] = false;
    axesTickStyles[0] = AXES_TICK_STYLE_MAJOR;
    axesTickStyles[1] = AXES_TICK_STYLE_MAJOR;

    // for axes labeling with numbers
    automaticAxesNumberingDistances[0] = true;
    automaticAxesNumberingDistances[1] = true;

    // distances between grid lines
    automaticGridDistance = true;

    setStandardCoordSystem(repaint);
  }

  public boolean[] isAutomaticAxesNumberingDistance() {
    return automaticAxesNumberingDistances;
  }

  public boolean isAutomaticGridDistance() {
    return automaticGridDistance;
  }

  /*
   * interface View implementation
   */

  public final boolean isGridOrAxesShown() {
    return showAxes[0] || showAxes[1] || showGrid;
  }

  /**
   * Tells if there are any traces in the background image.
   */
  private boolean isTracing() {
    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext())
      if (it.next().isTracing)
        return true;
    return false;
  }

  public void mouseEntered() {

  }

  public void mouseExited() {

  }

  /**
   * Draws all GeoElements except images.
   * 
   * protected void drawGeometricObjects(Graphics2D g2, int layer) {
   * 
   * if (previewDrawable != null && (layer == app.getMaxLayer() || layer == -1))
   * { // Michael Borcherds 2008-02-26 only draw once
   * previewDrawable.drawPreview(g2); }
   * 
   * // draw lists of objects drawListList.drawAll(g2);
   * 
   * // draw polygons drawPolygonList.drawAll(g2);
   * 
   * // draw conics drawConicList.drawAll(g2);
   * 
   * // draw angles and numbers drawNumericList.drawAll(g2);
   * 
   * // draw functions drawFunctionList.drawAll(g2);
   * 
   * // draw lines drawLineList.drawAll(g2);
   * 
   * // draw segments drawSegmentList.drawAll(g2);
   * 
   * // draw vectors drawVectorList.drawAll(g2);
   * 
   * // draw locus drawLocusList.drawAll(g2);
   * 
   * // draw points drawPointList.drawAll(g2);
   * 
   * // draw text drawTextList.drawAll(g2);
   * 
   * // boolean are not drawn as they are JToggleButtons and children of the
   * view }
   */

  // for use in AlgebraController
  final public void mouseMovedOver(GeoElement geo) {
    Hits geos = null;
    if (geo != null) {
      tempArrayList.clear();
      tempArrayList.add(geo);
      geos = tempArrayList;
    }
    boolean repaintNeeded = euclidianController.refreshHighlighting(geos);
    if (repaintNeeded)
      kernel.notifyRepaint();
  }

  final public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    // lastGraphics2D = g2;

    g2.setRenderingHints(defRenderingHints);
    // g2.setClip(0, 0, width, height);

    // BACKGROUND
    // draw background image (with axes and/or grid)
    if (bgImage == null) {
      if (firstPaint) {
        updateSize();
        g2.drawImage(bgImage, 0, 0, null);
        firstPaint = false;
      } else
        drawBackgroundWithImages(g2);
    } else
      // draw background image
      g2.drawImage(bgImage, 0, 0, null);

    /*
     * switch (drawMode) { case DRAW_MODE_BACKGROUND_IMAGE: // draw background
     * image (with axes and/or grid) if (bgImage == null) updateSize(); else
     * g2.drawImage(bgImage, 0,0, null); break;
     * 
     * default: // DRAW_MODE_DIRECT_DRAW: drawBackground(g2, true); }
     */

    // FOREGROUND
    if (antiAliasing)
      setAntialiasing(g2);

    // draw equations, checkboxes and all geo objects
    drawObjects(g2);

    if (selectionRectangle != null)
      drawZoomRectangle(g2);

    if (showMouseCoords && (showAxes[0] || showAxes[1] || showGrid))
      drawMouseCoords(g2);
    if (showAxesRatio)
      drawAxesRatio(g2);

    if (kernel.needToShowAnimationButton())
      drawAnimationButtons(g2);
  }

  public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
    if (pageIndex > 0)
      return NO_SUCH_PAGE;
    else {
      Graphics2D g2d = (Graphics2D) g;
      AffineTransform oldTransform = g2d.getTransform();

      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

      // construction title
      int y = 0;
      Construction cons = kernel.getConstruction();
      String title = cons.getTitle();
      if (!title.equals("")) {
        Font titleFont = app.getBoldFont().deriveFont(Font.BOLD,
            app.getBoldFont().getSize() + 2);
        g2d.setFont(titleFont);
        g2d.setColor(Color.black);
        // Font fn = g2d.getFont();
        FontMetrics fm = g2d.getFontMetrics();
        y += fm.getAscent();
        g2d.drawString(title, 0, y);
      }

      // construction author and date
      String author = cons.getAuthor();
      String date = cons.getDate();
      String line = null;
      if (!author.equals(""))
        line = author;
      if (!date.equals(""))
        if (line == null)
          line = date;
        else
          line = line + " - " + date;

      // scale string:
      // Scale in cm: 1:1 (x), 1:2 (y)
      String scaleString = null;
      if (app.isPrintScaleString()) {
        StringBuffer sb = new StringBuffer(Plain.ScaleInCentimeter);
        if (printingScale <= 1) {
          sb.append(": 1:");
          sb.append(printScaleNF.format(1 / printingScale));
        } else {
          sb.append(": ");
          sb.append(printScaleNF.format(printingScale));
          sb.append(":1");
        }

        // add yAxis scale too?
        if (scaleRatio != 1.0) {
          sb.append(" (x), ");
          double yPrintScale = printingScale * yscale / xscale;
          if (yPrintScale < 1) {
            sb.append("1:");
            sb.append(printScaleNF.format(1 / yPrintScale));
          } else {
            sb.append(printScaleNF.format(yPrintScale));
            sb.append(":1");
          }
          sb.append(" (y)");
        }
        scaleString = sb.toString();
      }

      if (scaleString != null)
        if (line == null)
          line = scaleString;
        else
          line = line + " - " + scaleString;

      if (line != null) {
        g2d.setFont(app.getPlainFont());
        g2d.setColor(Color.black);
        // Font fn = g2d.getFont();
        FontMetrics fm = g2d.getFontMetrics();
        y += fm.getHeight();
        g2d.drawString(line, 0, y);
      }
      if (y > 0)
        g2d.translate(0, y + 20); // space between title and drawing

      double scale = PRINTER_PIXEL_PER_CM / xscale * printingScale;
      exportPaint(g2d, scale);

      // clear page margins at bottom and right
      double pagewidth = pageFormat.getWidth();
      double pageheight = pageFormat.getHeight();
      double xmargin = pageFormat.getImageableX();
      double ymargin = pageFormat.getImageableY();

      g2d.setTransform(oldTransform);
      g2d.setClip(null);
      g2d.setPaint(Color.white);

      Rectangle2D.Double rect = new Rectangle2D.Double();
      rect.setFrame(0, pageheight - ymargin, pagewidth, ymargin);
      g2d.fill(rect);
      rect.setFrame(pagewidth - xmargin, 0, xmargin, pageheight);
      g2d.fill(rect);

      System.gc();
      return PAGE_EXISTS;
    }
  }

  /** remembers the origins values (xzero, ...) */
  public void rememberOrigins() {
    xZeroOld = xZero;
    yZeroOld = yZero;
  }

  /**
   * removes a GeoElement from this view
   */
  final public void remove(GeoElement geo) {
    Drawable d = (Drawable) DrawableMap.get(geo);
    int layer = geo.getLayer();

    if (d != null) {
      switch (geo.getGeoClassType()) {
        case GeoElement.GEO_CLASS_BOOLEAN :
          drawLayers[layer].remove(d);
          // remove checkbox
          ((DrawBoolean) d).remove();
          break;

        case GeoElement.GEO_CLASS_POINT :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_SEGMENT :
        case GeoElement.GEO_CLASS_RAY :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_LINE :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_POLYGON :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_ANGLE :
        case GeoElement.GEO_CLASS_NUMERIC :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_VECTOR :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_CONICPART :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_CONIC :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_FUNCTION :
        case GeoElement.GEO_CLASS_FUNCTIONCONDITIONAL :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_TEXT :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_IMAGE :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_LOCUS :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_CURVE_CARTESIAN :
          drawLayers[layer].remove(d);
          break;

        case GeoElement.GEO_CLASS_LIST :
          drawLayers[layer].remove(d);
          break;
      }

      allDrawableList.remove(d);

      DrawableMap.remove(geo);
      repaint();
    }
  }

  final void removeBackgroundImage(DrawImage img) {
    bgImageList.remove(img);
    // drawImageList.add(img);

    // Michael Borcherds 2008-02-29
    int layer = img.getGeoElement().getLayer();
    drawLayers[layer].add(img);
  }

  /**
   * renames an element
   */
  public void rename(GeoElement geo) {
    Object d = DrawableMap.get(geo);
    if (d != null) {
      ((Drawable) d).update();
      repaint();
    }
  }

  final public void repaintEuclidianView() {
    repaint();
  }

  final public void repaintView() {
    repaint();
  }

  public void reset() {
    resetMode();
    updateBackgroundImage();
  }

  private void resetLists() {
    DrawableMap.clear();
    allDrawableList.clear();
    bgImageList.clear();

    for (int i = 0; i <= MAX_LAYER_USED; i++)
      drawLayers[i].clear(); // Michael Borcherds 2008-02-29

    setToolTipText(null);
  }

  /***************************************************************************
   * ANIMATED ZOOMING
   **************************************************************************/

  public void resetMaxLayerUsed() {
    MAX_LAYER_USED = 0;

  }

  /**
   * clears all selections and highlighting
   */
  public void resetMode() {
    setMode(mode);
  }

  /**
   * Sets coord system of this view. Just like setCoordSystem but with previous
   * animation.
   * 
   * @param ox
   *          : x coord of new origin
   * @param oy
   *          : y coord of new origin
   * @param newscale
   */
  final public void setAnimatedCoordSystem(double ox, double oy,
      double newScale, int steps, boolean storeUndo) {
    if (!kernel.isEqual(xscale, newScale)) {
      // different scales: zoom back to standard view
      double factor = newScale / xscale;
      zoom((ox - xZero * factor) / (1.0 - factor), (oy - yZero * factor)
          / (1.0 - factor), factor, steps, storeUndo);
    } else {
      // same scales: translate view to standard origin
      // do this with the following action listener
      if (mover == null)
        mover = new MyMover();
      mover.init(ox, oy, storeUndo);
      mover.startAnimation();
    }
  }

  /**
   * Sets real world coord system using min and max values for both axes in real
   * world values.
   */
  final public void setAnimatedRealWorldCoordSystem(double xmin, double xmax,
      double ymin, double ymax, int steps, boolean storeUndo) {
    if (zoomerRW == null)
      zoomerRW = new MyZoomerRW();
    zoomerRW.init(xmin, xmax, ymin, ymax, steps, storeUndo);
    zoomerRW.startAnimation();
  }

  /**
   * Updates highlighting of animation buttons.
   * 
   * @return whether status was changed
   */
  public final boolean setAnimationButtonsHighlighted(boolean flag) {
    if (flag == highlightAnimationButtons)
      return false;
    else {
      highlightAnimationButtons = flag;
      return true;
    }
  }

  public void setAntialiasing(boolean flag) {
    if (flag == antiAliasing)
      return;
    antiAliasing = flag;
    repaint();
  }

  private void setAntialiasing(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  public void setAutomaticAxesNumberingDistance(boolean flag, int axis) {
    automaticAxesNumberingDistances[axis] = flag;
    if (axis == 0)
      setAxesIntervals(xscale, 0);
    else
      setAxesIntervals(yscale, 1);
  }

  public void setAutomaticGridDistance(boolean flag) {
    automaticGridDistance = flag;
    setAxesIntervals(xscale, 0);
    setAxesIntervals(yscale, 1);
  }

  public void setAxesColor(Color axesColor) {
    if (axesColor != null)
      this.axesColor = axesColor;
  }

  // axis: 0 for x-axis, 1 for y-axis
  private void setAxesIntervals(double scale, int axis) {
    double maxPix = 100; // only one tick is allowed per maxPix pixels
    double units = maxPix / scale;
    int exp = (int) Math.floor(Math.log(units) / Math.log(10));
    int maxFractionDigtis = Math.max(-exp, kernel.getPrintDecimals());

    if (automaticAxesNumberingDistances[axis])
      if (piAxisUnit[axis])
        axesNumberingDistances[axis] = Math.PI;
      else {
        double pot = Math.pow(10, exp);
        double n = units / pot;

        if (n > 5)
          axesNumberingDistances[axis] = 5 * pot;
        else if (n > 2)
          axesNumberingDistances[axis] = 2 * pot;
        else
          axesNumberingDistances[axis] = pot;
      }
    AxesTickInterval[axis] = axesNumberingDistances[axis] / 2.0;

    // set axes number format
    if (axesNumberFormat[axis] instanceof DecimalFormat) {
      DecimalFormat df = (DecimalFormat) axesNumberFormat[axis];

      // display large and small numbers in scienctific notation
      if (axesNumberingDistances[axis] < 10E-6
          || axesNumberingDistances[axis] > 10E6) {
        df.applyPattern("0.##E0");
        // avoid 4.00000000000004E-11 due to rounding error when computing
        // tick mark numbers
        maxFractionDigtis = Math.min(14, maxFractionDigtis);
      } else
        df.applyPattern("###0.##");
    }
    axesNumberFormat[axis].setMaximumFractionDigits(maxFractionDigtis);

    if (automaticGridDistance)
      gridDistances[axis] = axesNumberingDistances[axis]
          * automaticGridDistanceFactor;
  }

  public void setAxesLabels(String[] axesLabels) {
    this.axesLabels = axesLabels;
    for (int i = 0; i < 2; i++)
      if (axesLabels[i] != null && axesLabels[i].length() == 0)
        axesLabels[i] = null;
  }

  public void setAxesLineStyle(int axesLineStyle) {
    axesLineType = axesLineStyle;
  }

  /**
   * 
   * @param x
   * @param axis
   *          : 0 for xAxis, 1 for yAxis
   */
  public void setAxesNumberingDistance(double dist, int axis) {
    axesNumberingDistances[axis] = dist;
    setAutomaticAxesNumberingDistance(false, axis);
  }

  public void setAxesTickStyles(int[] axesTickStyles) {
    this.axesTickStyles = axesTickStyles;
  }

  public void setAxesUnitLabels(String[] axesUnitLabels) {
    this.axesUnitLabels = axesUnitLabels;

    // check if pi is an axis unit
    for (int i = 0; i < 2; i++)
      piAxisUnit[i] = axesUnitLabels[i] != null
          && axesUnitLabels[i].equals(PI_STRING);
    setAxesIntervals(xscale, 0);
    setAxesIntervals(yscale, 1);
  }

  public void setBackground(Color bgColor) {
    if (bgColor != null)
      this.bgColor = bgColor;
  }

  //	 
  /**
   * Sets the global size for checkboxes. Michael Borcherds 2008-05-12
   */
  public void setBooleanSize(int size) {

    // only 13 and 26 currently allowed
    booleanSize = size == 13 ? 13 : 26;

    updateAllDrawables(true);
  }

  /**
   * Sets real world coord system, where zero point has screen coords (xZero,
   * yZero) and one unit is xscale pixels wide on the x-Axis and yscale pixels
   * heigh on the y-Axis.
   */
  final public void setCoordSystem(double xZero, double yZero, double xscale,
      double yscale) {
    setCoordSystem(xZero, yZero, xscale, yscale, true);
  }

  public void setCoordSystem(double xZero, double yZero, double xscale,
      double yscale, boolean repaint) {
    if (Double.isNaN(xscale) || xscale < Kernel.MAX_DOUBLE_PRECISION
        || xscale > Kernel.INV_MAX_DOUBLE_PRECISION)
      return;
    if (Double.isNaN(yscale) || yscale < Kernel.MAX_DOUBLE_PRECISION
        || yscale > Kernel.INV_MAX_DOUBLE_PRECISION)
      return;

    this.xZero = xZero;
    this.yZero = yZero;
    this.xscale = xscale;
    this.yscale = yscale;
    scaleRatio = yscale / xscale;
    invXscale = 1.0d / xscale;
    invYscale = 1.0d / yscale;

    // set transform for my coord system:
    // ( xscale 0 xZero )
    // ( 0 -yscale yZero )
    // ( 0 0 1 )
    coordTransform.setTransform(xscale, 0.0d, 0.0d, -yscale, xZero, yZero);

    // real world values
    setRealWorldBounds();

    // if (drawMode == DRAW_MODE_BACKGROUND_IMAGE)
    if (repaint) {
      updateBackgroundImage();
      updateAllDrawables(repaint);
      // app.updateStatusLabelAxesRatio();
    }
  }

  /** Sets coord system from mouse move */
  final public void setCoordSystemFromMouseMove(int dx, int dy, int mode) {
    setCoordSystem(xZeroOld + dx, yZeroOld + dy, getXscale(), getYscale());
  }

  public void setDefaultCursor() {
    if (defaultCursor == null)
      setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    else
      setCursor(defaultCursor);
  }

  public void setDragCursor() {

    if (app.useTransparentCursorWhenDragging)
      setCursor(app.getTransparentCursor());
    else
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

  }

  public void setGridColor(Color gridColor) {
    if (gridColor != null)
      this.gridColor = gridColor;
  }

  public void setGridDistances(double[] dist) {
    gridDistances = dist;
    setAutomaticGridDistance(false);
  }

  // Michael Borcherds 2008-04-11
  public void setGridIsBold(boolean gridIsBold) {
    if (this.gridIsBold == gridIsBold)
      return;

    this.gridIsBold = gridIsBold;
    setGridLineStyle(gridLineStyle);

    updateBackgroundImage();
  }

  public void setGridLineStyle(int gridLineStyle) {
    this.gridLineStyle = gridLineStyle;
    gridStroke = getStroke(gridIsBold ? 2f : 1f, gridLineStyle); // Michael
    // Borcherds
    // 2008-04-11
    // added
    // gridisbold
  }

  /**
   * Set grid type.
   */
  public void setGridType(int type) {
    gridType = type;
  }

  public void setHitCursor() {
    if (defaultCursor == null)
      setCursor(Cursor.getDefaultCursor());
    else
      setCursor(defaultCursor);
  }

  /**
   * sets the hits of GeoElements whose visual representation is at screen
   * coords (x,y). order: points, vectors, lines, conics
   */
  final public void setHits(Point p) {

    hits.init();

    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      if (d.hit(p.x, p.y) || d.hitLabel(p.x, p.y)) {
        GeoElement geo = d.getGeoElement();
        if (geo.isEuclidianVisible())
          hits.add(geo);
      }
    }

    // look for axis
    if (hits.getImageCount() == 0) {
      if (showAxes[0] && Math.abs(yZero - p.y) < 3)
        hits.add(kernel.getXAxis());
      if (showAxes[1] && Math.abs(xZero - p.x) < 3)
        hits.add(kernel.getYAxis());
    }

    // remove all lists and images if there are other objects too
    if (hits.size() - (hits.getListCount() + hits.getImageCount()) > 0)
      for (int i = 0; i < hits.size(); ++i) {
        GeoElement geo = (GeoElement) hits.get(i);
        if (geo.isGeoList() || geo.isGeoImage())
          hits.remove(i);
      }

  }

  /**
   * sets array of GeoElements whose visual representation is inside of the
   * given screen rectangle
   */
  final public void setHits(Rectangle rect) {
    hits.init();

    if (rect == null)
      return;

    DrawableIterator it = allDrawableList.getIterator();
    while (it.hasNext()) {
      Drawable d = it.next();
      GeoElement geo = d.getGeoElement();
      if (geo.isEuclidianVisible() && d.isInside(rect))
        hits.add(geo);
    }
  }

  public void setMode(int mode) {
    this.mode = mode;
    initCursor();
    euclidianController.setMode(mode);
    setSelectionRectangle(null);
  }

  public void setMoveCursor() {
    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
  }

  /**
   * Set capturing of points to the grid.
   */
  public void setPointCapturing(int mode) {
    pointCapturingMode = mode;
  }

  /**
   * Sets the global style for point drawing.
   */
  public void setPointStyle(int style) {
    switch (style) {
      case 1 :
      case 2 :
        pointStyle = style;
        break;

      default :
        pointStyle = POINT_STYLE_DOT;
    }
    updateAllDrawables(true);
  }

  public void setPreview(Previewable p) {
    if (previewDrawable != null)
      previewDrawable.disposePreview();
    previewDrawable = p;
  }

  public final void setPrintingScale(double printingScale) {
    this.printingScale = printingScale;
  }

  final private void setRealWorldBounds() {
    xmin = -xZero * invXscale;
    xmax = (width - xZero) * invXscale;
    ymax = yZero * invYscale;
    ymin = (yZero - height) * invYscale;

    setAxesIntervals(xscale, 0);
    setAxesIntervals(yscale, 1);
    calcPrintingScale();

    // tell kernel
    kernel.setEuclidianViewBounds(xmin, xmax, ymin, ymax, xscale, yscale);
  }

  /**
   * Sets real world coord system using min and max values for both axes in real
   * world values.
   */
  final public void setRealWorldCoordSystem(double xmin, double xmax,
      double ymin, double ymax) {
    double calcXscale = width / (xmax - xmin);
    double calcYscale = height / (ymax - ymin);
    double calcXzero = -calcXscale * xmin;
    double calcYzero = calcYscale * ymax;

    setCoordSystem(calcXzero, calcYzero, calcXscale, calcYscale);
  }

  // added by Lo�c BEGIN
  /**
   * Sets the global style for rightAngle drawing.
   */
  public void setRightAngleStyle(int style) {
    rightAngleStyle = style;
    updateAllDrawables(true);
  }

  public void setSelectionRectangle(Rectangle selectionRectangle) {
    this.selectionRectangle = selectionRectangle;
  }

  public void setShowAxesNumbers(boolean[] showAxesNumbers) {
    this.showAxesNumbers = showAxesNumbers;
  }

  public void setShowAxesRatio(boolean b) {
    showAxesRatio = b;
  }

  /**
   * 
   * setters and getters for EuclidianViewInterface
   * 
   */

  public void setShowMouseCoords(boolean b) {
    showMouseCoords = b;
  }

  public void setStandardCoordSystem() {
    setStandardCoordSystem(true);
  }

  private void setStandardCoordSystem(boolean repaint) {
    setCoordSystem(XZERO_STANDARD, YZERO_STANDARD, SCALE_STANDARD,
        SCALE_STANDARD, repaint);
  }

  public final void setStandardView(boolean storeUndo) {
    final double xzero, yzero;

    // check if the window is so small that we need custom
    // positions.
    if (getWidth() < XZERO_STANDARD * 3)
      xzero = getWidth() / 3.0;
    else
      xzero = XZERO_STANDARD;

    if (getHeight() < YZERO_STANDARD * 1.6)
      yzero = getHeight() / 1.6;
    else
      yzero = YZERO_STANDARD;

    if (scaleRatio != 1.0) {
      // set axes ratio back to 1
      if (axesRatioZoomer == null)
        axesRatioZoomer = new MyAxesRatioZoomer();
      axesRatioZoomer.init(1, false);

      Thread waiter = new Thread() {
        public void run() {
          // wait until zoomer has finished
          axesRatioZoomer.startAnimation();
          while (axesRatioZoomer.isRunning())
            try {
              Thread.sleep(100);
            } catch (Exception e) {
            }
          setAnimatedCoordSystem(xzero, yzero, SCALE_STANDARD, 15, false);
        }
      };
      waiter.start();
    } else
      setAnimatedCoordSystem(xzero, yzero, SCALE_STANDARD, 15, false);
    if (storeUndo)
      app.storeUndoInfo();
  }

  public final void setViewShowAllObjects(boolean storeUndo) {

    double x0RW = xmin;
    double x1RW;
    double y0RW;
    double y1RW;
    double y0RWfunctions = 0;
    double y1RWfunctions = 0;
    double factor = 0.03d; // don't want objects at edge
    double xGap = 0;

    TreeSet<GeoElement> allFunctions = kernel.getConstruction()
        .getGeoSetLabelOrder(GeoElement.GEO_CLASS_FUNCTION);

    int noVisible = 0;
    // count no of visible functions
    Iterator<GeoElement> it = allFunctions.iterator();
    while (it.hasNext())
      if (((GeoFunction) it.next()).isEuclidianVisible())
        noVisible++;;

    Rectangle rect = getBounds();
    if (kernel.isZero(rect.getHeight()) || kernel.isZero(rect.getWidth())) {
      if (noVisible == 0)
        return; // no functions or objects

      // just functions
      x0RW = Double.MAX_VALUE;
      x1RW = -Double.MAX_VALUE;
      y0RW = Double.MAX_VALUE;
      y1RW = -Double.MAX_VALUE;

      // Application.debug("just functions");

    } else {

      // get bounds of points, circles etc
      x0RW = toRealWorldCoordX(rect.getMinX());
      x1RW = toRealWorldCoordX(rect.getMaxX());
      y0RW = toRealWorldCoordY(rect.getMaxY());
      y1RW = toRealWorldCoordY(rect.getMinY());
    }

    xGap = (x1RW - x0RW) * factor;

    boolean ok = false;

    if (noVisible != 0) {

      // if there are functions we don't want to zoom in horizintally
      x0RW = Math.min(xmin, x0RW);
      x1RW = Math.max(xmax, x1RW);

      if (kernel.isEqual(x0RW, xmin) && kernel.isEqual(x1RW, xmax))
        // just functions (at sides!), don't need a gap
        xGap = 0;
      else
        xGap = (x1RW - x0RW) * factor;

      // Application.debug("checking functions from "+x0RW+" to "+x1RW);

      y0RWfunctions = Double.MAX_VALUE;
      y1RWfunctions = -Double.MAX_VALUE;

      it = allFunctions.iterator();

      while (it.hasNext()) {
        GeoFunction fun = (GeoFunction) it.next();
        double abscissa;
        // check 100 random heights
        for (int i = 0; i < 200; i++) {

          if (i == 0)
            abscissa = fun.evaluate(x0RW); // check far left
          else if (i == 1)
            abscissa = fun.evaluate(x1RW); // check far right
          else
            abscissa = fun.evaluate(x0RW + Math.random() * (x1RW - x0RW));

          if (!Double.isInfinite(abscissa) && !Double.isNaN(abscissa)) {
            ok = true;
            if (abscissa > y1RWfunctions)
              y1RWfunctions = abscissa;
            // no else: there **might** be just one value
            if (abscissa < y0RWfunctions)
              y0RWfunctions = abscissa;
          }
        }
      }

    }

    if (!kernel.isZero(y1RWfunctions - y0RWfunctions) && ok) {
      y0RW = Math.min(y0RW, y0RWfunctions);
      y1RW = Math.max(y1RW, y1RWfunctions);
      // Application.debug("min height "+y0RW+" max height "+y1RW);
    }

    // don't want objects at edge
    double yGap = (y1RW - y0RW) * factor;

    final double x0RW2 = x0RW - xGap;
    final double x1RW2 = x1RW + xGap;
    final double y0RW2 = y0RW - yGap;
    final double y1RW2 = y1RW + yGap;

    setAnimatedRealWorldCoordSystem(x0RW2, x1RW2, y0RW2, y1RW2, 10, storeUndo);

  }

  public void showAxes(boolean xAxis, boolean yAxis) {

    if (xAxis == showAxes[0] && yAxis == showAxes[1])
      return;

    showAxes[0] = xAxis;
    showAxes[1] = yAxis;
    updateBackgroundImage();
  }

  public void showGrid(boolean show) {
    if (show == showGrid)
      return;
    showGrid = show;
    updateBackgroundImage();
  }

  /**
   * Converts real world coordinates to screen coordinates. If a coord value is
   * outside the screen it is clipped to a rectangle with border PIXEL_OFFSET
   * around the screen.
   * 
   * @param inOut
   *          : input and output array with x and y coords
   * @return true iff resulting coords are on screen, note: Double.NaN is NOT
   *         checked
   */
  final protected boolean toClippedScreenCoords(double[] inOut, int PIXEL_OFFSET) {
    inOut[0] = xZero + inOut[0] * xscale;
    inOut[1] = yZero - inOut[1] * yscale;

    boolean onScreen = true;

    // x-coord on screen?
    if (inOut[0] < 0) {
      inOut[0] = Math.max(inOut[0], -PIXEL_OFFSET);
      onScreen = false;
    } else if (inOut[0] > width) {
      inOut[0] = Math.min(inOut[0], width + PIXEL_OFFSET);
      onScreen = false;
    }

    // y-coord on screen?
    if (inOut[1] < 0) {
      inOut[1] = Math.max(inOut[1], -PIXEL_OFFSET);
      onScreen = false;
    } else if (inOut[1] > height) {
      inOut[1] = Math.min(inOut[1], height + PIXEL_OFFSET);
      onScreen = false;
    }

    return onScreen;
  }

  /**
   * convert real world coordinate x to screen coordinate x. If the value is
   * outside the screen it is clipped to one pixel outside.
   * 
   * @param xRW
   * @return
   */
  final protected int toClippedScreenCoordX(double xRW) {
    if (xRW > xmax)
      return width + 1;
    else if (xRW < xmin)
      return -1;
    else
      return toScreenCoordX(xRW);
  }

  /**
   * convert real world coordinate y to screen coordinate y. If the value is
   * outside the screen it is clipped to one pixel outside.
   * 
   * @param yRW
   * @return
   */
  final protected int toClippedScreenCoordY(double yRW) {
    if (yRW > ymax)
      return -1;
    else if (yRW < ymin)
      return height + 1;
    else
      return toScreenCoordY(yRW);
  }

  /**
   * convert screen coordinate x to real world coordinate x
   * 
   * @param x
   * @return
   */
  final public double toRealWorldCoordX(double x) {
    return (x - xZero) * invXscale;
  }

  /**
   * convert screen coordinate y to real world coordinate y
   * 
   * @param y
   * @return
   */
  final public double toRealWorldCoordY(double y) {
    return (yZero - y) * invYscale;
  }

  /**
   * Converts real world coordinates to screen coordinates.
   * 
   * @param inOut
   *          : input and output array with x and y coords
   */
  final public void toScreenCoords(double[] inOut) {
    inOut[0] = xZero + inOut[0] * xscale;
    inOut[1] = yZero - inOut[1] * yscale;

    // java drawing crashes for huge coord values
    if (Math.abs(inOut[0]) > MAX_SCREEN_COORD_VAL
        || Math.abs(inOut[0]) > MAX_SCREEN_COORD_VAL) {
      inOut[0] = Double.NaN;
      inOut[1] = Double.NaN;
    }
  }

  /**
   * convert real world coordinate x to screen coordinate x
   * 
   * @param xRW
   * @return
   */
  final public int toScreenCoordX(double xRW) {
    return (int) Math.round(xZero + xRW * xscale);
  }

  /**
   * convert real world coordinate x to screen coordinate x
   * 
   * @param xRW
   * @return
   */
  final protected double toScreenCoordXd(double xRW) {
    return xZero + xRW * xscale;
  }

  /**
   * convert real world coordinate y to screen coordinate y
   * 
   * @param yRW
   * @return
   */
  final public int toScreenCoordY(double yRW) {
    return (int) Math.round(yZero - yRW * yscale);
  }

  /**
   * convert real world coordinate y to screen coordinate y
   * 
   * @param yRW
   * @return
   */
  final protected double toScreenCoordYd(double yRW) {
    return yZero - yRW * yscale;
  }

  final public void update(GeoElement geo) {
    Object d = DrawableMap.get(geo);
    if (d != null)
      ((Drawable) d).update();
  }

  final private void updateAllDrawables(boolean repaint) {
    allDrawableList.updateAll();
    if (repaint)
      repaint();
  }

  final public void updateAuxiliaryObject(GeoElement geo) {
    // repaint();
  }

  final public void updateBackground() {
    // make sure axis number formats are up to date
    setAxesIntervals(xscale, 0);
    setAxesIntervals(yscale, 1);

    updateBackgroundImage();
    updateAllDrawables(true);
    // repaint();
  }

  final protected void updateBackgroundImage() {
    if (bgGraphics != null)
      drawBackgroundWithImages(bgGraphics);
  }

  final private void updateDrawableFontSize() {
    allDrawableList.updateFontSizeAll();
    repaint();
  }

  // ///////////////////////////////////////
  // previewables

  public void updateFonts() {
    fontSize = app.getEuclidianFontSize();

    fontPoint = app.getPlainFont().deriveFont(Font.PLAIN, fontSize);
    fontAngle = fontPoint;
    fontLine = fontPoint;
    fontVector = fontPoint;
    fontConic = fontPoint;
    fontCoords = app.getPlainFont().deriveFont(Font.PLAIN,
        app.getAxesFontSize());
    fontAxes = fontCoords;

    updateDrawableFontSize();
    updateBackground();
  }

  private void updateMaxLayerUsed(int layer) {
    if (layer > MAX_LAYERS)
      layer = MAX_LAYERS;
    if (layer > MAX_LAYER_USED)
      MAX_LAYER_USED = layer;
  }

  public void updatePreviewable() {
    Point mouseLoc = getEuclidianController().mouseLoc;
    getPreviewDrawable().updateMousePos(mouseLoc.x, mouseLoc.y);
  }

  public void updateRightAngleStyle(Locale locale) {
    // change rightAngleStyle for German to
    // EuclidianView.RIGHT_ANGLE_STYLE_DOT
    if (getRightAngleStyle() != RIGHT_ANGLE_STYLE_NONE)
      if (locale.getLanguage().equals("de"))
        setRightAngleStyle(RIGHT_ANGLE_STYLE_DOT);
      else
        setRightAngleStyle(RIGHT_ANGLE_STYLE_SQUARE);
  }

  public void updateSize() {
    width = getWidth();
    height = getHeight();
    if (width <= 0 || height <= 0)
      return;

    // real world values
    setRealWorldBounds();

    GraphicsConfiguration gconf = getGraphicsConfiguration();
    try {
      createImage(gconf);
    } catch (OutOfMemoryError e) {
      bgImage = null;
      bgGraphics = null;
      System.gc();
    }

    updateBackgroundImage();
    updateAllDrawables(true);
  }

  /**
   * Zooms around fixed point (px, py)
   */
  public final void zoom(double px, double py, double zoomFactor, int steps,
      boolean storeUndo) {
    if (zoomer == null)
      zoomer = new MyZoomer();
    zoomer.init(px, py, zoomFactor, steps, storeUndo);
    zoomer.startAnimation();

  }

  /**
   * Zooms towards the given axes scale ratio. Note: Only the y-axis is changed
   * here. ratio = yscale / xscale;
   */
  public final void zoomAxesRatio(double newRatio, boolean storeUndo) {
    if (axesRatioZoomer == null)
      axesRatioZoomer = new MyAxesRatioZoomer();
    axesRatioZoomer.init(newRatio, storeUndo);
    axesRatioZoomer.startAnimation();
  }

}