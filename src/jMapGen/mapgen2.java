// Display the voronoi graph produced in Map.as
// Author: amitp@cs.stanford.edu
// License: MIT

package jMapGen;

import java.awt.Graphics2D;

import za.co.iocom.math.MathUtil;

import jMapGen.graph.Edge;

public class mapgen2
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


	public mapgen2(int s, Graphics2D g) 
	{
		islandSeedInitial = s;
		graphics = g;
		map = new Map(SIZE, islandSeedInitial);
		go();
	}
	
	public mapgen2(int s) 
	{
		islandSeedInitial = s;
		map = new Map(SIZE, islandSeedInitial);
		go();
	}


	// Random parameters governing the overall shape of the island
	public void newIsland() 
	{
		map.newIsland(islandSeedInitial);
	}

	public void go() 
	{
		//roads = new Roads();
		lava = new Lava();
		watersheds = new Watersheds();
		//noisyEdges = new NoisyEdges();

		newIsland();

		map.go();

		//roads.createRoads(map);
		// lava.createLava(map, map.mapRandom.nextDouble);
		watersheds.createWatersheds(map);
		//noisyEdges.buildNoisyEdges(map, lava, map.mapRandom);
	}

}

