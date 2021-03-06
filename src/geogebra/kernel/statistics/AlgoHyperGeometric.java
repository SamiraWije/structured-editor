/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License v2 as published by 
the Free Software Foundation.

*/

package geogebra.kernel.statistics;

import geogebra.kernel.Construction;
import geogebra.kernel.arithmetic.NumberValue;

import org.apache.commons.math.distribution.HypergeometricDistribution;

/**
 * 
 * @author Michael Borcherds
 */

public class AlgoHyperGeometric extends AlgoDistribution {

	private static final long serialVersionUID = 1L;
    
    public AlgoHyperGeometric(Construction cons, String label, NumberValue a,NumberValue b, NumberValue c, NumberValue d) {
        super(cons, label, a, b, c, d);
    }

    protected String getClassName() {
        return "AlgoHyperGeometric";
    }

    @SuppressWarnings("deprecation")
	protected final void compute() {
    	
    	
    	if (input[0].isDefined() && input[1].isDefined() && input[2].isDefined()) {
		    int param = (int)Math.round(a.getDouble());
		    int param2 = (int)Math.round(b.getDouble());
		    int param3 = (int)Math.round(c.getDouble());
    		    double val = d.getDouble();
        		try {
        			HypergeometricDistribution dist = getHypergeometricDistribution(param, param2, param3);
        			num.setValue(dist.cumulativeProbability(val));     // P(T <= val)
        			
        		}
        		catch (Exception e) {
        			num.setUndefined();        			
        		}
    	} else
    		num.setUndefined();
    }       
        
    
}



