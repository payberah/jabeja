package peersim.jabeja;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.graph.GraphFactory;

/** 
* @author fatemeh
*/
public class NetworkInit implements Control {

	private static final String JABEJA_PROTOCOL = "jabeja";
	private static final String OVERLAY_PROTOCOL = "overlay";
	private static final String NEWSCAST_PROTOCOL = "newscast";
	private static final String TMAN_PROTOCOL = "tman";
	private static final String GRAPH_TYPE = "graphtype";
	private static final String GRAPH_FILE = "graphfile";
	private static final String WRITE_TO_FILE = "writetofile";
	private static final String NUM_COLORS = "colors";
	private static final String ID_SPACE = "idspace";
	private static final String DEGREE_SIZE = "view";
	private static final String CLUSTERS = "clusters";
	private static final String CLUSTER_INGDEGREE = "clusteringdegree";
	private static final String CACHE_SIZE = "newscastcache";
	private static final String TMAN_CACHE_SIZE = "tmancache";
	
	private final int jabejaPID;
	private final int overlayPID;
	private final int newscastPID;
	private final int tmanPID;
	private final int clusters;
	private final int degree;
	private final int idScpace;
	private final double clusteringDegree;
    private final int numOfColors;
    private final int newscastCacheSize;
    private final int tmanCacheSize;
	private final String graphType;
	private final String graphFile;
	private final boolean writeToFile;
	private int netSize;
	private OverlayGraph overlay;
	
//------------------------------------------------------------------------	
	public NetworkInit(String prefix) {
		this.jabejaPID = Configuration.getPid(prefix + "." + JABEJA_PROTOCOL);
		this.overlayPID = Configuration.getPid(prefix + "." + OVERLAY_PROTOCOL);
		this.newscastPID = Configuration.getPid(prefix + "." + NEWSCAST_PROTOCOL);
		this.tmanPID = Configuration.getPid(prefix + "." + TMAN_PROTOCOL);		
		this.degree = Configuration.getInt(prefix + "." + DEGREE_SIZE);	
		this.idScpace = Configuration.getInt(prefix + "." + ID_SPACE);
		this.clusters = Configuration.getInt(prefix + "." + CLUSTERS);	
		this.clusteringDegree = Configuration.getDouble(prefix + "." + CLUSTER_INGDEGREE);	
		this.numOfColors = Configuration.getInt(prefix + "." + NUM_COLORS);
		this.graphType = Configuration.getString(prefix + "." + GRAPH_TYPE);
		this.graphFile = Configuration.getString(prefix + "." + GRAPH_FILE);
		this.newscastCacheSize = Configuration.getInt(prefix + "." + CACHE_SIZE);
		this.tmanCacheSize = Configuration.getInt(prefix + "." + TMAN_CACHE_SIZE);
		this.writeToFile = Configuration.getBoolean(prefix + "." + WRITE_TO_FILE);
		
		this.netSize = Network.size();
		this.overlay = new OverlayGraph(this.overlayPID, false); // undirected graph
	}

//------------------------------------------------------------------------	
	public boolean execute() {
		int count = 0;
		int id;
		int randColor = -1;
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		int[] colors = new int[this.netSize];
		int[] population = new int[this.numOfColors];
		
		int pop = this.netSize / this.numOfColors;
		
		for (int c = 0; c < this.numOfColors; ++c)
			population[c] = pop;
				
		int idSpaceSize = (int)Math.pow(2, this.idScpace);
		
//		while (count < this.netSize) {
//			id = CommonState.r.nextInt(idSpaceSize);
//			
//			if (!ids.contains(id)) {
//				ids.add(id);				
//				randColor = selectColor(population, CommonState.r.nextInt(this.netSize - count));
//				population[randColor]--;
//				colors[count] = randColor;
//				count++;
//			}
//			else
//				System.err.println("NetworkInit: Id Collision!");
//		}	
		
		this.graphInit(ids, colors);
		if (this.writeToFile) {
			this.writeGraphToFile("mygraph.graph");
			System.out.println("The graph is written in the file mygraph.graph.");
			System.exit(0);
		}
		this.newscastInit();
		this.tmanInit();
		this.jabejaInit(ids, colors);
		
		return true;
	}
	
//------------------------------------------------------------------------	
	public void newscastInit() {
		Node node;
		Newscast newscast;
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Node> neighbors = new ArrayList<Node>();

		for (int i = 0; i < this.netSize; i++)
			nodes.add(Network.get(i));
		
		for (int i = 0; i < this.netSize; i++) {
			node = Network.get(i);
			newscast = (Newscast)node.getProtocol(this.newscastPID);
			
			if (this.netSize > this.newscastCacheSize) {
				for (int j = 0; j < this.newscastCacheSize; j++) {
					int rnd = CommonState.r.nextInt(this.netSize);
					Node rndNode = nodes.get(rnd);
					
					if (rndNode.getID() != node.getID() && !neighbors.contains(rndNode))
						neighbors.add(rndNode);
				}
			} else
				neighbors = nodes;
			
			neighbors.remove(node);			
			newscast.init(neighbors);			
			neighbors.clear();
		}
	}
	
//------------------------------------------------------------------------	
	public void tmanInit() {
		Node node;
		Tman tman;

		for (int i = 0; i < this.netSize; i++) {
			node = Network.get(i);
			tman = (Tman)node.getProtocol(this.tmanPID);
			tman.init(node, this.tmanCacheSize, this.newscastPID, this.jabejaPID);
		}		
	}

//------------------------------------------------------------------------	
	private void jabejaInit(ArrayList<Integer> ids, int[] colors) {
		Node node;
		JabejaNode partitionNode;
		ArrayList<Node> neighbors = new ArrayList<Node>();

		for (int i = 0; i < this.netSize; i++) {
			for (Integer index : this.overlay.getNeighbours(i))
				neighbors.add(Network.get(index));
			
			node = Network.get(i);
			partitionNode = (JabejaNode)node.getProtocol(this.jabejaPID);
			partitionNode.init(node, ids.get(i), colors[i], this.numOfColors, this.jabejaPID);
			neighbors.clear();
		}	
	}
		
//------------------------------------------------------------------------	
	public void graphInit(ArrayList<Integer> ids, int[] colors) {
		if (this.graphType.equalsIgnoreCase("synth"))
			this.buildGraph(ids, colors);
		else if (this.graphType.equalsIgnoreCase("file"))
			readGraph(this.graphFile);
		else if (this.graphType.equalsIgnoreCase("ws"))
			this.overlay = (OverlayGraph)GraphFactory.wireWS(this.overlay, 8, 0.02, CommonState.r);
		else if (this.graphType.equalsIgnoreCase("scale"))
			this.overlay = (OverlayGraph)GraphFactory.wireScaleFreeBA(this.overlay, 7, CommonState.r);
		else if (this.graphType.equalsIgnoreCase("twitter"))	
			this.overlay = (OverlayGraph)GraphFactory.wireTwitter(this.overlay, "twitter.net");
		else if (this.graphType.equalsIgnoreCase("facebook"))	
			this.overlay = (OverlayGraph)GraphFactory.wireFacebook(this.overlay, "web-Google.txt");
		else if (this.graphType.equalsIgnoreCase("CA-graph"))	
			this.overlay = (OverlayGraph)GraphFactory.wireGraph(this.overlay, "CBZ9z_1st_And_2nd_order_entity_edge_list.txt");
		else if (this.graphType.equalsIgnoreCase("unbalanced"))
			this.overlay = (OverlayGraph)GraphFactory.wireUnbalanced(this.overlay, 10, 0.5, 0.7);
		else {
			System.err.println("Invalid graph type. Please fix the graph type in the config file.");
			System.exit(1);
		}
	}
	
//------------------------------------------------------------------------	
	private void buildGraph(ArrayList<Integer> ids, int[] colors) {
		int index;
		Node node;
		int clusterSize;
		JabejaNode partitionNode;
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Node> neighbours = new ArrayList<Node>();
		HashMap<Integer, ArrayList<Node>> neighborMap = new HashMap<Integer, ArrayList<Node>>();

		clusterSize = this.netSize / this.clusters;

		for (int i = 0; i < this.netSize; i++)
			nodes.add(Network.get(i));

		for (int i = 0; i < this.netSize; i++) {
			node = Network.get(i);
			partitionNode = (JabejaNode)node.getProtocol(this.jabejaPID);

			if (this.netSize > this.degree) {
				for (int x = 0; x < this.degree ; x++) {
					if (CommonState.r.nextFloat() < this.clusteringDegree) {
						index = ((i / clusterSize) * clusterSize  + CommonState.r.nextInt(clusterSize)) % this.netSize;
							
						if (index != i && (neighborMap.get(index) == null || !neighborMap.get(index).contains(node)))
							neighbours.add(nodes.get(index));
					} else {
						int currentCluster = i / clusterSize + 1;
						index = (currentCluster * clusterSize + CommonState.r.nextInt(this.clusters * clusterSize)) % this.netSize;
						
						if (index != i && !neighbours.contains(nodes.get(index)) && (neighborMap.get(index) == null || !neighborMap.get(index).contains(node)))						
							neighbours.add(nodes.get(index));
					}
				}
			} else {
				neighbours = nodes;
			}
				
			neighbours.remove(node);

			partitionNode.init(node, ids.get(i), colors[i], this.numOfColors, this.jabejaPID);
			
			for (Node n : neighbours)
				this.overlay.setEdge(i, n.getIndex());
			
			neighborMap.put(i, partitionNode.getNeighbours());			
			neighbours.clear();
		}
	}
	
//------------------------------------------------------------------------	
	public void readGraph(String filename) {
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
			while ((strLine = br.readLine()) != null)   {
				if (strLine.startsWith("%") || strLine.startsWith("#"))
					continue;
			
				String[] parts = strLine.split(" ");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].equals(""))
						continue;
					this.overlay.setEdge(count, Integer.parseInt(parts[i]) - 1);
				}
				
				count++;
			 }			
			
			in.close();
		} catch (IOException e) {
			System.err.println("can not read from file " + filename);
		}
	}	
	
//------------------------------------------------------------------------	
	private int selectColor(int[] arr, int key) {
		int index = -1;
		int sum = 0;
			
		for (int i = 0; i < arr.length; ++i) {
			sum += arr[i];
				
			if (sum > key) {
				index = i;
				break;
			}
		}
			
		if (index != -1)
			return index;
		else
			return CommonState.r.nextInt(arr.length);
	}

//------------------------------------------------------------------------	
	private void writeGraphToFile(String filename) {
		Collection<Integer> neighbours;
		String str = new String();

		int edges = 0;
		for (int i = 0; i < this.netSize; i++) {
			edges += this.overlay.degree(i);
		}

		str = this.netSize + " " + (edges / 2) + "\n";
		FileIO.write(str, filename);
		
		System.out.println("WriteGraphToFile: overlay.size:" + overlay.size());
		
		
		for (int i = 0; i < this.netSize; i++) {
			neighbours = this.overlay.getNeighbours(i);

			str = "";
			for (Integer j : neighbours)
				str += " " + (j + 1);
			
			str += "\n";
			FileIO.append(str, filename);
		}
	}
}
