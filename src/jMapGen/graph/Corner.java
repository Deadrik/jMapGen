package jMapGen.graph;

import jMapGen.Point;
import jMapGen.com.nodename.Delaunay.Site;

import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class Corner
{
	public int index;

	public Point point;  // location
	public Boolean ocean = false;  // ocean
	public Boolean water = false;  // lake or ocean
	public Boolean coast = false;  // touches ocean and land polygons
	public Boolean border = false;  // at the edge of the map
	public double elevation;  // 0.0-1.0
	public double moisture;  // 0.0-1.0

	public Vector<Center> touches;
	public Vector<Edge> protrudes;
	public Vector<Corner> adjacent;

	public int river;  // 0 if no river, or volume of water in river
	public Corner downslope;  // pointer to adjacent corner most downhill
	public Corner watershed;  // pointer to coastal corner, or null
	public int watershed_size;
	
	public Corner()
	{
		ocean = false;
		water = false;
		coast = false;
		border = false;
		elevation = Double.MAX_VALUE;
	}
}
