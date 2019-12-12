package bconsensus;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Process {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;

	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
	}
	
	

}
