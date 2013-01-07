package jMapGen;


//Factory class to build the 'inside' function that tells us whether
//a point should be on the island or in the water.
class IslandShape 
{
	// This class has factory functions for generating islands of
	// different shapes. The factory returns a function that takes a
	// normalized point (x and y are -1 to +1) and returns true if the
	// point should be on the island, and false if it should be water
	// (lake or ocean).


	// The radial island radius is based on overlapping sine waves 
	static public double ISLAND_FACTOR = 1.07;  // 1.0 means no small islands; 2.0 leads to a lot

	static public IslandShape makeRadial(int seed) 
	{
		Random islandRandom = new Random();
		islandRandom.seed = seed;
		int bumps = islandRandom.nextIntRange(1, 6);
		double startAngle = islandRandom.nextDoubleRange(0, 2*Math.PI);
		double dipAngle = islandRandom.nextDoubleRange(0, 2*Math.PI);
		double dipWidth = islandRandom.nextDoubleRange(0.2, 0.7);

		boolean inside(Point q) 
		{
			double angle = Math.atan2(q.y, q.x);
			double length = 0.5 * (Math.max(Math.abs(q.x), Math.abs(q.y)) + q.getLength());

			double r1 = 0.5 + 0.40*Math.sin(startAngle + bumps*angle + Math.cos((bumps+3)*angle));
			double r2 = 0.7 - 0.20*Math.sin(startAngle + bumps*angle - Math.sin((bumps+2)*angle));
			if (Math.abs(angle - dipAngle) < dipWidth
					|| Math.abs(angle - dipAngle + 2*Math.PI) < dipWidth
					|| Math.abs(angle - dipAngle - 2*Math.PI) < dipWidth) {
				r1 = r2 = 0.2;
			}
			return  (length < r1 || (length > r1*ISLAND_FACTOR && length < r2));
		}

		return inside;
	}


	// The Perlin-based island combines perlin noise with the radius
	static public IslandShape makePerlin(int seed) 
	{
		var perlin:BitmapData = new BitmapData(256, 256);
	perlin.perlinNoise(64, 64, 8, seed, false, true);

	return function (q:Point):Boolean {
		var c:Number = (perlin.getPixel(int((q.x+1)*128), int((q.y+1)*128)) & 0xff) / 255.0;
	return c > (0.3+0.3*q.length*q.length);
	};
	}
