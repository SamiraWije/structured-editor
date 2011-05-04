/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License v2 as published by 
the Free Software Foundation.

*/

package geogebra.kernel;

import java.util.Iterator;
import java.util.TreeSet;


/**
 * Sort a list. Adapted from AlgoMax and AlgoIterationList
 * @author Michael Borcherds
 * @version 04-01-2008
 */

public class AlgoSort extends AlgoElement {

	private static final long serialVersionUID = 1L;
	private GeoList inputList; //input
    private GeoList outputList; //output	
    private int size;

    AlgoSort(Construction cons, String label, GeoList inputList) {
        super(cons);
        this.inputList = inputList;
               
        outputList = new GeoList(cons);

        setInputOutput();
        compute();
        outputList.setLabel(label);
    }

    protected String getClassName() {
        return "AlgoSort";
    }

    protected void setInputOutput(){
        input = new GeoElement[1];
        input[0] = inputList;

        output = new GeoElement[1];
        output[0] = outputList;
        setDependencies(); // done by AlgoElement
    }

    GeoList getResult() {
        return outputList;
    }

    protected final void compute() {
    	
    	size = inputList.size();
    	if (!inputList.isDefined() ||  size == 0) {
    		outputList.setUndefined();
    		return;
    	} 

    	GeoElement geo0 = inputList.get(0); 
    	
    	Class<? extends GeoElement> geoClass = geo0.getClass();
    	
    	TreeSet<GeoElement> sortedSet;
    	
    	if (geo0.isGeoPoint()) {
    		sortedSet = new TreeSet<GeoElement>(GeoPoint.getComparatorX());
    		
    	} else if (geo0.isGeoText()) {
    		sortedSet = new TreeSet<GeoElement>(GeoText.getComparator());
    		
    	} else if (geo0.isNumberValue()) {
    		sortedSet = new TreeSet<GeoElement>(GeoNumeric.getComparator());
    		
    	} else {
    		outputList.setUndefined();
    		return;    		
    	}
    	
    	
        // copy inputList into treeset
        for (int i=0 ; i<size ; i++)
        {
	      	GeoElement geo = inputList.get(i); 
	   		if (geo.getClass().equals(geoClass)) {
	   			sortedSet.add(geo);
	   		}
	   		else
	   		{
	   		  outputList.setUndefined();
	       	  return;			
	   		}
        }
        
        // copy the sorted treeset back into a list
        outputList.setDefined(true);
        outputList.clear();
        
        Iterator<GeoElement> iterator = sortedSet.iterator();
        while (iterator.hasNext()) {
     	   outputList.add((iterator.next()));
        }      

    }
  
}