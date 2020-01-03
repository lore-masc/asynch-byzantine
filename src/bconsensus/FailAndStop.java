package bconsensus;

import message.Message;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class FailAndStop extends Process {

	public FailAndStop(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		super(space, grid, id);
	}
	
	@Override
	protected void initial(Process sender, int v, int k) {

	}
	
	@Override
	protected void echo(Process sender, int v, int k) {

	}
	
	@Override
	protected void ready(Process sender, int v, int k) {
		
	}
	
	@Override
	public void receive() {	
		
	}

}
