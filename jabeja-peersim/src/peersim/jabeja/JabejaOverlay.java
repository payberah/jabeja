package peersim.jabeja;

import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.core.Linkable;
import peersim.core.Node;

public class JabejaOverlay implements CDProtocol, Linkable {
	private ArrayList<Node> neighbours = new ArrayList<Node>();

//---------------------------------------------------------------------
	public JabejaOverlay(String prefix) {
		this.neighbours = new ArrayList<Node>();
	}
	
//---------------------------------------------------------------------
	@Override
	public void onKill() {
		// TODO Auto-generated method stub
	}

//---------------------------------------------------------------------
	@Override
	public int degree() {
		return this.neighbours.size();
	}
	
//---------------------------------------------------------------------
	@Override
	public Node getNeighbor(int i) {
		return this.neighbours.get(i);
	}

//---------------------------------------------------------------------
	public ArrayList<Node> getNeighbors() {
		return this.neighbours;
	}

//---------------------------------------------------------------------
	@Override
	public boolean addNeighbor(Node neighbour) {
		if (!this.neighbours.contains(neighbour))
			this.neighbours.add(neighbour);
		return false;
	}

//---------------------------------------------------------------------
	@Override
	public boolean contains(Node neighbor) {
		return this.neighbours.contains(neighbor);
	}

//---------------------------------------------------------------------
	@Override
	public void pack() {
		// TODO Auto-generated method stub
	}

//---------------------------------------------------------------------
	@Override
	public void nextCycle(Node node, int protocolID) {
		// TODO Auto-generated method stub
	}

//---------------------------------------------------------------------
	public Object clone() {
		JabejaOverlay sn = null;
		try { 
			sn = (JabejaOverlay) super.clone(); 
		} catch(CloneNotSupportedException e) {} // never happens
		
		sn.neighbours = new ArrayList<Node>();
		
		return sn;
	}
}
