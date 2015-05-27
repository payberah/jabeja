package peersim.jabeja;

import java.util.ArrayList;
import java.util.HashMap;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.jabeja.JabejaOverlay;
import peersim.jabeja.Newscast;
import peersim.jabeja.NodeAddress;
import peersim.jabeja.Tman;

public class JabejaNodeOne2One implements CDProtocol {
	private static final String NEWSCAST_PROTOCOL = "newscast";
	private final int newscastPID;
	private Newscast newscast;
	
	private static final String TMAN_PROTOCOL = "tman";
	private final int tmanPID;
	private Tman tman;
	
	private static final String OVERLAY_PROTOCOL = "overlay";
	private final int overlayPID;
	private JabejaOverlay overlay;
	
	private static final String NOISE = "noise";
	private static final String NOISE_DELTA = "noisedelta";
	private static final String POW = "power";
	
	private static int NUM_OF_WALKS = 1;
	
	private int protocolId;
	private NodeAddress self;
	private int color;
	private int numColors;
	private int walkLength = 1;
	public double noise;
	private double noiseDelta;
	private int power;
	private static int numberOfSwaps = 0;
	private static int numberOfLocalSwaps = 0;
	public int initialColor;

//---------------------------------------------------------------------
	public JabejaNodeOne2One(String prefix) {
		this.newscastPID = Configuration.getPid(prefix + "." + NEWSCAST_PROTOCOL);
		this.tmanPID = Configuration.getPid(prefix + "." + TMAN_PROTOCOL);
		this.overlayPID = Configuration.getPid(prefix + "." + OVERLAY_PROTOCOL);
		this.noise = Configuration.getDouble(prefix + "." + NOISE);
		this.noiseDelta = Configuration.getDouble(prefix + "." + NOISE_DELTA);
		this.power = Configuration.getInt(prefix + "." + POW);
	}
	
//---------------------------------------------------------------------
	public Object clone() {
		JabejaNodeOne2One partitionNode = null;
		
		try { 
			partitionNode = (JabejaNodeOne2One)super.clone(); 
		} catch(CloneNotSupportedException e) {
			// never happens
		} 

		return partitionNode;
	}

//--------------------------------------------------------------------
	public void init(Node node, int id, int color, int numColors, int protocolId) {
		this.self = new NodeAddress(node, id);
		this.color = color;
		this.initialColor = color;
		this.numColors = numColors;
		this.protocolId = protocolId;
    	this.newscast = (Newscast)node.getProtocol(this.newscastPID);
    	this.tman = (Tman)node.getProtocol(this.tmanPID);
    	this.overlay = (JabejaOverlay)node.getProtocol(this.overlayPID);
	}

//--------------------------------------------------------------------
	public int getId() {
		return this.self.getId();
	}

//--------------------------------------------------------------------
	public Node getNode() {
		return this.self.getNode();
	}

//--------------------------------------------------------------------
	public int getColor() {
		return this.color;
	}
	
//--------------------------------------------------------------------
	public void assignColor(int color) {
		this.color = color;
	}

//--------------------------------------------------------------------
	public HashMap<Integer, Integer> getNeighboursColors() {
		int nodeColor;
		Integer neighbourColor;
		JabejaNodeOne2One neighbourNode;
		HashMap<Integer, Integer> neighboursColors = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < this.numColors; i++)
			neighboursColors.put(i, 0);
		
		ArrayList<Node> neighbours = this.overlay.getNeighbors();
		for (Node node : neighbours) {
			neighbourNode = (JabejaNodeOne2One)node.getProtocol(this.protocolId);
			nodeColor = neighbourNode.getColor();
			neighbourColor = neighboursColors.get(nodeColor);
			neighboursColors.put(nodeColor, neighbourColor + 1);			
		}
		
		return neighboursColors;
	}

//--------------------------------------------------------------------
	public void nextCycle(Node n, int protocolId) {
		this.swap(n, protocolId);
	}

//--------------------------------------------------------------------
	public void swap(Node n, int protocolId) {
		JabejaNodeOne2One bestNode = null;
		double highestBenefit = 0;
		Node next = null;
		JabejaNodeOne2One selfNode = (JabejaNodeOne2One)n.getProtocol(protocolId);
		ArrayList<JabejaNodeOne2One> randomNodes = new ArrayList<JabejaNodeOne2One>();
		ArrayList<Node> newscastNodes = this.newscast.getNeighbors();
		ArrayList<Node> tmanNodes = this.tman.getNeighbors();
		
		int selfNodeColor = selfNode.getColor();
		HashMap<Integer, Integer> selfNodeNeighboursColors = selfNode.getNeighboursColors();
		
		// random walk
		for (int j = 0; j < NUM_OF_WALKS; j++) {
			JabejaNodeOne2One randomNode = selfNode;
			
			ArrayList<Node> visited = new ArrayList<Node>();
			visited.add(this.self.getNode());
			
			for (int i = 0; i < this.walkLength; i++) {
				next = randomNode.getRandomNode();
				randomNode = (JabejaNodeOne2One)next.getProtocol(protocolId);
			}
			
			randomNodes.add(randomNode);
		}
		
		// calculate the cost function
		for (JabejaNodeOne2One tempNode : randomNodes) {
			int tempNodeColor = tempNode.getColor();
			HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
		
			int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
			int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
		
			int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
			int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
			
			double oldBenefit = Math.pow(c1SelfNodeBenefit, this.power) + Math.pow(c1TempNodeBenefit, this.power);
			double newBenefit = Math.pow(c2SelfNodeBenefit, this.power) + Math.pow(c2TempNodeBenefit, this.power);
			
			if (newBenefit * this.noise > oldBenefit && newBenefit > highestBenefit) {
				bestNode = tempNode;
				highestBenefit = newBenefit;
			}
		}
		
		// try with the newscast nodes
		if (bestNode == null) {
			randomNodes.clear();
			
			for (Node node : newscastNodes) {
				if (node != null)
					randomNodes.add((JabejaNodeOne2One)node.getProtocol(protocolId));
			}

			for (JabejaNodeOne2One tempNode : randomNodes) {
				int tempNodeColor = tempNode.getColor();
				HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
			
				int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
				int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
			
				int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
				int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
				
				double oldBenefit = Math.pow(c1SelfNodeBenefit, this.power) + Math.pow(c1TempNodeBenefit, this.power);
				double newBenefit = Math.pow(c2SelfNodeBenefit, this.power) + Math.pow(c2TempNodeBenefit, this.power);
				
				if (newBenefit * this.noise > oldBenefit && newBenefit > highestBenefit) {
					bestNode = tempNode;
					highestBenefit = newBenefit;
				}
			}
		}
			
		// try with the tman nodes
		if (bestNode == null && tmanNodes != null) {
			randomNodes.clear();			
			
			for (Node node : tmanNodes) {
				if (node != null)
					randomNodes.add((JabejaNodeOne2One)node.getProtocol(protocolId));
			}

			for (JabejaNodeOne2One tempNode : randomNodes) {
				int tempNodeColor = tempNode.getColor();
				HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
			
				int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
				int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
			
				int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
				int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
				
				double oldBenefit = Math.pow(c1SelfNodeBenefit, this.power) + Math.pow(c1TempNodeBenefit, this.power);
				double newBenefit = Math.pow(c2SelfNodeBenefit, this.power) + Math.pow(c2TempNodeBenefit, this.power);
				
				if (newBenefit * this.noise > oldBenefit && newBenefit > highestBenefit) {
					bestNode = tempNode;
					highestBenefit = newBenefit;
				}
			}
		}
			
		// swap the colors
		if (bestNode != null) {
			selfNode.assignColor(bestNode.color);
			bestNode.assignColor(selfNodeColor);
			numberOfSwaps++;
			
			if (selfNode.initialColor == bestNode.initialColor)
				numberOfLocalSwaps++;
		} 
		
		if (this.noise > 1) 
			this.noise -= this.noiseDelta;
		if (this.noise < 1)
			this.noise = 1;
		
	}
	
//--------------------------------------------------------------------
	public Node getRandomNode() {
		Node next = null;
		JabejaNodeOne2One partitionNode;
		int degree = this.overlay.degree();
		ArrayList<Node> neighbours = this.overlay.getNeighbors();
		
		while(true) {
			if (degree == 0) {
				next = this.self.getNode();
				break;
			} else if (degree == 1)
				next = neighbours.get(0);
			else
				next = neighbours.get(CommonState.r.nextInt(degree));
			
			partitionNode = (JabejaNodeOne2One)next.getProtocol(this.protocolId);

			if (partitionNode.getId() != this.getId()) 
				break;
		}
		
		return next;
	}

//--------------------------------------------------------------------
	public ArrayList<Node> getNeighbours() {
		return this.overlay.getNeighbors();
	}
	
//--------------------------------------------------------------------
	public int hasDiffColorNeibour() {
		int diffColorCount = 0;
		JabejaNodeOne2One neighbourNode;
			
		ArrayList<Node> neighbours = this.overlay.getNeighbors();
		for (Node node : neighbours) {
			neighbourNode = (JabejaNodeOne2One)node.getProtocol(this.protocolId);
				
			if (this.color != neighbourNode.getColor())
				diffColorCount++;
		}
			
		return diffColorCount;
	}
	
//--------------------------------------------------------------------
	public static int getNumberOfSwaps() {
		return numberOfSwaps;
	}

//--------------------------------------------------------------------
	public static int getNumberOfLocalSwaps() {
		return numberOfLocalSwaps;
	}

//--------------------------------------------------------------------
	public static void resetNumberOfSwaps() {
		numberOfSwaps = 0;
		numberOfLocalSwaps = 0;
	}

//--------------------------------------------------------------------
	@Override
	public String toString() {
		
		return this.self.getId() + "";
	}
}