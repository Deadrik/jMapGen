package jMapGen.graph;

import jMapGen.BiomeType;
import jMapGen.Point;
import java.util.Vector;

  
  public class Center 
  {
    public int index;
  
    public Point point;  // location
    public Boolean water;  // lake or ocean
    public Boolean ocean;  // ocean
    public Boolean coast;  // land polygon touching an ocean
    public Boolean border;  // at the edge of the map
    public BiomeType biome;  // biome type (see article)
    public double elevation;  // 0.0-1.0
    public double moisture;  // 0.0-1.0

    public Vector<Center> neighbors;
    public Vector<Edge> borders;
    public Vector<Corner> corners;
  }
