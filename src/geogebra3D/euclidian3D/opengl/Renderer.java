package geogebra3D.euclidian3D.opengl;





import geogebra.euclidian.EuclidianView;
import geogebra.main.Application;
import geogebra3D.Matrix.Ggb3DMatrix;
import geogebra3D.Matrix.Ggb3DMatrix4x4;
import geogebra3D.Matrix.Ggb3DVector;
import geogebra3D.euclidian3D.DrawList3D;
import geogebra3D.euclidian3D.Drawable3D;
import geogebra3D.euclidian3D.EuclidianController3D;
import geogebra3D.euclidian3D.EuclidianView3D;
import geogebra3D.euclidian3D.Hits3D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.media.opengl.glu.GLUtessellator;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.j2d.TextRenderer;


/**
 * 
 * Used for openGL display.
 * <p>
 * It provides:
 * <ul>
 * <li> methods for displaying {@link Drawable3D}, with painting parameters </li>
 * <li> methods for picking object </li>
 * </ul>
 * 
 * @author ggb3D
 * 
 * 
 * 
 */
public class Renderer implements GLEventListener {
	
	// openGL variables
	private GLU glu= new GLU();
	//private GLUT glut = new GLUT();
	private TextRenderer textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 16));
	/** default text scale factor */
	private static final float DEFAULT_TEXT_SCALE_FACTOR = 0.8f;

	/** matrix changing Y-direction to Z-direction */
	//private double[] matrixYtoZ = {1,0,0,0, 0,0,1,0, 0,1,0,0, 0,0,0,1}; 
	
	/** canvas usable for a JPanel */
	public GLCanvas canvas;
	//public GLJPanel canvas;

	private GLCapabilities caps;
	private GL gl;
	private GLUquadric quadric;
	private FPSAnimator animator;
	
	/** for polygon tesselation */
	private GLUtessellator tobj;
	
	private IntBuffer selectBuffer;
	private int BUFSIZE = 512;
	private static int MOUSE_PICK_WIDTH = 3;
	
	
	// other
	private DrawList3D drawList3D;
	
	private EuclidianView3D view3D;
	
	// for drawing
	private Ggb3DMatrix4x4 m_drawingMatrix; //matrix for drawing
	
	
	///////////////////
	//primitives
	private RendererPrimitives primitives;
	
	///////////////////
	//dash
	
    /** opengl organization of the dash textures */
    private int[] texturesDash;
    
    /** number of dash styles */
    private int DASH_NUMBER = 3;
    
	/** no dash. */
	static public int DASH_NONE = -1;
	
	/** simple dash: 1-(1), ... */
	static public int DASH_SIMPLE = 0;
	
	/** dotted dash: 1-(3), ... */
	static public int DASH_DOTTED = 1;
		
	/** dotted/dashed dash: 7-(4)-1-(4), ... */
	static public int DASH_DOTTED_DASHED = 2;
	
	/** description of the dash styles */
	static private boolean[][] DASH_DESCRIPTION = {
		{true, false}, // DASH_SIMPLE
		{true, false, false, false}, // DASH_DOTTED
		{true,true,true,true, true,true,true,false, false,false,false,true, false,false,false,false} // DASH_DOTTED_DASHED
	};
	
	
	/** # of the dash */
	private int dash = DASH_NONE; 
	
	/** scale factor for dash */
	private float dashScale = 1f;
	
	
	/////////////////////
	// spencil attributes
	
	/** current drawing color {r,g,b} */
	private Color color; 
	/** current alpha blending */
	private double alpha;
	/** current text color {r,g,b} */
	private Color textColor; 
	
	private double thickness;
	
	
	
	///////////////////
	// arrows
	
	/** no arrows */
	static final public int ARROW_TYPE_NONE=0;
	/** simple arrows */
	static final public int ARROW_TYPE_SIMPLE=1;
	private int m_arrowType=ARROW_TYPE_NONE;
	
	private double m_arrowLength, m_arrowWidth;
	
	
	///////////////////
	// dilation
	
	private static final int DILATION_NONE = 0;
	private static final int DILATION_HIGHLITED = 1;
	private int dilation = DILATION_NONE;
	private double[] dilationValues = {
			1,  // DILATION_NONE
			1.3 // DILATION_HIGHLITED
	};
	
	
	///////////////////
	// for picking
	
	private int mouseX, mouseY;
	private boolean waitForPick = false;
	private boolean doPick = false;
	public static final int PICKING_MODE_OBJECTS = 0;
	public static final int PICKING_MODE_LABELS = 1;
	private int pickingMode = PICKING_MODE_OBJECTS;
	
	/**
	 * creates a renderer linked to an {@link EuclidianView3D} 
	 * @param view the {@link EuclidianView3D} linked to 
	 */
	public Renderer(EuclidianView3D view){
		super();
		
	    caps = new GLCapabilities();
	    
	    //anti-aliasing
    	caps.setSampleBuffers(true);
        caps.setNumSamples(4);    	
        
        //avoid flickering
    	caps.setDoubleBuffered(true);	    
    	
    	//canvas
	    canvas = new GLCanvas(caps);
	    //canvas = new GLJPanel(caps);

	    
        
        
	    canvas.addGLEventListener(this);
	    
	    //animator : 60 frames per second
	    
	    
	    animator = new FPSAnimator( canvas, 60 );
        animator.setRunAsFastAsPossible(true);	  
        animator.start();
        

        //link to 3D view
		this.view3D=view;
		
		
		
	}
	
	
	/**
	 * set the list of {@link Drawable3D} to be drawn
	 * @param dl list of {@link Drawable3D}
	 */
	public void setDrawList3D(DrawList3D dl){
		drawList3D = dl;
	}
	
	
	
	/**
	 * re-calc the display immediately
	 */
	public void display(){
	
		canvas.display();
	}
	
	

	
	
	/**
	 * 
	 * openGL method called when the display is to be computed.
	 * <p>
	 * First, it calls {@link #doPick()} if a picking is to be done.
	 * Then, for each {@link Drawable3D}, it calls:
	 * <ul>
	 * <li> {@link Drawable3D#drawHidden(EuclidianRenderer3D)} to draw hidden parts (dashed segments, lines, ...) </li>
	 * <li> {@link Drawable3D#drawHighlighting(EuclidianRenderer3D)} to show objects that are picked (highlighted) </li>
	 * <li> {@link Drawable3D#drawTransp(EuclidianRenderer3D)} to draw transparent objects (planes, spheres, ...) </li>
	 * <li> {@link Drawable3D#drawSurfacesForHiding(EuclidianRenderer3D)} to draw in the z-buffer objects that hides others (planes, spheres, ...) </li>
	 * <li> {@link Drawable3D#drawTransp(EuclidianRenderer3D)} to re-draw transparent objects for a better alpha-blending </li>
	 * <li> {@link Drawable3D#draw(EuclidianRenderer3D)} to draw not hidden parts (dash-less segments, lines, ...) </li>
	 * </ul>
	 */
    public void display(GLAutoDrawable gLDrawable) {
    	
    	//Application.debug("display");

        
        gl = gLDrawable.getGL();
        
        
        
        //picking        
        if(waitForPick){
        	doPick();
        	//Application.debug("doPick");
        	//return;
        }
        
        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        
        

  

        
        
        //start drawing
        viewOrtho();
        
        
        //update 3D controller
        ((EuclidianController3D) view3D.getEuclidianController()).processMouseMoved();
        
        // update 3D view
        view3D.update();
        
        


    	
    	
        
        //init drawing matrix to view3D toScreen matrix
        gl.glLoadMatrixd(view3D.getToScreenMatrix().get(),0);
        
        
 


        //drawing the cursor
        view3D.drawCursor(this);
        
        
        
        primitives.enableVBO(gl);
        
        //drawing hidden part
        drawList3D.drawHidden(this);


        //drawing picked parts        
        gl.glDepthMask(false);
        //setMaterial(new Color(0f,0f,0f),0.75f);
        dilation = DILATION_HIGHLITED;
    	gl.glCullFace(GL.GL_FRONT); //draws inside parts
    	drawList3D.drawHighlighting(this);
        dilation = DILATION_NONE;
        gl.glCullFace(GL.GL_BACK);
        gl.glDepthMask(true);
        

        

        
        
        
        
        
        
        //drawing transparents parts
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDepthMask(false);
        drawList3D.drawTransp(this);
        gl.glDepthMask(true);

        
        //drawing labels
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_LIGHTING);
        drawList3D.drawLabel(this);
        gl.glEnable(GL.GL_LIGHTING);

        
        //drawing hiding parts
        gl.glColorMask(false,false,false,false); //no writing in color buffer		
        gl.glCullFace(GL.GL_FRONT); //draws inside parts    
        drawList3D.drawClosedSurfacesForHiding(this); //closed surfaces back-faces
        gl.glDisable(GL.GL_CULL_FACE);
        drawList3D.drawSurfacesForHiding(this); //non closed surfaces
        gl.glColorMask(true,true,true,true);


        
 

        //re-drawing transparents parts for better transparent effect
        //TODO improve it !
        gl.glDepthMask(false);
        drawList3D.drawTransp(this);
        gl.glDepthMask(true);

        
        //drawing hiding parts
        gl.glColorMask(false,false,false,false); //no writing in color buffer		
        gl.glCullFace(GL.GL_BACK); //draws inside parts
        gl.glEnable(GL.GL_CULL_FACE);
        drawList3D.drawClosedSurfacesForHiding(this); //closed surfaces front-faces
        gl.glColorMask(true,true,true,true);
        
        
        
        
        //re-drawing transparents parts for better transparent effect
        //TODO improve it !
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDepthMask(false);
        drawList3D.drawTransp(this);
        gl.glDepthMask(true);

        

       
        //drawing not hidden parts
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_BLEND);
        drawList3D.draw(this);
        gl.glEnable(GL.GL_BLEND);
        
        
        primitives.disableVBO(gl);
     
     
        

        
        
        
        /*
        //FPS
        gl.glDisable(GL.GL_LIGHTING);
        gl.glDisable(GL.GL_DEPTH_TEST);

    	drawFPS();
    	gl.glEnable(GL.GL_DEPTH_TEST);
    	gl.glEnable(GL.GL_LIGHTING);
         */

    	
    	
        gLDrawable.swapBuffers(); //TODO
        

    }    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * openGL method called when the canvas is reshaped.
     */
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
    {
      GL gl = drawable.getGL();
      
      //Application.debug("reshape\n x = "+x+"\n y = "+y+"\n w = "+w+"\n h = "+h);
      
      //TODO change this
      viewOrtho(x,y,w,h);
      /*
      gl.glViewport(0, 0, w, h);
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glOrtho(0.0, 8.0, 0.0, 8.0, -0.5, 2.5);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();
      */
    }

    /**
     * openGL method called when the display change.
     * empty method
     */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
        boolean deviceChanged)
    {
    }    
    
    
    
    
    
    
    
    
    
    
    
    
    ///////////////////////////////////////////////////
    //
    // pencil methods
    //
    /////////////////////////////////////////////////////
    
    
    
    /** sets the color of the text
     * @param c color of the text
     */
    public void setTextColor(Color c){
    	
    	textColor = c;
    	
	
    }
    
    
    /**
     * sets the material used by the pencil
     * 
     * @param c the color of the pencil
     * @param alpha the alpha value for blending
     */
    public void setMaterial(Color c, double alpha){

    	color = c;
    	this.alpha = alpha;
 
    	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, 
    			new float[] {(c.getRed())/256f,
    							(c.getGreen())/256f,
    							(c.getBlue())/256f,
    							(float) alpha},
    			0);
    	
    	/*
    	gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, 
    			new float[] {1f,
    							1f,
    							1f,
    							1f},
    			0);
    			*/
    }
    
    
    /** return (r,g,b) current color
     * @return (r,g,b) current color
     */
    public Color getColor(){
    	return color;
    }
    
    /** return current alpha
     * @return current alpha
     */
    public double getAlpha(){
    	return alpha;
    }
    
    

    
    /**
     * sets the thickness used by the pencil.
     * 
     * @param a_thickness the thickness
     */
    public void setThickness(double a_thickness){
    	this.thickness = a_thickness;
    }
    
    
    /**
     * gets the current thickness of the pencil.
     * 
     * @return the thickness
     */
    public double getThickness(){
    	return thickness;
    }
    
    //arrows
    
    /**
     * sets the type of arrow used by the pencil.
     * 
     * @param a_arrowType type of arrow, see {@link #ARROW_TYPE_NONE}, {@link #ARROW_TYPE_SIMPLE}, ... 
     */
    public void setArrowType(int a_arrowType){
    	m_arrowType = a_arrowType;
    } 
    
    /**
     * sets the width of the arrows painted by the pencil.
     * 
     * @param a_arrowWidth the width of the arrows
     */
    public void setArrowWidth(double a_arrowWidth){
    	m_arrowWidth = a_arrowWidth;
    } 
    
    
    /**
     * sets the length of the arrows painted by the pencil.
     * 
     * @param a_arrowLength the length of the arrows
     */
    public void setArrowLength(double a_arrowLength){
    	m_arrowLength = a_arrowLength;
    } 
    
    
    
    
    //layer
    /**
     * sets the layer to l. Use gl.glPolygonOffset( ).
     * @param l the layer
     */
    public void setLayer(float l){
    	
    	// 0<=l<10
    	// l2-l1>=1 to see something
    	gl.glPolygonOffset(-l*0.05f, -l*10);
    }
    
    
    
    
    
    
    //drawing matrix
    
    /**
     * sets the matrix in which coord sys the pencil draws.
     * 
     * @param a_matrix the matrix
     */
    public void setMatrix(Ggb3DMatrix4x4 a_matrix){
    	m_drawingMatrix=a_matrix;
    }
    
    
    /**
     * gets the matrix describing the coord sys used by the pencil.
     * 
     * @return the matrix
     */
    public Ggb3DMatrix4x4 getMatrix(){
    	return m_drawingMatrix;
    }
    
    
    /**
     * sets the drawing matrix to openGL.
     * same as initMatrix(m_drawingMatrix)
     */
    private void initMatrix(){
    	initMatrix(m_drawingMatrix);
    }
    
    /**
     * sets a_drawingMatrix to openGL.
     * @param a_drawingMatrix the matrix
     */
    private void initMatrix(Ggb3DMatrix a_drawingMatrix){
    	initMatrix(a_drawingMatrix.get());
    }   
    
    
    /**
     * sets a_drawingMatrix to openGL.
     * @param a_drawingMatrix the matrix
     */
    private void initMatrix(double[] a_drawingMatrix){
    	gl.glPushMatrix();
		gl.glMultMatrixd(a_drawingMatrix,0);
    }     
    

    
    /**
     * turn off the last drawing matrix set in openGL.
     */
    private void resetMatrix(){
    	gl.glPopMatrix();
    }
    
    
    
    ////////////////////////////////////////////
    //
    // TEXTURES
    //
    ////////////////////////////////////////////
    

    
    private void initTextures(){
    	
    	gl.glEnable(GL.GL_TEXTURE_2D);
    	
    	
    	
    	// dash textures
    	texturesDash = new int[DASH_NUMBER];
        gl.glGenTextures(DASH_NUMBER, texturesDash, 0);
        for(int i=0; i<DASH_NUMBER; i++)
        	initDashTexture(texturesDash[i],DASH_DESCRIPTION[i]);
         
        
        
        gl.glDisable(GL.GL_TEXTURE_2D);
    }
    
    
    // dash
    
    private void initDashTexture(int n, boolean[] description){
    	
        //int sizeX = 1; 
        //int sizeY = description.length;
        int sizeX = description.length; 
        int sizeY = 1;
        
        byte[] bytes = new byte[4*sizeX*sizeY];
        
        // if description[i]==true, then texture is white opaque, else is transparent
        for (int i=0; i<sizeX; i++)
        	if (description[i])      		
        		bytes[4*i+0]=
        			bytes[4*i+1]= 
        				bytes[4*i+2]= 
        					bytes[4*i+3]= (byte) 255;
              
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        gl.glBindTexture(GL.GL_TEXTURE_2D, n);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MIN_FILTER,GL.GL_NEAREST);
        
        
        //TODO use gl.glTexImage1D
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0,  4, sizeX, sizeY, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buf);
        
    }
    

    
    /**
     * sets the dash used by the pencil.
     * 
     * @param dash # of the dash, see EuclidianView, ...
     */
    public void setDash(int dash){
    	
    	
    	switch (dash) {
		case EuclidianView.LINE_TYPE_DOTTED:
			this.dash=DASH_DOTTED;
			dashScale = 0.08f;
			break;

		case EuclidianView.LINE_TYPE_DASHED_SHORT:
			this.dash=DASH_SIMPLE;
			dashScale = 0.08f;
			break;

		case EuclidianView.LINE_TYPE_DASHED_LONG:
			this.dash=DASH_SIMPLE;
			dashScale = 0.04f;
			break;

		case EuclidianView.LINE_TYPE_DASHED_DOTTED:
			this.dash=DASH_DOTTED_DASHED;
			dashScale = 0.04f;
			break;

		default: // EuclidianView.LINE_TYPE_FULL
			this.dash = DASH_NONE;
		}
    	
    }
    
    
    
    
    ///////////////////////////////////////////////////////////
    //drawing geometries
    
    /**
     * draws a segment from x=x1 to x=x2 according to drawing matrix
     * 
     * @param a_x1 start of the segment
     * @param a_x2 end of the segment
     * 
     */
    public void drawSegment(double a_x1, double a_x2){

    	switch(m_arrowType){
    	case ARROW_TYPE_NONE:
    	default:
    		drawSegment(a_x1, a_x2, dash!=DASH_NONE);
    	break;
    	case ARROW_TYPE_SIMPLE:
    		double x3=a_x2-m_arrowLength/m_drawingMatrix.getUnit(Ggb3DMatrix4x4.X_AXIS);
    		double thickness = getThickness();
    		setThickness(m_arrowWidth);
    		drawCone(x3,a_x2);
    		setThickness(thickness);
    		if (x3>a_x1)
    			drawSegment(a_x1, x3, dash!=DASH_NONE);
    		break;
    	}

    } 
    
    
    
    private void drawSegment(double a_x1, double a_x2, boolean dashed){


    	initMatrix(m_drawingMatrix.segmentX(a_x1, a_x2));
    	
    	if (dashed){
    		gl.glEnable(GL.GL_TEXTURE_2D);
    		//TODO use object properties
    		gl.glBindTexture(GL.GL_TEXTURE_2D, texturesDash[dash]);


    		gl.glMatrixMode(GL.GL_TEXTURE);
    		gl.glLoadIdentity();
    		float b = (float) (dashScale*(a_x2-a_x1)*m_drawingMatrix.getUnit(Ggb3DMatrix4x4.X_AXIS)*view3D.getScale());
    		float a = 0.75f/b-0.5f;
    		
    		gl.glScalef(b,1f,1f);
    		gl.glTranslatef(a,0f,0f);
    		
    		gl.glMatrixMode(GL.GL_MODELVIEW);
    	}
    	
    	double s = thickness*dilationValues[dilation]/view3D.getScale();
    	gl.glScaled(1,s,s);
       	primitives.segment(gl, (int) thickness);
    	
       	if (dashed)
       		gl.glDisable(GL.GL_TEXTURE_2D);
    	
    	resetMatrix();

    }
   
    
    /** 
     * draws a segment from x=0 to x=1 according to current drawing matrix.
     */
    public void drawSegment(){
    	drawSegment(0,1);
    }
    
    
    
    
    
    
    /**
     * draws "coordinates segments" from the point origin of the drawing matrix to the axes
     * @param axisX color of the x axis
     * @param axisY color of the y axis
     * @param axisZ color of the z axis
     */
    public void drawCoordSegments(Color axisX, Color axisY, Color axisZ){
    	
    	Ggb3DMatrix4x4 drawingMatrixOld = m_drawingMatrix;
    	Color colorOld = getColor();
    	double alphaOld = getAlpha();
 

    	Ggb3DMatrix4x4 matrix = new Ggb3DMatrix4x4();
    	matrix.setOrigin(m_drawingMatrix.getOrigin());
    	matrix.set(3,4,0); //sets the origin's altitude to 0
    	
    	// z-segment
    	double altitude = m_drawingMatrix.getOrigin().get(3);//altitude du point
 
    	matrix.setVx((Ggb3DVector) EuclidianView3D.vz.mul(altitude));
    	
    	if(altitude>0){
    		matrix.setVy(EuclidianView3D.vx);
    		matrix.setVz(EuclidianView3D.vy);
    	}else{
    		matrix.setVy(EuclidianView3D.vy);
    		matrix.setVz(EuclidianView3D.vx);
    	}
    	
    	setMaterial(axisZ, 1);
    	setMatrix(matrix);
    	drawSegment();
    	resetMatrix();
    	
    	
    	
    	
    	// x-segment  	
    	double x = m_drawingMatrix.getOrigin().get(1);//x-coord of the point
    	
    	matrix.setVx((Ggb3DVector) EuclidianView3D.vx.mul(-x));
    	
    	if(x>0){
    		matrix.setVy(EuclidianView3D.vz);
    		matrix.setVz(EuclidianView3D.vy);
    	}else{
    		matrix.setVy(EuclidianView3D.vy);
    		matrix.setVz(EuclidianView3D.vz);
    	}
    	
    	setMaterial(axisX, 1);
    	setMatrix(matrix);
    	drawSegment();
    	resetMatrix();
    	
    	
    	// y-segment  	
    	double y = m_drawingMatrix.getOrigin().get(2);//y-coord of the point
    	
    	matrix.setVx((Ggb3DVector) EuclidianView3D.vy.mul(-y));
    	
    	if(y>0){
    		matrix.setVy(EuclidianView3D.vx);
    		matrix.setVz(EuclidianView3D.vz);
    	}else{
    		matrix.setVy(EuclidianView3D.vz);
    		matrix.setVz(EuclidianView3D.vx);
    	}
    	
    	setMaterial(axisY, 1);
    	setMatrix(matrix);
    	drawSegment();
    	resetMatrix();
    	
    	
    	
    	
    	// reset the drawing matrix and color
    	setMatrix(drawingMatrixOld);
    	setMaterial(colorOld, alphaOld);
    	
    }
    
    
    
    
    /** 
     * draws a ray (half-line) according drawing matrix.
     */
    /*
    public void drawRay(){
    	//TODO use frustum
    	drawSegment(0,21);
    }  
    */
    
    
    
    
    
    
    
    /** draws a cone from x=x1 to x=x2 according to current drawing matrix.
     * @param a_x1 x-coordinate of the basis
     * @param a_x2 x-coordinate of the top
     */
    public void drawCone(double a_x1, double a_x2){
    	initMatrix(m_drawingMatrix.segmentX(a_x1, a_x2));
    	drawCone(thickness);
    	resetMatrix();
    } 
 
    
    
    
    
    

    
    /** draws a quad according to current drawing matrix.  
     * @param a_x1 x-coordinate of the top-left corner
     * @param a_y1 y-coordinate of the top-left corner
     * @param a_x2 x-coordinate of the bottom-right corner
     * @param a_y2 y-coordinate of the bottom-right corner
     */
    public void drawQuad(double a_x1, double a_y1, double a_x2, double a_y2){
    	initMatrix(m_drawingMatrix.quad(a_x1, a_y1, a_x2, a_y2));
    	drawQuad();
    	resetMatrix();
    }
    
    /*
    public void drawQuad2(double a_x1, double a_y1, double a_x2, double a_y2){
    	initMatrix(m_drawingMatrix.quad(a_x1, a_y1, a_x2, a_y2));
    	drawQuad2();
    	resetMatrix();
    }
    
    private void drawQuad2(){    	
    	 
       	gl.glDisable(GL.GL_CULL_FACE);	
       	gl.glEnable(GL.GL_TEXTURE_2D);
    	
        gl.glBegin(GL.GL_QUADS);	
        
        
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
        
        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        
        
        float v = 2f;
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        
        gl.glTexCoord2f(v, 0.0f);
        gl.glVertex3f(1.0f, 0.0f, 0.0f);
        
        gl.glTexCoord2f(v, v);
        gl.glVertex3f(1.0f, 1.0f, 0.0f);	
        
        gl.glTexCoord2f(0.0f, v);
        gl.glVertex3f(0.0f, 1.0f, 0.0f);
              
        gl.glEnd();		
        
	   	gl.glEnable(GL.GL_CULL_FACE);
	   	gl.glDisable(GL.GL_TEXTURE_2D);
       
    }
    */
    
    /** draws a grid according to current drawing matrix.
     * @param a_x1 x-coordinate of the top-left corner
     * @param a_y1 y-coordinate of the top-left corner
     * @param a_x2 x-coordinate of the bottom-right corner
     * @param a_y2 y-coordinate of the bottom-right corner
     * @param a_dx distance between two x-lines
     * @param a_dy distance between two y-lines
     */
    public void drawGrid(double a_x1, double a_y1, 
    		double a_x2, double a_y2, 
    		double a_dx, double a_dy){
    	
    	double xmin, xmax;
    	if (a_x1<a_x2){
    		xmin=a_x1;
    		xmax=a_x2;
    	}else{
    		xmin=a_x2;
    		xmax=a_x1;
    	}
    	
    	double ymin, ymax;
    	if (a_y1<a_y2){
    		ymin=a_y1;
    		ymax=a_y2;
    	}else{
    		ymin=a_y2;
    		ymax=a_y1;
    	}
    	
    	
    	int nXmin= (int) Math.ceil(xmin/a_dx);
    	int nXmax= (int) Math.floor(xmax/a_dx);
    	int nYmin= (int) Math.ceil(ymin/a_dy);
    	int nYmax= (int) Math.floor(ymax/a_dy);
    	
    	//Application.debug("n = "+nXmin+","+nXmax+","+nYmin+","+nYmax);
    	
    	Ggb3DMatrix4x4 matrix = new Ggb3DMatrix4x4();
     	
    	matrix.set(getMatrix());
    	for (int i=nYmin; i<=nYmax; i++){
    		setMatrix(matrix.translateY(i*a_dy));
        	drawSegment(xmin, xmax);
    	}
    	setMatrix(matrix);
    	
    	matrix.set(getMatrix());
    	Ggb3DMatrix4x4 matrix2 = matrix.mirrorXY();
    	for (int i=nXmin; i<=nXmax; i++){
    		setMatrix(matrix2.translateY(i*a_dx));
        	drawSegment(ymin, ymax);
    	}
    	setMatrix(matrix);     		

    	 
    }  
    
    
    /**
     * draws a sphere according to current drawing matrix.
     * 
     * @param radius radius of the sphere
     */
    public void drawSphere(float radius){
    	initMatrix();
    	primitives.drawSphere(gl,radius/view3D.getScale(),16,16);
    	resetMatrix();
    }
    
    /**
     * draws a point according to current drawing matrix.
     * 
     * @param radius radius of the point
     */
    public void drawPoint(int size){
    	initMatrix();
    	double s = size*dilationValues[dilation]/view3D.getScale();
    	gl.glScaled(s,s,s);
    	primitives.point(gl,size);
    	resetMatrix();
    	
    	
    }
    

 
    
    
    
    /** draws a cross cursor using size and getThickness() parameters
     * @param size size of the cross
     */    
    public void drawCursorCross(double size){
    	
    	
    	double thickness = getThickness()/2;
    	
    	double[][] cross = {
    	    	{size, thickness},
    	       	{thickness, thickness},
    	       	{thickness, size},
    	       	{-thickness, size},
    	       	{-thickness, thickness},
    	       	{-size, thickness},
    	       	
    	       	{-size, -thickness},
    	       	{-thickness, -thickness},
    	       	{-thickness, -size},
    	       	{thickness, -size},
    	       	{thickness, -thickness},
    	    	{size, -thickness}
    	};
    	
    	double thickness2 = thickness;
    	
    	double[][] outlineCross = {
    	    	{size+thickness2, thickness+thickness2},
    	       	{thickness+thickness2, thickness+thickness2},
    	       	{thickness+thickness2, size+thickness2},
    	       	{-thickness-thickness2, size+thickness2},
    	       	{-thickness-thickness2, thickness+thickness2},
    	       	{-size-thickness2, thickness+thickness2},
    	       	
    	       	{-size-thickness2, -thickness-thickness2},
    	       	{-thickness-thickness2, -thickness-thickness2},
    	       	{-thickness-thickness2, -size-thickness2},
    	       	{thickness+thickness2, -size-thickness2},
    	       	{thickness+thickness2, -thickness-thickness2},
    	    	{size+thickness2, -thickness-thickness2}
    	};
    	
    	
    	gl.glDisable(GL.GL_LIGHTING);
    	
    	initMatrix();
    	
    	
    	// the cross itself
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	startPolygon();   	    	
    	for (int i=0; i<cross.length; i++)
    		addToPolygon(cross[i][0], cross[i][1], thickness);
    	endPolygon();
    	
    	// the cross itself (back)
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	startPolygon();   	    	
    	for (int i=cross.length-1; i>=0; i--)
    		addToPolygon(cross[i][0], cross[i][1], -thickness);
    	endPolygon(); 
    	
    	
    	
    	// outline
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	startPolygon();   	    	
    	for (int i=0; i<cross.length; i++)
    		addToPolygon(cross[i][0], cross[i][1], thickness);
    	addToPolygon(cross[0][0], cross[0][1], thickness);
    	for (int i=0; i<outlineCross.length; i++)
    		addToPolygon(outlineCross[i][0], outlineCross[i][1], thickness);
    	addToPolygon(outlineCross[0][0], outlineCross[0][1], thickness);
    	endPolygon();   	
    	

    	// outline (back)
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	startPolygon();   	    
    	addToPolygon(cross[0][0], cross[0][1], -thickness);
    	for (int i=cross.length-1; i>=0; i--)
    		addToPolygon(cross[i][0], cross[i][1], -thickness); 
    	addToPolygon(outlineCross[0][0], outlineCross[0][1], -thickness);
    	for (int i=cross.length-1; i>=0; i--)
    		addToPolygon(outlineCross[i][0], outlineCross[i][1], -thickness);    	
    	endPolygon();     	
    	
    	
    	// edges
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	for (int i=0; i<outlineCross.length-1; i++){
    		startPolygon();
    		addToPolygon(outlineCross[i][0], outlineCross[i][1], thickness);
    		addToPolygon(outlineCross[i][0], outlineCross[i][1], -thickness);
    		addToPolygon(outlineCross[i+1][0], outlineCross[i+1][1], -thickness);
    		addToPolygon(outlineCross[i+1][0], outlineCross[i+1][1], thickness);  		
    		endPolygon();
    	}  	
		startPolygon();
		addToPolygon(outlineCross[outlineCross.length-1][0], outlineCross[outlineCross.length-1][1], thickness);
		addToPolygon(outlineCross[outlineCross.length-1][0], outlineCross[outlineCross.length-1][1], -thickness);
		addToPolygon(outlineCross[0][0], outlineCross[0][1], -thickness);
		addToPolygon(outlineCross[0][0], outlineCross[0][1], thickness);  		
		endPolygon();
    	
    	
		resetMatrix();
    	
    	gl.glEnable(GL.GL_LIGHTING);
    	
    }
    
    
    
    /** draws a cylinder cursor using size and getThickness() parameters
     * @param size size of the cross
     */    
    public void drawCursorCylinder(double size){
 
    	int latitude = 16;
    	float center = 0.5f;
    	
    	gl.glDisable(GL.GL_LIGHTING);  
    	
    	initMatrix();
    	gl.glScalef((float) size, 1f, 1f);
    	
    	// top - black
    	gl.glPushMatrix();
    	gl.glTranslatef(2f, 0, 0);
    	setLayer(0);
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	drawDisc(getThickness(), latitude);
    	gl.glPopMatrix();
    	
    	// top - white
    	
    	gl.glPushMatrix();
    	gl.glTranslatef(2f, 0, 0);
    	setLayer(10);
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	drawDisc(getThickness()*center, latitude);
    	gl.glPopMatrix();
    	
    	
    	// cylinder - black and white
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	gl.glPushMatrix();
    	gl.glTranslatef(-2f, 0, 0);
    	drawCylinder(getThickness(), latitude);
    	gl.glPopMatrix();
    	
    	gl.glPushMatrix();
    	gl.glTranslatef(1f, 0, 0);
    	drawCylinder(getThickness(), latitude);
    	gl.glPopMatrix();
    	
    	gl.glPushMatrix();
    	gl.glScalef(2f, 1f, 1f);
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	gl.glTranslatef(-0.5f, 0, 0);
    	drawCylinder(getThickness(), latitude);   	
    	gl.glPopMatrix();
 

    	
    	// bottom - black
    	gl.glPushMatrix();
    	gl.glScalef(1f, 1, -1);
    	gl.glTranslatef(-2f, 0, 0);
    	setLayer(0);
    	gl.glColor3f(0.0f, 0.0f, 0.0f);
    	drawDisc(getThickness(), latitude);	
    	gl.glPopMatrix();
    	
    	// bottom - white  	
    	gl.glScalef(1f, 1, -1);
    	gl.glTranslatef(-2f, 0, 0);
    	setLayer(10);
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	drawDisc(getThickness()*center, latitude);	
    	
    	setLayer(0);
    	
    	
    	resetMatrix();
    	
    	gl.glEnable(GL.GL_LIGHTING);

    	
    }
    
    
    

    
    /** draws a diamond cursor using getThickness() parameters
     * @param size size of the cross
     */    
    public void drawCursorDiamond(){
 
    	float t1 = 0.15f;
    	float t2 = 1f-2*t1;
    	
    	gl.glDisable(GL.GL_LIGHTING);  
    	
    	initMatrix();
        gl.glScaled(getThickness(), getThickness(), getThickness());
  	
        gl.glBegin(GL.GL_TRIANGLES);	
        
        




        
        //white, center of faces
        gl.glColor3f(1f, 1f, 1f);
        
        gl.glVertex3f(t2, t1, t1);	
        gl.glVertex3f(t1, t2, t1);	
        gl.glVertex3f(t1, t1, t2);
 
        gl.glVertex3f(t2, -t1, -t1);	
        gl.glVertex3f(t1, -t2, -t1);	
        gl.glVertex3f(t1, -t1, -t2);

        gl.glVertex3f(-t2, t1, -t1);	
        gl.glVertex3f(-t1, t2, -t1);	
        gl.glVertex3f(-t1, t1, -t2);

        gl.glVertex3f(-t2, -t1, t1);	
        gl.glVertex3f(-t1, -t2, t1);	
        gl.glVertex3f(-t1, -t1, t2);
        
        
        

        gl.glVertex3f(t2, t1, -t1);	
        gl.glVertex3f(t1, t1, -t2);
        gl.glVertex3f(t1, t2, -t1);	
        
        
        gl.glVertex3f(t2, -t1, t1);	
        gl.glVertex3f(t1, -t1, t2);
        gl.glVertex3f(t1, -t2, t1);	

        gl.glVertex3f(-t2, t1, t1);	
        gl.glVertex3f(-t1, t1, t2);
        gl.glVertex3f(-t1, t2, t1);     
        
        gl.glVertex3f(-t2, -t1, -t1);	
        gl.glVertex3f(-t1, -t1, -t2);
        gl.glVertex3f(-t1, -t2, -t1);         
        
        
        gl.glEnd();	
    	
        
        gl.glBegin(GL.GL_QUADS);	
        
        
        //black - outline of faces
        gl.glColor3f(0f, 0f, 0f);
        
        gl.glVertex3f(1f, 0f, 0f);	        
        gl.glVertex3f(t2, t1, t1);	        
        gl.glVertex3f(t1, t1, t2);
        gl.glVertex3f(0f, 0f, 1f);
        
        gl.glVertex3f(0f, 0f, 1f);
        gl.glVertex3f(t1, t1, t2);
        gl.glVertex3f(t1, t2, t1);	
        gl.glVertex3f(0f, 1f, 0f);	
        
        gl.glVertex3f(0f, 1f, 0f);	
        gl.glVertex3f(t1, t2, t1);	
        gl.glVertex3f(t2, t1, t1);	        
        gl.glVertex3f(1f, 0f, 0f);
        
        
        gl.glVertex3f(1f, -0f, -0f);	        
        gl.glVertex3f(t2, -t1, -t1);	        
        gl.glVertex3f(t1, -t1, -t2);
        gl.glVertex3f(0f, -0f, -1f);
        
        gl.glVertex3f(0f, -0f, -1f);
        gl.glVertex3f(t1, -t1, -t2);
        gl.glVertex3f(t1, -t2, -t1);	
        gl.glVertex3f(0f, -1f, -0f);	
        
        gl.glVertex3f(0f, -1f, -0f);	
        gl.glVertex3f(t1, -t2, -t1);	
        gl.glVertex3f(t2, -t1, -t1);	        
        gl.glVertex3f(1f, -0f, -0f);	  
 
        
        gl.glVertex3f(-1f, -0f, 0f);	        
        gl.glVertex3f(-t2, -t1, t1);	        
        gl.glVertex3f(-t1, -t1, t2);
        gl.glVertex3f(-0f, -0f, 1f);
        
        gl.glVertex3f(-0f, -0f, 1f);
        gl.glVertex3f(-t1, -t1, t2);
        gl.glVertex3f(-t1, -t2, t1);	
        gl.glVertex3f(-0f, -1f, 0f);	
        
        gl.glVertex3f(-0f, -1f, 0f);	
        gl.glVertex3f(-t1, -t2, t1);	
        gl.glVertex3f(-t2, -t1, t1);	        
        gl.glVertex3f(-1f, -0f, 0f);	  

        
        gl.glVertex3f(-1f, 0f, -0f);	        
        gl.glVertex3f(-t2, t1, -t1);	        
        gl.glVertex3f(-t1, t1, -t2);
        gl.glVertex3f(-0f, 0f, -1f);
        
        gl.glVertex3f(-0f, 0f, -1f);
        gl.glVertex3f(-t1, t1, -t2);
        gl.glVertex3f(-t1, t2, -t1);	
        gl.glVertex3f(-0f, 1f, -0f);	
        
        gl.glVertex3f(-0f, 1f, -0f);	
        gl.glVertex3f(-t1, t2, -t1);	
        gl.glVertex3f(-t2, t1, -t1);	        
        gl.glVertex3f(-1f, 0f, -0f);	  
        
        
        
        
        gl.glVertex3f(0f, 0f, -1f);
        gl.glVertex3f(t1, t1, -t2);
        gl.glVertex3f(t2, t1, -t1);	        
        gl.glVertex3f(1f, 0f, -0f);	        
        
        gl.glVertex3f(0f, 1f, -0f);	
        gl.glVertex3f(t1, t2, -t1);	
        gl.glVertex3f(t1, t1, -t2);
        gl.glVertex3f(0f, 0f, -1f);
        
        gl.glVertex3f(1f, 0f, -0f);	        
        gl.glVertex3f(t2, t1, -t1);	        
        gl.glVertex3f(t1, t2, -t1);	
        gl.glVertex3f(0f, 1f, -0f);	

 
        gl.glVertex3f(0f, -0f, 1f);
        gl.glVertex3f(t1, -t1, t2);
        gl.glVertex3f(t2, -t1, t1);	        
        gl.glVertex3f(1f, -0f, 0f);	        
        
        gl.glVertex3f(0f, -1f, 0f);	
        gl.glVertex3f(t1, -t2, t1);	
        gl.glVertex3f(t1, -t1, t2);
        gl.glVertex3f(0f, -0f, 1f);
        
        gl.glVertex3f(1f, -0f, 0f);	        
        gl.glVertex3f(t2, -t1, t1);	        
        gl.glVertex3f(t1, -t2, t1);	
        gl.glVertex3f(0f, -1f, 0f);	

        
        gl.glVertex3f(-0f, 0f, 1f);
        gl.glVertex3f(-t1, t1, t2);
        gl.glVertex3f(-t2, t1, t1);	        
        gl.glVertex3f(-1f, 0f, 0f);	        
        
        gl.glVertex3f(-0f, 1f, 0f);	
        gl.glVertex3f(-t1, t2, t1);	
        gl.glVertex3f(-t1, t1, t2);
        gl.glVertex3f(-0f, 0f, 1f);
        
        gl.glVertex3f(-1f, 0f, 0f);	        
        gl.glVertex3f(-t2, t1, t1);	        
        gl.glVertex3f(-t1, t2, t1);	
        gl.glVertex3f(-0f, 1f, 0f);	    
        
        
        gl.glVertex3f(-0f, -0f, -1f);
        gl.glVertex3f(-t1, -t1, -t2);
        gl.glVertex3f(-t2, -t1, -t1);	        
        gl.glVertex3f(-1f, -0f, -0f);	        
        
        gl.glVertex3f(-0f, -1f, -0f);	
        gl.glVertex3f(-t1, -t2, -t1);	
        gl.glVertex3f(-t1, -t1, -t2);
        gl.glVertex3f(-0f, -0f, -1f);
        
        gl.glVertex3f(-1f, -0f, -0f);	        
        gl.glVertex3f(-t2, -t1, -t1);	        
        gl.glVertex3f(-t1, -t2, -t1);	
        gl.glVertex3f(-0f, -1f, -0f);	
        
        gl.glEnd();	
        
        
    	
    	resetMatrix();
    	
    	gl.glEnable(GL.GL_LIGHTING);

    	
    }
    
    /**
     * set the tesselator to start drawing a new polygon
     * and inits the matrix
     */
    public void startPolygonAndInitMatrix(){
    	initMatrix();
    	startPolygon();
    }  
    
   
    
    
    /**
     * set the tesselator to start drawing a new polygon
     * @param cullFace says if the faces have to be culled
     */
    private void startPolygon(){
    	
 
    	
	    RendererTesselCallBack tessCallback = new RendererTesselCallBack(gl, glu);

	    
	    tobj = glu.gluNewTess();

	    glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);// vertexCallback);
	    glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);// beginCallback);
	    glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);// endCallback);
	    glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);// errorCallback);
	    glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);// combineCallback);
	    
	    //TODO glu.gluTessNormal(tobj, 0, 0, 1);

    	gl.glNormal3f(0, 0, 1);
    	
	    //gl.glShadeModel(GL.GL_SMOOTH);
	    glu.gluTessBeginPolygon(tobj, null);
	    glu.gluTessBeginContour(tobj);
	    

    }
    
    
    /** add the (x,y) point as a new vertex for the current polygon
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void addToPolygon(double x, double y){
    	//double[] point = {x,y,0};//new double
    	//glu.gluTessVertex(tobj, point, 0, point);
    	addToPolygon(x, y, 0);
    }
    
    //TODO remove this
    /** add the (x,y,z) point as a new vertex for the current polygon
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public void addToPolygon(double x, double y, double z){
    	double[] point = {x,y,z};//new double
    	glu.gluTessVertex(tobj, point, 0, point);
    }    
    
    

    
    /**
     * end of the current polygon
     * @param cullFace says if the faces have been culled
     */
    private void endPolygon(){
	    glu.gluTessEndContour(tobj);
	    glu.gluTessEndPolygon(tobj);
	    
	    glu.gluDeleteTess(tobj);
        
	
    }
    
    /**
     * end of the current polygon
     * and reset the matrix
     */
    public void endPolygonAndResetMatrix(){

    	endPolygon();
    	resetMatrix(); 
    }
    
    
    /**
     * draw a circle with center (x,y) and radius R
     * @param x x coord of the center
     * @param y y coord of the center
     * @param R radius
     */
    public void drawCircle(double x, double y, double R){

    	initMatrix();
    	drawCircleArcDashedOrNot((float) x, (float) y, (float) R, 0, 2f * (float) Math.PI, dash!=0);
    	resetMatrix();
    }
    
    
    
    /**
     * draw a circle with center (x,y) and radius R
     * @param x x coord of the center
     * @param y y coord of the center
     * @param R radius
     * @param startAngle starting angle for the arc
     * @param endAngle ending angle for the arc
     * @param dash says if the circle is dashed
     */
    private void drawCircleArcDashedOrNot(float x, float y, float R, float startAngle, float endAngle, boolean dash){
    	
    	if (!dash)
    		drawCircleArcNotDashed(x, y, R, startAngle, endAngle);
    	else
    		drawCircleArcNotDashed(x,y,R, startAngle, endAngle);
    }
    
    /**
     * draw a dashed circle with center (x,y) and radius R
     * @param x x coord of the center
     * @param y y coord of the center
     * @param R radius
     * @param startAngle starting angle for the arc
     * @param endAngle ending angle for the arc
     */
    
    /*
    private void drawCircleArcDashed(float x, float y, float R, float startAngle, float endAngle){
    	
    	m_dash_factor = 1/(R*m_drawingMatrix.getUnit(Ggb3DMatrix4x4.X_AXIS));
    	for(double l1=startAngle; l1<endAngle;){
    		double l2=l1;
    		for(int i=0; (i<m_dash.length)&&(l1<endAngle); i++){
    			l2=l1+m_dash_factor*m_dash[i][0];
    			if (l2>endAngle) l2=endAngle;
    			//Application.debug("l1,l2="+l1+","+l2);
    			drawCircleArcNotDashed(x,y,R,(float) l1, (float) l2);
    			l1=l2+m_dash_factor*m_dash[i][1];
    		}	
    	} 	
    }  
    */ 
    
    /**
     * draw a dashed circle with center (x,y) and radius R
     * @param x x coord of the center
     * @param y y coord of the center
     * @param R radius
     * @param startAngle starting angle for the arc
     * @param endAngle ending angle for the arc
     */
    private void drawCircleArcNotDashed(float x, float y, float R, float startAngle, float endAngle){
    	int nsides = 16; //TODO use thickness

    	int rings = (int) (60*(endAngle-startAngle)) +2;
    	drawTorusArc(x, y, R, startAngle, endAngle, nsides, rings);
    }

    
    
    
    
    
    

    
    
    /**
     * draw a torus arc (using getThickness() for thickness)
     * @param x x coord of the center of the torus
     * @param y y coord of the center of the torus
     * @param R radius of the torus
     * @param startAngle starting angle for the arc
     * @param endAngle ending angle for the arc
     * @param nsides number of sides in a ring
     * @param rings number of rings
     */
    private void drawTorusArc(float x, float y, float R, float startAngle, float endAngle, int nsides, int rings) {
    	
    	float r = (float) getThickness();
    	
        float ringDelta = (endAngle-startAngle) / rings;
        float sideDelta = 2.0f * (float) Math.PI / nsides;
        float theta = startAngle; 
        float cosTheta = (float) Math.cos(theta); 
        float sinTheta = (float) Math.sin(theta);
        for (int i = rings - 1; i >= 0; i--) {
          float theta1 = theta + ringDelta;
          float cosTheta1 = (float) Math.cos(theta1);
          float sinTheta1 = (float) Math.sin(theta1);
          gl.glBegin(GL.GL_QUAD_STRIP);
          float phi = 0.0f;
          for (int j = nsides; j >= 0; j--) {
            phi += sideDelta;
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float dist = R + r * cosPhi;
            gl.glNormal3f(cosTheta1 * cosPhi, sinTheta1 * cosPhi, -sinPhi);
            gl.glVertex3f(x+cosTheta1 * dist, y+sinTheta1 * dist, r * -sinPhi);
            gl.glNormal3f(cosTheta * cosPhi, sinTheta * cosPhi, -sinPhi);
            gl.glVertex3f(x+cosTheta * dist, y+sinTheta * dist, r * -sinPhi);
          }
          gl.glEnd();
          theta = theta1;
          cosTheta = cosTheta1;
          sinTheta = sinTheta1;
        }
      }

   
    ///////////////////////////////////////////////////////////
    //drawing primitives TODO use VBO
    

    private void drawCylinder(double radius){
    	drawCylinder(radius,8);
    }
    
    private void drawCylinder(double radius, int latitude){
     	
    	gl.glScaled(1, radius, radius);

    	float dt = (float) 1/latitude;
    	float da = (float) (2*Math.PI *dt) ; 
    	gl.glBegin(GL.GL_QUADS); 
    	
    	for( int i = 0; i < latitude + 1 ; i++ ) { 
    		float y0 = (float) Math.sin ( i * da ); 
    		float z0 = (float) Math.cos ( i * da ); 
    		float y1 = (float) Math.sin ( (i+1) * da ); 
    		float z1 = (float) Math.cos ( (i+1) * da ); 

    		gl.glTexCoord2f(0,i*dt);
    		gl.glNormal3f(0,y0,z0); 
    		gl.glVertex3f(0,y0,z0); 


    		gl.glTexCoord2f(1,i*dt);
    		gl.glNormal3f(1,y0,z0); 
    		gl.glVertex3f(1,y0,z0); 

    		gl.glTexCoord2f(1,(i+1)*dt);
    		gl.glNormal3f(1,y1,z1); 
    		gl.glVertex3f(1,y1,z1); 

    		gl.glTexCoord2f(0,(i+1)*dt);
    		gl.glNormal3f(0,y1,z1); 
    		gl.glVertex3f(0,y1,z1); 

    		
    	} 
    	gl.glEnd();  
    }
    
    private void drawDisc(double radius, int latitude){
     	
    	gl.glScaled(1, radius, radius);

    	float dt = (float) 1/latitude;
    	float da = (float) (2*Math.PI *dt) ; 
    	
    	startPolygon();
    	
    	
    	for( int i = 0; i < latitude ; i++ ) { 
    		float y = (float) Math.cos ( i * da ); 
    		float z = (float) Math.sin ( i * da ); 

    		addToPolygon(0, y, z);
    		
    	} 
    	
    	endPolygon();
    	 
    }
    
    
    
    
    private void drawCone(double a_thickness){
    	gl.glRotatef(90f, 0.0f, 1.0f, 0.0f); //switch z-axis to x-axis
    	glu.gluCylinder(quadric, a_thickness, 0, 1.0f, 8, 1);
    }   
    
    
    
    public void drawTriangle(){    	
    	initMatrix();
    	
        gl.glBegin(GL.GL_TRIANGLES);	
        
        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);	
        gl.glVertex3f(1.0f, 0.0f, 0.0f);	
        gl.glVertex3f(0.0f, 1.0f, 0.0f);
                
        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);	
        gl.glVertex3f(0.0f, 1.0f, 0.0f);	
        gl.glVertex3f(1.0f, 0.0f, 0.0f);        	
        
        gl.glEnd();	
        
        resetMatrix();
    }
    
    
    
    
    private void drawQuad(){    	
 
       	gl.glDisable(GL.GL_CULL_FACE);
 	
    	
        gl.glBegin(GL.GL_QUADS);	
        
        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);	
        gl.glVertex3f(1.0f, 0.0f, 0.0f);	
        gl.glVertex3f(1.0f, 1.0f, 0.0f);	
        gl.glVertex3f(0.0f, 1.0f, 0.0f);
              
        gl.glEnd();		
        
	   	gl.glEnable(GL.GL_CULL_FACE);
       
    }
    
    
    
    
    
    /** draws the text s
     * @param x x-coord
     * @param y y-coord
     * @param s text
     * @param colored says if the text has to be colored
     */
    public void drawText(float x, float y, String s, boolean colored){
    	
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();
        
    	gl.glMatrixMode(GL.GL_MODELVIEW);
    	
    	
    	initMatrix();
    	initMatrix(view3D.getUndoRotationMatrix());
    	

    	textRenderer.begin3DRendering();

    	if (colored)
    		textRenderer.setColor(textColor);
    	
        /*
    	Rectangle2D bounds = textRenderer.getBounds(s);
        float w = (float) bounds.getWidth();
        float h = (float) bounds.getHeight();
        */
        float textScaleFactor = DEFAULT_TEXT_SCALE_FACTOR/((float) view3D.getScale());
    	
    	
    	textRenderer.draw3D(s,
                x*textScaleFactor,//w / -2.0f * textScaleFactor,
                y*textScaleFactor,//h / -2.0f * textScaleFactor,
                0,
                textScaleFactor);
    	
        textRenderer.end3DRendering();
     
 
    	
        resetMatrix(); //initMatrix(m_view3D.getUndoRotationMatrix());
    	resetMatrix(); //initMatrix();
    }
    
    
    
    
    
    
    
    
    /////////////////////////
    // FPS
    
	double displayTime;
	double fps;
    
    private void drawFPS(){
    	
    	
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();
        
    	gl.glMatrixMode(GL.GL_MODELVIEW);
    	
    	gl.glPushMatrix();
    	gl.glLoadIdentity();
    	
    	
    	textRenderer.begin3DRendering();

    	
    	textRenderer.setColor(Color.BLACK);
    	

    	double displayTimeOld = displayTime;
    	displayTime = System.currentTimeMillis();
    	fps = 0.9*fps + 0.1*1000/(displayTime-displayTimeOld);
    	if (fps>100)
    		fps=100;
    	
        
    	textRenderer.draw3D("FPS="+((int) fps),left,bottom,0,1);
    	
        textRenderer.end3DRendering();
        
        gl.glPopMatrix();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //////////////////////////////////////
    // picking
    
    /**
     * sets the mouse locations to (x,y) and asks for picking.
     * 
     * @param x x-coordinate of the mouse
     * @param y y-coordinate of the mouse
     */
    public void setMouseLoc(int x, int y, int pickingMode){
    	mouseX = x;
    	mouseY = y;
    	
    	this.pickingMode = pickingMode;
    	
    	// on next rending, a picking will be done : see doPick()
    	waitForPick = true;
    	
    	//thread = new Thread(picking);
       
    	//thread.setPriority(Thread.MIN_PRIORITY);
    	
    	//thread.start();
        //return thread;
    	
    }
    

    
    /**
     * does the picking to sets which objects are under the mouse coordinates.
     */
    public void doPick(){
    	
    	

    	BUFSIZE = drawList3D.size()*2+1;
    	selectBuffer = BufferUtil.newIntBuffer(BUFSIZE); // Set Up A Selection Buffer
        int hits; // The Number Of Objects That We Selected
        gl.glSelectBuffer(BUFSIZE, selectBuffer); // Tell OpenGL To Use Our Array For Selection
        
        
        // The Size Of The Viewport. [0] Is <x>, [1] Is <y>, [2] Is <length>, [3] Is <width>
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);      
        //System.out.println("viewport= "+viewport[0]+","+viewport[1]+","+viewport[2]+","+viewport[3]);
        
        
        Dimension dim = canvas.getSize();
        //System.out.println("dimension= "+dim.width +","+dim.height);
        
        
        
        
        // Puts OpenGL In Selection Mode. Nothing Will Be Drawn.  Object ID's and Extents Are Stored In The Buffer.
        gl.glRenderMode(GL.GL_SELECT);
        gl.glInitNames(); // Initializes The Name Stack
        gl.glPushName(0); // Push 0 (At Least One Entry) Onto The Stack
        
        
        // This Creates A Matrix That Will Zoom Up To A Small Portion Of The Screen, Where The Mouse Is.

        
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
      
        
        /* create MOUSE_PICK_WIDTH x MOUSE_PICK_WIDTH pixel picking region near cursor location */
        //glu.gluPickMatrix((double) mouseX, (double) (470 - mouseY),  5.0, 5.0, viewport, 0);
        //mouseY+=30; //TODO understand this offset
        //glu.gluPickMatrix((double) mouseX, (double) (viewport[3] - mouseY), 5.0, 5.0, viewport, 0);
        glu.gluPickMatrix(mouseX, (dim.height - mouseY), MOUSE_PICK_WIDTH, MOUSE_PICK_WIDTH, viewport, 0);
        gl.glOrtho(left,right,bottom,top,front,back);
    	gl.glMatrixMode(GL.GL_MODELVIEW);
        
    	

    	
    	
    	Drawable3D[] drawHits = new Drawable3D[BUFSIZE];
  
    	primitives.enableVBO(gl);
    	
        // picking objects
        int loop = drawList3D.drawForPicking(this,drawHits,0);
        int labelLoop = loop;
        
        if (pickingMode == PICKING_MODE_LABELS){
        	// picking labels
        	loop = drawList3D.drawLabelForPicking(this,drawHits,loop);
        }

        primitives.disableVBO(gl);
        
        hits = gl.glRenderMode(GL.GL_RENDER); // Switch To Render Mode, Find Out How Many
             
        //hits are stored
        Hits3D hits3D = new Hits3D();
        hits3D.init();
        //view3D.getHits().init();
        
        //String s="doPick (labelLoop = "+labelLoop+")";
        
        int names, ptr = 0;
        float zMax, zMin;
        int num;
        for (int i = 0; i < hits; i++) { 
        	     
          names = selectBuffer.get(ptr);  
          ptr++; // min z    
          zMin = getDepth(ptr);
          ptr++; // max z
          zMax = getDepth(ptr);           
          
          ptr++;
          
          for (int j = 0; j < names; j++){ 
        	num = selectBuffer.get(ptr);
        	//((Hits3D) view3D.getHits()).addDrawable3D(drawHits[num],num>labelLoop);
        	hits3D.addDrawable3D(drawHits[num],num>labelLoop);
        	//s+="\n("+num+") "+drawHits[num].getGeoElement().getLabel();
        	drawHits[num].zPickMin = zMin;
        	drawHits[num].zPickMax = zMax;
        	ptr++;
          }
          
          
        }
        
        //Application.debug(s);
        
        // sets the GeoElements in view3D
        //((Hits3D) view3D.getHits()).sort();
        hits3D.sort();
        view3D.setHits(hits3D);
        //Application.debug(hits3D.toString());
       
        waitForPick = false;
    }
    
    
    public void glLoadName(int loop){
    	gl.glLoadName(loop);
    }
    
    /** returns the depth between 0 and 2, in double format, from an integer offset 
     *  lowest is depth, nearest is the object
     *  
     *  @param ptr the integer offset
     * */
    private float getDepth(int ptr){
     	
    	float depth = (float) selectBuffer.get(ptr)/0x7fffffff;
    	if (depth<0)
    		depth+=2;
    	return depth;
    	
    	
    }
    
    
    
    
    
    
    
    
    
    
    //////////////////////////////////
    // initializations
    
    /** Called by the drawable immediately after the OpenGL context is
     * initialized for the first time. Can be used to perform one-time OpenGL
     * initialization such as setup of lights and display lists.
     * @param gLDrawable The GLAutoDrawable object.
     */
    public void init(GLAutoDrawable drawable) {
    	
        
        gl = drawable.getGL();
        
        // Check For VBO support
        final boolean VBOsupported = gl.isFunctionAvailable("glGenBuffersARB") &&
                gl.isFunctionAvailable("glBindBufferARB") &&
                gl.isFunctionAvailable("glBufferDataARB") &&
                gl.isFunctionAvailable("glDeleteBuffersARB");
        Application.debug("vbo supported : "+VBOsupported);
        
        if (VBOsupported)
        	//primitives = new RendererPrimitivesVBO(gl);
        	primitives = new RendererPrimitives(gl);
        else
        	primitives = new RendererPrimitives(gl);
        
        
        //light
        /*
        float pos[] = { 1.0f, 1.0f, 1.0f, 0.0f };
        //float pos[] = { 0.0f, 0.0f, 1.0f, 0.0f };
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, pos, 0);        
        gl.glEnable(GL.GL_LIGHTING);     
        gl.glEnable(GL.GL_LIGHT0);
        */
        
        
        float[] lightAmbient0 = {0.1f, 0.1f, 0.1f, 1.0f};
        float[] lightDiffuse0 = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] lightPosition0 = {1.0f, 1.0f, 1.0f, 0.0f};
        float[] lightSpecular0 = {0f, 0f, 0f, 1f};
       
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient0, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse0, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition0, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, lightSpecular0, 0);
        gl.glEnable(GL.GL_LIGHT0);
        
        
        /*
        float[] lightAmbient1 = {0.1f, 0.1f, 0.1f, 1.0f};
        float[] lightDiffuse1 = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] lightPosition1 = {-1.0f, -1.0f, -1.0f, 0.0f};
        float[] lightSpecular1 = {0f, 0f, 0f, 1f};
       
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, lightAmbient1, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, lightDiffuse1, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, lightPosition1, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, lightSpecular1, 0);
        gl.glEnable(GL.GL_LIGHT1);      
        */
        
        
        gl.glEnable(GL.GL_LIGHTING);
        gl.glLightModelf(GL.GL_LIGHT_MODEL_TWO_SIDE,GL.GL_TRUE);
        gl.glShadeModel(GL.GL_SMOOTH);
        
        //common enabling
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

        //gl.glPolygonOffset(1.0f, 2f);

        gl.glEnable(GL.GL_CULL_FACE);
        
        //blending
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);	
        //gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);  
        
        gl.glAlphaFunc(GL.GL_NOTEQUAL, 0);//pixels with alpha=0 are not drawn
        gl.glEnable(GL.GL_ALPHA_TEST);       
        
        //using glu quadrics
        quadric = glu.gluNewQuadric();// Create A Pointer To The Quadric Object (Return 0 If No Memory) (NEW)
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);          // Create Smooth Normals (NEW)
        glu.gluQuadricTexture(quadric, true);                    // Create Texture Coords (NEW)
        
        //projection type
        //viewOrtho(gl); 
        
        
        //normal anti-scaling
        gl.glEnable(GL.GL_NORMALIZE);
        //gl.glEnable(GL.GL_RESCALE_NORMAL);
        
        
        
        //textures
        initTextures();
        
         
    }

    
    
    
    
    
    
    
    
    //projection mode
    
	int left = 0; int right = 640;
	int bottom = 0; int top = 480;
	int front = -1000; int back = 1000;
	
	
	public int getLeft(){ return left;	}
	public int getRight(){ return right;	}
	public int getBottom(){ return bottom;	}
	public int getTop(){ return top;	}
	public int getFront(){ return front;	}
	public int getBack(){ return back;	}
	
	
	
	
	/** for a line described by (o,v), return the min and max parameters to draw the line
	 * @param minmax initial interval
	 * @param o origin of the line
	 * @param v direction of the line
	 * @return interval to draw the line
	 */
	public double[] getIntervalInFrustum(double[] minmax, Ggb3DVector o, Ggb3DVector v){
		

		
		
		double left = (getLeft() - o.get(1))/v.get(1);
		double right = (getRight() - o.get(1))/v.get(1);		
		updateIntervalInFrustum(minmax, left, right);
		
		double top = (getTop() - o.get(2))/v.get(2);
		double bottom = (getBottom() - o.get(2))/v.get(2);
		updateIntervalInFrustum(minmax, top, bottom);
		
		double front = (getFront() - o.get(3))/v.get(3);
		double back = (getBack() - o.get(3))/v.get(3);
		updateIntervalInFrustum(minmax, front, back);
			
		
		/*
		Application.debug("intersection = ("+left+","+right+
				")/("+top+","+bottom+")/("+front+","+back+")"+
				"\ninterval = ("+minmax[0]+","+minmax[1]+")");
				*/
				
		
		return minmax;
	}
	
	/** return the intersection of intervals [minmax] and [v1,v2]
	 * @param minmax initial interval
	 * @param v1 first value
	 * @param v2 second value
	 * @return intersection interval
	 */
	private double[] updateIntervalInFrustum(double[] minmax, double v1, double v2){
		
		if (v1>v2){
			double v = v1;
			v1 = v2; v2 = v;
		}
		
		if (v1>minmax[0])
			minmax[0] = v1;
		if (v2<minmax[1])
			minmax[1] = v2;
		
		return minmax;
	}
   
	
    /**
     * Set Up An Ortho View regarding left, right, bottom, front values
     * 
     */
    private void viewOrtho()                                      
    {

    	gl.glViewport(0,0,right-left,top-bottom);
    	
    	gl.glMatrixMode(GL.GL_PROJECTION);
    	gl.glLoadIdentity();

    	gl.glOrtho(left,right,bottom,top,front,back);
    	gl.glMatrixMode(GL.GL_MODELVIEW);
    	
    	
    	
    }
    
    
	
    /**
     * Set Up An Ortho View after setting left, right, bottom, front values
     * @param x left
     * @param y bottom
     * @param w width
     * @param h height
     * 
     */
    private void viewOrtho(int x, int y, int w, int h){
    	left=x-w/2;
    	bottom=y-h/2;
    	right=left+w;
    	top = bottom+h;
    	
    	/*
    	Application.debug("viewOrtho:"+
    			"\n left="+left+"\n right="+right+
    			"\n top="+top+"\n bottom="+bottom+
    			"\n front="+front+"\n back="+back
    	);
    	*/
    	
    	viewOrtho();
    }
   
    




    private void viewPerspective(GL gl)                                  // Set Up A Perspective View
    {
        gl.glMatrixMode(GL.GL_PROJECTION);                               // Select Projection
        gl.glPopMatrix();                                                // Pop The Matrix
        gl.glMatrixMode(GL.GL_MODELVIEW);                                // Select Modelview
        gl.glPopMatrix();                                                // Pop The Matrix
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    
    
    


}
