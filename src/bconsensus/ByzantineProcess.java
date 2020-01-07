package bconsensus;

import message.Message;
import message.Message.MessageType;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class ByzantineProcess extends Process {

	public ByzantineProcess(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		super(space, grid, id);
	}

	@Override
	protected void initial(Process sender, int v, int k) {
		byzBehaviour(sender, k, Message.MessageType.INITIAL);
	}
	
	@Override
	protected void echo(Process sender, int v, int k) {
		byzBehaviour(sender, k, Message.MessageType.ECHO);
	}
	
	@Override
	protected void ready(Process sender, int v, int k) {
		byzBehaviour(sender, k, Message.MessageType.READY);
	}
	
	private void byzBehaviour (Process sender, int k, MessageType msgType) {
		boolean label;
		if (RandomHelper.nextIntFromTo(0, 1) == 1)
			label = true;
		else
			label = false;
		
		for (Process to : this.processes) 
			this.out_messages.add(new Message(sender, to, msgType, RandomHelper.nextIntFromTo(0, 1), label, k));
	} 
	
}
