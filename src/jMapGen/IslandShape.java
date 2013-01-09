package jMapGen;

import java.util.Random;

import net.royawesome.jlibnoise.model.Plane;
import net.royawesome.jlibnoise.module.Module;
import net.royawesome.jlibnoise.module.modifier.Clamp;
import net.royawesome.jlibnoise.module.modifier.ScalePoint;
import net.royawesome.jlibnoise.module.source.Perlin;


//Factory class to build the 'inside' function that tells us whether
//a point should be on the island or in the water.
public class IslandShape 
{
	// This class has factory functions for generating islands of
	// different shapes. The factory returns a function that takes a
	// normalized point (x and y are -1 to +1) and returns true if the
	// point should be on the island, and false if it should be water
	// (lake or ocean).

	public Module baseModule;

	// The radial island radius is based on overlapping sine waves 
	static public double ISLAND_FACTOR = 1.07;  // 1.0 means no small islands; 2.0 leads to a lot

	static public boolean makeRadial(int seed, Point point) 
	{
		Random islandRandom = new Random();
		islandRandom.setSeed(seed);
		int bumps = 1 + islandRandom.nextInt(6);
		double startAngle = islandRandom.nextDouble() * (2*Math.PI);
		double dipAngle = islandRandom.nextDouble() * (2*Math.PI);
		double dipWidth = 0.2 + islandRandom.nextDouble()*0.5;

		boolean inside = false;
		double angle = Math.atan2(point.y, point.x);
		double length = 0.5 * (Math.max(Math.abs(point.x), Math.abs(point.y)) + point.getLength());

		double r1 = 0.5 + 0.40*Math.sin(startAngle + bumps*angle + Math.cos((bumps+3)*angle));
		double r2 = 0.7 - 0.20*Math.sin(startAngle + bumps*angle - Math.sin((bumps+2)*angle));
		if (Math.abs(angle - dipAngle) < dipWidth
				|| Math.abs(angle - dipAngle + 2*Math.PI) < dipWidth
				|| Math.abs(angle - dipAngle - 2*Math.PI) < dipWidth) {
			r1 = r2 = 0.2;
		}
		inside = (length < r1 || (length > r1*ISLAND_FACTOR && length < r2));


		return inside;
	}


	// The Perlin-based island combines perlin noise with the radius
	public IslandShape (int seed) 
	{
		Perlin perlinModule = new Perlin();
		perlinModule.setSeed(seed);
		perlinModule.setFrequency(0.001);
		perlinModule.setOctaveCount(6);
		perlinModule.setPersistence(0.5);
		perlinModule.setLacunarity(2.3);

		ScalePoint spModule = new ScalePoint();
		spModule.setSourceModule(0, perlinModule);
		spModule.setxScale(0.5);
		spModule.setyScale(0.5);
		spModule.setzScale(0.5);

		Clamp clModule = new Clamp();
		clModule.setSourceModule(0, spModule);
		clModule.setLowerBound(-1);
		clModule.setUpperBound(1);

		baseModule = clModule;
	}
	
	public boolean insidePerlin(Point q)
	{
		Plane perlin = new Plane(baseModule);
		double height = (perlin.getModule().GetValue(q.x+1, 0, q.y+1)+1)/2;
		return height > (0.3 + 0.3 * q.getLength() * q.getLength());
	}
}
