package jMapGen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

import net.royawesome.jlibnoise.NoiseQuality;
import net.royawesome.jlibnoise.model.Plane;
import net.royawesome.jlibnoise.module.Module;
import net.royawesome.jlibnoise.module.modifier.Clamp;
import net.royawesome.jlibnoise.module.modifier.ScaleBias;
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

	int bumps;
	double startAngle;
	double dipAngle;
	double dipWidth;
	
	double oceanRatio = 0.5;

	// The radial island radius is based on overlapping sine waves 
	static public double ISLAND_FACTOR = 1.07;  // 1.0 means no small islands; 2.0 leads to a lot

	public boolean insideRadial(Point point) {
		double angle = Math.atan2(point.y, point.x);
		double length = 0.5 * (Math.max(Math.abs(point.x), Math.abs(point.y)) + point.getLength());

		double r1 = 0.5 + 0.40*Math.sin(startAngle + bumps*angle + Math.cos((bumps+3)*angle));
		double r2 = 0.7 - 0.20*Math.sin(startAngle + bumps*angle - Math.sin((bumps+2)*angle));
		if (Math.abs(angle - dipAngle) < dipWidth
				|| Math.abs(angle - dipAngle + 2*Math.PI) < dipWidth
				|| Math.abs(angle - dipAngle - 2*Math.PI) < dipWidth) {
			r1 = r2 = 0.2;
		}
		return (length < r1 || (length > r1*ISLAND_FACTOR && length < r2));
	}


	// The Perlin-based island combines perlin noise with the radius
	public IslandShape (int seed, double oceans) 
	{
		double landRatioMinimum = 0.1;
		double landRatioMaximum = 0.5;
		oceanRatio = ((landRatioMaximum - landRatioMinimum) * oceans) + landRatioMinimum;
		
		Random islandRandom = new Random();
		islandRandom.setSeed(seed);
		bumps = 3 + islandRandom.nextInt(6);
		startAngle = islandRandom.nextDouble() * (2*Math.PI);
		dipAngle = islandRandom.nextDouble() * (2*Math.PI);
		dipWidth = 0.2 + islandRandom.nextDouble()*0.5;

		Perlin modulePerl = new Perlin();
		modulePerl.setSeed(seed);
		modulePerl.setFrequency(0.004);
		modulePerl.setOctaveCount(8);
		modulePerl.setNoiseQuality(NoiseQuality.BEST);
		
		ScaleBias sb = new ScaleBias();
		sb.setSourceModule(0, modulePerl);
		sb.setBias(0.6);
		sb.setScale(1.5);

		Clamp moduleClamp = new Clamp();
		moduleClamp.setSourceModule(0, sb);
		moduleClamp.setLowerBound(0);
		moduleClamp.setUpperBound(1);
		
		Perlin modulePerl2 = new Perlin();
		modulePerl2.setSeed(seed+1);
		modulePerl2.setFrequency(0.3);
		modulePerl2.setOctaveCount(8);
		modulePerl2.setNoiseQuality(NoiseQuality.BEST);

		module = moduleClamp;

//		try
//		{
//			BufferedImage outBitmap = new BufferedImage(640,640,BufferedImage.TYPE_INT_RGB);
//			Graphics2D g = (Graphics2D) outBitmap.getGraphics();
//
//			for(int x = 0; x < 640; x++)
//			{
//				for(int z = 0; z < 640; z++)
//				{
//					float h = (float) (module.GetValue(x, 0, z));
//					g.setColor(Color.getHSBColor(0, 0, h));
//					g.fillRect(x, z, 1, 1);
//
//				}
//			}
//			ImageIO.write(outBitmap, "BMP", new File("hm-perlin.bmp"));
//		}
//		catch(Exception e){e.printStackTrace();}
	}

	public Module module;

	public boolean insidePerlin(Point q)
	{
		Point np = new Point(2*(q.x/640 - 0.5), 2*(q.y/640 - 0.5));
		double height = (module.GetValue(q.x, 0, q.y))/1;

		return height > oceanRatio+oceanRatio*np.getLength()*np.getLength();
	}
	
	public double elevPerlin(Point q)
	{
		Plane perlin = new Plane(module);
		Point np = new Point(2*(q.x/640 - 0.5), 2*(q.y/640 - 0.5));
		double height = (perlin.getModule().GetValue(q.x, 0, q.y))/1;

		return height;
	}
}
