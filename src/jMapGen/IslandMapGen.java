// Display the voronoi graph produced in Map.as
// Author: amitp@cs.stanford.edu
// License: MIT

package jMapGen;

import java.awt.Graphics2D;

import za.co.iocom.math.MathUtil;

import jMapGen.graph.Edge;

public class IslandMapGen
{
	static public int SIZE = 1024;

	// Island shape is controlled by the islandRandom seed and the
	// type of island. The islandShape function uses both of them to
	// determine whether any point should be water or land.
	static public int islandSeedInitial = 665;


	// The map data
	public Map map;
	//public Roads roads;
	public Lava lava;
	public Watersheds watersheds;
	public NoisyEdges noisyEdges;
	
	public static Graphics2D graphics;


	public IslandMapGen(int s, Graphics2D g) 
	{
		islandSeedInitial = s;
		graphics = g;
		map = new Map(SIZE, islandSeedInitial);
	}
	
	public IslandMapGen(int s) 
	{
		islandSeedInitial = s;
		map = new Map(SIZE, islandSeedInitial);
	}

	public void createNewIsland() 
	{
		watersheds = new Watersheds();
		map.newIsland(islandSeedInitial);
		map.go();
		watersheds.createWatersheds(map);
	}
}

