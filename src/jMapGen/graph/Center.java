package jMapGen.graph;

import jMapGen.BiomeType;
import jMapGen.Point;
import java.util.Vector;


public class Center 
{
	public int index;

	public Point point;  // location
	public Boolean water = false;  // lake or ocean
	public Boolean ocean = false;  // ocean
	public Boolean coast = false;  // land polygon touching an ocean
	public Boolean border = false;  // at the edge of the map
	public BiomeType biome;  // biome type (see article)
	public double elevation = 0;  // 0.0-1.0
	public double moisture = 0;  // 0.0-1.0

	public Vector<Center> neighbors;
	public Vector<Edge> borders;
	public Vector<Corner> corners;

	public Corner getClosestCorner(Point p)
	{
		Corner closest = corners.get(0);
		double distance = p.distanceSq(corners.get(0).point);

		for (int i = 1; i < corners.size(); i++)
		{
			double newDist = p.distanceSq(corners.get(i).point);
			if(newDist < distance)
			{
				distance = newDist;
				closest = corners.get(i);
			}
		}
		return closest;
	}

	public Corner getNextClosestCorner(Point p)
	{
		Corner first = getClosestCorner(p);
		Corner closest = first.adjacent.get(0);
		double distance = p.distanceSq(first.adjacent.get(0).point);

		for (int i = 1; i < first.adjacent.size(); i++)
		{

				double newDist = p.distanceSq(first.adjacent.get(i).point);
				if(newDist < distance)
				{
					distance = newDist;
					closest = first.adjacent.get(i);
				}

		}
		return closest;
	}
	
	public Corner getNextClosestCorner(Point p, Corner first)
	{
		Corner closest = first.adjacent.get(0);
		double distance = p.distanceSq(first.adjacent.get(0).point);

		for (int i = 1; i < first.adjacent.size(); i++)
		{

				double newDist = p.distanceSq(first.adjacent.get(i).point);
				if(newDist < distance)
				{
					distance = newDist;
					closest = first.adjacent.get(i);
				}

		}
		return closest;
	}
}
