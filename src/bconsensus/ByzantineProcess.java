package bconsensus;

import message.Message;
import message.Message.MessageType;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class ByzantineProcess extends Process {

	public ByzantineProcess(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		super(space, grid, id);
	}

	@Override
	protected void initial(Process sender, int v, int k) {
		byzBehaviour(sender, v, k, Message.MessageType.INITIAL);
	}
	
	@Override
	protected void echo(Process sender, int v, int k) {
		byzBehaviour(sender, v, k, Message.MessageType.ECHO);
	}
	
	@Override
	protected void ready(Process sender, int v, int k) {
		byzBehaviour(sender, v, k, Message.MessageType.READY);
	}
	
	private void byzBehaviour (Process sender, int v, int k, MessageType msgType) {
		int byz = v;
		
		if(v == 0)
			byz = 1;
		else
			byz = 0;
		
		if(msgType == Message.MessageType.INITIAL) {
			System.out.println("! " +  sender.id + " broadcasts " + byz + " !" );
		}
		for (Process to : this.processes) 
			this.out_messages.add(new Message(sender, to, msgType, byz, this.label, k));
	} 
	
}
