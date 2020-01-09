package styles;

import java.awt.Color;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import bconsensus.FailAndStop;
import bconsensus.Process;

public class ProcessValueStyle extends DefaultStyleOGL2D {

    @Override
    public Color getColor(Object object) {
        Process a;
        Color color = Color.BLUE;
        if (object instanceof FailAndStop)
        	color = Color.BLACK;
        else {
            a = (Process) object;
            if (a.getDecision() != null) {
            	if (a.getDecision() == 0)
	            	color = Color.BLUE;
	            else 
	            	color = Color.ORANGE;
            } else if (a.getValue() != null) {
        		if (a.getValue() == 0)
	            	color = Color.BLUE;
	            else 
	            	color = Color.ORANGE;
            }
        }
        return color;
    }
}
