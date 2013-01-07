// Display the voronoi graph produced in Map.as
// Author: amitp@cs.stanford.edu
// License: MIT

package jMapGen
import jMapGen.graph.Edge;



public class mapgen2
{
	static public int SIZE = 600;

	// Island shape is controlled by the islandRandom seed and the
	// type of island. The islandShape function uses both of them to
	// determine whether any point should be water or land.
	static public int islandSeedInitial = 666;


	// The map data
	public Map map;
	public Roads roads;
	public Lava lava;
	public Watersheds watersheds;
	public NoisyEdges noisyEdges;


	public mapgen2() 
	{
		stage.scaleMode = 'noScale';
		stage.align = 'TL';

		addChild(noiseLayer);
		noiseLayer.bitmapData.noise(555, 128-10, 128+10, 7, true);
		noiseLayer.blendMode = BlendMode.HARDLIGHT;

		addChild(controls);

		addExportButtons();
		addViewButtons();
		addGenerateButtons();
		addMiscLabels();

		map = new Map(SIZE);
		go(islandType);
	}


	// Random parameters governing the overall shape of the island
	public void newIsland(String type) 
	{

	map.newIsland(type, seed, variant);
	}

	public void go(String type):void {
		cancelCommands();

		roads = new Roads();
		lava = new Lava();
		watersheds = new Watersheds();
		noisyEdges = new NoisyEdges();

		commandExecute("Shaping map...",
				function():void {
					newIsland(type);
				});

		commandExecute("Placing points...",
				function():void {
					map.go(0, 1);
					drawMap('polygons');
				});

		commandExecute("Improving points...",
				function():void {
					map.go(1, 2);
					drawMap('polygons');
				});

		commandExecute("Building graph...",
				function():void {
					map.go(2, 3);
					map.assignBiomes();
					drawMap('polygons');
				});

		commandExecute("Features...",
				function():void {
					map.go(3, 6);
					map.assignBiomes();
					drawMap('polygons');
				});

		commandExecute("Edges...",
				function():void {
					roads.createRoads(map);
					// lava.createLava(map, map.mapRandom.nextDouble);
					watersheds.createWatersheds(map);
					noisyEdges.buildNoisyEdges(map, lava, map.mapRandom);
					drawMap(mapMode);
				});
	}

}

