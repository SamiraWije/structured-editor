package geogebra3D.kernel3D.commands;

import geogebra.kernel.GeoElement;
import geogebra.kernel.Kernel;
import geogebra.kernel.arithmetic.Command;
import geogebra.kernel.commands.CmdSegment;
import geogebra.main.Application;
import geogebra.main.MyError;
import geogebra3D.kernel3D.GeoElement3D;
import geogebra3D.kernel3D.GeoPoint3D;
import geogebra3D.kernel3D.Kernel3D;



/*
 * Segment[ <GeoPoint3D>, <GeoPoint3D> ] or CmdSegment
 */
public class CmdSegment3D extends CmdSegment {
	
	Kernel3D kernel3D;

	public CmdSegment3D(Kernel kernel) {
		super(kernel);
		//Application.debug("CmdSegment3D");
		
	}
	
	public CmdSegment3D(Kernel3D kernel3D) {
		this( (Kernel) kernel3D);
		this.kernel3D = kernel3D;
		//Application.debug("CmdSegment3D");
		
	}	
	
	
	public GeoElement[] process(Command c) throws MyError {	
		
		
	    int n = c.getArgumentNumber();
	    boolean[] ok = new boolean[n];
	    GeoElement[] arg;

	    Application.debug("CmdSegment3D.process, n="+n);
	    
	   if (n==2) {
            arg = resArgs(c);
            if (arg[0].isGeoElement3D() && arg[1].isGeoElement3D()){
            	
            	GeoElement3D geo0 = (GeoElement3D) arg[0];
            	GeoElement3D geo1 = (GeoElement3D) arg[1];
            	
            	// segment between two 3D points
            	if ((ok[0] = (geo0.isGeoPoint()))
            			&& (ok[1] = (geo1.isGeoPoint()))) {
            		GeoElement[] ret =
            		{
            				kernel3D.Segment3D(
            						c.getLabel(),
            						(GeoPoint3D) geo0,
            						(GeoPoint3D) geo1)};
            		return ret;
            	}
            }
	    }
		
		return super.process(c);
	}

}
