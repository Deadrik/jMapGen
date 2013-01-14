package jMapGen.graph;

import jMapGen.Point;
import jMapGen.com.nodename.Delaunay.Site;

import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class CornerElevationSorter extends Corner implements Comparator<Corner>
{
	@Override
	public int compare(Corner arg0, Corner arg1) {
		int returnValue = 0;
		
		if(arg0.elevation < arg1.elevation)
			returnValue = -1;
		else if(arg0.elevation > arg1.elevation)
			returnValue = 1;
		
		return returnValue;
	}
}
