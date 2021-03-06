package geogebra3D.euclidian3D;

import geogebra.euclidian.*;
import geogebra.kernel.GeoElement;
import geogebra.main.View;
import geogebra3D.Matrix.Ggb3DMatrix;
import geogebra3D.Matrix.Ggb3DMatrix4x4;
import geogebra3D.Matrix.Ggb3DVector;
import geogebra3D.euclidian3D.opengl.Renderer;
import geogebra3D.kernel3D.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;

import javax.media.opengl.GLCanvas;
import javax.swing.JPanel;

public class EuclidianView3D extends JPanel
    implements
      View,
      Printable,
      EuclidianConstants3D,
      EuclidianViewInterface {

  private static final long serialVersionUID = -8414195993686838278L;

  static final boolean DEBUG = false; // conditionnal compilation

  // private Kernel kernel;
  private final Kernel3D kernel3D;
  private final EuclidianController3D euclidianController3D;
  private final Renderer renderer;

  // viewing values
  private double XZero = 0;
  private double YZero = 0;
  private double ZZero = 0;

  private double XZeroOld = 0;
  private double YZeroOld = 0;

  // list of 3D objects
  private boolean waitForUpdate = true; // says if it waits for update...
  DrawList3D drawList3D = new DrawList3D();

  // matrix for changing coordinate system
  private final Ggb3DMatrix4x4 m = Ggb3DMatrix4x4.Identity();
  private final Ggb3DMatrix4x4 mInv = Ggb3DMatrix4x4.Identity();
  private final Ggb3DMatrix4x4 undoRotationMatrix = Ggb3DMatrix4x4.Identity();
  double a = 0;
  double b = 0;// angles
  private double aOld, bOld;

  // picking and hits
  private Hits3D hits = new Hits3D(); // objects picked from openGL

  // base vectors for moving a point
  static public Ggb3DVector vx = new Ggb3DVector(new double[]{1.0, 0.0, 0.0,
      0.0});
  static public Ggb3DVector vy = new Ggb3DVector(new double[]{0.0, 1.0, 0.0,
      0.0});
  static public Ggb3DVector vz = new Ggb3DVector(new double[]{0.0, 0.0, 1.0,
      0.0});

  // preview
  private Previewable previewDrawable;
  private final GeoPoint3D cursor3D;
  private final GeoElement[] cursor3DIntersetionOf = new GeoElement[2];

  public static final int PREVIEW_POINT_ALREADY = 0;
  public static final int PREVIEW_POINT_FREE = 1;
  public static final int PREVIEW_POINT_PATH = 2;
  public static final int PREVIEW_POINT_REGION = 3;
  public static final int PREVIEW_POINT_DEPENDENT = 4;
  private int cursor3DType = PREVIEW_POINT_ALREADY;

  // cursor
  private static final int CURSOR_DEFAULT = 0;
  private static final int CURSOR_DRAG = 1;
  private static final int CURSOR_MOVE = 2;
  private static final int CURSOR_HIT = 3;
  private int cursor = CURSOR_DEFAULT;

  // mouse
  private boolean hasMouse = false;

  // stuff TODO
  private final Rectangle selectionRectangle = new Rectangle();

  // TODO specific scaling for each direction
  private double scale = 100;

  private final double scaleMin = 10;

  public EuclidianView3D(EuclidianController3D ec) {

    /*
     * setSize(new
     * Dimension(EuclidianGLDisplay.DEFAULT_WIDTH,EuclidianGLDisplay.
     * DEFAULT_HEIGHT)); setPreferredSize(new
     * Dimension(EuclidianGLDisplay.DEFAULT_WIDTH
     * ,EuclidianGLDisplay.DEFAULT_HEIGHT));
     */

    euclidianController3D = ec;
    kernel3D = (Kernel3D) ec.getKernel();
    euclidianController3D.setView(this);

    // TODO replace canvas3D with GLDisplay
    renderer = new Renderer(this);
    renderer.setDrawList3D(drawList3D);

    GLCanvas canvas = renderer.canvas;

    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, canvas);

    attachView();

    // register Listener
    canvas.addMouseMotionListener(euclidianController3D);
    canvas.addMouseListener(euclidianController3D);
    canvas.addMouseWheelListener(euclidianController3D);
    canvas.setFocusable(true);

    // previewables
    kernel3D.setSilentMode(true);
    cursor3D = kernel3D.Point3D(null, 1, 1, 0);
    cursor3D.setIsPickable(false);
    cursor3D.setLabelOffset(5, -5);
    cursor3D.setEuclidianVisible(false);
    kernel3D.setSilentMode(false);

  }

  /**
   * adds a GeoElement3D to this view
   */
  public void add(GeoElement geo) {

    if (geo.isGeoElement3D()) {
      Drawable3D d = null;
      d = createDrawable(geo);
      if (d != null)
        drawList3D.add(d);
      // repaint();
    }
  }

  public void addRotXY(int da, int db, boolean repaint) {

    setRotXYinDegrees(a + da, b + db, repaint);
  }

  /**
   * add a drawable to the current hits (used when a new object is created)
   * 
   * @param d
   *          drawable to add
   */
  protected void addToHits3D(Drawable3D d) {
    hits.addDrawable3D(d, false);
    hits.sort();
  }

  public void attachView() {
    kernel3D.notifyAddAll(this);
    kernel3D.attach(this);
  }

  final private void changeCoords(Ggb3DMatrix mat, Ggb3DMatrix mInOut) {
    Ggb3DMatrix m1 = mInOut.copy();
    mInOut.set(mat.mul(m1));
  }

  final private void changeCoords(Ggb3DMatrix mat, Ggb3DVector vInOut) {
    Ggb3DVector v1 = vInOut.getCoordsLast1();
    vInOut.set(mat.mul(v1));
  }

  public void clearView() {
    drawList3D.clear();

  }

  /**
   * Create a {@link Drawable3D} linked to the {@link GeoElement3D}
   * 
   * <h3>Exemple:</h3>
   * 
   * For a GeoElement3D called "GeoNew3D", add in the switch the following code:
   * <p>
   * <code>
	    case GeoElement3D.GEO_CLASS_NEW3D: <br> &nbsp;&nbsp;                   
           d = new DrawNew3D(this, (GeoNew3D) geo); <br> &nbsp;&nbsp;
           break; <br> 
        }
        </code>
   * 
   * 
   * @param geo
   *          GeoElement for which the drawable is created
   * @return the drawable
   */
  protected Drawable3D createDrawable(GeoElement geo) {
    Drawable3D d = null;
    if (geo.isGeoElement3D())
      if (d == null)
        switch (geo.getGeoClassType()) {

          case GeoElement3D.GEO_CLASS_POINT3D :
            d = new DrawPoint3D(this, (GeoPoint3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_VECTOR3D :
            d = new DrawVector3D(this, (GeoVector3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_SEGMENT3D :
            d = new DrawSegment3D(this, (GeoSegment3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_PLANE3D :
            d = new DrawPlane3D(this, (GeoPlane3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_POLYGON3D :
            d = new DrawPolygon3D(this, (GeoPolygon3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_LINE3D :
            d = new DrawLine3D(this, (GeoLine3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_RAY3D :
            d = new DrawRay3D(this, (GeoRay3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_CONIC3D :
            d = new DrawConic3D(this, (GeoConic3D) geo);
            break;

          case GeoElement3D.GEO_CLASS_QUADRIC :
            d = new DrawQuadric(this, (GeoQuadric) geo);
            break;
        }

    return d;
  }

  public Previewable createPreviewLine(ArrayList<Object> selectedPoints) {

    // Application.debug("createPreviewLine");

    Drawable3D d = new DrawLine3D(this, selectedPoints);

    return (Previewable) d;

  }

  public Previewable createPreviewPolygon(ArrayList<Object> selectedPoints) {
    return null;
  }

  public Previewable createPreviewRay(ArrayList<Object> selectedPoints) {
    return new DrawRay3D(this, selectedPoints);
  }

  public Previewable createPreviewSegment(ArrayList<Object> selectedPoints) {
    return new DrawSegment3D(this, selectedPoints);
  }

  public void drawCursor(Renderer renderer) {

    if (hasMouse)
      switch (cursor) {
        case CURSOR_DEFAULT :
          // if(getCursor3DType()!=PREVIEW_POINT_ALREADY)
          drawCursorCross(renderer);
          break;
        case CURSOR_HIT :
          switch (getCursor3DType()) {
            case PREVIEW_POINT_FREE :
            case PREVIEW_POINT_REGION :
              drawCursorCross(renderer);
              break;
            case PREVIEW_POINT_PATH :
              drawCursorOnPath(renderer);
              break;
            case PREVIEW_POINT_DEPENDENT :
              drawCursorDependent(renderer);
              break;
          }
          break;
      }
  }

  private void drawCursorCross(Renderer renderer) {

    switch (getCursor3DType()) {
      case PREVIEW_POINT_FREE :
        // use default directions for the cross
        getCursor3D().getDrawingMatrix().setVx(vx);
        getCursor3D().getDrawingMatrix().setVy(vy);
        getCursor3D().getDrawingMatrix().setVz(vz);
        break;
      case PREVIEW_POINT_REGION :
        // use region drawing directions for the cross
        getCursor3D().getDrawingMatrix().setVx(
            ((GeoElement3DInterface) getCursor3D().getRegion())
                .getDrawingMatrix().getVx());
        getCursor3D().getDrawingMatrix().setVy(
            ((GeoElement3DInterface) getCursor3D().getRegion())
                .getDrawingMatrix().getVy());
        getCursor3D().getDrawingMatrix().setVz(
            ((GeoElement3DInterface) getCursor3D().getRegion())
                .getDrawingMatrix().getVz());
        break;
    }

    renderer.setMatrix(getCursor3D().getDrawingMatrix());
    renderer.setThickness(2.5 / getScale());
    renderer.drawCursorCross(12 / getScale());

  }

  private void drawCursorDependent(Renderer renderer) {

    getCursor3D().getDrawingMatrix().setVx(vx);
    getCursor3D().getDrawingMatrix().setVy(vy);
    getCursor3D().getDrawingMatrix().setVz(vz);
    renderer.setMatrix(getCursor3D().getDrawingMatrix());

    int t1 = getCursor3DIntersetionOf(0).getLineThickness();
    int t2 = getCursor3DIntersetionOf(1).getLineThickness();
    if (t1 > t2)
      t2 = t1;
    renderer.setThickness((t2 + 6) / getScale());
    renderer.drawCursorDiamond();

  }

  private void drawCursorOnPath(Renderer renderer) {

    // use path drawing directions for the cross
    getCursor3D().getDrawingMatrix().setVx(
        ((GeoElement3DInterface) getCursor3D().getPath()).getDrawingMatrix()
            .getVx().normalized());
    getCursor3D().getDrawingMatrix().setVy(
        ((GeoElement3DInterface) getCursor3D().getPath()).getDrawingMatrix()
            .getVy());
    getCursor3D().getDrawingMatrix().setVz(
        ((GeoElement3DInterface) getCursor3D().getPath()).getDrawingMatrix()
            .getVz());

    renderer.setThickness((3 + ((GeoElement) getCursor3D().getPath())
        .getLineThickness())
        / getScale());
    renderer.setMatrix(getCursor3D().getDrawingMatrix());

    renderer.drawCursorCylinder(1.25 / getScale());

  }

  /**
   * return the point used for 3D cursor
   * 
   * @return the point used for 3D cursor
   */
  public GeoPoint3D getCursor3D() {
    return cursor3D;
  }

  /**
   * return the i-th GeoElement of intersection
   * 
   * @param i
   *          number of GeoElement of intersection
   * @return GeoElement of intersection
   */
  protected GeoElement getCursor3DIntersetionOf(int i) {
    return cursor3DIntersetionOf[i];
  }
  public int getCursor3DType() {
    return cursor3DType;
  }
  public Drawable getDrawableFor(GeoElement geo) {
    // TODO Auto-generated method stub
    return null;
  }

  public EuclidianController getEuclidianController() {
    return euclidianController3D;
  }
  public double getGridDistances(int i) {
    // TODO Auto-generated method stub
    return 0;
  }
  public int getGridType() {
    // TODO Auto-generated method stub
    return 0;
  }

  public Hits getHits() {
    // return hits;
    return hits.clone();
  }
  public double getInvXscale() {
    // TODO Auto-generated method stub
    return 0;
  }
  public double getInvYscale() {
    // TODO Auto-generated method stub
    return 0;
  }
  /**
   * return the 3D kernel
   * 
   * @return the 3D kernel
   */
  public Kernel3D getKernel() {
    return kernel3D;
  }
  public GeoElement getLabelHit(Point p) {

    // Application.debug("getLabelHit");

    // sets the flag and mouse location for openGL picking
    // renderer.setMouseLoc(p.x,p.y,EuclidianRenderer3D.PICKING_MODE_LABELS);

    // calc immediately the hits
    // renderer.display();

    // Application.debug("end-getLabelHit");

    // return null;
    return hits.getLabelHit();
  }

  /** p scene coords, (dx,dy) 2D mouse move -> 3D physical coords */
  protected Ggb3DVector getPickFromScenePoint(Ggb3DVector p, int dx, int dy) {
    Ggb3DVector point = p.copyVector();
    toScreenCoords3D(point);
    Ggb3DVector ret = new Ggb3DVector(new double[]{point.get(1) + dx,
        point.get(2) - dy, 0, 1.0});

    return ret;

  }

  /** (x,y) 2D screen coords -> 3D physical coords */
  protected Ggb3DVector getPickPoint(int x, int y) {

    Dimension d = new Dimension();
    this.getSize(d);

    if (d != null) {
      Ggb3DVector ret = new Ggb3DVector(new double[]{
          (double) x + renderer.getLeft(), (double) -y + renderer.getTop(),
          // ((double) (x-w)/w),
          // ((double) (-y+h)/w),
          0, 1.0});

      // ret.SystemPrint();
      return ret;
    } else
      return null;

  }

  public int getPointCapturingMode() {
    // TODO Auto-generated method stub
    return 0;
  }

  // ////////////////////////////////////
  // update

  public Previewable getPreviewDrawable() {

    return previewDrawable;
  }

  /*
   * private void setWaitForUpdate(boolean v){ waitForUpdate = v; }
   */

  public Renderer getRenderer() {
    return renderer;
  }

  public double getScale() {
    return scale;
  }

  // ////////////////////////////////////
  // toolbar and euclidianController3D

  public Rectangle getSelectionRectangle() {
    return selectionRectangle;
  }

  // ////////////////////////////////////
  // picking

  public boolean getShowMouseCoords() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean getShowXaxis() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean getShowYaxis() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * return the matrix : screen coords -> scene coords.
   * 
   * @return the matrix : screen coords -> scene coords.
   */
  final public Ggb3DMatrix4x4 getToSceneMatrix() {

    return mInv;
  }

  /**
   * return the matrix : scene coords -> screen coords.
   * 
   * @return the matrix : scene coords -> screen coords.
   */
  final public Ggb3DMatrix4x4 getToScreenMatrix() {

    return m;
  }

  /**
   * return the matrix undoing the rotation : scene coords -> screen coords.
   * 
   * @return the matrix undoing the rotation : scene coords -> screen coords.
   */
  final public Ggb3DMatrix4x4 getUndoRotationMatrix() {

    return undoRotationMatrix;
  }

  public int getViewHeight() {
    return getHeight();
  }

  public int getViewWidth() {
    return getWidth();
  }

  /**
   * returns settings in XML format
   * 
   * @return the XML description of 3D view settings
   */
  public String getXML() {
    StringBuffer sb = new StringBuffer();
    sb.append("<euclidianView3D>\n");

    // coord system
    sb.append("\t<coordSystem");

    sb.append(" xZero=\"");
    sb.append(getXZero());
    sb.append("\"");
    sb.append(" yZero=\"");
    sb.append(getYZero());
    sb.append("\"");
    sb.append(" zZero=\"");
    sb.append(getZZero());
    sb.append("\"");

    sb.append(" scale=\"");
    sb.append(getXscale());
    sb.append("\"");

    sb.append(" xAngle=\"");
    sb.append(b);
    sb.append("\"");
    sb.append(" zAngle=\"");
    sb.append(a);
    sb.append("\"");

    sb.append("/>\n");

    sb.append("</euclidianView3D>\n");
    return sb.toString();
  }

  public double getXscale() {
    return scale;
  }

  /*
   * TODO interaction - note : methods are called by
   * EuclidianRenderer3D.viewOrtho() to re-center the scene
   */
  public double getXZero() {
    return XZero;
  }

  // ////////////////////////////////////////////
  // EuclidianViewInterface

  public double getYscale() {
    return scale;
  }

  public double getYZero() {
    return YZero;
  }

  public double getZscale() {
    return scale;
  }

  public double getZZero() {
    return ZZero;
  }

  public boolean hitAnimationButton(MouseEvent e) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isGridOrAxesShown() {
    // TODO Auto-generated method stub
    return false;
  }

  public void mouseEntered() {
    // Application.debug("mouseEntered");
    hasMouse = true;
  }

  public void mouseExited() {
    hasMouse = false;
  }

  @Override
  public void paint(Graphics g) {
    update();
    // setWaitForUpdate(true);
  }

  public int print(Graphics arg0, PageFormat arg1, int arg2)
      throws PrinterException {
    // TODO Raccord de méthode auto-généré
    return 0;
  }

  /** remembers the origins values (xzero, ...) */
  public void rememberOrigins() {
    aOld = a;
    bOld = b;
    XZeroOld = XZero;
    YZeroOld = YZero;
  }

  /**
   * remove a GeoElement3D from this view
   */
  public void remove(GeoElement geo) {

    if (geo.isGeoElement3D()) {
      Drawable3D d = ((GeoElement3DInterface) geo).getDrawable3D();
      drawList3D.remove(d);
    }
  }

  public void rename(GeoElement geo) {
    // TODO Raccord de méthode auto-généré

  }

  public void repaintEuclidianView() {
    // Application.debug("repaintEuclidianView");

  }

  public void repaintView() {
    // setWaitForUpdate(true);

    update();

    // Application.debug("repaint View3D");

  }

  public void reset() {
    // drawList3D.clear();

  }

  public void resetMode() {
    // TODO Auto-generated method stub

  }

  public void setAnimatedCoordSystem(double ox, double oy, double newScale,
      int steps, boolean storeUndo) {
    // TODO Auto-generated method stub

  }

  public void setAnimatedRealWorldCoordSystem(double xmin, double xmax,
      double ymin, double ymax, int steps, boolean storeUndo) {
    // TODO Auto-generated method stub

  }

  public boolean setAnimationButtonsHighlighted(boolean hitAnimationButton) {
    // TODO Auto-generated method stub
    return false;
  }

  public void setCoordSystem(double x, double y, double xscale, double yscale) {
    // TODO Auto-generated method stub

  }

  /** Sets coord system from mouse move */
  final public void setCoordSystemFromMouseMove(int dx, int dy, int mode) {
    switch (mode) {
      case EuclidianController3D.MOVE_ROTATE_VIEW :
        setRotXYinDegrees(aOld + dx, bOld + dy, true);
        break;
      case EuclidianController3D.MOVE_VIEW :
        setXZero(XZeroOld + dx);
        setYZero(YZeroOld - dy);
        updateMatrix();
        update();
        break;
    }
  }

  /**
   * sets that the current 3D cursor is at the intersection of the two
   * GeoElement parameters
   * 
   * @param cursor3DIntersetionOf1
   *          first GeoElement of intersection
   * @param cursor3DIntersetionOf2
   *          second GeoElement of intersection
   */
  protected void setCursor3DIntersetionOf(GeoElement cursor3DIntersetionOf1,
      GeoElement cursor3DIntersetionOf2) {
    cursor3DIntersetionOf[0] = cursor3DIntersetionOf1;
    cursor3DIntersetionOf[1] = cursor3DIntersetionOf2;
  }

  /*
   * Point pOld = null;
   * 
   * 
   * public void setHits(Point p) {
   * 
   * 
   * 
   * if (p.equals(pOld)){ //Application.printStacktrace(""); return; }
   * 
   * 
   * pOld = p;
   * 
   * //sets the flag and mouse location for openGL picking
   * renderer.setMouseLoc(p.x,p.y,Renderer.PICKING_MODE_LABELS);
   * 
   * //calc immediately the hits renderer.display();
   * 
   * 
   * }
   */

  public void setCursor3DType(int v) {
    cursor3DType = v;
  }

  public void setDefaultCursor() {
    // Application.printStacktrace("setDefaultCursor");
    cursor = CURSOR_DEFAULT;
  }

  public void setDragCursor() {
    // Application.printStacktrace("setDragCursor");
    cursor = CURSOR_DRAG;
  }

  public void setHitCursor() {
    // Application.printStacktrace("setHitCursor");
    cursor = CURSOR_HIT;
  }

  /**
   * init the hits for this view
   * 
   * @param hits
   */
  public void setHits(Hits3D hits) {
    this.hits = hits;
  }

  // empty method : setHits3D() used instead
  public void setHits(Point p) {

  }

  public void setHits(Rectangle rect) {
    // TODO Auto-generated method stub

  }

  /**
   * sets the 3D hits regarding point location
   * 
   * @param p
   *          point location
   */
  public void setHits3D(Point p) {

    // sets the flag and mouse location for openGL picking
    renderer.setMouseLoc(p.x, p.y, Renderer.PICKING_MODE_LABELS);

    // calc immediately the hits
    // renderer.display();

  }

  /** sets EuclidianController3D mode */
  public void setMode(int mode) {
    euclidianController3D.setMode(mode);
  }

  public void setMoveCursor() {

    // Application.printStacktrace("setMoveCursor");
    cursor = CURSOR_MOVE;
  }

  public void setPreview(Previewable previewDrawable) {

    if (previewDrawable == null) {
      if (this.previewDrawable != null)
        drawList3D.remove((Drawable3D) this.previewDrawable);
    } else
      drawList3D.add((Drawable3D) previewDrawable);

    setCursor3DType(PREVIEW_POINT_ALREADY);

    this.previewDrawable = previewDrawable;

  }

  public void setRemoveHighlighting(boolean flag) {
  }

  public void setRotXY(double a, double b, boolean repaint) {

    setRotXYinDegrees(a / EuclidianController3D.ANGLE_TO_DEGREES, b
        / EuclidianController3D.ANGLE_TO_DEGREES, repaint);

  }

  // ///////////////////////////////////////
  // previewables

  public void setRotXYinDegrees(double a, double b, boolean repaint) {

    // Application.debug("setRotXY");

    this.a = a;
    this.b = b;

    if (this.b > EuclidianController3D.ANGLE_MAX)
      this.b = EuclidianController3D.ANGLE_MAX;
    else if (this.b < -EuclidianController3D.ANGLE_MAX)
      this.b = -EuclidianController3D.ANGLE_MAX;

    updateMatrix();
    // setWaitForUpdate(repaint);
    if (repaint)
      update();
  }

  public void setScale(double val) {
    scale = val;
    if (scale < scaleMin)
      scale = scaleMin;
  }

  public void setSelectionRectangle(Rectangle selectionRectangle) {
    // TODO Auto-generated method stub

  }

  public void setShowAxesRatio(boolean b) {
    // TODO Auto-generated method stub

  }

  public void setShowMouseCoords(boolean b) {
    // TODO Auto-generated method stub

  }

  public void setXZero(double val) {
    XZero = val;
  }

  public void setYZero(double val) {
    YZero = val;
  }

  public void setZZero(double val) {
    ZZero = val;
  }

  public void showAxes(boolean b, boolean showYaxis) {
    // TODO Auto-generated method stub

  }

  public double toRealWorldCoordX(double minX) {
    // TODO Auto-generated method stub
    return 0;
  }

  public double toRealWorldCoordY(double maxY) {
    // TODO Auto-generated method stub
    return 0;
  }

  final public void toSceneCoords3D(Ggb3DMatrix mInOut) {
    changeCoords(mInv, mInOut);
  }

  // ///////////////////////////////////////////////////
  // 
  // CURSOR
  //
  // ///////////////////////////////////////////////////

  final protected void toSceneCoords3D(Ggb3DVector vInOut) {
    changeCoords(mInv, vInOut);
  }

  final public void toScreenCoords3D(Ggb3DMatrix mInOut) {
    changeCoords(m, mInOut);
  }

  /**
   * Converts real world coordinates to screen coordinates.
   * 
   * @param inOut
   *          : input and output array with x, y, z, w coords (
   */
  final public void toScreenCoords3D(Ggb3DVector vInOut) {
    changeCoords(m, vInOut);
  }

  /** update the drawables for 3D view, called by EuclidianRenderer3D */
  public void update() {

    /*
     * if (waitForUpdate){
     * 
     * //picking if ((waitForPick)&&(!removeHighlighting)){
     * 
     * waitForPick = false; }
     * 
     * 
     * //other drawList3D.updateAll(); //TODO waitForUpdate for each object
     * 
     * waitForUpdate = false;
     * 
     * }
     */

    if (waitForUpdate) {
      drawList3D.updateAll();
      waitForUpdate = false;
    }

  }

  public void update(GeoElement geo) {
    if (geo.isGeoElement3D()) {
      Drawable3D d = ((GeoElement3DInterface) geo).getDrawable3D();
      if (d != null)
        ((GeoElement3DInterface) geo).getDrawable3D().update();
      // repaintView();
    }
  }

  public void updateAuxiliaryObject(GeoElement geo) {
    // TODO Raccord de méthode auto-généré

  }

  /**
   * update the 3D cursor with current hits
   */
  protected void updateCursor3D() {

    // Application.debug("hits ="+getHits().toString());

    if (hasMouse)
      getEuclidianController().updateNewPoint(true, getHits().getTopHits(),
          true, true, true, false, // TODO doSingleHighlighting = false ?
          false);

  }

  /**
   * set Matrix for view3D
   */
  protected void updateMatrix() {

    // TODO use Ggb3DMatrix4x4

    // rotations
    Ggb3DMatrix m1 = Ggb3DMatrix.Rotation3DMatrix(Ggb3DMatrix.X_AXIS, b
        * EuclidianController3D.ANGLE_TO_DEGREES - Math.PI / 2.0);
    Ggb3DMatrix m2 = Ggb3DMatrix.Rotation3DMatrix(Ggb3DMatrix.Z_AXIS, a
        * EuclidianController3D.ANGLE_TO_DEGREES);
    Ggb3DMatrix m3 = m1.mul(m2);

    undoRotationMatrix.set(m3.inverse());

    // scaling
    Ggb3DMatrix m4 = Ggb3DMatrix.ScaleMatrix(new double[]{getXscale(),
        getYscale(), getZscale()});

    // translation
    Ggb3DMatrix m5 = Ggb3DMatrix.TranslationMatrix(new double[]{getXZero(),
        getYZero(), getZZero()});

    m.set(m5.mul(m3.mul(m4)));

    mInv.set(m.inverse());

    waitForUpdate = true;

    // Application.debug("m = "); m.SystemPrint();

  }

  public void updatePreviewable() {

    getPreviewDrawable().updatePreview();
  }

  public void updateSize() {
    // TODO Auto-generated method stub

  }

  public void zoom(double px, double py, double zoomFactor, int steps,
      boolean storeUndo) {
    // TODO Auto-generated method stub

  }

}
