// Define watersheds: if a drop of rain falls on any polygon, where
// does it exit the island? We follow the map corner downslope field.
// Author: amitp@cs.stanford.edu
// License: MIT

package jMapGen;

import java.util.ArrayList;

import jMapGen.graph.Center;
import jMapGen.graph.Corner;

  
  public class Watersheds {
    public ArrayList<Integer> lowestCorner = new ArrayList<Integer>();  // polygon index -> corner index
    public ArrayList<Integer> watersheds = new ArrayList<Integer>();  // polygon index -> corner index

    // We want to mark each polygon with the corner where water would
    // exit the island.
    public void createWatersheds(Map map) {
    	Center p; Corner q, s;

      // Find the lowest corner of the polygon, and set that as the
      // exit point for rain falling on this polygon
      for(int i = 0; i < map.centers.size(); i++) {
    	  p = map.centers.get(i);
          s = null;
          for(int j = 0; j < p.corners.size(); j++)
          {
        	  q = p.corners.get(j);
        	  
              if (s == null || q.elevation < s.elevation) 
              {
                s = q;
              }
            }
          lowestCorner.add((s == null)? -1 : s.index);
          watersheds.add((s == null)? -1 : (s.watershed == null)? -1 : s.watershed.index);
        }
    }
    
  }

