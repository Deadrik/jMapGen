package jMapGen.graph;

import jMapGen.BiomeType;
import jMapGen.Point;
import java.util.Vector;

  
  public class Center 
  {
    public int index;
  
    public Point point;  // location
    public Boolean water = false;  // lake or ocean
    public Boolean ocean = false;  // ocean
    public Boolean coast = false;  // land polygon touching an ocean
    public Boolean border = false;  // at the edge of the map
    public BiomeType biome;  // biome type (see article)
    public double elevation = 0;  // 0.0-1.0
    public double moisture = 0;  // 0.0-1.0

    public Vector<Center> neighbors;
    public Vector<Edge> borders;
    public Vector<Corner> corners;
  }
