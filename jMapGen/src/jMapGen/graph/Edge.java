package jMapGen.graph;

import java.awt.Point;

public class Edge 
{
	public int index;
	public Center d0, d1;  // Delaunay edge
	public Corner v0, v1;  // Voronoi edge
	public Point midpoint;  // halfway between v0,v1
	public int river;  // volume of water, or 0
}

