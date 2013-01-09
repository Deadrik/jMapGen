package jMapGen.graph;

import jMapGen.Point;

import java.util.List;
import java.util.Vector;

public class Corner 
{
	public int index;

	public Point point;  // location
	public Boolean ocean;  // ocean
	public Boolean water;  // lake or ocean
	public Boolean coast;  // touches ocean and land polygons
	public Boolean border;  // at the edge of the map
	public double elevation;  // 0.0-1.0
	public double moisture;  // 0.0-1.0

	public Vector<Center> touches;
	public Vector<Edge> protrudes;
	public Vector<Corner> adjacent;

	public int river;  // 0 if no river, or volume of water in river
	public Corner downslope;  // pointer to adjacent corner most downhill
	public Corner watershed;  // pointer to coastal corner, or null
	public int watershed_size;
}
