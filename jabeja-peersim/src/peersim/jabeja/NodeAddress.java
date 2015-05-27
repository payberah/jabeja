package peersim.jabeja;

import peersim.core.Node;

public class NodeAddress {
	private int id;
	private Node node;
	
//------------------------------------------------------------------------	
	public NodeAddress(Node node, int id) {
		this.id = id;
		this.node = node;
	}

//------------------------------------------------------------------------	
	public void setId(int id) {
		this.id = id;
	}

//------------------------------------------------------------------------	
	public Node getNode() {
		return this.node;
	}

//------------------------------------------------------------------------	
	public int getId() {
		return this.id;
	}

//------------------------------------------------------------------------
	@Override
	public String toString() {
		return "id: " + id;
	}

//------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(id);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

//------------------------------------------------------------------------
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeAddress other = (NodeAddress) obj;
		if (Double.doubleToLongBits(id) != Double.doubleToLongBits(other.id))
			return false;
		return true;
	}
}
