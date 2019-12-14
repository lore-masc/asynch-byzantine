package bconsensus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import message.Message;
import message.Message.MessageType;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Process {
	static final int ACTIVITIES = 2;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;
	private int step;
	private Message last_sent;
	Queue<Message> in_messages;
	Queue<Message> out_messages;
	ArrayList<Process> processes;
	
	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.step = 0;
		this.in_messages = new LinkedList<Message>();
		this.out_messages = new LinkedList<Message>();
		this.processes = new ArrayList<Process>();
	}
	
	public void initial(int v) {	
		// send to all
		for (Process to : this.processes) {
			Message new_broadcast = new Message(to, Message.MessageType.INITIAL, v);
		}
		this.step = 1;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void broadcast() {
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> send_net = (Network<Object>) context.getProjection("network");
		
		// How many work this process can do in this step
		Parameters params = RunEnvironment.getInstance().getParameters();
		int min_workload = (Integer) params.getValue("min_workload");
		int max_workload = (Integer) params.getValue("max_workload");
		int workload = RandomHelper.nextIntFromTo(min_workload, max_workload);
		
		while(workload > 0 && (!this.out_messages.isEmpty() || !this.in_messages.isEmpty())) {				
			// select the type of work to do
			boolean done = false;
			int work = RandomHelper.nextIntFromTo(1, Process.ACTIVITIES);
			if (work == 1 && !this.in_messages.isEmpty()) {
				this.receive();
				done = true;
			} else if (work == 2 && !this.out_messages.isEmpty()) {
				this.send();
				done = true;
			}
			if (done)
				workload--;
		}
		
		if (this.step == 0) {
			int new_broadcast = (Integer) params.getValue("send_broadcast");
			if (RandomHelper.nextInt() < new_broadcast) {
				int proposed_v = RandomHelper.nextInt();
				this.initial(proposed_v);
			}
		}
		
		if (this.step == 1) {
			
		}
		
		if (this.step == 2) {
			
		}
		
		if (this.step == 3) {
			
		}
		
	}
	
	private void send() {
		Message msg = this.out_messages.remove();
		Process to = msg.to;
		to.in_messages.add(msg);
		this.last_sent = msg;
	}
	
	public void receive() {
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		
		if (msg.getType() == Message.MessageType.INITIAL) 
			for (Process to : this.processes)
				this.out_messages.add(new Message(to, Message.MessageType.ECHO, v));
		
		if (msg.getType() == Message.MessageType.ECHO) {
			
		}
		
		if (msg.getType() == Message.MessageType.READY) {
			
		}
		
		if (msg.getType() == Message.MessageType.ACCEPT) {
			
		}
	}

}
