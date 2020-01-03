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
		
		// create direct link networks to represent broadcasting
		NetworkBuilder<Object> initial_net = new NetworkBuilder<Object>(
				"initial_net", context, true);
		initial_net.buildNetwork();
		NetworkBuilder<Object> echo_net = new NetworkBuilder<Object>(
				"echo_net", context, true);
		echo_net.buildNetwork();
		NetworkBuilder<Object> ready_net = new NetworkBuilder<Object>(
				"ready_net", context, true);
		ready_net.buildNetwork();
		
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
		int byz = (Integer) params.getValue("byzantine_processes");
		int fail = (Integer) params.getValue("fail_processes");
		ArrayList<Integer> byz_indexes = new ArrayList<Integer>();
		ArrayList<Integer> fail_indexes = new ArrayList<Integer>();
		int i = byz;
		while (i > 0 && byz_indexes.size() < processesCount) {
			int n = RandomHelper.nextIntFromTo(0, processesCount - 1);
			if (!byz_indexes.contains(n)) {
				byz_indexes.add(n);
				i--;
			}
		}
		
		i = fail;
		while (i > 0 && fail_indexes.size() < processesCount) { 
			int n = RandomHelper.nextIntFromTo(0, processesCount - 1);
			if (!fail_indexes.contains(n) && !byz_indexes.contains(n)) {
				fail_indexes.add(n);
				i--;
			}
		}
		
		ArrayList<Process> processes = new ArrayList<Process>();
		for (int id = 0; id < processesCount; id++) {
			Process p;
			if (fail_indexes.contains(id))
				p = new FailAndStop(space, grid, id);
			else if (byz_indexes.contains(id))
				p = new ByzantineProcess(space, grid, id);
			else
				p = new Process(space, grid, id);
			
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