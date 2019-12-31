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
	private Integer decision;
	private int round;
	private boolean label;
	private Integer value;
	private HashMap< Integer, ArrayList<Message> > validatedSet; 
	private HashMap<Pair<Integer, Integer>, Counter> counter; 			// get the echo/ready counter given a sender process id
	private HashMap<Pair<Integer, Integer>, Integer> steps;
	//private HashMap<Integer, Integer> rounds;
	Queue<Message> in_messages;
	Queue<Message> out_messages;
	ArrayList<Process> processes;
	
	
	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.decision = null;
		this.round = 0;
		this.label = false;
		this.value = null;
		this.validatedSet = new HashMap< Integer, ArrayList<Message> >();
		this.in_messages = new LinkedList<Message>();
		this.out_messages = new LinkedList<Message>();
		this.processes = new ArrayList<Process>();
		this.counter = new HashMap<Pair<Integer, Integer>, Counter>();
		this.steps = new HashMap<Pair<Integer,Integer>, Integer>();
		//this.rounds = new HashMap<Integer, Integer>();
	}
	
	public int getID() {
		return this.id;
	}
	
	private void initial(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes) 
			this.out_messages.add(new Message(sender, to, Message.MessageType.INITIAL, v, this.label, k));
	}
	
	private void echo(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.ECHO, v, this.label, k));		
	}
	
	private void ready(Process sender, int v, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.READY, v, this.label, k));		
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
		
		if (this.round % 3 == 0) {					// Phase 1
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				
				int proposed_v = RandomHelper.nextIntFromTo(0, 1);
				this.broadcast(this.round, proposed_v);
				System.out.println(this.id + " broadcasts " + proposed_v);
			}
		} else if (this.round % 3 == 1) {			// Phase 2
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);

				System.out.println("PHASE 2: " + this.id);
				System.out.println(this.id + " broadcasts " + this.value);
				this.broadcast(this.round, this.value);
			}
		} else if (this.round % 3 == 2) {			// Phase 3
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				
				System.out.println("PHASE 3: " + this.id);
				System.out.println(this.id + " broadcasts " + this.value + " with label = " + this.label);
				
				this.broadcast(this.round, this.value);
			}
		}
		
		/*if (this.id == 0) {			
			for(Pair<Integer, Integer> p : this.counter.keySet()) {
				Counter count = new Counter();
				count = counter.getOrDefault(p, count);
				synchronized (count) {
					System.out.println("PAIR: (" + p.getLeft() + " ; " + p.getRight() + ") - Echo 0: " + count.getEcho0() + " - Echo 1: " + count.getEcho1() + " Ready 0: " + count.getReady0() + " Ready 1: " + count.getReady1());
				}
			}
		}*/
		
		if(this.decision != null)
			System.out.println(this.id + " DECIDES " + this.decision);
	}
	
	private void broadcast(int r, int v) {
		this.initial(this, v, r);
		this.steps.put(Pair.of(this.id, r), 1);
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
		}
	}
	
	public void receive() {		
		Parameters params = RunEnvironment.getInstance().getParameters();
		int n = this.processes.size();
		int t = ((n / 3 - 1) > 0) ? n / 3 - 1 : 0;			// number of byzantine processes
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		Process sender = msg.getSender();

		/*if(!this.rounds.containsKey(sender.id)) {
			this.rounds.put(sender.id, msg.getRound());
		}*/
		if(!this.steps.containsKey(Pair.of(sender.id, msg.getRound()))) {
			this.steps.put(Pair.of(sender.id, msg.getRound()), 0);
		}
		
		
		if (msg.getType() == Message.MessageType.INITIAL /*&& this.round == msg.getRound()*/ && this.steps.get(Pair.of(sender.id, msg.getRound())) <= 1) {
			//if (this.step == 0)
			//	this.initial(sender, v, this.round);
			this.echo(sender, v, msg.getRound());
			this.steps.put(Pair.of(sender.id, msg.getRound()), 2);
		}
		else if (msg.getType() == Message.MessageType.ECHO) {
			if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) > 0 /*&& this.round == msg.getRound()*/) {
				Counter count = new Counter();
				
				//Increment or create the counter
				if(counter.keySet().contains(Pair.of(sender.id, msg.getRound()))) {
					count = counter.get(Pair.of(sender.id, msg.getRound()));
					count.incrementEchoCounter(v);
				} else {
					ArrayList<Integer> echoArray = new ArrayList<Integer>();
					echoArray.add(0);
					echoArray.add(0);
					echoArray.set(v, echoArray.get(v) + 1);
					count.setEchoCounter(echoArray);	
				}
				counter.put(Pair.of(sender.id, msg.getRound()), count);
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(Pair.of(sender.id, msg.getRound()), def);
			if (/*this.round == msg.getRound() &&*/ count.mostEchoValueCounter() > (n + t) / 2) {
				
				if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) == 1 || this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
					if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) == 1) {
						this.echo(sender, count.mostEchoValue(), msg.getRound());
					} else if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 2) == 2) {
						this.ready(sender, count.mostEchoValue(), msg.getRound());
					}
					
					int step = this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1);
					step++;
					this.steps.put(Pair.of(sender.id, msg.getRound()), step);
				}
			}
		}
		else if (msg.getType() == Message.MessageType.READY) {
			if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) > 0 /*&& this.round == msg.getRound()*/) {
				Counter count = new Counter();
				//Increment or create the counter
				if(counter.keySet().contains(Pair.of(sender.id, msg.getRound())) && v <= 1) {
					count = counter.getOrDefault(Pair.of(sender.id, msg.getRound()), count);
					count.incrementReadyCounter(v);
				} else {
					ArrayList<Integer> readyArray = new ArrayList<Integer>();
					readyArray.add(0);
					readyArray.add(0);
					readyArray.set(v, readyArray.get(v) + 1);
					count.setReadyCounter(readyArray);
				}
				counter.put(Pair.of(sender.id, msg.getRound()), count);
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(Pair.of(sender.id, msg.getRound()), def);
			if (/*this.round == msg.getRound() && */count.mostReadyValueCounter() > t + 1) {
				if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 1 || this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
					if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 1) {
						this.echo(sender, count.mostReadyValue(), msg.getRound());
					} else if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
						this.ready(sender, count.mostReadyValue(), msg.getRound());
					}
					int step = this.steps.get(Pair.of(sender.id, msg.getRound()));
					step++;
					this.steps.put(Pair.of(sender.id, msg.getRound()), step);
				}
			}
			if (count.mostReadyValueCounter() > 2 * t + 1 && this.steps.get(Pair.of(sender.id, msg.getRound())) == 3) {
				update_validate_set(msg);
				check_validate_set(msg.getRound());
			}
			
		}
		
	}
	
	public void check_validate_set(int msgRound) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int n = this.processes.size();
		int t = ((n / 3 - 1) > 0) ? n / 3 - 1 : 0;			// number of byzantine processes
		int set[] = new int[2];
		boolean msgLabel = true;
		
		if(this.validatedSet.containsKey(msgRound)) {
			ArrayList<Message> messages = this.validatedSet.get(msgRound);
			
			for (Message message : messages) {
				set[message.getV()]++;
				if(!message.getLabel())
					msgLabel = message.getLabel();
			}
			
			if(messages.size() == (n-t) && this.round == msgRound) {
				switch (this.round % 3) {
				case 0:
					if(set[0] >= set[1]) {
						this.value = 0;
					} else {
						this.value = 1;
					}
					break;
				case 1:
					for (int i = 0; i < set.length; i++)
						if(set[i] > (n/2)) {
							this.value = i;
							this.label = true;
							break;
						}
					break;
				case 2:
					boolean d = false;
					for (int i = 0; i < set.length && !d; i++) {
						if(set[i] > (2*t) && msgLabel) {
							this.decision = i;
							d = true;
						} else if (set[i] > t && msgLabel) {
							this.value = i;
							d = true;
						}
					}
					if (!d)
						this.value = RandomHelper.nextIntFromTo(0, 1);
					
					break;
				}
				
				if(this.decision == null)
					this.round++;
			}
		}
	}
	
	public void update_validate_set(Message msg) {
		//System.out.println(msg.getSender().id + " ACCEPTED " + msg.getV() + " with ROUND " + msg.getRound());
		int msgRound = msg.getRound();
		
		ArrayList<Message> messages;
		if (!this.validatedSet.containsKey(msgRound)) {
			messages = new ArrayList<Message>();
		} else {
			messages = this.validatedSet.get(msgRound);
		}
		
		if (!messages.contains(msg)) {
			messages.add(msg);
			this.validatedSet.put(msgRound, messages);
		}
		
		System.out.print("(round: " + msgRound + ", id: " + this.id + "[" + this.round + "]) : [");
		for(Message m : messages)
			System.out.print("(s:" + m.getSender().id + ", v:" + m.getV() + "), ");
		System.out.println("]");
	}
	
	@ScheduledMethod(start = 3, interval = 3)
	public void clearNetowrks() {
		// Repast utilities used to draw edges
		Context<Object> context = ContextUtils.getContext(this);
		Network<Object> initial_net = (Network<Object>) context.getProjection("initial_net");
		Network<Object> echo_net = (Network<Object>) context.getProjection("echo_net");
		Network<Object> ready_net = (Network<Object>) context.getProjection("ready_net");
		
		// Clear all printed edges
		initial_net.removeEdges();
		echo_net.removeEdges();
		ready_net.removeEdges();
	}
}
