package bconsensus;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class BConsensusBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		context.setId("bconsensus");
		
		// create a direct link network to represent broadcasting
		NetworkBuilder<Object> netBuilder1 = new NetworkBuilder<Object>(
				"network", context, true);
		netBuilder1.buildNetwork();
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 50,
				50);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, 50, 50));

		Parameters params = RunEnvironment.getInstance().getParameters();
		int processesCount = (Integer) params.getValue("processes_count");
		ArrayList<Process> processes = new ArrayList<Process>();
		for (int i = 0; i < processesCount; i++) {
			Process p = new Process(space, grid, i);
			processes.add(p);
			context.add(p);
		}
		
		for (Process p : processes)
			p.processes = processes;
		
		// Set a start location for each process
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			Object other;
			// Processes have to be located in different places!
			do {
				double x = RandomHelper.nextDoubleFromTo(0, grid.getDimensions().getWidth());
				double y = RandomHelper.nextDoubleFromTo(0, grid.getDimensions().getHeight());
				pt = new NdPoint(x, y);
				other = grid.getObjectAt((int) pt.getX(), (int) pt.getY());
			} while (other != null);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		if (RunEnvironment.getInstance().isBatch()) {
			RunEnvironment.getInstance().endAt(20);
		}
		
		return context;
	}
	
}