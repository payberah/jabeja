package peersim.jabeja;

import java.util.ArrayList;

import peersim.cdsim.*;
import peersim.config.Configuration;
import peersim.core.*;

public class Newscast implements CDProtocol {
	private static final String CACHE_SIZE = "init.node.newscastcache";
	
    private final int cacheSize;	
	private static Node[] tn;
	private static int[] ts;
	private Node[] cache;
	private int[] tstamps;

//---------------------------------------------------------------------
	public Newscast(String prefix) {
		this.cacheSize = Configuration.getInt(CACHE_SIZE);
		
		if (Newscast.tn == null || Newscast.tn.length < this.cacheSize) {
			Newscast.tn = new Node[cacheSize];
			Newscast.ts = new int[cacheSize];
		}
	
		this.cache = new Node[cacheSize];
		this.tstamps = new int[cacheSize];
	}
	
//---------------------------------------------------------------------
	public void init(ArrayList<Node> neighbors) {
		for (Node n : neighbors)
			addNeighbor(n);
	}

//---------------------------------------------------------------------
	public Object clone() {
	
		Newscast sn = null;
		
		try { 
			sn = (Newscast) super.clone(); 
		} catch(CloneNotSupportedException e) {} // never happens
		
		sn.cache = new Node[this.cache.length];
		sn.tstamps = new int[this.tstamps.length];
		System.arraycopy(this.cache, 0, sn.cache, 0, this.cache.length);
		System.arraycopy(this.tstamps, 0, sn.tstamps, 0, this.tstamps.length);
		
		return sn;
	}

//---------------------------------------------------------------------
	private Node getPeer() {

		final int d = degree();
		if (d == 0)
			return null;
		int index = CommonState.r.nextInt(d);
		Node result = this.cache[index];
	
		if (result.isUp())
			return result;
	
		// proceed towards older entries
		for (int i = index + 1; i < d; ++i)
			if (this.cache[i].isUp())
				return this.cache[i];
	
		// proceed towards younger entries
		for (int i = index - 1; i >= 0; --i)
			if (this.cache[i].isUp())
				return this.cache[i];
	
		// no accessible peer
		return null;
	}

//--------------------------------------------------------------------
	private void merge(Node thisNode, Newscast peer, Node peerNode) {
		int i1 = 0; /* Index first cache */
		int i2 = 0; /* Index second cache */
		boolean first;
		boolean lastTieWinner = CommonState.r.nextBoolean();
		int i = 1; // Index new cache. first element set in the end
		// SimpleNewscast.tn[0] is always null. it's never written anywhere
		final int d1 = degree();
		final int d2 = peer.degree();
		// cachesize is cache.length
	
		// merging two arrays
		while (i < this.cache.length && i1 < d1 && i2 < d2) {
			if (this.tstamps[i1] == peer.tstamps[i2]) {
				lastTieWinner = first = !lastTieWinner;
			} else {
				first = this.tstamps[i1] > peer.tstamps[i2];
			}
	
			if (first) {
				if (this.cache[i1] != peerNode && !Newscast.contains(i, this.cache[i1])) {
					Newscast.tn[i] = this.cache[i1];
					Newscast.ts[i] = this.tstamps[i1];
					i++;
				}				
				i1++;
			} else {
				if (peer.cache[i2] != thisNode && !Newscast.contains(i, peer.cache[i2])) {
					Newscast.tn[i] = peer.cache[i2];
					Newscast.ts[i] = peer.tstamps[i2];
					i++;
				}
				i2++;
			}
		}
	
		// if one of the original arrays got fully copied into
		// tn and there is still place, fill the rest with the other
		// array
		if (i < this.cache.length) {
			// only one of the for cycles will be entered
	
			for (; i1 < d1 && i < this.cache.length; ++i1) {
				if (cache[i1] != peerNode && !Newscast.contains(i, this.cache[i1])) {
					Newscast.tn[i] = this.cache[i1];
					Newscast.ts[i] = this.tstamps[i1];
					i++;
				}
			}
	
			for (; i2 < d2 && i < this.cache.length; ++i2) {
				if (peer.cache[i2] != thisNode && !Newscast.contains(i, peer.cache[i2])) {
					Newscast.tn[i] = peer.cache[i2];
					Newscast.ts[i] = peer.tstamps[i2];
					i++;
				}
			}
		}
	
		// if the two arrays were not enough to fill the buffer
		// fill in the rest with nulls
		if (i < this.cache.length) {
			for (; i < this.cache.length; ++i) {
				Newscast.tn[i] = null;
			}
		}
	}

//--------------------------------------------------------------------
	private static boolean contains(int size, Node peer) {
		for (int i = 0; i < size; i++) {
			if (Newscast.tn[i] == peer)
				return true;
		}
		return false;
	}

//--------------------------------------------------------------------
	public Node getNeighbor(int i) {
		return this.cache[i];
	}

//--------------------------------------------------------------------
	public ArrayList<Node> getNeighbors() {
		ArrayList<Node> neighbours = new ArrayList<Node>();
		for (Node node : this.cache)
			neighbours.add(node);
		
		return neighbours;
	}
	
//--------------------------------------------------------------------
	public int degree() {
	
		int len = this.cache.length - 1;
		while (len >= 0 && this.cache[len] == null)
			len--;
		return len + 1;
	}

//--------------------------------------------------------------------
	public boolean addNeighbor(Node node) {
	
		int i;
		for (i = 0; i < this.cache.length && this.cache[i] != null; i++) {
			if (this.cache[i] == node)
				return false;
		}
	
		if (i < this.cache.length) {
			if (i > 0 && this.tstamps[i - 1] < CommonState.getIntTime()) {
				// we need to insert to the first position
				for (int j = this.cache.length - 2; j >= 0; --j) {
					this.cache[j + 1] = this.cache[j];
					this.tstamps[j + 1] = this.tstamps[j];
				}
				i = 0;
			}
			
			this.cache[i] = node;
			this.tstamps[i] = CommonState.getIntTime();
			
			return true;
		} else
			throw new IndexOutOfBoundsException();
	}

//--------------------------------------------------------------------
	public boolean contains(Node n) {
		for (int i = 0; i < this.cache.length; i++) {
			if (this.cache[i] == n)
				return true;
		}
		
		return false;
	}

//--------------------------------------------------------------------
	public void onKill() {
		this.cache = null;
		this.tstamps = null;
	}

//--------------------------------------------------------------------
	public void nextCycle(Node n, int protocolID) {
		Node peerNode = getPeer();
		if (peerNode == null) {
			System.err.println("Newscast: no accessible peer");
			return;
		}
	
		Newscast peer = (Newscast) (peerNode.getProtocol(protocolID));
		merge(n, peer, peerNode);
	
		// set new cache in this and peer
		System.arraycopy(Newscast.tn, 0, cache, 0, cache.length);
		System.arraycopy(Newscast.ts, 0, tstamps, 0, tstamps.length);
		System.arraycopy(Newscast.tn, 0, peer.cache, 0, cache.length);
		System.arraycopy(Newscast.ts, 0, peer.tstamps, 0, tstamps.length);
	
		// set first element
		this.tstamps[0] = peer.tstamps[0] = CommonState.getIntTime();
		this.cache[0] = peerNode;
		peer.cache[0] = n;
		//System.out.println(n.getID() + " --> " + this.toString());
		
		
	}

//--------------------------------------------------------------------
	public String toString() {
		if (cache == null) 
			return "DEAD!";
		
		StringBuffer sb = new StringBuffer();
	
		for (int i = 0; i < degree(); ++i) {
			sb.append(" (" + cache[i].getIndex() + "," + tstamps[i] + ")");
		}
		
		return sb.toString();
	}
}
