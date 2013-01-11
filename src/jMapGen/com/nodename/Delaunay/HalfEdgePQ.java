package jMapGen.com.nodename.Delaunay;

import java.util.LinkedList;

public class HalfEdgePQ 
{
	private LinkedList<Halfedge> _list = new LinkedList<Halfedge>();
    private int _maxSize;
    
    public HalfEdgePQ(int maxSize)
    {
        _maxSize = maxSize;
    }
	
}
