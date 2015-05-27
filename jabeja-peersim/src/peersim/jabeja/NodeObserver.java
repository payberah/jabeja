package peersim.jabeja;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Node;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.util.FileIO;
import peersim.util.FileNameGenerator;

public class NodeObserver implements Control {
    private static final String FILENAME_BASE = "filename";
	private static final String JABEJA_PROTOCOL = "jabeja";
	private static final String NUM_COLORS = "init.node.colors";

    private final int jabejaPID;
    private final String graphFilename;
    private final int numColors;
    
    private FileNameGenerator fng;
    private int round = 0;
    private int grayLinks = 0;
    private int[] colorPopulation;
        
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------
    public NodeObserver(String prefix) {
    	this.jabejaPID = Configuration.getPid(prefix + "." + JABEJA_PROTOCOL);
    	this.graphFilename = Configuration.getString(prefix + "." + FILENAME_BASE, "graph_dump");
    	this.numColors = Configuration.getInt(NUM_COLORS);
    	
        this.colorPopulation = new int[this.numColors];
        this.fng = new FileNameGenerator(this.graphFilename, ".dat");
        
//        FileIO.write("", this.graphFilename + "statistics.txt");
    }
    
//------------------------------------------------------------------------
    private void graphToFile(boolean dump) {
        String[] colors = {"red", "blue", "green", "yellow", "violet", "pink", "skyblue", "gold", "darkgreen", "purple"};
        double noise = 2;
        this.grayLinks = 0;
        int migrations = 0;

    	for (int c = 0; c < this.numColors; ++c)
    		this.colorPopulation[c] = 0;

    	float[] nonDominant = new float[this.numColors];
    	for (int c = 0; c < this.numColors; ++c)
    		nonDominant[c] = 0;
    	
    	String redStr = "\n{node [style=filled, color=red]";
    	String blueStr = "\n{node [style=filled, color=blue]";
    	String greenStr = "\n{node [style=filled, color=green]";
    	String yellowStr = "\n{node [style=filled, color=yellow]";
    	String violetStr = "\n{node [style=filled, color=violet]";
    	String pinkStr = "\n{node [style=filled, color=pink]";
    	String grayStr = "\n{node [style=filled, color=skyblue]";
      	String goldStr = "\n{node [style=filled, color=gold]";
    	String darkgreenStr = "\n{node [style=filled, color=darkgreen]";
    	String purpleStr = "\n{node [style=filled, color=purple]";
    	String blackStr = "\n{node [style=filled, color=black]";
    	
//    	if (dump == true) 
//    		ps.println("graph round_" + this.round + " {\n node [height=\"0.2\",width=\"0.2\",label=\"\"];"); // Neato Style
    	
        for (int i = 0; i < Network.size(); i++) {
        	Node node = Network.get(i);
            JabejaNode current = (JabejaNode)node.getProtocol(this.jabejaPID);
            int currentColor = current.getColor();
            if (currentColor != current.initialColor)
            	migrations++;
            
            String label = " " + current.getId() + "[label=\"";// + current.getId() + ":";
            label += currentColor + "\"]";

            this.colorPopulation[currentColor]++;
            
            switch (currentColor) {
            	case 0: redStr += label;
            		break;
            	case 1: blueStr += label;
            		break;
            	case 2: greenStr += label;
            		break;
            	case 3: yellowStr += label;
            		break;
            	case 4: violetStr += label;
            		break;
            	case 5: pinkStr += label;
            		break;
            	case 6: grayStr += label;
            		break;
            	case 7: goldStr += label;
            		break;
            	case 8: darkgreenStr += label;
            		break;			
            	case 9: purpleStr += label;;
            		break;
	        default:
            	break;
            }
        }
    
//        if (dump == true) {
//	        ps.println(redStr + "}");
//	        ps.println(blueStr + "}");
//	        ps.println(greenStr + "}");
//	        ps.println(yellowStr + "}");
//	        ps.println(violetStr + "}");
//	        ps.println(pinkStr + "}");
//	        ps.println(purpleStr + "}");
//	        ps.println(goldStr + "}");
//	        ps.println(grayStr + "}");
//	        ps.println(darkgreenStr + "}");
//	        ps.println(blackStr + "}");
//	        ps.println("edge [len = 1];");
//        }
        for (int i = 0; i < Network.size(); i++) {
        	Node node = Network.get(i);
        	JabejaNode current = (JabejaNode)node.getProtocol(this.jabejaPID);
        	noise = current.noise;
        	ArrayList<Node> currentNeighbours = current.getNeighbours();
        	int currentColor = current.getColor();
        	
            if (currentNeighbours != null) {
	            for (Node n : currentNeighbours) {
	            	if (n != null) {
	            		JabejaNode p = (JabejaNode)n.getProtocol(this.jabejaPID);
	            		int pColor = p.getColor();
	            		
	            		if (currentColor != pColor)
	            			grayLinks ++;
	            		
//	            		if (dump == true) {
//		            		if (currentColor == pColor) // Neato Style
//		            			ps.println(current.getId() + " -- " +  p.getId() + "[color=" + colors[currentColor] + "] [dir = forward] [len=5];");
//		            		else
//		            			ps.println(current.getId() + " -- " +  p.getId() + "[color=gray] [dir=forward] [len=5];");
//		            		
//			                ps.println();
//	            		}
		            }
	            }
            }
        }
        
//        if (dump == true)
//        	ps.println("}"); // Neato Style
                
        StringBuffer sb = new StringBuffer();
        if (round == 0)
        	sb.append("\nseed: " + CommonState.r.getLastSeed() + "\t");
        
//        sb.append("ROUND " + this.round + ":\t");        
        System.out.println("ROUND " + this.round + " (noise=" + noise + ")\t\t\tedge cut:" + this.grayLinks / 2 + "\t#swaps: " + JabejaNode.getNumberOfSwaps() + "\t#localswaps: " + JabejaNode.getNumberOfLocalSwaps() + "\t#cost: " + (JabejaNode.getNumberOfSwaps() - JabejaNode.getNumberOfLocalSwaps()) + "\t#migrations: " + migrations);
        
//        for (int c = 0; c < this.numColors; ++c) {
//        	System.out.println("#" + c + ":\t" + this.colorPopulation[c] + "\t-> " + (int)nonDominant[c] + "\t");
//    		sb.append(colorPopulation[c] + "\t\t");
//        }

        
        sb.append("\t" + this.grayLinks / 2 + "\t#swaps: " + JabejaNode.getNumberOfSwaps() + "\t#localswaps: " + JabejaNode.getNumberOfLocalSwaps() + "\t#cost: " + (JabejaNode.getNumberOfSwaps() - JabejaNode.getNumberOfLocalSwaps()) + "\t#migrations: " + migrations + "\n");
        FileIO.append(sb.toString(), this.graphFilename + "statistics.txt");
        JabejaNode.resetNumberOfSwaps();
    }
    
//------------------------------------------------------------------------
//    private void graphToFile(boolean dump, PrintStream ps) {
//        String[] colors = {"red", "blue", "green", "yellow", "violet", "pink", "skyblue", "gold", "darkgreen", "purple"};
//        float noise = 2;
//        this.grayLinks = 0;
//        int migrations = 0;
//
//    	for (int c = 0; c < this.numColors; ++c)
//    		this.colorPopulation[c] = 0;
//
//    	float[] nonDominant = new float[this.numColors];
//    	for (int c = 0; c < this.numColors; ++c)
//    		nonDominant[c] = 0;
//    	
//    	String redStr = "\n{node [style=filled, color=red]";
//    	String blueStr = "\n{node [style=filled, color=blue]";
//    	String greenStr = "\n{node [style=filled, color=green]";
//    	String yellowStr = "\n{node [style=filled, color=yellow]";
//    	String violetStr = "\n{node [style=filled, color=violet]";
//    	String pinkStr = "\n{node [style=filled, color=pink]";
//    	String grayStr = "\n{node [style=filled, color=skyblue]";
//      	String goldStr = "\n{node [style=filled, color=gold]";
//    	String darkgreenStr = "\n{node [style=filled, color=darkgreen]";
//    	String purpleStr = "\n{node [style=filled, color=purple]";
//    	String blackStr = "\n{node [style=filled, color=black]";
//    	
//    	if (dump == true) 
//    		ps.println("graph round_" + this.round + " {\n node [height=\"0.2\",width=\"0.2\",label=\"\"];"); // Neato Style
//    	
//        for (int i = 0; i < Network.size(); i++) {
//        	Node node = Network.get(i);
//            JabejaNode current = (JabejaNode)node.getProtocol(this.jabejaPID);
//            int currentColor = current.getColor();
//            if (currentColor != current.initialColor)
//            	migrations++;
//            
//            String label = " " + current.getId() + "[label=\"";// + current.getId() + ":";
//            label += currentColor + "\"]";
//
//            this.colorPopulation[currentColor]++;
//            
//            switch (currentColor) {
//            	case 0: redStr += label;
//            		break;
//            	case 1: blueStr += label;
//            		break;
//            	case 2: greenStr += label;
//            		break;
//            	case 3: yellowStr += label;
//            		break;
//            	case 4: violetStr += label;
//            		break;
//            	case 5: pinkStr += label;
//            		break;
//            	case 6: grayStr += label;
//            		break;
//            	case 7: goldStr += label;
//            		break;
//            	case 8: darkgreenStr += label;
//            		break;			
//            	case 9: purpleStr += label;;
//            		break;
//	        default:
//            	break;
//            }
//        }
//    
//        if (dump == true) {
//	        ps.println(redStr + "}");
//	        ps.println(blueStr + "}");
//	        ps.println(greenStr + "}");
//	        ps.println(yellowStr + "}");
//	        ps.println(violetStr + "}");
//	        ps.println(pinkStr + "}");
//	        ps.println(purpleStr + "}");
//	        ps.println(goldStr + "}");
//	        ps.println(grayStr + "}");
//	        ps.println(darkgreenStr + "}");
//	        ps.println(blackStr + "}");
//	        ps.println("edge [len = 1];");
//        }
//        for (int i = 0; i < Network.size(); i++) {
//        	Node node = Network.get(i);
//        	JabejaNode current = (JabejaNode)node.getProtocol(this.jabejaPID);
//        	noise = current.noise;
//        	ArrayList<Node> currentNeighbours = current.getNeighbours();
//        	int currentColor = current.getColor();
//        	
//            if (currentNeighbours != null) {
//	            for (Node n : currentNeighbours) {
//	            	if (n != null) {
//	            		JabejaNode p = (JabejaNode)n.getProtocol(this.jabejaPID);
//	            		int pColor = p.getColor();
//	            		
//	            		if (currentColor != pColor)
//	            			grayLinks ++;
//	            		
//	            		if (dump == true) {
//		            		if (currentColor == pColor) // Neato Style
//		            			ps.println(current.getId() + " -- " +  p.getId() + "[color=" + colors[currentColor] + "] [dir = forward] [len=5];");
//		            		else
//		            			ps.println(current.getId() + " -- " +  p.getId() + "[color=gray] [dir=forward] [len=5];");
//		            		
//			                ps.println();
//	            		}
//		            }
//	            }
//            }
//        }
//        
//        if (dump == true)
//        	ps.println("}"); // Neato Style
//                
//        StringBuffer sb = new StringBuffer();
//        if (round == 0)
//        	sb.append("\nseed: " + CommonState.r.getLastSeed() + "\t");
//        
////        sb.append("ROUND " + this.round + ":\t");        
//        System.out.println("ROUND " + this.round + " (noise=" + noise + ")\t\t\tedge cut:" + this.grayLinks / 2 + "\t#swaps: " + JabejaNode.getNumberOfSwaps() + "\t#migrations: " + migrations);
//        
////        for (int c = 0; c < this.numColors; ++c) {
////        	System.out.println("#" + c + ":\t" + this.colorPopulation[c] + "\t-> " + (int)nonDominant[c] + "\t");
////    		sb.append(colorPopulation[c] + "\t\t");
////        }
//
//        
//        sb.append("\t" + this.grayLinks / 2 + "\t#swaps: " + JabejaNode.getNumberOfSwaps()+  "\t#migrations: " + migrations);
//        FileIO.append(sb.toString(), this.graphFilename + "statistics.txt");    
//        
//        
//    }
    
//-------------------------------------------------------------------------------------    
	@Override
	public boolean execute() {

			System.out.print("\nPartition Observer:");
	        
			//        try {
			//          String fname = this.fng.nextCounterName();
			//          FileOutputStream fos = new FileOutputStream(fname);
			//          System.out.println("Writing to file " + fname);
			//          PrintStream pstr = new PrintStream(fos);
			          graphToFile(true);
			          System.out.println("done!");
			          this.round++;
			//          fos.close();
			//          
			//      } catch (IOException e) {
			//          throw new RuntimeException(e);
			//      }
		return false;
	}
}
