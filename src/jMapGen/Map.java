// Make a map out of a voronoi graph
// Original Author: amitp@cs.stanford.edu
// License: MIT
package jMapGen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import za.co.iocom.math.MathUtil;
import za.co.luma.math.function.*;
import za.co.luma.math.sampling.PoissonDiskMultiSampler;
import za.co.luma.math.sampling.PoissonDiskSampler;
import za.co.luma.math.sampling.Sampler;
import za.co.luma.math.sampling.UniformPoissonDiskSampler;

import jMapGen.com.nodename.Delaunay.DelaunayUtil;
import jMapGen.com.nodename.Delaunay.Voronoi;
import jMapGen.com.nodename.geom.LineSegment;
import jMapGen.graph.Center;
import jMapGen.graph.Corner;
import jMapGen.graph.CornerElevationSorter;
import jMapGen.graph.Edge;


public class Map 
{
	public static int NUM_POINTS = 500;
	static double LAKE_THRESHOLD = 0.3;  // 0 to 1, fraction of water corners for water polygon



	// Passed in by the caller:
	public int SIZE;

	// Island shape is controlled by the islandRandom seed and the
	// type of island, passed in when we set the island shape. The
	// islandShape function uses both of them to determine whether any
	// point should be water or land.
	public IslandShape islandShape;


	// Island details are controlled by this random generator. The
	// initial map upon loading is always deterministic, but
	// subsequent maps reset this random number generator with a
	// random seed.
	public Random mapRandom = new Random();

	// These store the graph data
	public Vector<Point> points;  // Only useful during map construction
	public Vector<Center> centers;
	public Vector<Corner> corners;
	public Vector<Edge> edges;

	public int seed;

	public Map(int size, int s) 
	{
		SIZE = size;
		seed = s;
	}

	// Random parameters governing the overall shape of the island
	public void newIsland(int seed) 
	{
		islandShape = new IslandShape(seed, SIZE, 0.6);
		mapRandom.setSeed(seed);
		MathUtil.random = mapRandom;

		//mapgen2.graphics.translate(10, 32);
	}


	public void go() 
	{
		setup();
		//Generate the initial random set of points
		//points = generateRandomPoints();

		//RealFunction2DWrapper realfn = new RealFunction2DWrapper(new PerlinFunction2D((int)SIZE, (int)SIZE, 3), 0.1f, 1, 0.0001f, 1);
		//Sampler<Point> sampler = new PoissonDiskSampler(10, 10, SIZE-10, SIZE-10, 10, realfn);

		double[] radii = {4};	
		double[] minRadii = {2};	
		double[] minDist = {15};

		PoissonDiskMultiSampler sampler = new PoissonDiskMultiSampler(10, 10, SIZE-10, SIZE-10, minDist, minRadii, radii, null, true);
		List<PoissonDiskMultiSampler.Circle>[] pointList = sampler.sample();

		for (List<PoissonDiskMultiSampler.Circle> list : pointList)
		{

			for (PoissonDiskMultiSampler.Circle point : list)
			{
				//Color c = new Color(255, 255 - i * 255 / list.size(), i * 255 / list.size());
				//image.setRGB((int) point.x, (int) point.y, c.getRGB());
				double r = point.getRadius();
				points.add(new Point(point.x, point.y));
			}

		}

		//		for(int i = 0; i < pointList.size(); i++)
		//		{
		//			points.add(pointList.get(i));
		//		}

		Rectangle R = new Rectangle();
		R.setFrame(0, 0, SIZE, SIZE);
		System.out.println("Starting Creating map Voronoi...");
		Voronoi voronoi = new Voronoi(points, R);
		System.out.println("Finished Creating map Voronoi...");
		buildGraph(points, voronoi);


		// Determine the elevations and water at Voronoi corners.
		assignCornerElevations();

		// Determine polygon and corner type: ocean, coast, land.
		assignOceanCoastAndLand();



		// Rescale elevations so that the highest is 1.0, and they're
		// distributed well. We want lower elevations to be more common
		// than higher elevations, in proportions approximately matching
		// concentric rings. That is, the lowest elevation is the
		// largest ring around the island, and therefore should more
		// land area than the highest elevation, which is the very
		// center of a perfectly circular island.
		redistributeElevations(landCorners(corners));

		for(int i = 0; i < this.corners.size(); i++)
		{
			Corner c = corners.get(i);

			//			if(c.water)
			//			{
			//				mapgen2.graphics.setColor(Color.BLUE);
			//				mapgen2.graphics.fillRect((int)c.point.x-3, (int)c.point.y-3, 5,5);
			//			}
			//			else if (c.coast) 
			//			{
			//				mapgen2.graphics.setColor(Color.getHSBColor(0.122222f, 0.27f, 0.91f));
			//				mapgen2.graphics.fillRect((int)c.point.x-3, (int)c.point.y-3, 5,5);
			//			}
			//			else
			//			{
			//				
			//				mapgen2.graphics.setColor(Color.getHSBColor(0, 0, Math.max((float)(c.elevation), 0)));
			//				mapgen2.graphics.fillRect((int)c.point.x-2, (int)c.point.y-2, 5, 5);
			//			}
		}



		// Assign elevations to non-land corners
		for(Iterator<Corner> i = corners.iterator(); i.hasNext();)
		{
			Corner q = (Corner)i.next();
			if (q.ocean || q.coast) 
			{
				q.elevation = 0.0;
			}
		}

		// Polygon elevations are the average of their corners
		assignPolygonElevations();
		if(IslandMapGen.graphics != null)
		{
			for(Iterator<Center> i = centers.iterator(); i.hasNext();)
			{
				Center q = (Center)i.next();

				if (q.water) 
				{
					IslandMapGen.graphics.setColor(Color.BLUE);
					IslandMapGen.graphics.fillRect((int)q.point.x-3, (int)q.point.y-3, 5,5);
				}
				else if (q.coast) 
				{
					IslandMapGen.graphics.setColor(Color.getHSBColor(0.122222f, 0.27f, 0.91f));
					IslandMapGen.graphics.fillRect((int)q.point.x-3, (int)q.point.y-3, 5,5);
				}
				else
				{
					IslandMapGen.graphics.setColor(Color.getHSBColor(0, 0, Math.max((float)(q.elevation), 0)));
					IslandMapGen.graphics.fillRect((int)q.point.x-2, (int)q.point.y-2, 5, 5);
				}
			}
		}

		// Determine downslope paths.
		calculateDownslopes();

		// Determine watersheds: for every corner, where does it flow
		// out into the ocean? 
		calculateWatersheds();

		// Create rivers.
		createRivers();

		for(int i = 0; i < this.edges.size(); i++)
		{
			Edge e = edges.get(i);
			if(e.river != 0 && !e.vCorner1.water)
			{
				//				mapgen2.graphics.setColor(Color.CYAN);
				//				mapgen2.graphics.drawLine((int)e.vCorner0.point.x, (int)e.vCorner0.point.y, 
				//						(int)e.vCorner1.point.x, (int)e.vCorner1.point.y);
			}
		}

		// Determine moisture at corners, starting at rivers
		// and lakes, but not oceans. Then redistribute
		// moisture to cover the entire range evenly from 0.0
		// to 1.0. Then assign polygon moisture as the average
		// of the corner moisture.
		assignCornerMoisture();
		redistributeMoisture(landCorners(corners));
		assignPolygonMoisture();

		assignBiomes();

	}

	private void drawCorners(String suffix) {
		try 
		{
			System.out.println("Drawing hm-corners-"+suffix+".bmp");
			BufferedImage outBitmap = new BufferedImage(640,640,BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) outBitmap.getGraphics();
			for(int i = 0; i < corners.size(); i++)
			{
				Corner c = corners.get(i);

				if(c.elevation < 1)
					g.setColor(Color.blue);
				else
					g.setColor(Color.green);

				g.drawRect((int)c.point.x, (int)c.point.y, 1,1);
			}
			ImageIO.write(outBitmap, "BMP", new File("hm-corners-"+suffix+".bmp"));
		} catch (IOException e) {e.printStackTrace();}
	}

	public void setup() 
	{
		// Clear the previous graph data.
		points = new Vector<Point>();
		edges = new Vector<Edge>();
		centers = new Vector<Center>();
		corners = new Vector<Corner>();
	}


	// Generate random points and assign them to be on the island or
	// in the water. Some water points are inland lakes; others are
	// ocean. We'll determine ocean later by looking at what's
	// connected to ocean.
	public Vector<Point> generateRandomPoints()
	{
		Point p; 
		int i; 
		Vector<Point> points = new Vector<Point>();

		for (i = 0; i < NUM_POINTS; i++) 
		{
			p = new Point(10 + mapRandom.nextDouble() * (SIZE-10),
					10 + mapRandom.nextDouble() * (SIZE-10));
			points.add(p);
		}
		return points;
	}

	// Although Lloyd relaxation improves the uniformity of polygon
	// sizes, it doesn't help with the edge lengths. Short edges can
	// be bad for some games, and lead to weird artifacts on
	// rivers. We can easily lengthen short edges by moving the
	// corners, but **we lose the Voronoi property**.  The corners are
	// moved to the average of the polygon centers around them. Short
	// edges become longer. Long edges tend to become shorter. The
	// polygons tend to be more uniform after this step.
	public void improveCorners() 
	{
		System.out.println("Starting improveCorners...");
		Vector<Point> newCorners = new Vector<Point>(corners.size());

		drawCorners("StartImprove");

		Corner q; 
		Center r; 
		Point point; 
		int i; 
		Edge edge;

		// First we compute the average of the centers next to each corner.
		for(int ind = 0; ind < corners.size(); ind++)  
		{
			q = corners.get(ind);
			if (q.border) 
			{
				DelaunayUtil.setAtPosition(newCorners, q.index, q.point);
			} else {
				point = new Point(0.0, 0.0);
				for(int ind2 = 0; ind2 < centers.size(); ind2++)  
				{
					r = centers.get(ind2);
					point.x += r.point.x;
					point.y += r.point.y;
				}
				point.x /= q.touches.size();
				point.y /= q.touches.size();
				DelaunayUtil.setAtPosition(newCorners, q.index, point);
			}
		}

		// Move the corners to the new locations.
		for (i = 0; i < corners.size(); i++) {
			corners.get(i).point = newCorners.get(i);
		}

		// The edge midpoints were computed for the old corners and need
		// to be recomputed.
		for(i = 0; i < edges.size(); i++) 
		{
			edge = edges.get(i);
			if (edge.vCorner0 != null && edge.vCorner1 != null) 
			{
				edge.midpoint = Point.interpolate(edge.vCorner0.point, edge.vCorner1.point, 0.5);
			}
		}

		drawCorners("FinishImprove");
		System.out.println("Finished improveCorners...");
	}


	// Create an array of corners that are on land only, for use by
	// algorithms that work only on land.  We return an array instead
	// of a vector because the redistribution algorithms want to sort
	// this array using Array.sortOn.
	public Vector<Corner> landCorners(Vector<Corner> corners) {
		Corner q; 
		Vector<Corner> locations = new Vector<Corner>();
		for (int i = 0; i < corners.size(); i++) {
			q = corners.get(i);
			if (!q.ocean && !q.coast) {
				locations.add(q);
			}
		}
		return locations;
	}


	// Build graph data structure in 'edges', 'centers', 'corners',
	// based on information in the Voronoi results: point.neighbors
	// will be a list of neighboring points of the same type (corner
	// or center); point.edges will be a list of edges that include
	// that point. Each edge connects to four points: the Voronoi edge
	// edge.{v0,v1} and its dual Delaunay triangle edge edge.{d0,d1}.
	// For boundary polygons, the Delaunay edge will have one null
	// point, and the Voronoi edge may be null.
	public void buildGraph(Vector<Point> points, Voronoi voronoi) 
	{
		Center p; 
		Corner q; 
		Point point;
		Point other;

		Vector<jMapGen.com.nodename.Delaunay.Edge> libedges = voronoi.getEdges();
		HashMap<Point, Center> centerLookup = new HashMap<Point, Center>();

		System.out.println("Starting buildGraph...");

		// Build Center objects for each of the points, and a lookup map
		// to find those Center objects again as we build the graph
		System.out.println("Building Centers from " + points.size() + " total Points");
		for(int i = 0; i < points.size(); i++) 
		{
			point = points.get(i);
			p = new Center();
			p.index = centers.size();
			p.point = point;
			p.neighbors = new  Vector<Center>();
			p.borders = new Vector<Edge>();
			p.corners = new Vector<Corner>();
			centers.add(p);
			centerLookup.put(point, p);
		}

		// Workaround for Voronoi lib bug: we need to call region()
		// before Edges or neighboringSites are available
		System.out.println("Calling region() " + centers.size() + " times for Voronoi lib bug fix.");
		for(int i = 0; i < centers.size(); i++) 
		{
			//System.out.println("Calling region() " + i + " time.");
			p = centers.get(i);
			voronoi.region(p.point);
		}

		//		for(int i = 0; i < voronoi.siteCoords().size(); i++) 
		//		{
		//			Point pnt = voronoi.siteCoords().get(i);
		//			mapgen2.graphics.setColor(Color.GREEN);
		//			mapgen2.graphics.drawString(""+i, (int)pnt.x, (int)pnt.y);
		//		}

		// The Voronoi library generates multiple Point objects for
		// corners, and we need to canonicalize to one Corner object.
		// To make lookup fast, we keep an array of Points, bucketed by
		// x value, and then we only have to look at other Points in
		// nearby buckets. When we fail to find one, we'll create a new
		// Corner object.
		Vector<Vector<Corner>> _cornerMap = new Vector<Vector<Corner>>();
		_cornerMap.setSize((int)SIZE);

		System.out.println("Delaunay Edges Size:" + libedges.size());
		for(int i = 0; i < libedges.size(); i++) 
		{
			jMapGen.com.nodename.Delaunay.Edge libedge = libedges.get(i);
			LineSegment dedge = libedge.delaunayLine();
			LineSegment vedge = libedge.voronoiEdge();

			// Fill the graph data. Make an Edge object corresponding to
			// the edge from the voronoi library.
			Edge edge = new Edge();
			edge.index = edges.size();
			edge.river = 0;
			edges.add(edge);
			edge.midpoint = vedge.p0 != null && vedge.p1 != null ? Point.interpolate(vedge.p0, vedge.p1, 0.5) : null;

			Corner c0 = makeCorner(vedge.p0, _cornerMap);
			Corner c1 = makeCorner(vedge.p1, _cornerMap);

			edge.setVoronoiEdge(c0, c1);
			edge.dCenter0 = centerLookup.get(dedge.p0);
			edge.dCenter1 = centerLookup.get(dedge.p1);

			// Centers point to edges. Corners point to edges.
			if (edge.dCenter0 != null) { edge.dCenter0.borders.add(edge); }
			if (edge.dCenter1 != null) { edge.dCenter1.borders.add(edge); }
			if (edge.vCorner0 != null) { edge.vCorner0.protrudes.add(edge); }
			if (edge.vCorner1 != null) { edge.vCorner1.protrudes.add(edge); }



			// Centers point to centers.
			if (edge.dCenter0 != null && edge.dCenter1 != null) 
			{
				addToCenterList(edge.dCenter0.neighbors, edge.dCenter1);
				addToCenterList(edge.dCenter1.neighbors, edge.dCenter0);
			}
			// Centers point to corners
			if (edge.dCenter0 != null) 
			{
				addToCornerList(edge.dCenter0.corners, edge.vCorner0);
				addToCornerList(edge.dCenter0.corners, edge.vCorner1);
			}
			if (edge.dCenter1 != null) 
			{
				addToCornerList(edge.dCenter1.corners, edge.vCorner0);
				addToCornerList(edge.dCenter1.corners, edge.vCorner1);
			}

			// Corners point to centers
			if (edge.vCorner0 != null) 
			{
				addToCenterList(edge.vCorner0.touches, edge.dCenter0);
				addToCenterList(edge.vCorner0.touches, edge.dCenter1);
			}
			if (edge.vCorner1 != null) 
			{
				addToCenterList(edge.vCorner1.touches, edge.dCenter0);
				addToCenterList(edge.vCorner1.touches, edge.dCenter1);
			}
		}

		System.out.println("Finished buildGraph...");
	}

	@SuppressWarnings("unchecked")
	public Corner makeCorner(Point point, Vector<Vector<Corner>> _cornerMap) 
	{
		Corner q;
		int bucket;

		if (point == null) 
			return null;

		int minBucket = (int)(point.x) - 1;
		int maxBucket = (int)(point.x) + 1;

		for (bucket = minBucket; bucket <= maxBucket; bucket++) 
		{
			Vector<Corner> cornermap = (Vector<Corner>) DelaunayUtil.getAtPosition(_cornerMap, bucket);
			for(int i = 0; cornermap != null && i < cornermap.size(); i++) 
			{
				q = cornermap.get(i);
				double dx = point.x - q.point.x;
				double dy = point.y - q.point.y;
				if (dx*dx + dy*dy < 1e-6) 
				{
					return q;
				}
			}
		}

		bucket = (int)(point.x);
		if (_cornerMap.size() <= bucket || _cornerMap.get(bucket) == null)
		{
			DelaunayUtil.setAtPosition(_cornerMap, bucket, new Vector<Corner>());
		}
		q = new Corner();
		q.index = corners.size();
		corners.add(q);

		q.point = point;
		q.border = (point.x == 0 || point.x == SIZE
				|| point.y == 0 || point.y == SIZE);
		q.touches = new Vector<Center>();
		q.protrudes = new Vector<Edge>();
		q.adjacent = new Vector<Corner>();		

		_cornerMap.get(bucket).add(q);

		return q;

	}


	void addToCornerList(Vector<Corner> v, Corner x) 
	{
		if (x != null && !v.contains(x)) { v.add(x); }
	}

	void addToCenterList(Vector<Center> v, Center x) 
	{
		if (x != null && v.indexOf(x) < 0) { v.add(x); }
	}


	// Determine elevations and water at Voronoi corners. By
	// construction, we have no local minima. This is important for
	// the downslope vectors later, which are used in the river
	// construction algorithm. Also by construction, inlets/bays
	// push low elevation areas inland, which means many rivers end
	// up flowing out through them. Also by construction, lakes
	// often end up on river paths because they don't raise the
	// elevation as much as other terrain does.
	public void assignCornerElevations() 
	{
		Corner baseCorner, adjacentCorner;
		LinkedList<Corner> queue = new LinkedList<Corner>();

		/**
		 * First we check each corner to see if it is land or water
		 * */
		for(int i = 0; i < corners.size(); i++)
		{
			baseCorner = corners.get(i);
			//mapgen2.graphics.setColor(Color.CYAN);
			//mapgen2.graphics.drawString(""+baseCorner.index, (int)baseCorner.point.x+2, (int)baseCorner.point.y-3);
			baseCorner.water = !inside(baseCorner.point);

			if (baseCorner.border) 
			{
				baseCorner.elevation = 0;
				queue.add(baseCorner);
			}
		}

		/**
		 * Next we assign the borders to have 0 elevation and all other corners to have MAX_VALUE. We also add
		 * the border points to a queue which contains all start points for elevation distribution.
		 */

		for(int i = 0; i < corners.size(); i++)
		{
			baseCorner = corners.get(i);
			// The edges of the map are elevation 0			
			for(int j = 0; j < baseCorner.protrudes.size(); j++)
			{
				Edge e = baseCorner.protrudes.get(j);
				//				mapgen2.graphics.setColor(Color.DARK_GRAY);
				//				mapgen2.graphics.drawLine((int)e.vCorner0.point.x, (int)e.vCorner0.point.y, 
				//						(int)e.vCorner1.point.x, (int)e.vCorner1.point.y);
			}
		}

		// Traverse the graph and assign elevations to each point. As we
		// move away from the map border, increase the elevations. This
		// guarantees that rivers always have a way down to the coast by
		// going downhill (no local minima).
		while (queue.size() > 0) {
			baseCorner = queue.pollFirst();

			//			mapgen2.graphics.setColor(Color.WHITE);
			//			mapgen2.graphics.fillRect((int)baseCorner.point.x-1, (int)baseCorner.point.y-1, 3,3);

			numberProcessed++;
			//mapgen2.graphics.drawString(""+baseCorner.index, (int)baseCorner.point.x+2, (int)baseCorner.point.y-3);

			for(int i = 0; i < baseCorner.adjacent.size(); i++)
			{

				adjacentCorner = baseCorner.adjacent.get(i);

				if(!adjacentCorner.border)
				{
					//					mapgen2.graphics.setColor(Color.RED);
					//					mapgen2.graphics.drawLine((int)baseCorner.point.x, (int)baseCorner.point.y, 
					//							(int)adjacentCorner.point.x, (int)adjacentCorner.point.y);

					try {
						//Thread.sleep(50);
					} catch (Exception e) {
						e.printStackTrace();
					}

					//					mapgen2.graphics.setColor(Color.LIGHT_GRAY);
					//					mapgen2.graphics.drawLine((int)baseCorner.point.x, (int)baseCorner.point.y, 
					//							(int)adjacentCorner.point.x, (int)adjacentCorner.point.y);


					// Every step up is epsilon over water or 1 over land. The
					// number doesn't matter because we'll rescale the
					// elevations later.				
					double newElevation = 0.01 + baseCorner.elevation;

					if (!baseCorner.water && !adjacentCorner.water) 
					{
						newElevation += 1;
					}
					// If this point changed, we'll add it to the queue so
					// that we can process its neighbors too.
					if (newElevation < adjacentCorner.elevation) 
					{
						//						mapgen2.graphics.setColor(Color.GRAY);
						//						mapgen2.graphics.drawLine((int)baseCorner.point.x, (int)baseCorner.point.y, 
						//								(int)adjacentCorner.point.x, (int)adjacentCorner.point.y);

						adjacentCorner.elevation = newElevation;

						if(newElevation > maxElevation)
							maxElevation = newElevation;
						queue.add(adjacentCorner);

						numberOfAdjacents++;
						//						mapgen2.graphics.setColor(Color.GREEN);
						//						mapgen2.graphics.fillRect((int)adjacentCorner.point.x-1, (int)adjacentCorner.point.y-1, 3,3);

					}
					else
					{
						//						mapgen2.graphics.setColor(Color.WHITE);
						//						mapgen2.graphics.fillRect((int)adjacentCorner.point.x-1, (int)adjacentCorner.point.y-1, 3,3);
					}
				}
			}
		}
		System.out.println("Number of Corners Total: " + corners.size());
		System.out.println("Number of Corners Processed: " + numberProcessed);
		System.out.println("Number of Adjacents Added: " + numberOfAdjacents);
	}
	private static int numberProcessed = 0;
	private static int numberOfAdjacents = 0;
	private static double maxElevation = 0;

	public Vector<Corner> sortElevation(Vector<Corner> locations)
	{
		Vector<Corner> locationsOut = new Vector<Corner>();
		for(Iterator<Corner> iter = locations.iterator(); iter.hasNext();)
		{
			Corner c = iter.next();
			for(int o = 0; o < locationsOut.size(); o++)
			{
				Corner cOut = locationsOut.get(o);
				if(cOut.elevation < c.elevation)
				{
					locationsOut.add(o, c);
					if(cOut.elevation < 0)
						cOut.elevation = 0;
					break;
				}
			}
		}
		return locationsOut;
	}

	public Vector<Corner> sortMoisture(Vector<Corner> locations)
	{
		Vector<Corner> locationsOut = new Vector<Corner>();
		for(Iterator<Corner> iter = locations.iterator(); iter.hasNext();)
		{
			Corner c = iter.next();
			for(int o = 0; o < locationsOut.size(); o++)
			{
				Corner cOut = locationsOut.get(o);
				if(cOut.moisture < c.moisture)
				{
					locationsOut.add(o, c);
					break;
				}
			}
		}
		return locationsOut;
	}

	// Change the overall distribution of elevations so that lower
	// elevations are more common than higher
	// elevations. Specifically, we want elevation X to have frequency
	// (1-X).  To do this we will sort the corners, then set each
	// corner to its desired elevation.
	public void redistributeElevations(Vector<Corner> locations) {
		// SCALE_FACTOR increases the mountain area. At 1.0 the maximum
		// elevation barely shows up on the map, so we set it to 1.1.
		double SCALE_FACTOR = 1.1;
		int i; 
		double y, x;

		Collections.sort(locations, new CornerElevationSorter());
		//sortElevation(locations);
		for (i = 0; i < locations.size(); i++) {
			// Let y(x) be the total area that we want at elevation <= x.
			// We want the higher elevations to occur less than lower
			// ones, and set the area to be y(x) = 1 - (1-x)^2.
			y = (double)i/(double)(locations.size()-1);
			// Now we have to solve for x, given the known y.
			//  *  y = 1 - (1-x)^2
			//  *  y = 1 - (1 - 2x + x^2)
			//  *  y = 2x - x^2
			//  *  x^2 - 2x + y = 0
			// From this we can use the quadratic equation to get:
			x = Math.sqrt(SCALE_FACTOR) - Math.sqrt(SCALE_FACTOR*(1-y));
			if (x > 1.0) x = 1.0;  // TODO: does this break downslopes?
			locations.get(i).elevation = x;
		}
	}


	// Change the overall distribution of moisture to be evenly distributed.
	public void redistributeMoisture(Vector<Corner> locations) {
		int i;
		sortMoisture(locations);
		for (i = 0; i < locations.size(); i++) {
			locations.get(i).moisture = i/(locations.size()-1);
		}
	}


	// Determine polygon and corner types: ocean, coast, land.
	public void assignOceanCoastAndLand() {
		// Compute polygon attributes 'ocean' and 'water' based on the
		// corner attributes. Count the water corners per
		// polygon. Oceans are all polygons connected to the edge of the
		// map. In the first pass, mark the edges of the map as ocean;
		// in the second pass, mark any water-containing polygon
		// connected an ocean as ocean.
		LinkedList<Center> queue = new LinkedList<Center>();
		Center p = null, r = null; 
		Corner q; 
		int numWater;

		for(int i = 0; i < centers.size(); i++)
		{
			p = centers.get(i);
			numWater = 0;
			for(int j = 0; j < p.corners.size(); j++)
			{
				q = p.corners.get(j);
				if (q.border) {
					p.border = true;
					p.ocean = true;
					q.water = true;
					queue.add(p);
				}
				if (q.water) {
					numWater += 1;
				}
			}
			p.water = (p.ocean || numWater >= p.corners.size() * LAKE_THRESHOLD);
		}
		while (queue.size() > 0) 
		{
			p = queue.pop();

			for(int j = 0; j < p.neighbors.size(); j++)
			{
				r = p.neighbors.get(j);
				if (r.water && !r.ocean) {
					r.ocean = true;
					queue.add(r);
				}
			}
		}

		int numOcean = 0;
		int numLand = 0;

		// Set the polygon attribute 'coast' based on its neighbors. If
		// it has at least one ocean and at least one land neighbor,
		// then this is a coastal polygon.
		for(int i = 0; i < centers.size(); i++)
		{
			p = centers.get(i);
			numOcean = 0;
			numLand = 0;

			for(int j = 0; j < p.neighbors.size(); j++)
			{
				r = p.neighbors.get(j);
				numOcean += (r.ocean ? 1 : 0);
				numLand += (!r.water ? 1 : 0);
			}

			p.coast = (numOcean > 0) && (numLand > 0);
		}


		// Set the corner attributes based on the computed polygon
		// attributes. If all polygons connected to this corner are
		// ocean, then it's ocean; if all are land, then it's land;
		// otherwise it's coast.
		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			numOcean = 0;
			numLand = 0;
			for(int i = 0; i < q.touches.size(); i++)
			{
				p = q.touches.get(i);
				numOcean += (p.ocean ? 1 : 0);
				numLand += (!p.water ? 1 : 0);
			}
			q.ocean = (numOcean == q.touches.size());
			q.coast = (numOcean > 0) && (numLand > 0);
			q.water = q.border || ((numLand != q.touches.size()) && !q.coast);

		}
	}


	// Polygon elevations are the average of the elevations of their corners.
	public void assignPolygonElevations() 
	{
		Center p; 
		Corner q; 
		double sumElevation;
		for(int i = 0; i < centers.size(); i++)
		{
			p = centers.get(i);
			sumElevation = 0.0;
			for(int j = 0; j < p.corners.size(); j++)
			{
				q = p.corners.get(j);
				sumElevation += q.elevation;
			}
			p.elevation = sumElevation / p.corners.size();
		}
	}


	// Calculate downslope pointers.  At every point, we point to the
	// point downstream from it, or to itself.  This is used for
	// generating rivers and watersheds.
	public void calculateDownslopes() 
	{
		Corner q, s, r;

		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			r = q;
			for(int i = 0; i < q.adjacent.size(); i++)
			{
				s= q.adjacent.get(i);
				if (s.elevation <= r.elevation) 
				{
					r = s;
				}
			}	
			q.downslope = r;
		}
	}


	// Calculate the watershed of every land point. The watershed is
	// the last downstream land point in the downslope graph. TODO:
	// watersheds are currently calculated on corners, but it'd be
	// more useful to compute them on polygon centers so that every
	// polygon can be marked as being in one watershed.
	public void calculateWatersheds() 
	{
		Corner q, r; int i; boolean changed;

		// Initially the watershed pointer points downslope one step.      
		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			q.watershed = q;
			if (!q.ocean && !q.coast) 
			{
				q.watershed = q.downslope;
			}
		}
		// Follow the downslope pointers to the coast. Limit to 100
		// iterations although most of the time with NUM_POINTS=2000 it
		// only takes 20 iterations because most points are not far from
		// a coast.  TODO: can run faster by looking at
		// p.watershed.watershed instead of p.downslope.watershed.
		for (i = 0; i < 100; i++) {
			changed = false;
			for(int j = 0; j < corners.size(); j++)
			{
				q = corners.get(j);
				if (!q.ocean && !q.coast && !q.watershed.coast) {
					r = q.downslope.watershed;
					if (!r.ocean) q.watershed = r;
					changed = true;
				}
			}
			if (!changed) break;
		}
		// How big is each watershed?
		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			r = q.watershed;
			r.watershed_size = 1 + ( r != null ? r.watershed_size : 0);
		}
	}


	// Create rivers along edges. Pick a random corner point, then
	// move downslope. Mark the edges and corners as rivers.
	public void createRivers() {
		int i; Corner q; Edge edge;

		for (i = 0; i < SIZE/6; i++) {
			q = corners.get(mapRandom.nextInt(corners.size()-1));

			if (q.ocean || q.elevation < 0.3 || q.elevation > 0.9) continue;

			while (!q.coast) {
				if (q == q.downslope) {
					break;
				}
				edge = lookupEdgeFromCorner(q, q.downslope);
				edge.river = edge.river + 1;
				q.river = (q != null ? q.river : 0) + 1;
				q.downslope.river = (q != null ? q.downslope.river : 0) + 1;  // TODO: fix double count
				q = q.downslope;
			}
		}
	}


	// Calculate moisture. Freshwater sources spread moisture: rivers
	// and lakes (not oceans). Saltwater sources have moisture but do
	// not spread it (we set it at the end, after propagation).
	public void assignCornerMoisture() 
	{
		Corner q, r; 
		double newMoisture;

		ArrayList<Corner> queue = new ArrayList<Corner>();
		// Fresh water
		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			if ((q.water || q.river > 0) && !q.ocean) {
				q.moisture = q.river > 0? Math.min(3.0, (0.2 * q.river)) : 1.0;
				queue.add(q);
			} else {
				q.moisture = 0.0;
			}
		}
		while (queue.size() > 0) 
		{
			q = queue.get(0);
			queue.remove(0);

			for(int j = 0; j < q.adjacent.size(); j++)
			{
				r = q.adjacent.get(j);
				newMoisture = q.moisture * 0.9;
				if (newMoisture > r.moisture) {
					r.moisture = newMoisture;
					queue.add(r);
				}
			}
		}
		// Salt water
		for(int j = 0; j < corners.size(); j++)
		{
			q = corners.get(j);
			if (q.ocean || q.coast) {
				q.moisture = 1.0;
			}
		}
	}


	// Polygon moisture is the average of the moisture at corners
	public void assignPolygonMoisture() {
		Center p; Corner q; double sumMoisture;
		for(int i = 0; i < centers.size(); i++)
		{
			p = centers.get(i);

			sumMoisture = 0.0;
			for(int j = 0; j < corners.size(); j++)
			{
				q = corners.get(j);
				if (q.moisture > 1.0) q.moisture = 1.0;
				sumMoisture += q.moisture;
			}
			p.moisture = sumMoisture / p.corners.size();
		}
	}


	// Assign a biome type to each polygon. If it has
	// ocean/coast/water, then that's the biome; otherwise it depends
	// on low/high elevation and low/medium/high moisture. This is
	// roughly based on the Whittaker diagram but adapted to fit the
	// needs of the island map generator.
	static public BiomeType getBiome(Center p) {
		if (p.ocean) {
			return BiomeType.OCEAN;
		} else if (p.water) {
			if (p.elevation < 0.1) return BiomeType.MARSH;
			if (p.elevation > 0.8) return BiomeType.ICE;
			return BiomeType.LAKE;
		} else if (p.coast) {
			return BiomeType.BEACH;
		} else if (p.elevation > 0.8) {
			if (p.moisture > 0.50) return BiomeType.SNOW;
			else if (p.moisture > 0.33) return BiomeType.TUNDRA;
			else if (p.moisture > 0.16) return BiomeType.BARE;
			else return BiomeType.SCORCHED;
		} else if (p.elevation > 0.6) {
			if (p.moisture > 0.66) return BiomeType.TAIGA;
			else if (p.moisture > 0.33) return BiomeType.SHRUBLAND;
			else return BiomeType.TEMPERATE_DESERT;
		} else if (p.elevation > 0.3) {
			if (p.moisture > 0.83) return BiomeType.TEMPERATE_RAIN_FOREST;
			else if (p.moisture > 0.50) return BiomeType.TEMPERATE_DECIDUOUS_FOREST;
			else if (p.moisture > 0.16) return BiomeType.GRASSLAND;
			else return BiomeType.TEMPERATE_DESERT;
		} else {
			if (p.moisture > 0.66) return BiomeType.TROPICAL_RAIN_FOREST;
			else if (p.moisture > 0.33) return BiomeType.TROPICAL_SEASONAL_FOREST;
			else if (p.moisture > 0.16) return BiomeType.GRASSLAND;
			else return BiomeType.SUBTROPICAL_DESERT;
		}
	}

	public void assignBiomes() 
	{
		for(int j = 0; j < centers.size(); j++)
		{
			Center p = centers.get(j);
			p.biome = getBiome(p);
		}
	}


	// Look up a Voronoi Edge object given two adjacent Voronoi
	// polygons, or two adjacent Voronoi corners
	public Edge lookupEdgeFromCenter(Center p, Center r) {
		for(int j = 0; j < p.borders.size(); j++)
		{
			Edge edge = p.borders.get(j);

			if (edge.dCenter0 == r || edge.dCenter1 == r) return edge;
		}
		return null;
	}

	public Edge lookupEdgeFromCorner(Corner q, Corner s) 
	{
		for(int j = 0; j < q.protrudes.size(); j++)
		{
			Edge edge = q.protrudes.get(j);
			if (edge.vCorner0 == s || edge.vCorner1 == s) return edge;
		}
		return null;
	}


	// Determine whether a given point should be on the island or in the water.
	public Boolean inside(Point p) 
	{
		return islandShape.insidePerlin(p);
	}

	double elevationBucket(Center p) 
	{
		if (p.ocean) return -1;
		else return Math.floor(p.elevation*10);
	}
	
	public Center getClosestCenter(Point p)
	{
		Center closest = centers.get(0);
		double distance = p.distanceSq(centers.get(0).point);
		
		for (int i = 1; i < centers.size(); i++)
		{
			double newDist = p.distanceSq(centers.get(i).point);
			if(newDist < distance)
			{
				distance = newDist;
				closest = centers.get(i);
			}
		}
		return closest;
	}
	
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
}