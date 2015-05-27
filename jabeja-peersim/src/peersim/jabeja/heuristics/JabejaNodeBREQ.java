package peersim.jabeja.heuristics;

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

public class JabejaNodeBREQ implements CDProtocol {
	private static final String NEWSCAST_PROTOCOL = "newscast";
	private final int newscastPID;
	private Newscast newscast;
	
	private static final String TMAN_PROTOCOL = "tman";
	private final int tmanPID;
	private Tman tman;
	
	private static final String OVERLAY_PROTOCOL = "overlay";
	private final int overlayPID;
	private JabejaOverlay overlay;
	
	private static int NUM_OF_WALKS = 1;
	
	private int protocolId;
	private NodeAddress self;
	private int color;
	private int numColors;
	private int walkLength = 1;
	public float noise = (float)1;

//---------------------------------------------------------------------
	public JabejaNodeBREQ(String prefix) {
		this.newscastPID = Configuration.getPid(prefix + "." + NEWSCAST_PROTOCOL);
		this.tmanPID = Configuration.getPid(prefix + "." + TMAN_PROTOCOL);
		this.overlayPID = Configuration.getPid(prefix + "." + OVERLAY_PROTOCOL);
	}
	
//---------------------------------------------------------------------
	public Object clone() {
		JabejaNodeBREQ partitionNode = null;
		
		try { 
			partitionNode = (JabejaNodeBREQ)super.clone(); 
		} catch(CloneNotSupportedException e) {
			// never happens
		} 

		return partitionNode;
	}

//--------------------------------------------------------------------
	public void init(Node node, int id, int color, int numColors, int protocolId) {
		this.self = new NodeAddress(node, id);
		this.color = color;
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
		JabejaNodeBREQ neighbourNode;
		HashMap<Integer, Integer> neighboursColors = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < this.numColors; i++)
			neighboursColors.put(i, 0);
		
		ArrayList<Node> neighbours = this.overlay.getNeighbors();
		for (Node node : neighbours) {
			neighbourNode = (JabejaNodeBREQ)node.getProtocol(this.protocolId);
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
		JabejaNodeBREQ bestNode = null;
		double highestBenefit = 0;
		Node next = null;
		JabejaNodeBREQ selfNode = (JabejaNodeBREQ)n.getProtocol(protocolId);
		ArrayList<JabejaNodeBREQ> randomNodes = new ArrayList<JabejaNodeBREQ>();
		ArrayList<Node> newscastNodes = this.newscast.getNeighbors();
		ArrayList<Node> tmanNodes = this.tman.getNeighbors();
		
		int selfNodeColor = selfNode.getColor();
		HashMap<Integer, Integer> selfNodeNeighboursColors = selfNode.getNeighboursColors();
		
		// random walk
		for (int j = 0; j < NUM_OF_WALKS; j++) {
			JabejaNodeBREQ randomNode = selfNode;
			
			ArrayList<Node> visited = new ArrayList<Node>();
			visited.add(this.self.getNode());
			
			for (int i = 0; i < this.walkLength; i++) {
				next = randomNode.getRandomNode();
				randomNode = (JabejaNodeBREQ)next.getProtocol(protocolId);
			}
			
			randomNodes.add(randomNode);
		}
		
		// calculate the cost function
		for (JabejaNodeBREQ tempNode : randomNodes) {
			int tempNodeColor = tempNode.getColor();
			HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
		
			int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
			int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
		
			int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
			int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
			
			double oldBenefit = Math.pow(c1SelfNodeBenefit, 2) + Math.pow(c1TempNodeBenefit, 2);
			double newBenefit = Math.pow(c2SelfNodeBenefit, 2) + Math.pow(c2TempNodeBenefit, 2);
			
			if (newBenefit * this.noise >= oldBenefit && newBenefit > highestBenefit) {
				bestNode = tempNode;
				highestBenefit = newBenefit;
			}
		}
		
		// try with the newscast nodes
		if (bestNode == null) {
			randomNodes.clear();
			
			for (Node node : newscastNodes) {
				if (node != null)
					randomNodes.add((JabejaNodeBREQ)node.getProtocol(protocolId));
			}

			for (JabejaNodeBREQ tempNode : randomNodes) {
				int tempNodeColor = tempNode.getColor();
				HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
			
				int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
				int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
			
				int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
				int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
				
				double oldBenefit = Math.pow(c1SelfNodeBenefit, 2) + Math.pow(c1TempNodeBenefit, 2);
				double newBenefit = Math.pow(c2SelfNodeBenefit, 2) + Math.pow(c2TempNodeBenefit, 2);
				
				if (newBenefit * this.noise >= oldBenefit && newBenefit > highestBenefit) {
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
					randomNodes.add((JabejaNodeBREQ)node.getProtocol(protocolId));
			}

			for (JabejaNodeBREQ tempNode : randomNodes) {
				int tempNodeColor = tempNode.getColor();
				HashMap<Integer, Integer> tempNodeNeighboursColors = tempNode.getNeighboursColors();
			
				int c1SelfNodeBenefit = selfNodeNeighboursColors.get(selfNodeColor);
				int c1TempNodeBenefit = tempNodeNeighboursColors.get(tempNodeColor);
			
				int c2SelfNodeBenefit = selfNodeNeighboursColors.get(tempNodeColor);
				int c2TempNodeBenefit = tempNodeNeighboursColors.get(selfNodeColor);
				
				double oldBenefit = Math.pow(c1SelfNodeBenefit, 2) + Math.pow(c1TempNodeBenefit, 2);
				double newBenefit = Math.pow(c2SelfNodeBenefit, 2) + Math.pow(c2TempNodeBenefit, 2);
				
				if (newBenefit * this.noise >= oldBenefit && newBenefit > highestBenefit) {
					bestNode = tempNode;
					highestBenefit = newBenefit;
				}
			}
		}
			
		// swap the colors
		if (bestNode != null) {
			selfNode.assignColor(bestNode.color);
			bestNode.assignColor(selfNodeColor);
		} 
	}
	
//--------------------------------------------------------------------
	public Node getRandomNode() {
		Node next = null;
		JabejaNodeBREQ partitionNode;
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
			
			partitionNode = (JabejaNodeBREQ)next.getProtocol(this.protocolId);

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
		JabejaNodeBREQ neighbourNode;
			
		ArrayList<Node> neighbours = this.overlay.getNeighbors();
		for (Node node : neighbours) {
			neighbourNode = (JabejaNodeBREQ)node.getProtocol(this.protocolId);
				
			if (this.color != neighbourNode.getColor())
				diffColorCount++;
		}
			
		return diffColorCount;
	}

//--------------------------------------------------------------------
	@Override
	public String toString() {
		
		return this.self.getId() + "";
	}
}