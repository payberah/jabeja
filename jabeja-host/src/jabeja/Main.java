package jabeja;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Main {
	public static int ROUNDS = 1000;
	public static int NUM_COLORS = 4;
	public static int RND_LIST_SIZE = 5;
	public static float NOISE = 2;
	public static float NOISE_DELTA = (float)0.003;
	public static int SEED = 654;
	public static int CLOSEBY_NEIGHBOURS = 3;
	public static String GRAPH = "web-Google.graph";
	
//-------------------------------------------------------------------	
	private static HashMap<Integer, Node> readGraph(String filename) {
		// for debug only
		HashMap<Integer, Integer> colorDistribution = new HashMap<Integer, Integer>();
		for (int c = 0; c < NUM_COLORS; c++)
			colorDistribution.put(c, 0);
		
		Random rnd = new Random(Main.SEED);
		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
		
		try {
			String strLine;
			File file = new File(filename);
			FileInputStream fStream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int numNodes = 0;
			int numEdges = 0;
			
			while ((strLine = br.readLine()) != null)   {
				System.out.println(strLine);
					
				if (strLine.startsWith("%") || strLine.startsWith("#"))
					continue;
			
				String[] parts = strLine.split(" ");
				numNodes = Integer.parseInt(parts[0]);
				numEdges = Integer.parseInt(parts[1]);
				break;
			 }
			
			System.out.println("-----------------------> nodes: " + numNodes + ", edges: " + numEdges);

			int count = 0;
			ArrayList<Integer> neighbours = new ArrayList<Integer>();
			
			while ((strLine = br.readLine()) != null)   {
				if (strLine.startsWith("%") || strLine.startsWith("#"))
					continue;
			
				neighbours.clear();
				String[] parts = strLine.split(" ");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].equals(""))
						continue;
					neighbours.add(Integer.parseInt(parts[i]) - 1);
				}
				int c = rnd.nextInt(Main.NUM_COLORS);
				int d = colorDistribution.get(c);
				while (d > numNodes / NUM_COLORS) {
					c = (c + 1) % NUM_COLORS;
					d = colorDistribution.get(c);
				}
					
				colorDistribution.put(c, d + 1);
				Node node = new Node(count, c);
				node.setNeighbours(neighbours);
				nodes.put(count, node);
				
				count++;
			 }			
			
			in.close();
		} catch (IOException e) {
			System.err.println("can not read from file " + filename);
		}
		
		System.out.println("Color Distribution:" + colorDistribution);
		
		return nodes;
	}
//------------------------------------------------------------------------------------------------
	private static HashMap<Integer, Node> readGraph_noRandomInitialization(String filename) {

		
		Random rnd = new Random(Main.SEED);
		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
		
		try {
			String strLine;
			File file = new File(filename);
			FileInputStream fStream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int numNodes = 0;
			int numEdges = 0;
			int partitionSize = numNodes / NUM_COLORS;
			int colorCount = 0;
			int color = 0;
			
			while ((strLine = br.readLine()) != null)   {
				System.out.println(strLine);
					
				if (strLine.startsWith("%") || strLine.startsWith("#"))
					continue;
			
				String[] parts = strLine.split(" ");
				numNodes = Integer.parseInt(parts[0]);
				numEdges = Integer.parseInt(parts[1]);
				break;
			 }
			
			System.out.println("-----------------------> nodes: " + numNodes + ", edges: " + numEdges);

			int count = 0;
			ArrayList<Integer> neighbours = new ArrayList<Integer>();
			
			while ((strLine = br.readLine()) != null)   {
				if (strLine.startsWith("%") || strLine.startsWith("#"))
					continue;
			
				neighbours.clear();
				String[] parts = strLine.split(" ");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].equals(""))
						continue;
					neighbours.add(Integer.parseInt(parts[i]) - 1);
				}
				
				if (colorCount <= partitionSize)
					colorCount++;
				else {
					color = (color + 1) % NUM_COLORS;
					colorCount = 1;
				}
				Node node = new Node(count, color);
				node.setNeighbours(neighbours);
				nodes.put(count, node);
				
				count++;
			 }			
			
			in.close();
		} catch (IOException e) {
			System.err.println("can not read from file " + filename);
		}
		
		
		return nodes;
	}
		
//-------------------------------------------------------------------	
	public static void main(String[] args) {
		HashMap<Integer, Node> graph = Main.readGraph_noRandomInitialization(Main.GRAPH);

		Host host = new Host(graph);
		host.run();
	}
}
