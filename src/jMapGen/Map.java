// Make a map out of a voronoi graph
// Original Author: amitp@cs.stanford.edu
// License: MIT
package jMapGen;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import jMapGen.com.nodename.Delaunay.Voronoi;
import jMapGen.com.nodename.geom.LineSegment;
import jMapGen.graph.Center;
import jMapGen.graph.Corner;
import jMapGen.graph.Edge;


public class Map 
{
	static int NUM_POINTS = 2000;
	static double LAKE_THRESHOLD = 0.3;  // 0 to 1, fraction of water corners for water polygon
	static int NUM_LLOYD_ITERATIONS = 2;


	// Passed in by the caller:
	public double SIZE;

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

	public Map(double size, int s) 
	{
		SIZE = size;
		seed = s;
	}

	// Random parameters governing the overall shape of the island
	public void newIsland(int seed) 
	{
		islandShape = new IslandShape(seed);
		mapRandom.setSeed(seed);
	}


	public void go() 
	{
		setup();
		//Generate the initial random set of points
		points = generateRandomPoints();
		improveRandomPoints(points);

		Rectangle R = new Rectangle();
		R.setFrame(0, 0, SIZE, SIZE);
		Voronoi voronoi = new Voronoi(points, R);
		
		buildGraph(points, voronoi);
		
		improveCorners();

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

		// Determine downslope paths.
		calculateDownslopes();

		// Determine watersheds: for every corner, where does it flow
		// out into the ocean? 
		calculateWatersheds();

		// Create rivers.
		createRivers();

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


	// Improve the random set of points with Lloyd Relaxation.
	public void improveRandomPoints(Vector<Point> points) 
	{
		// We'd really like to generate "blue noise". Algorithms:
		// 1. Poisson dart throwing: check each new point against all
		//     existing points, and reject it if it's too close.
		// 2. Start with a hexagonal grid and randomly perturb points.
		// 3. Lloyd Relaxation: move each point to the centroid of the
		//     generated Voronoi polygon, then generate Voronoi again.
		// 4. Use force-based layout algorithms to push points away.
		// 5. More at http://www.cs.virginia.edu/~gfx/pubs/antimony/
		// Option 3 is implemented here. If it's run for too many iterations,
		// it will turn into a grid, but convergence is very slow, and we only
		// run it a few times.
		int i;
		Point p, q; 
		Voronoi voronoi;
		Vector<Point> region;

		for (i = 0; i < NUM_LLOYD_ITERATIONS; i++) 
		{
			Rectangle R = new Rectangle();
			R.setFrame(0, 0, SIZE, SIZE);
			voronoi = new Voronoi(points ,R);

			for(int ind = 0; ind < points.size(); ind++)  
			{
				p = points.get(ind);
				region = voronoi.region(p);
				p.x = 0.0;
				p.y = 0.0;
				for(int ind2 = 0; ind2 < region.size(); ind2++) 
				{
					q = region.get(ind2);
					p.x += q.x;
					p.y += q.y;
				}
				p.x /= region.size();
				p.y /= region.size();
				//region.splice(0, region.size());
			}
		}
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
		Vector<Point> newCorners = new Vector<Point>(corners.size());

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
				newCorners.set(q.index, q.point);
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
				newCorners.set(q.index, point);
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
			if (edge.v0 != null && edge.v1 != null) 
			{
				edge.midpoint = Point.interpolate(edge.v0.point, edge.v1.point, 0.5);
			}
		}
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

		// Build Center objects for each of the points, and a lookup map
		// to find those Center objects again as we build the graph
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
		for(int i = 0; i < centers.size(); i++) 
		{
			p = centers.get(i);
			voronoi.region(p.point);
		}

		// The Voronoi library generates multiple Point objects for
		// corners, and we need to canonicalize to one Corner object.
		// To make lookup fast, we keep an array of Points, bucketed by
		// x value, and then we only have to look at other Points in
		// nearby buckets. When we fail to find one, we'll create a new
		// Corner object.
		Vector<Vector<Corner>> _cornerMap = new Vector<Vector<Corner>>();

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

			// Edges point to corners. Edges point to centers. 
			edge.v0 = makeCorner(vedge.p0, _cornerMap);
			edge.v1 = makeCorner(vedge.p1, _cornerMap);
			edge.d0 = centerLookup.get(dedge.p0);
			edge.d1 = centerLookup.get(dedge.p1);

			// Centers point to edges. Corners point to edges.
			if (edge.d0 != null) { edge.d0.borders.add(edge); }
			if (edge.d1 != null) { edge.d1.borders.add(edge); }
			if (edge.v0 != null) { edge.v0.protrudes.add(edge); }
			if (edge.v1 != null) { edge.v1.protrudes.add(edge); }



			// Centers point to centers.
			if (edge.d0 != null && edge.d1 != null) 
			{
				addToCenterList(edge.d0.neighbors, edge.d1);
				addToCenterList(edge.d1.neighbors, edge.d0);
			}

			// Corners point to corners
			if (edge.v0 != null && edge.v1 != null) 
			{
				addToCornerList(edge.v0.adjacent, edge.v1);
				addToCornerList(edge.v1.adjacent, edge.v0);
			}

			// Centers point to corners
			if (edge.d0 != null) 
			{
				addToCornerList(edge.d0.corners, edge.v0);
				addToCornerList(edge.d0.corners, edge.v1);
			}
			if (edge.d1 != null) 
			{
				addToCornerList(edge.d1.corners, edge.v0);
				addToCornerList(edge.d1.corners, edge.v1);
			}

			// Corners point to centers
			if (edge.v0 != null) 
			{
				addToCenterList(edge.v0.touches, edge.d0);
				addToCenterList(edge.v0.touches, edge.d1);
			}
			if (edge.v1 != null) 
			{
				addToCenterList(edge.v1.touches, edge.d0);
				addToCenterList(edge.v1.touches, edge.d1);
			}
		}
	}

	public Corner makeCorner(Point point, Vector<Vector<Corner>> _cornerMap) 
	{
		Corner q;
		int bucket;

		if (point == null) return null;

		for (bucket = (int) ((point.x)-1); bucket <= (int)((point.x)+1); bucket++) 
		{
			for(int i = 0; i < _cornerMap.get(bucket).size(); i++) 
			{
				q = _cornerMap.get(bucket).get(i);
				double dx = point.x - q.point.x;
				double dy = point.y - q.point.y;
				if (dx*dx + dy*dy < 1e-6) 
				{
					return q;
				}
			}
		}
		bucket = (int)(point.x);
		if (_cornerMap.get(bucket) != null) 
		{
			_cornerMap.set(bucket, null);
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

		Vector<Corner> map = new Vector<Corner>();
		map.add(q);
		_cornerMap.set(bucket, map);
		return q;

	}


	void addToCornerList(Vector<Corner> v, Corner x) 
	{
		if (x != null && v.indexOf(x) < 0) { v.add(x); }
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
		Corner q, s;
		Vector<Corner> queue = new Vector<Corner>();

		for(int i = 0; i < corners.size(); i++)
		{
			q = corners.get(i);
			q.water = !inside(q.point);
		}

		for(int i = 0; i < corners.size(); i++)
		{
			q = corners.get(i);
			// The edges of the map are elevation 0
			if (q.border) {
				q.elevation = 0.0;
				queue.add(q);
			} else {
				q.elevation = Double.MAX_VALUE;
			}
		}
		// Traverse the graph and assign elevations to each point. As we
		// move away from the map border, increase the elevations. This
		// guarantees that rivers always have a way down to the coast by
		// going downhill (no local minima).
		while (queue.size() > 0) {
			q = queue.firstElement();
			queue.remove(0);

			for(int i = 0; i < q.adjacent.size(); i++)
			{
				s = q.adjacent.get(i);
				// Every step up is epsilon over water or 1 over land. The
				// number doesn't matter because we'll rescale the
				// elevations later.
				double newElevation = 0.01 + q.elevation;
				if (!q.water && !s.water) {
					newElevation += 1;
				}
				// If this point changed, we'll add it to the queue so
				// that we can process its neighbors too.
				if (newElevation < s.elevation) {
					s.elevation = newElevation;
					queue.add(s);
				}
			}
		}
	}

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

		sortElevation(locations);
		for (i = 0; i < locations.size(); i++) {
			// Let y(x) be the total area that we want at elevation <= x.
			// We want the higher elevations to occur less than lower
			// ones, and set the area to be y(x) = 1 - (1-x)^2.
			y = i/(locations.size()-1);
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
		ArrayList<Center> queue = new ArrayList<Center>();
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
			p = queue.get(0);
			queue.remove(0);

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
		for(int j = 0; j < p.corners.size(); j++)
		{
			q = p.corners.get(j);
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
			for(int j = 0; j < corners.size(); j++)
			{
				q = corners.get(j);
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

		for (i = 0; i < SIZE/2; i++) {
			q = corners.get(mapRandom.nextInt(corners.size()-1));
			if (q.ocean || q.elevation < 0.3 || q.elevation > 0.9) continue;
			// Bias rivers to go west: if (q.downslope.x > q.x) continue;
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

			if (edge.d0 == r || edge.d1 == r) return edge;
		}
		return null;
	}

	public Edge lookupEdgeFromCorner(Corner q, Corner s) 
	{
		for(int j = 0; j < q.protrudes.size(); j++)
		{
			Edge edge = q.protrudes.get(j);
			if (edge.v0 == s || edge.v1 == s) return edge;
		}
		return null;
	}


	// Determine whether a given point should be on the island or in the water.
	public Boolean inside(Point p) 
	{
		return islandShape.insidePerlin(new Point(2*(p.x/SIZE - 0.5), 2*(p.y/SIZE - 0.5)));
	}

	double elevationBucket(Center p) 
	{
		if (p.ocean) return -1;
		else return Math.floor(p.elevation*10);
	}

	public Voronoi buildVoronoiGraph()
	{
		Rectangle R = new Rectangle();
		R.setFrame(0, 0, SIZE, SIZE);
		Voronoi voronoi = new Voronoi(points, R);
		buildGraph(points, voronoi);
		improveCorners();

		return voronoi;
	}
}