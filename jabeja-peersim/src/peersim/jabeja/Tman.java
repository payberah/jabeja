package peersim.jabeja;

import java.util.ArrayList;

import peersim.core.*;
import peersim.cdsim.CDProtocol;
import peersim.jabeja.Newscast;

public class Tman implements CDProtocol {
	private Newscast newscast;
	private Node selfNode;
	private Node[] view = null;
	private int viewSize;
	private int jabejaPID;
	
	
//------------------------------------------------------------------------
	public Tman(String prefix) {
	}
	
//------------------------------------------------------------------------
	public Object clone() {
		Tman tman = new Tman("");
		
		return tman;
	}

//------------------------------------------------------------------------
    public void init(Node node, int cacheSize, int newscastPID, int partitionPID) {
    	this.selfNode = node;
    	this.viewSize = cacheSize;
    	this.jabejaPID = partitionPID;
    	this.newscast = (Newscast)node.getProtocol(newscastPID);
    }

//------------------------------------------------------------------------
    public Node[] getView() {
       	return this.view;
    }
    
//--------------------------------------------------------------------
  	public ArrayList<Node> getNeighbors() {
  		if (this.view == null)
  			return null;
  		
  		ArrayList<Node> neighbours = new ArrayList<Node>();
  		for (Node node : this.view)
  			neighbours.add(node);
  		
  		return neighbours;
  	}

//------------------------------------------------------------------------
    public void nextCycle(Node node, int protocolID) {
    	if (this.view != null && this.view.length > 0) {
    		int index = CommonState.r.nextInt(this.view.length);
			Node selectedNode = this.view[index];
			
			if (selectedNode == null || !selectedNode.isUp())
				return;

			Buffer buffer = new Buffer(this.selfNode, this.view);
			buffer.merge(this.newscast.getNeighbors());
			
			// replace that selected peer's entry with your own and send this buffer to the selected peer
			buffer.remove(selectedNode);
			buffer.add(this.selfNode);
			ArrayList<Node> repliedBuffer = ((Tman)selectedNode.getProtocol(protocolID)).passiveThread(this.selfNode, buffer);

			
			buffer.merge(repliedBuffer);
			buffer.remove(this.selfNode);
			
			this.view = buffer.selectView(this.viewSize, this.jabejaPID);			
		}
		else {
			ArrayList<Node> nodes = this.newscast.getNeighbors();
			this.view = new Node[nodes.size()];
	        for (int i = 0; i < nodes.size(); ++i)
	        	this.view[i] = nodes.get(i);
		}
    }

//------------------------------------------------------------------------
    public ArrayList<Node> passiveThread(Node sender, Buffer receivedBuffer) {
    	Buffer tempBuffer = new Buffer(this.selfNode, this.view);
    	
    	tempBuffer.merge(newscast.getNeighbors());

    	Buffer buffer = new Buffer(this.selfNode, tempBuffer.getNodes());
		buffer.merge(receivedBuffer.getNodes());

		buffer.remove(this.selfNode);
		
		this.view = buffer.selectView(this.viewSize, this.jabejaPID);
		
		// replace that selected peer's entry with your own
		tempBuffer.remove(sender);
		tempBuffer.add(this.selfNode);
		
    	return tempBuffer.getNodes();
    }
}

