package peersim.jabeja;

import java.util.ArrayList;
import java.util.Collections;

import peersim.core.CommonState;
import peersim.core.Node;


public class Buffer {
	private ArrayList<Node> peers;
	private Node selfNode;

//---------------------------------------------------------------------------------		
	public Buffer(Node self, Node[] nodes) {
		this.selfNode = self;
		this.peers = new ArrayList<Node>();
		
		for (int i = 0; i < nodes.length; ++i)
			this.peers.add(nodes[i]);
	}
//---------------------------------------------------------------------------------		
	public Buffer(Node self, ArrayList<Node> nodes) {
		this.selfNode = self;
		this.peers = new ArrayList<Node>();
		
		for (int i = 0; i < nodes.size(); ++i)
			this.peers.add(nodes.get(i));
	}
//---------------------------------------------------------------------------------		
	public void merge(Node[] newPeers) {
		Node n;
		
		if (newPeers == null)
			return;
		
		for (int i = 0; i < newPeers.length; ++i) {
			n = newPeers[i];
			
			if (n.equals(this.selfNode))
				continue;
			
			if (!this.peers.contains(n))
				this.peers.add(n);
			else
				this.peers.set(this.peers.indexOf(n), n); // refresh the entry with the latest object
			
		}
	}	
//---------------------------------------------------------------------------------		
	public void merge(ArrayList<Node> newPeers) {
		Node n;
		
		if (newPeers == null)
			return;
		
		for (int i = 0; i < newPeers.size(); ++i) {
			n = newPeers.get(i);
			
			if (n == null || n.equals(this.selfNode))
				continue;
			
			if (!this.peers.contains(n))
				this.peers.add(n);
			else
				this.peers.set(this.peers.indexOf(n), n); // refresh the entry with the latest object
			
		}
	}
	
//---------------------------------------------------------------------------------		
	public Node[] selectView(int size, int partitionPID) {
		int count = 0;
		JabejaNode partitionNode;		
		Node[] selectedView = new Node[size];

		Collections.shuffle(this.peers, CommonState.r);		
		
		// the select function goes here
		for (Node node : this.peers) {
			if (node == null)
				continue;
			partitionNode = (JabejaNode)node.getProtocol(partitionPID);
				
			if (partitionNode.hasDiffColorNeibour() > 0) {
				selectedView[count] = node;
				count++;
				
				if (count == size)
					break;
			}
		}
			
		return selectedView;
	}
	
//---------------------------------------------------------------------------------		
	public void add(Node n) {
		if (n != null && !this.peers.contains(n))
			peers.add(n);
	}
	
//---------------------------------------------------------------------------------		
	public void remove(Node n) {
		if (n == null)
			return;
		
		while (this.peers.contains(n))
			this.peers.remove(n);
	}
//---------------------------------------------------------------------------------
	public ArrayList<Node> getNodes() {
		return this.peers;
	}
}
