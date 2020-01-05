package styles;

import java.awt.Color;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import bconsensus.ByzantineProcess;
import bconsensus.FailAndStop;
import bconsensus.Process;

public class ProcessStyle extends DefaultStyleOGL2D {

    @Override
    public Color getColor(Object object) {
        Process a;
        Color color = Color.BLUE;
        if (object instanceof FailAndStop)
        	color = Color.BLACK;
        else if (object instanceof Process) {
            a = (Process) object;
            if (a.getDecision() != null) {
            	if (a.getDecision() == 0)
	            	color = Color.GREEN;
	            else 
	            	color = Color.RED;
            } else if (a.getValue() != null) {
            	if (a instanceof ByzantineProcess) {
	            	if (a.getValue() == 0)
		            	color = Color.RED;
		            else 
		            	color = Color.GREEN;
            	} else {
            		if (a.getValue() == 0)
		            	color = Color.GREEN;
		            else 
		            	color = Color.RED;
            	}
            }
        }
        return color;
    }
}
