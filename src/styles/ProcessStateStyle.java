package styles;

import java.awt.Color;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import bconsensus.ByzantineProcess;
import bconsensus.FailAndStop;

public class ProcessStateStyle extends DefaultStyleOGL2D {

    @Override
    public Color getColor(Object object) {
        Color color = Color.BLUE;
        if (object instanceof FailAndStop)
        	color = Color.BLACK;
        else if (object instanceof ByzantineProcess) 
            color = Color.RED;
        else
        	color = Color.GREEN;
        
        return color;
    }
}
