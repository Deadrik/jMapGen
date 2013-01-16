package co.janicek.core.math;

import jMapGen.com.nodename.Delaunay.DelaunayUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

public class PerlinNoise 
{

	private static int seed = 666;
	private static int octaves = 4;
	private static float falloff = 0.5f;

	public static Vector<Vector<Integer>> makePerlinNoise(int width,int height,float _x,float _y,float _z,int _seed, int _octaves)  
	{

		seed = _seed;
		octaves = _octaves;
		
		float baseFactor = 1f / 64f;
		
		Random R = new Random(seed);

		int iXoffset = seed = (int)Math.floor( (seed * 16807) % 2147483647);
		int iYoffset = seed = (int)Math.floor( (seed * 16807) % 2147483647);
		int iZoffset = seed = (int)Math.floor( (seed * 16807) % 2147483647);
		
		iXoffset = R.nextInt(100000);
		iYoffset = R.nextInt(100000);
		iZoffset = R.nextInt(100000);

		ArrayList<Float> aOctFreq = new ArrayList<Float>(); // frequency per octave
		ArrayList<Float> aOctPers = new ArrayList<Float>(); // persistence per octave
		float fPersMax = 0.0f; // 1 / max persistence

		float fFreq, fPers;

		for (int i = 0; i < octaves; i++) 
		{
			fFreq = (float) Math.pow(2, i);
			fPers = (float) Math.pow(falloff, i);
			fPersMax += fPers;
			aOctFreq.add( fFreq );
			aOctPers.add( fPers );
		}

		fPersMax = 1 / fPersMax;

		Vector<Vector<Integer>> bitmap = new Vector<Vector<Integer>>();
		bitmap.setSize(width);
		for(int i = 0; i < width; i++)
		{
			Vector<Integer> b = new Vector<Integer>();
			b.setSize(height);
			bitmap.set(i, b);
		}
		//int[][] bitmap = new int[width][height]; 

		float baseX = _x * baseFactor + iXoffset;
		_y = _y * baseFactor + iYoffset;
		_z = _z * baseFactor + iZoffset;

		for (int py = 0; py < height; py++) 
		{
			_x = baseX;

			for (int px = 0; px < width; px++) 
			{
				float s = 0.0f;

				for (int i = 0; i < octaves; i++) 
				{
					fFreq = aOctFreq.get(i);
					fPers = aOctPers.get(i);

					float x = _x * fFreq;
					float y = _y * fFreq;
					float z = _z * fFreq;

					float xf = (float) (x - (x % 1));
					float yf = (float) (y - (y % 1));
					float zf = (float) (z - (z % 1));

					int X = (int)(Math.floor(xf)) & 255;
					int Y = (int)(Math.floor(yf)) & 255;
					int Z = (int)(Math.floor(zf)) & 255;

					x -= xf;
					y -= yf;
					z -= zf;

					float u = x * x * x * (x * (x*6 - 15) + 10);
					float v = y * y * y * (y * (y*6 - 15) + 10);
					float w = z * z * z * (z * (z*6 - 15) + 10);

					int A = (p[X]) + Y;
					int AA = (p[A]) + Z;
					int AB = (p[A+1]) + Z;
					int B = (p[X+1]) + Y;
					int BA = (p[B]) + Z;
					int BB = (p[B+1]) + Z;

					float x1 = x-1;
					float y1 = y-1;
					float z1 = z-1;

					int hash = (p[BB+1]) & 15;
					float g1 = ((hash&1) == 0 ? (hash<8 ? x1 : y1) : (hash<8 ? -x1 : -y1)) + ((hash&2) == 0 ? hash<4 ? y1 : ( hash==12 ? x1 : z1 ) : hash<4 ? -y1 : ( hash==14 ? -x1 : -z1 ));

					hash = (p[AB+1]) & 15;
					float g2 = ((hash&1) == 0 ? (hash<8 ? x : y1) : (hash<8 ? -x : -y1)) + ((hash&2) == 0 ? hash<4 ? y1 : ( hash==12 ? x : z1 ) : hash<4 ? -y1 : ( hash==14 ? -x : -z1 ));

					hash = (p[BA+1]) & 15;
					float g3 = ((hash&1) == 0 ? (hash<8 ? x1 : y ) : (hash<8 ? -x1 : -y )) + ((hash&2) == 0 ? hash<4 ? y : ( hash==12 ? x1 : z1 ) : hash<4 ? -y : ( hash==14 ? -x1 : -z1 ));

					hash = (p[AA+1]) & 15;
					float g4 = ((hash&1) == 0 ? (hash<8 ? x : y ) : (hash<8 ? -x : -y )) + ((hash&2) == 0 ? hash<4 ? y : ( hash==12 ? x : z1 ) : hash<4 ? -y : ( hash==14 ? -x : -z1 ));

					hash = (p[BB]) & 15;
					float g5 = ((hash&1) == 0 ? (hash<8 ? x1 : y1) : (hash<8 ? -x1 : -y1)) + ((hash&2) == 0 ? hash<4 ? y1 : ( hash==12 ? x1 : z ) : hash<4 ? -y1 : ( hash==14 ? -x1 : -z ));

					hash = (p[AB]) & 15;
					float g6 = ((hash&1) == 0 ? (hash<8 ? x : y1) : (hash<8 ? -x : -y1)) + ((hash&2) == 0 ? hash<4 ? y1 : ( hash==12 ? x : z ) : hash<4 ? -y1 : ( hash==14 ? -x : -z ));

					hash = (p[BA]) & 15;
					float g7 = ((hash&1) == 0 ? (hash<8 ? x1 : y ) : (hash<8 ? -x1 : -y )) + ((hash&2) == 0 ? hash<4 ? y : ( hash==12 ? x1 : z ) : hash<4 ? -y : ( hash==14 ? -x1 : -z ));

					hash = (p[AA]) & 15;
					float g8 = ((hash&1) == 0 ? (hash<8 ? x : y ) : (hash<8 ? -x : -y )) + ((hash&2) == 0 ? hash<4 ? y : ( hash==12 ? x : z ) : hash<4 ? -y : ( hash==14 ? -x : -z ));

					g2 += u * (g1 - g2);
					g4 += u * (g3 - g4);
					g6 += u * (g5 - g6);
					g8 += u * (g7 - g8);

					g4 += v * (g2 - g4);
					g8 += v * (g6 - g8);

					s += ( g8 + w * (g4 - g8)) * fPers;
				}

				int color = Math.max(Math.min((int)Math.floor( ( s * fPersMax + 1 ) * 128 ), 255), 0);

				bitmap.get(px).set(py, color);

				_x += baseFactor;
			}

			_y += baseFactor;
		}

		return bitmap;
	}


	private static int[] p = {
		151,160,137,91,90,15,131,13,201,95,
		96,53,194,233,7,225,140,36,103,30,69,
		142,8,99,37,240,21,10,23,190,6,148,
		247,120,234,75,0,26,197,62,94,252,
		219,203,117,35,11,32,57,177,33,88,
		237,149,56,87,174,20,125,136,171,
		168,68,175,74,165,71,134,139,48,27,
		166,77,146,158,231,83,111,229,122,
		60,211,133,230,220,105,92,41,55,46,
		245,40,244,102,143,54,65,25,63,161,
		1,216,80,73,209,76,132,187,208,89,
		18,169,200,196,135,130,116,188,159,
		86,164,100,109,198,173,186,3,64,52,
		217,226,250,124,123,5,202,38,147,118,
		126,255,82,85,212,207,206,59,227,47,
		16,58,17,182,189,28,42,223,183,170,
		213,119,248,152,2,44,154,163,70,221,
		153,101,155,167,43,172,9,129,22,39,
		253,19,98,108,110,79,113,224,232,
		178,185,112,104,218,246,97,228,251,
		34,242,193,238,210,144,12,191,179,
		162,241,81,51,145,235,249,14,239,
		107,49,192,214,31,181,199,106,157,
		184,84,204,176,115,121,50,45,127,4,
		150,254,138,236,205,93,222,114,67,29,
		24,72,243,141,128,195,78,66,215,61,
		156,180,151,160,137,91,90,15,131,13,
		201,95,96,53,194,233,7,225,140,36,
		103,30,69,142,8,99,37,240,21,10,23,
		190,6,148,247,120,234,75,0,26,197,
		62,94,252,219,203,117,35,11,32,57,
		177,33,88,237,149,56,87,174,20,125,
		136,171,168,68,175,74,165,71,134,139,
		48,27,166,77,146,158,231,83,111,229,
		122,60,211,133,230,220,105,92,41,55,
		46,245,40,244,102,143,54,65,25,63,
		161,1,216,80,73,209,76,132,187,208,
		89,18,169,200,196,135,130,116,188,
		159,86,164,100,109,198,173,186,3,64,
		52,217,226,250,124,123,5,202,38,147,
		118,126,255,82,85,212,207,206,59,
		227,47,16,58,17,182,189,28,42,223,
		183,170,213,119,248,152,2,44,154,
		163,70,221,153,101,155,167,43,172,9,
		129,22,39,253,19,98,108,110,79,113,
		224,232,178,185,112,104,218,246,97,
		228,251,34,242,193,238,210,144,12,
		191,179,162,241,81,51,145,235,249,
		14,239,107,49,192,214,31,181,199,
		106,157,184,84,204,176,115,121,50,
		45,127,4,150,254,138,236,205,93,
		222,114,67,29,24,72,243,141,128,
		195,78,66,215,61,156,180
	};
}
