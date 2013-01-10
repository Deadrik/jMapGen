/*
 * The author of this software is Steven Fortune.  Copyright (c) 1994 by AT&T
 * Bell Laboratories.
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 */
package jMapGen.com.nodename.Delaunay;

import jMapGen.Point;
import jMapGen.com.nodename.geom.Circle;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Vector;

public class Voronoi
{
	private SiteList _sites;
	private HashMap<Point, Site> _sitesIndexedByLocation;
	private Vector<Triangle> _triangles;
	private Vector<Edge> _edges;


	// TODO generalize this so it doesn't have to be a rectangle;
	// then we can make the fractal voronois-within-voronois
	private Rectangle _plotBounds;
	public Rectangle getPlotBounds()
	{
		return _plotBounds;
	}

	public Voronoi(Vector<Point> points, Rectangle plotBounds)
	{
		_sites = new SiteList();
		_sitesIndexedByLocation = new HashMap<Point, Site>();
		addSites(points);
		_plotBounds = plotBounds;
		_triangles = new Vector<Triangle>();
		_edges = new Vector<Edge>();
		fortunesAlgorithm();
	}

	private void addSites(Vector<Point> points)
	{
		int length = points.size();
		for (int i = 0; i < length; ++i)
		{
			addSite(points.get(i), i);
		}
	}

	private void addSite(Point p, int index)
	{
		Site site = Site.create(p, index);
		_sites.push(site);
		_sitesIndexedByLocation.put(p, site);
	}

	public Vector<Edge> getEdges()
	{
		return _edges;
	}

	public Vector<Point> region(Point p)
	{
		Site site = _sitesIndexedByLocation.get(p);
		if (site == null)
		{
			return new Vector<Point>();
		}
		return site.region(_plotBounds);
	}

	// TODO: bug: if you call this before you call region(), something goes wrong :(
	public Vector<Point> neighborSitesForSite(Point coord)
	{
		Vector<Point> points = new Vector<Point>();
		Site site = _sitesIndexedByLocation.get(coord);
		if (site == null)
		{
			return points;
		}
		Vector<Site> sites = site.neighborSites();
		for (int i = 0; i < sites.size(); ++i)
		{
			Site neighbor = sites.get(i);
			points.add(neighbor.getCoord());
		}
		return points;
	}

	public Vector<Circle> circles()
	{
		return _sites.circles();
	}

//	public Vector<LineSegment> voronoiBoundaryForSite(Point coord)
//	{
//		return visibleLineSegments(selectEdgesForSitePoint(coord, _edges));
//	}
//
//	public Vector<LineSegment> delaunayLinesForSite(Point coord)
//	{
//		return delaunayLinesForEdges(selectEdgesForSitePoint(coord, _edges));
//	}
//
//	public Vector<LineSegment> voronoiDiagram()
//	{
//		return visibleLineSegments(_edges);
//	}

	//			public Vector<LineSegment> delaunayTriangulation(BitmapData keepOutMask)
	//			{
	//				return delaunayLinesForEdges(selectNonIntersectingEdges(keepOutMask, _edges));
	//			}

//	public Vector<LineSegment> hull()
//	{
//		return delaunayLinesForEdges(hullEdges());
//	}

	private Vector<Edge> hullEdges()
	{
		Vector<Edge> outEdges = new Vector<Edge>();
		for(int iter = 0; iter < _edges.size(); iter++)
		{
			if(_edges.get(iter).isPartOfConvexHull())
				outEdges.add(_edges.get(iter));
		}
		return outEdges;
	}

	public Vector<Point> hullPointsInOrder()
	{
		Vector<Edge> hullEdges = hullEdges();

		Vector<Point> points = new Vector<Point>();
		if (hullEdges.size() == 0)
		{
			return points;
		}

		EdgeReorderer reorderer = null;
		try {
			reorderer = new EdgeReorderer(hullEdges, Site.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hullEdges = reorderer.getEdges();
		Vector<LR> orientations = reorderer.getEdgeOrientations();
		reorderer.dispose();

		LR orientation;

		int n = hullEdges.size();
		for (int i = 0; i < n; ++i)
		{
			Edge edge = hullEdges.get(i);
			orientation = orientations.get(i);
			points.add(edge.site(orientation).getCoord());
		}
		return points;
	}

	//			public function spanningTree(type:String = "minimum", keepOutMask:BitmapData = null):Vector.<LineSegment>
	//			{
	//				var edges:Vector.<Edge> = selectNonIntersectingEdges(keepOutMask, _edges);
	//			var segments:Vector.<LineSegment> = delaunayLinesForEdges(edges);
	//			return kruskal(segments, type);
	//			}

	public Vector<Vector<Point>> regions()
	{
		return _sites.regions(_plotBounds);
	}


	/**
	 * 
	 * @param proximityMap a BitmapData whose regions are filled with the site index values; see PlanePointsCanvas::fillRegions()
	 * @param x
	 * @param y
	 * @return coordinates of nearest Site to (x, y)
	 * 
	 */
	//			public function nearestSitePoint(proximityMap:BitmapData, x:Number, y:Number):Point
	//			{
	//				return _sites.nearestSitePoint(proximityMap, x, y);
	//			}

	public Vector<Point> siteCoords()
	{
		return _sites.siteCoords();
	}
	
	private static Site bottomMostSite = null;

	private void fortunesAlgorithm()
	{
		Site newSite, bottomSite, topSite, tempSite;
		Vertex v, vertex;
		Point newintstar = new Point();
		LR leftRight;
		Halfedge lbnd, rbnd, llbnd, rrbnd, bisector;
		Edge edge;

		Rectangle dataBounds = _sites.getSitesBounds();

		int sqrt_nsites = (int) (Math.sqrt(_sites.getLength() + 4));
		HalfedgePriorityQueue heap = new HalfedgePriorityQueue(dataBounds.y, dataBounds.height, sqrt_nsites);
		EdgeList edgeList = new EdgeList(dataBounds.x, dataBounds.width, sqrt_nsites);
		Vector<Halfedge>  halfEdges = new Vector<Halfedge>();
		Vector<Vertex> vertices = new Vector<Vertex>();

		bottomMostSite = _sites.next();
		newSite = _sites.next();

		for (;;)
		{
			if (heap.empty() == false)
			{
				newintstar = heap.min();
			}

			if (newSite != null 
					&&  (heap.empty() || compareByYThenX(newSite, newintstar) < 0))
			{
				/* new site is smallest */
				//trace("smallest: new site " + newSite);

				// Step 8:
				lbnd = edgeList.edgeListLeftNeighbor(newSite.getCoord());	// the Halfedge just to the left of newSite
				//trace("lbnd: " + lbnd);
				rbnd = lbnd.edgeListRightNeighbor;		// the Halfedge just to the right
				//trace("rbnd: " + rbnd);
				bottomSite = rightRegion(lbnd);		// this is the same as leftRegion(rbnd)
				// this Site determines the region containing the new site
				//trace("new Site is in region of existing site: " + bottomSite);

				// Step 9:
				edge = Edge.createBisectingEdge(bottomSite, newSite);
				//trace("new edge: " + edge);
				_edges.add(edge);

				bisector = Halfedge.create(edge, LR.LEFT);
				halfEdges.add(bisector);
				// inserting two Halfedges into edgeList constitutes Step 10:
				// insert bisector to the right of lbnd:
				edgeList.insert(lbnd, bisector);

				// first half of Step 11:
				if ((vertex = Vertex.intersect(lbnd, bisector)) != null) 
				{
					vertices.add(vertex);
					heap.remove(lbnd);
					lbnd.vertex = vertex;
					lbnd.ystar = vertex.getY() + newSite.dist(vertex);
					heap.insert(lbnd);
				}

				lbnd = bisector;
				bisector = Halfedge.create(edge, LR.RIGHT);
				halfEdges.add(bisector);
				// second Halfedge for Step 10:
				// insert bisector to the right of lbnd:
				edgeList.insert(lbnd, bisector);

				// second half of Step 11:
				if (rbnd != null && (vertex = Vertex.intersect(bisector, rbnd)) != null)
				{
					vertices.add(vertex);
					bisector.vertex = vertex;
					bisector.ystar = vertex.getY() + newSite.dist(vertex);
					heap.insert(bisector);	
				}

				newSite = _sites.next();	
			}
			else if (heap.empty() == false) 
			{
				/* intersection is smallest */
				lbnd = heap.extractMin();
				llbnd = lbnd.edgeListLeftNeighbor;
				rbnd = lbnd.edgeListRightNeighbor;
				rrbnd = rbnd.edgeListRightNeighbor;
				bottomSite = leftRegion(lbnd);
				topSite = rightRegion(rbnd);
				// these three sites define a Delaunay triangle
				// (not actually using these for anything...)
				//_triangles.push(new Triangle(bottomSite, topSite, rightRegion(lbnd)));

				v = lbnd.vertex;
				v.setIndex();
				lbnd.edge.setVertex(lbnd.leftRight, v);
				rbnd.edge.setVertex(rbnd.leftRight, v);
				edgeList.remove(lbnd); 
				heap.remove(rbnd);
				edgeList.remove(rbnd); 
				leftRight = LR.LEFT;
				if (bottomSite.getY() > topSite.getY())
				{
					tempSite = bottomSite; bottomSite = topSite; topSite = tempSite; leftRight = LR.RIGHT;
				}
				edge = Edge.createBisectingEdge(bottomSite, topSite);
				_edges.add(edge);
				bisector = Halfedge.create(edge, leftRight);
				halfEdges.add(bisector);
				edgeList.insert(llbnd, bisector);
				edge.setVertex(LR.other(leftRight), v);
				if ((vertex = Vertex.intersect(llbnd, bisector)) != null)
				{
					vertices.add(vertex);
					heap.remove(llbnd);
					llbnd.vertex = vertex;
					llbnd.ystar = vertex.getY() + bottomSite.dist(vertex);
					heap.insert(llbnd);
				}
				if ((vertex = Vertex.intersect(bisector, rrbnd)) != null)
				{
					vertices.add(vertex);
					bisector.vertex = vertex;
					bisector.ystar = vertex.getY() + bottomSite.dist(vertex);
					heap.insert(bisector);
				}
			}
			else
			{
				break;
			}
		}

		// we need the vertices to clip the edges
		for(int i = 0; i < _edges.size(); i++)
		{
			edge = _edges.get(i);
			edge.clipVertices(_plotBounds);
		}
	}
	
	public static Site leftRegion(Halfedge he)
	{
		Edge edge = he.edge;
		if (edge == null)
		{
			return bottomMostSite;
		}
		return edge.site(he.leftRight);
	}

	public static  Site rightRegion(Halfedge he)
	{
		Edge edge = he.edge;
		if (edge == null)
		{
			return bottomMostSite;
		}
		return edge.site(LR.other(he.leftRight));
	}

	public static int compareByYThenX(Site s1, Site s2)
	{
		if (s1.getY() < s2.getY()) return -1;
		if (s1.getY() > s2.getY()) return 1;
		if (s1.getX() < s2.getX()) return -1;
		if (s1.getX() > s2.getX()) return 1;
		return 0;
	}
	
	public static int compareByYThenX(Site s1, Point s2)
	{
		if (s1.getY() < s2.getY()) return -1;
		if (s1.getY() > s2.getY()) return 1;
		if (s1.getX() < s2.getX()) return -1;
		if (s1.getX() > s2.getX()) return 1;
		return 0;
	}

}