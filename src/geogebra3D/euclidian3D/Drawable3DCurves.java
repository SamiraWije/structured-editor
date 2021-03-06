package geogebra3D.euclidian3D;

import java.awt.Color;

import geogebra.kernel.GeoElement;
import geogebra.main.Application;
import geogebra3D.Matrix.Ggb3DMatrix;
import geogebra3D.euclidian3D.opengl.Renderer;
import geogebra3D.kernel3D.GeoElement3D;
import geogebra3D.kernel3D.GeoElement3DInterface;
import geogebra3D.kernel3D.GeoSegment3D;


/**
 * 
 * @author ggb3D
 *	
 *	for "solid" drawables, like lines, segments, etc.
 *	these are drawable that are not to become transparent
 *
 */


public abstract class Drawable3DCurves extends Drawable3D {


	
	
	public Drawable3DCurves(EuclidianView3D a_view3d, GeoElement a_geo) {
		super(a_view3d, a_geo);
	}

	public Drawable3DCurves(EuclidianView3D a_view3d) {
		super(a_view3d);
	}

	public void draw(Renderer renderer) {
		if(!getGeoElement().isEuclidianVisible() || !getGeoElement().isDefined())
			return;	
		
		renderer.setMaterial(getGeoElement().getObjectColor(),1.0f);//TODO geo.getAlphaValue());
		renderer.setDash(Renderer.DASH_NONE); 
		renderer.setMatrix(getMatrix());
		
		drawGeometry(renderer);
		
		

	}

	
	public void drawHighlighting(Renderer renderer) {
				

		if(!getGeoElement().isEuclidianVisible() || !getGeoElement().isDefined())
			return;
		if (!getGeoElement().doHighlighting())
			return;
		
		renderer.setMaterial(new Color(0f,0f,0f),1f);
		renderer.setDash(Renderer.DASH_NONE);
		renderer.setMatrix(getMatrix());
		drawGeometryPicked(renderer);		
		

	}
	
	
	public void drawHidden(Renderer renderer){
		
		if(!getGeoElement().isEuclidianVisible() || !getGeoElement().isDefined())
			return;
		
				
		renderer.setMaterial(getGeoElement().getObjectColor(),1.0f);
		renderer.setDash(getGeoElement().getLineType()); 
		renderer.setMatrix(getMatrix());		
		drawGeometryHidden(renderer);		
		


	} 


	
	// methods not used for solid drawables
	public void drawHiding(Renderer renderer) {}
	public void drawTransp(Renderer renderer) {}

	public boolean isTransparent() {
		return false;
	}
	
	
	public int getType(){
		return DRAW_TYPE_CURVES;
	}
	
	

}
