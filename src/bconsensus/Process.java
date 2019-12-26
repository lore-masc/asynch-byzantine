package bconsensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;

import counter.Counter;
import message.Message;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Process {
	static final int ACTIVITIES = 2;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;
	private int round;
	private int step;
	private Message last_sent;
	private Integer decision;
	private HashMap< Integer, ArrayList<Message> > validatedSet; 
	private HashMap< Integer, Counter> counter; 			// get the echo/ready counter given a sender process id
	Queue<Message> in_messages;
	Queue<Message> out_messages;
	ArrayList<Process> processes;
	
	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.round = 0;
		this.step = 0;
		this.decision = null;
		this.validatedSet = new HashMap< Integer, ArrayList<Message> >();
		this.in_messages = new LinkedList<Message>();
		this.out_messages = new LinkedList<Message>();
		this.processes = new ArrayList<Process>();
		this.counter = new HashMap<Integer, Counter>();
	}
	
	private void initial(Process sender, int v, int k) {
		// save my proposal
		Message msg = new Message(this, null, Message.MessageType.INITIAL, v, k);
		this.last_sent = msg;
				
		// send to all
		for (Process to : this.processes) 
			this.out_messages.add(new Message(sender, to, Message.MessageType.INITIAL, v, k));
	}
	
	private void echo(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.ECHO, v, k));		
	}
	
	private void ready(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.READY, v, k));		
	}
	
	private void accept(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.ACCEPT, v, k));		
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void phase() {	
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
		
		if (this.id == 0) {
			if (this.round % 3 == 0) {					// Phase 1
				if (this.step == 0) {
					int proposed_v = RandomHelper.nextIntFromTo(0, 1);
					this.broadcast(this.round, proposed_v);
					System.out.println(this.id + " broadcasts " + proposed_v);
				}
			} else if (this.round % 3 == 1) {			// Phase 2
				
			} else if (this.round % 3 == 2) {			// Phase 3
				
			}
		}
		
		int step_0 = 0, step_1 = 0, step_2 = 0, step_3 = 0;
		int value_0 = 0, value_1 = 0;
		for (Process p : this.processes) {
			switch (p.getProposedValue()) {
				case 0:
					value_0++;
					break;
				case 1:
					value_1++;
					break;
			}
			switch (p.getStep()) {
				case 0:
					step_0++;
					break;
				case 1:
					step_1++;
					break;
				case 2:
					step_2++;
					break;
				case 3:
					step_3++;
					break;
			}
		}
		
		/*System.out.println("(0)=" + value_0 + " (1)=" + value_1 + " f_step0 = " + step_0 + " f_step1 = " + step_1 + " f_step2 = " + step_2 + " f_step3 = " + step_3);
		if (this.last_sent != null) 
			System.out.println(this.id + "(" + this.last_sent.getV() + ") - step = " + this.step + " - Echo = [" + this.echoCount[0] + ", " + this.echoCount[1] + "] - Ready = [" + this.readyCount[0] + ", " + this.readyCount[1] + "]");
		 */
	}
	
	private void broadcast(int r, int v) {
		this.initial(this, v, r);
		this.step = 1;
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
		int n = this.processes.size();
		int t = ((n / 3 - 1) > 0) ? n / 3 - 1 : 0; //(Integer) params.getValue("byzantine_processes");			// number of byzantine processes
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		boolean done = false;
		Process sender = msg.getSender();
		
		if (msg.getType() == Message.MessageType.INITIAL && this.round == msg.getRound() && this.step <= 1) {
			if (this.step == 0)
				this.initial(sender, v, this.round);
			this.echo(sender, v, this.round);
			this.step = 2;
			done = true;
		}
		else if (msg.getType() == Message.MessageType.ECHO) {
			if (this.step > 0 && this.round == msg.getRound()) {
				if(counter.keySet().contains(sender.id)) {
					Counter count = new Counter();
					count = counter.getOrDefault(sender.id, count);
					count.incrementEchoCounter(v);
					counter.put(sender.id, count);
				} else {
					ArrayList<Integer> echoArray = new ArrayList<Integer>();
					echoArray.add(0);
					echoArray.add(0);
					echoArray.set(v, echoArray.get(v) + 1);
					Counter count = new Counter();
					count.setEchoCounter(echoArray);
					counter.put(sender.id, count);
				}
				done = true;
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(sender.id, def);
			if (this.round == msg.getRound() && count.mostEchoValueCounter() > (n + t) / 2) {
				if (this.step == 1 || this.step == 2) {
					if (this.step == 1) {
						this.echo(sender, count.mostEchoValue(), this.round);
					} else if (this.step == 2) {
						this.ready(sender, count.mostEchoValue(), this.round);
					}
					this.step++;
					done = true;
				}
			}
		}
		else if (msg.getType() == Message.MessageType.READY) {
			if (this.step > 0 && this.round == msg.getRound()) {
				if(counter.keySet().contains(sender.id) && v <= 1) {
					Counter count = new Counter();
					count = counter.getOrDefault(sender.id, count);
					count.incrementReadyCounter(v);
					counter.put(sender.id, count);
				} else {
					ArrayList<Integer> readyArray = new ArrayList<Integer>();
					readyArray.add(0);
					readyArray.add(0);
					readyArray.set(v, readyArray.get(v) + 1);
					Counter count = new Counter();
					count.setReadyCounter(readyArray);
					counter.put(sender.id, count);
				}
				done = true;
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(sender.id, def);
			if (this.last_sent != null && /*this.last_sent.getV() == v &&*/ this.round == msg.getRound() && count.mostReadyValueCounter() > t + 1) {
				if (this.step == 1 || this.step == 2) {
					if (this.step == 1) {
						this.echo(sender, count.mostReadyValue(), this.round);
					} else if (this.step == 2) {
						this.ready(sender, count.mostReadyValue(), this.round);
					}
					this.step++;
					done = true;
				}
			}
			if (this.round == msg.getRound() && count.mostReadyValueCounter() > 2 * t + 1 && this.step == 3) {
				accept(sender, count.mostReadyValue(), this.round);
				update_validate_set(msg);
				this.round++;
				this.step = 0;		// restart proposing a new value
				done = true;
			}
		}
		else if (msg.getType() == Message.MessageType.ACCEPT) {
			update_validate_set(msg);
			done = true;
			//System.out.println("ACQUISITO " + msg.getType() + " dal processo " + this.id + "(" + this.last_sent.getV() + ") nello step " + this.step + " v = " + v);
		}
		
		/*
		if (done)
			System.out.println("ACQUISITO " + msg.getType() + " dal processo " + this.id + "(" + this.last_sent.getV() + ") nello step " + this.step + " v = " + v);
		else
			System.out.println("scartato " + msg.getType() + " dal processo " + this.id + "(" + this.last_sent.getV() + ") nello step " + this.step + " v = " + v);
		*/
	}

	/*private int getMostEchoValue(int process_id) {
		return (this.echoCount[0] > this.echoCount[1]) ? 0: 1;
	}
	
	private int getMostReadyValue(int process_id) {
		return (this.readyCount[0] > this.readyCount[1]) ? 0: 1;
	}

	private void keepEcho(int process_id, int v) {
		echoCount[v]++;
	}

	private void keepReady(int process_id, int v) {		
		this.readyCount[v]++;
	}*/
	
	
	public void update_validate_set(Message msg) {
		System.out.println("ACCEPTED " + msg.getV());
		/*
		int key = msg.getRound();
		
		if (this.validatedSet.containsKey(key))
			this.validatedSet.get(key).add(msg);
		else {
			this.validatedSet.put(key, new ArrayList());
			this.validatedSet.get(key).add(msg);
		}
		*/
	}
	
	@ScheduledMethod(start = 3, interval = 3)
	public void clearNetowrks() {
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
	}
	
	public int getStep() {
		return this.step;
	}

	public int getProposedValue() {
		return (this.last_sent != null) ? this.last_sent.getV() : -1;
	}
}
