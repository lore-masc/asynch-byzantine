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
	
	private void initial(int v) {
		// save my proposal
		Message msg = new Message(null, Message.MessageType.INITIAL, v);
		this.last_sent = msg;
		
		// send to all
		for (Process to : this.processes) 
			this.out_messages.add(new Message(to, Message.MessageType.INITIAL, v));
	}
	
	private void echo(int v) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(to, Message.MessageType.ECHO, v));		
	}
	
	private void ready(int v) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(to, Message.MessageType.READY, v));		
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void broadcast() {
		// Repast utilities used to draw edges
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> initial_net = (Network<Object>) context.getProjection("initial_net");
		Network<Object> echo_net = (Network<Object>) context.getProjection("echo_net");
		Network<Object> ready_net = (Network<Object>) context.getProjection("ready_net");
		Network<Object> accept_net = (Network<Object>) context.getProjection("accept_net");
		
		// Clear all printed edges
		initial_net.removeEdges();
		echo_net.removeEdges();
		ready_net.removeEdges();
		accept_net.removeEdges();
		
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
			double new_broadcast = (Double) params.getValue("send_broadcast");
			if (RandomHelper.nextInt() < new_broadcast) {
				int proposed_v = RandomHelper.nextIntFromTo(0, 1);
				this.initial(proposed_v);
				this.step = 1;
			}
		}
		
		if (this.id == 0) {
			System.out.println("Step: " + this.step);
			if (this.last_sent != null) {
				System.out.println("Proposed value: " + this.last_sent.getV());
				System.out.println("Echo received: " + this.last_sent.getEcho());
				System.out.println("Ready received: " + this.last_sent.getReady());
				System.out.println("Accepted: " + this.last_sent.getAcceptState());
			}
		}
		
	}
	
	private void send() {
		Message msg = this.out_messages.remove();
		Process to = msg.to;
		to.in_messages.add(msg);
		
		// Draw the current passage
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> initial_net = (Network<Object>) context.getProjection("initial_net");
		Network<Object> echo_net = (Network<Object>) context.getProjection("echo_net");
		Network<Object> ready_net = (Network<Object>) context.getProjection("ready_net");
		Network<Object> accept_net = (Network<Object>) context.getProjection("accept_net");
		switch (msg.getType()) {
			case INITIAL:
				initial_net.addEdge(this, to);
				break;
			case ECHO:
				echo_net.addEdge(this, to);
				break;
			case READY:
				ready_net.addEdge(this, to);
				break;
			case ACCEPT:
				accept_net.addEdge(this, to);
				break;	
		}
		
	}
	
	public void receive() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int t = (Integer) params.getValue("byzantine_processes");			// number of byzantine processes
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		
		if (msg.getType() == Message.MessageType.INITIAL && this.step == 1) {
			this.echo(v);
			this.step = 2;
		}
		
		if (msg.getType() == Message.MessageType.ECHO && (this.step == 1 || this.step == 2) ) {
			if (this.last_sent != null && this.last_sent.getV() == msg.getV() && this.last_sent.getEcho() < processes.size() / 2)
				this.last_sent.keepEcho();
			else if (this.last_sent != null && this.last_sent.getV() == msg.getV()) {
				if (this.step == 1)
					this.echo(v);
				else
					this.ready(v);
				this.step++;
			}
		}
		
		if (msg.getType() == Message.MessageType.READY && (this.step == 1 || this.step == 2) ) {
			if (this.last_sent != null && this.last_sent.getV() == msg.getV() && this.last_sent.getReady() < t + 1)
				this.last_sent.keepReady();
			else if (this.last_sent != null && this.last_sent.getV() == msg.getV()) {
				this.ready(v);
				this.step++;
			}
		}
		
		if (msg.getType() == Message.MessageType.READY && this.step == 3 ) {
			if (this.last_sent != null && this.last_sent.getV() == msg.getV() && this.last_sent.getReady() < 2 * t + 1)
				this.last_sent.keepReady();
			else if (this.last_sent != null && this.last_sent.getV() == msg.getV()) {
				this.last_sent.accept();
				this.step = 0;		// restart proposing a new value
			}
		}
		
		if (msg.getType() == Message.MessageType.ACCEPT) {
			System.out.println("Someone accepts a value: " + msg.getV());
		}
	}

}
