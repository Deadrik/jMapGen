package jMapGen.graph;

import jMapGen.BiomeType;
import jMapGen.Point;
import java.util.Vector;

import pythagoras.d.Vector3;


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

	public Corner getNextClosestCorner(Point local, Corner first)
	{
		Corner closest = first.adjacent.get(0);

		pythagoras.d.Vector p = new pythagoras.d.Vector(local.x, local.y);
		pythagoras.d.Vector a = new pythagoras.d.Vector(point.x, point.y);
		pythagoras.d.Vector b = new pythagoras.d.Vector(first.point.x, first.point.y);
		pythagoras.d.Vector c;

		for (int i = 0; i < corners.size(); i++)
		{
			Corner _corner = corners.get(i);
			if(_corner != first)
			{
				c = new pythagoras.d.Vector(_corner.point.x, _corner.point.y);
				if(PointInTriangle(p,a,b,c))
				{
					return _corner;
				}
			}


		}
		return closest;
	}

	boolean SameSide(pythagoras.d.Vector p1, pythagoras.d.Vector p2, pythagoras.d.Vector a, pythagoras.d.Vector b)
	{
		pythagoras.d.Vector cp1 = b.subtract(a).cross(p1.subtract(a));
		pythagoras.d.Vector cp2 = b.subtract(a).cross(p2.subtract(a));

		if (cp1.dot(cp2) >= 0) 
			return true;
		else return false;
	}

	boolean PointInTriangle(pythagoras.d.Vector p, pythagoras.d.Vector a, pythagoras.d.Vector b, pythagoras.d.Vector c)
	{
		if (SameSide(p,a, b,c) && SameSide(p,b, a,c) && SameSide(p,c, a,b)) 
			return true;
		else return false;
	}
}
