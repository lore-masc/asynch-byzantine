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
	protected ContinuousSpace<Object> space;
	protected Grid<Object> grid;
	protected int id;
	protected Integer decision;
	protected int round;
	protected boolean label;
	protected Integer value;
	protected HashMap< Integer, ArrayList<Message> > validatedSet; 
	protected HashMap<Pair<Integer, Integer>, Counter> counter; 			// get the echo/ready counter given a sender process id
	protected HashMap<Pair<Integer, Integer>, Integer> steps;
	protected HashMap<Integer, Integer> winner;								// given the round (key) it returns the winner's value
	protected Queue<Message> in_messages;
	protected Queue<Message> out_messages;
	protected ArrayList<Process> processes;
	protected ArrayList<Message> prematured_messages;
	protected Integer proposed_v;
	
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
		this.prematured_messages = new ArrayList<Message>();
		this.counter = new HashMap<Pair<Integer, Integer>, Counter>();
		this.steps = new HashMap<Pair<Integer,Integer>, Integer>();
		this.winner = new HashMap<Integer, Integer>();
		this.proposed_v = null;
	}
	
	public int getID() {
		return this.id;
	}
	
	public double getPhase() {
		return this.round/3;
	}
	
	public int getRound() {
		return ((this.round%3) + 1);
	}
	
	public Integer getValue() {
		if (this.proposed_v == null)
			return -1;
		return this.proposed_v;
	}
	
	public Integer getDecision() {
		if (this.decision== null)
			return -1;
		return this.decision;
	}
	
	public Boolean getLabel() {
		 return this.label;
	}
		 
	public Object getProcess() {
		 return this.getClass();
	}
	
	protected void initial(Process sender, int v, boolean label, int k) {
		// send to all
		for (Process to : this.processes) 
			this.out_messages.add(new Message(sender, to, Message.MessageType.INITIAL, v, label, k));
	}
	
	protected void echo(Process sender, int v, boolean label, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.ECHO, v, label, k));		
	}
	
	protected void ready(Process sender, int v, boolean label, int k) {
		// send to all
		for (Process to : this.processes)
			this.out_messages.add(new Message(sender, to, Message.MessageType.READY, v, label, k));		
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void node() {	
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
		
		this.consensus();
		
		if(this.decision != null)
			System.out.println(this.id + " DECIDES " + this.decision);
	}
	
	protected void consensus() {
		if (this.round % 3 == 0) {					// Round 1
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				if (!(this instanceof FailAndStop)) {
					this.steps.put(Pair.of(this.id, this.round), 0);
					this.label = false;
					
					if (this.getPhase() == 0)
						this.proposed_v = RandomHelper.nextIntFromTo(0, 1);
					else
						this.proposed_v = this.value;
					this.broadcast(this.round, this.proposed_v);
					
					System.out.println(this.id + " broadcasts " + this.proposed_v);
				}
			}
		} else if (this.round % 3 == 1) {			// Round 2
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);

				System.out.println("PHASE 2: " + this.id);
				System.out.println(this.id + " broadcasts " + this.value);
				this.proposed_v = this.value;
				this.broadcast(this.round, this.value);
			}
		} else if (this.round % 3 == 2) {			// Round 3
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				
				System.out.println("PHASE 3: " + this.id);
				System.out.println(this.id + " broadcasts " + this.value + " with label = " + this.label);
				
				this.proposed_v = this.value;
				this.broadcast(this.round, this.value);
			}
		}
	}
	
	protected void broadcast(int r, int v) {
		this.initial(this, v, this.label, r);
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
		int t = ((n / 3 - 1) > 0) ? n / 3 - 1 : 0;			// number of faulty processes
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		Process sender = msg.getSender();
		
		// Postpone the elaboration of message, if it anticipates the current round for process p
		if (msg.getRound() > this.round) {
			this.prematured_messages.add(msg);
			return;
		}
		
		if(!this.steps.containsKey(Pair.of(sender.id, msg.getRound()))) {
			this.steps.put(Pair.of(sender.id, msg.getRound()), 0);
		}
		
		if (msg.getType() == Message.MessageType.INITIAL && this.steps.get(Pair.of(sender.id, msg.getRound())) <= 1) {
			this.echo(sender, v, msg.getLabel(), msg.getRound());
			this.steps.put(Pair.of(sender.id, msg.getRound()), 2);
		}
		else if (msg.getType() == Message.MessageType.ECHO) {
			if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) > 0) {
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
			if (count.mostEchoValueCounter() > (n + t) / 2) {
				
				if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) == 1 || this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
					if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) == 1) {
						this.echo(sender, count.mostEchoValue(), msg.getLabel(), msg.getRound());
					} else if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 2) == 2) {
						this.ready(sender, count.mostEchoValue(), msg.getLabel(), msg.getRound());
					}
					
					int step = this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1);
					step++;
					this.steps.put(Pair.of(sender.id, msg.getRound()), step);
				}
			}
		}
		else if (msg.getType() == Message.MessageType.READY) {
			if (this.steps.getOrDefault(Pair.of(sender.id, msg.getRound()), 1) > 0) {
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
			if (count.mostReadyValueCounter() > t + 1) {
				if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 1 || this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
					if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 1) {
						this.echo(sender, count.mostReadyValue(), msg.getLabel(), msg.getRound());
					} else if (this.steps.get(Pair.of(sender.id, msg.getRound())) == 2) {
						this.ready(sender, count.mostReadyValue(), msg.getLabel(), msg.getRound());
					}
					int step = this.steps.get(Pair.of(sender.id, msg.getRound()));
					step++;
					this.steps.put(Pair.of(sender.id, msg.getRound()), step);
				}
			}

			if (msg.getV() == count.mostReadyValue() && count.mostReadyValueCounter() > 2 * t + 1 && this.steps.get(Pair.of(sender.id, msg.getRound())) == 3) {
				update_validate_set(msg);
				check_validate_set(msg.getRound());
			}
			
		}
		
	}
	
	public void check_validate_set(int msgRound) {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int n = this.processes.size();
		int t = (((n / 3) - 1) > 0) ? (n / 3) - 1 : 0;			// number of faulty processes
		int set[] = new int[2];
		int msgLabel = 0;
		
		if(this.validatedSet.containsKey(msgRound)) {
			ArrayList<Message> messages = this.validatedSet.get(msgRound);
			ArrayList<Message> majority_messages = new ArrayList<>();
			
			// Count the frequencies
			for (Message message : messages) 
				set[message.getV()]++;
						
			// Collect the majority messages
			int majority_value;
			if(set[0] >= set[1]) {
				majority_value = 0;
			} else {
				majority_value = 1;
			}
			
			for (Message message : messages)
				if (message.getV() == majority_value)
					majority_messages.add(message);
						
			// Count how many of them are labeled
			for (Message message : majority_messages) 
				if(message.getLabel())
					msgLabel++;
			
			// Check validate set in order to decide the next step
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
						if(set[i] > (2*t) && msgLabel > (2*t)) {
							this.decision = i;
							d = true;
						} else if (set[i] > t && msgLabel > t) {
							this.value = i;
							d = true;
						}
					}
					if (!d)
						this.value = RandomHelper.nextIntFromTo(0, 1);
					
					break;
				}
				
				if(this.decision == null) {
					this.winner.put(this.round, this.value);
					this.round++;
					this.check_prematured_messages();
				}
			}
		}
	}
	
	public void update_validate_set(Message msg) {
		int msgRound = msg.getRound();
		
		ArrayList<Message> messages;
		if (!this.validatedSet.containsKey(msgRound)) {
			messages = new ArrayList<Message>();
		} else {
			messages = this.validatedSet.get(msgRound);
		}
		
		if (!messages.contains(msg)) {			
			if (msgRound == 0 || (msgRound > 0 && msg.getV() == this.winner.get(msgRound - 1)) ) {
				messages.add(msg);
				this.validatedSet.put(msgRound, messages);
			}
		}
		
		System.out.print("(round: " + msgRound + ", id: " + this.id + "[" + this.round + "]) : [");
		for(Message m : messages)
			System.out.print("(s:" + m.getSender().id + ", v:" + m.getV() + ((m.getLabel()) ? "*" : "") + "), ");
		System.out.println("]");
	}
	
	public void check_prematured_messages() {
		while (!this.prematured_messages.isEmpty()) {
			Message msg = this.prematured_messages.remove(0);
			if (msg.getRound() == this.round)
				this.in_messages.add(msg);
			else
				this.prematured_messages.add(msg);
		}
	}
	
	@ScheduledMethod(start = 1, interval = 3)
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
