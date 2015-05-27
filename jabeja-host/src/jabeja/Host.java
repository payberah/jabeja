package jabeja;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class Host {
	private Random rnd = new Random(Main.SEED);
	private HashMap<Integer, Node> nodes;
	private ArrayList<Integer> activeNodes = new ArrayList<Integer>();
	Set<Integer> activate = new TreeSet<Integer>();
	
	private int numberOfSwaps = 0;
	private int round = 0;
	private float temperature = Main.NOISE;
	
	int skipped;
	
//-------------------------------------------------------------------	
	public Host(HashMap<Integer, Node> nodes) {
		this.nodes = nodes;
		activeNodes.addAll(this.nodes.keySet());
	}

//-------------------------------------------------------------------
	public void run() {
		System.out.println("System Time:" + System.currentTimeMillis());
		for (round = 0; round < Main.ROUNDS; round++) {
			this.numberOfSwaps = 0;
			
			if (temperature > 1) 
				temperature -= Main.NOISE_DELTA;
			if (temperature < 1)
				temperature = 1;
			
			skipped = 0;

			System.out.println("noes.size:" + nodes.size() + ", activeNodes.size:" + activeNodes.size());
			for (int id : this.activeNodes)
				this.swap(id);
			
			activeNodes.clear();
			activeNodes.addAll(activate);
			activate.clear();

			this.report();
		}
	}

//-------------------------------------------------------------------	
	private void swap(int id) {
		Node bestNode = null;
		double highestBenefit = 0;
		
		Node selfNode = this.nodes.get(id);
		int selfNodeColor = selfNode.getColor();
		int[] selfNodeNeighboursColors = this.getNeighboursColors(id);
		
		if (selfNodeNeighboursColors[selfNodeColor] >= selfNode.getDegree() * 0.5) {
			skipped++;
			return;
		}
		
		Integer[] selfNodeNeighbours = this.getRandomList(selfNode.getNeighbours());
		
		int neighborCount = 0;
		for (int tempNodeId : selfNodeNeighbours) {
			if (neighborCount++ > Main.CLOSEBY_NEIGHBOURS)
				break;
			
			int tempNodeColor = this.nodes.get(tempNodeId).getColor();
				
			if (tempNodeColor == selfNodeColor)
				continue;
				
			int []tempNodeNeighboursColors = this.getNeighboursColors(tempNodeId);
			
			int c1SelfNodeBenefit = selfNodeNeighboursColors[selfNodeColor];
			int c1TempNodeBenefit = tempNodeNeighboursColors[tempNodeColor];
			
			int c2SelfNodeBenefit = selfNodeNeighboursColors[tempNodeColor] - 1;
			int c2TempNodeBenefit = tempNodeNeighboursColors[selfNodeColor] - 1;
				
			double oldBenefit = Math.pow(c1SelfNodeBenefit, 2) + Math.pow(c1TempNodeBenefit, 2);
			double newBenefit = Math.pow(c2SelfNodeBenefit, 2) + Math.pow(c2TempNodeBenefit, 2);
				
			if (newBenefit * temperature > oldBenefit && newBenefit > highestBenefit) {
				bestNode = this.nodes.get(tempNodeId);
				highestBenefit = newBenefit;
//				break;
			}
		}
			
		// try with some sampled nodes
		if (bestNode == null) {
			Integer[] randomNodes = this.getRandomNodes(id);
				
			for (int tempNodeId : randomNodes) {
				int tempNodeColor = this.nodes.get(tempNodeId).getColor();
					
				if (tempNodeColor == selfNodeColor)
					continue;
					
				int[] tempNodeNeighboursColors = this.getNeighboursColors(tempNodeId);
				
				int c1SelfNodeBenefit = selfNodeNeighboursColors[selfNodeColor];
				int c1TempNodeBenefit = tempNodeNeighboursColors[tempNodeColor];
				
				int c2SelfNodeBenefit = selfNodeNeighboursColors[tempNodeColor];
				int c2TempNodeBenefit = tempNodeNeighboursColors[selfNodeColor];
				
				if (selfNode.getNeighbours().contains(tempNodeId)) {
					c2SelfNodeBenefit--;
					c2TempNodeBenefit--;
				}
					
				
				double oldBenefit = Math.pow(c1SelfNodeBenefit, 2) + Math.pow(c1TempNodeBenefit, 2);
				double newBenefit = Math.pow(c2SelfNodeBenefit, 2) + Math.pow(c2TempNodeBenefit, 2);
					
				if (newBenefit * temperature > oldBenefit && newBenefit > highestBenefit) {
					bestNode = this.nodes.get(tempNodeId);
					highestBenefit = newBenefit;
//					break;
				}
			}
		}
				
		// swap the colors
		if (bestNode != null) {
			selfNode.setColor(bestNode.getColor());
			bestNode.setColor(selfNodeColor);
			
			activate.add(bestNode.getId());
			
			this.numberOfSwaps++;
		}
		activate.add(id);
		

	}
	
//-------------------------------------------------------------------	
	private int[] getNeighboursColors(int id) {
		int nodeColor;
		Integer neighbourColor;
		Node neighbourNode;
		int[] neighboursColors = new int[Main.NUM_COLORS];
		
		for (int i = 0; i < Main.NUM_COLORS; i++)
			neighboursColors[i] = 0;
		
		ArrayList<Integer> neighbours = this.nodes.get(id).getNeighbours();
		for (Integer neighbourId : neighbours) {
			neighbourNode = this.nodes.get(neighbourId);
			nodeColor = neighbourNode.getColor();
			neighbourColor = neighboursColors[nodeColor];
			neighboursColors[nodeColor] = neighbourColor + 1;			
		}
		
		return neighboursColors;
	}

//-------------------------------------------------------------------	
	private Integer[] getRandomNodes(int id) {
		int count = Main.RND_LIST_SIZE;
		int rndId;
		int index;
		int size = this.nodes.size();
		ArrayList<Integer> ids = new ArrayList<Integer>(this.nodes.keySet());
		ArrayList<Integer> rndIds = new ArrayList<Integer>();
					
		while (true) {
			index = this.rnd.nextInt(size);
			rndId = ids.get(index);
			if (rndId != id && !rndIds.contains(rndId)) {
				rndIds.add(rndId);
				count--;
			}
			
			if (count == 0)
				break;			
		}
		
		Integer[] arr = new Integer[rndIds.size()];
		return rndIds.toArray(arr);
	}

//-------------------------------------------------------------------	
	private Integer[] getRandomList(ArrayList<Integer> list) {
			int count = Main.CLOSEBY_NEIGHBOURS;
			int rndId;
			int index;
			int size = list.size();
			ArrayList<Integer> rndIds = new ArrayList<Integer>();
			
			if (size <= count)
				rndIds.addAll(list);
			else {
				while (true) {
					index = this.rnd.nextInt(size);
					rndId = list.get(index);
					if (!rndIds.contains(rndId)) {
						rndIds.add(rndId);
						count--;
					}
				
					if (count == 0)
						break;			
				}
			}
			
			Integer[] arr = new Integer[rndIds.size()];
			return rndIds.toArray(arr);
		}
//-------------------------------------------------------------------	
//	public Integer findPartner(Integer pid, ArrayList<Integer> nodes) {
//		double highestBenefit = 0;
//		Integer bestNodeId = null;
//		
//		Node p = this.nodes.get(pid);
//		Node q;
//		
//		int pColor = p.getColor();
//		
//		ArrayList<Integer> pNeighbours = this.getRandomList(p.getNeighbours());
//		
//		HashMap<Integer, Integer> pNeighboursColors = this.getNeighboursColors(pid);
//		
//		for (int qid : nodes) {
//			q = this.nodes.get(qid);
//			int qColor = q.getColor();
//			if (qColor == pColor)
//				continue;
//
//			HashMap<Integer, Integer> qNeighboursColors = this.getNeighboursColors(qid);
//						
//			int c1pBenefit = pNeighboursColors.get(pColor);
//			int c1qBenefit = qNeighboursColors.get(qColor);
//			
//			int c2pBenefit = pNeighboursColors.get(qColor);
//			int c2qBenefit = qNeighboursColors.get(pColor);
//				
//			if (pNeighbours.contains(q)) {
//				c2pBenefit--;
//				c2qBenefit--;
//			}
//
//			double oldBenefit = Math.pow(c1pBenefit, 2) + Math.pow(c1qBenefit, 2);
//			double newBenefit = Math.pow(c2pBenefit, 2) + Math.pow(c2qBenefit, 2);
//				
//			if (newBenefit * temperature > oldBenefit && newBenefit > highestBenefit) {
//				bestNodeId = qid;
//				highestBenefit = newBenefit;
//			}
//		}
//			
//		return bestNodeId;
//	}
//	
//-------------------------------------------------------------------	
	public int getNumberOfSwaps() {
		return this.numberOfSwaps;
	}

//------------------------------------------------------------------------
    private void report() {
        int grayLinks = 0;
        int migrations = 0;
        int size = this.nodes.size();
    	
        for (int i = 0; i < size; i++) {
        	Node node = this.nodes.get(i);
            int nodeColor = node.getColor();
        	ArrayList<Integer> nodeNeighbours = node.getNeighbours();
            
            if (nodeColor != node.getInitColor())
            	migrations++;
        	
            if (nodeNeighbours != null) {
	            for (int n : nodeNeighbours) {
	            	Node p = this.nodes.get(n);
	            	int pColor = p.getColor();
	            		
	            	if (nodeColor != pColor)
	            		grayLinks++;
	            }
            }
        }
        
        System.out.println("round: " + this.round + ", edge cut:" + grayLinks / 2 + ", swaps: " + this.numberOfSwaps + ", migrations: " + migrations + ", skipped = " + skipped + ", System Time:" + System.currentTimeMillis());
    }
}
