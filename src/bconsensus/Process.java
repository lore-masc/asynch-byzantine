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
	private Message last_sent;
	private Integer decision;
	private HashMap< Integer, ArrayList<Message> > validatedSet; 
	private HashMap<Pair<Integer, Integer>, Counter> counter; 			// get the echo/ready counter given a sender process id
	private HashMap<Pair<Integer, Integer>, Integer> steps;
	private HashMap<Integer, Integer> rounds;
	Queue<Message> in_messages;
	Queue<Message> out_messages;
	ArrayList<Process> processes;
	
	public Process(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.decision = null;
		this.validatedSet = new HashMap< Integer, ArrayList<Message> >();
		this.in_messages = new LinkedList<Message>();
		this.out_messages = new LinkedList<Message>();
		this.processes = new ArrayList<Process>();
		this.counter = new HashMap<Pair<Integer, Integer>, Counter>();
		this.steps = new HashMap<Pair<Integer,Integer>, Integer>();
		this.rounds = new HashMap<Integer, Integer>();
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
		
		if (!this.rounds.containsKey(this.id)) {
			rounds.put(this.id, 0);
		}
		
		if (this.id == 0 || this.id == 5) {
			if (this.rounds.get(this.id) % 3 == 0) {					// Phase 1
				if (!this.steps.containsKey(Pair.of(this.id, this.rounds.get(this.id)))) {
					steps.put(Pair.of(this.id, this.rounds.get(this.id)), 0);
				
					//steps.put(Pair.of(this.id, this.round + 1), 0);
					//---------
					
					int proposed_v = RandomHelper.nextIntFromTo(0, 1);
					this.broadcast(this.rounds.get(this.id), proposed_v);
					//this.broadcast(this.round + 1, proposed_v);
					System.out.println(this.id + " broadcasts " + proposed_v);
				}
				
			} else if (this.rounds.get(this.id) % 3 == 1) {			// Phase 2
				
			} else if (this.rounds.get(this.id) % 3 == 2) {			// Phase 3
				
			}
		}
		
		if (this.last_sent != null) {			
			for(Pair<Integer, Integer> p : this.counter.keySet()) {
				Counter count = new Counter();
				count = counter.getOrDefault(p, count);
				synchronized (count) {
					System.out.println("PAIR: (" + p.getLeft() + " ; " + p.getRight() + ") - Echo 0: " + count.getEcho0() + " - Echo 1: " + count.getEcho1() + " Ready 0: " + count.getReady0() + " Ready 1: " + count.getReady1());
				}
			}
		}
	}
	
	private void broadcast(int r, int v) {
		this.initial(this, v, r);
		this.steps.put(Pair.of(this.id, this.rounds.get(this.id)), 1);
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
		int t = ((n / 3 - 1) > 0) ? n / 3 - 1 : 0;			// number of byzantine processes
		Message msg = this.in_messages.remove();
		int v = msg.getV();
		boolean done = false;
		Process sender = msg.getSender();

		if(!this.rounds.containsKey(sender.id)) {
			this.rounds.put(sender.id, msg.getRound());
		}
		if(!this.steps.containsKey(Pair.of(sender.id, this.rounds.get(sender.id)))) {
			this.steps.put(Pair.of(sender.id, this.rounds.get(sender.id)), 0);
		}
		
		
		if (msg.getType() == Message.MessageType.INITIAL && this.rounds.get(sender.id) == msg.getRound() && this.steps.get(Pair.of(sender.id, this.rounds.get(sender.id))) <= 1) {
			//if (this.step == 0)
			//	this.initial(sender, v, this.round);
			this.echo(sender, v, rounds.get(sender.id));
			this.steps.put(Pair.of(sender.id, rounds.get(sender.id)), 2);
			done = true;
		}
		else if (msg.getType() == Message.MessageType.ECHO) {
			if (this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1) > 0 && rounds.get(sender.id) == msg.getRound()) {
				Counter count = new Counter();
				if(counter.keySet().contains(Pair.of(sender.id, rounds.get(sender.id)))) {
					count = counter.get(Pair.of(sender.id, rounds.get(sender.id)));
					count.incrementEchoCounter(v);
				} else {
					ArrayList<Integer> echoArray = new ArrayList<Integer>();
					echoArray.add(0);
					echoArray.add(0);
					echoArray.set(v, echoArray.get(v) + 1);
					count.setEchoCounter(echoArray);	
				}
				counter.put(Pair.of(sender.id, rounds.get(sender.id)), count);
				done = true;
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), def);
			if (this.rounds.get(sender.id) == msg.getRound() && count.mostEchoValueCounter() > (n + t) / 2) {
				if (this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1) == 1 || this.steps.get(Pair.of(sender.id, this.rounds.get(sender.id))) == 2) {
					if (this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1) == 1) {
						this.echo(sender, count.mostEchoValue(), rounds.get(sender.id));
					} else if (this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1) == 2) {
						this.ready(sender, count.mostEchoValue(), rounds.get(sender.id));
					}
					
					int step = this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1);
					step++;
					this.steps.put(Pair.of(sender.id, rounds.get(sender.id)), step);
					done = true;
				}
			}
		}
		else if (msg.getType() == Message.MessageType.READY) {
			if (this.steps.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), 1) > 0 && rounds.get(sender.id) == msg.getRound()) {
				Counter count = new Counter();
				if(counter.keySet().contains(Pair.of(sender.id, rounds.get(sender.id))) && v <= 1) {
					count = counter.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), count);
					count.incrementReadyCounter(v);
				} else {
					ArrayList<Integer> readyArray = new ArrayList<Integer>();
					readyArray.add(0);
					readyArray.add(0);
					readyArray.set(v, readyArray.get(v) + 1);
					count.setReadyCounter(readyArray);
				}
				counter.put(Pair.of(sender.id, rounds.get(sender.id)), count);
				done = true;
			}
			Counter def = new Counter();
			Counter count = counter.getOrDefault(Pair.of(sender.id, rounds.get(sender.id)), def);
			if (this.last_sent != null && rounds.get(sender.id) == msg.getRound() && count.mostReadyValueCounter() > t + 1) {
				if (this.steps.get(Pair.of(sender.id, rounds.get(sender.id))) == 1 || this.steps.get(Pair.of(sender.id, rounds.get(sender.id))) == 2) {
					if (this.steps.get(Pair.of(sender.id, rounds.get(sender.id))) == 1) {
						this.echo(sender, count.mostReadyValue(), rounds.get(sender.id));
					} else if (this.steps.get(Pair.of(sender.id, rounds.get(sender.id))) == 2) {
						this.ready(sender, count.mostReadyValue(), rounds.get(sender.id));
					}
					int step = this.steps.get(Pair.of(sender.id, rounds.get(sender.id)));
					step++;
					this.steps.put(Pair.of(sender.id, rounds.get(sender.id)), step);
					done = true;
				}
			}
			if (this.rounds.get(sender.id) == msg.getRound() && count.mostReadyValueCounter() > 2 * t + 1 && this.steps.get(Pair.of(sender.id, rounds.get(sender.id))) == 3) {
				accept(sender, count.mostReadyValue(), rounds.get(sender.id));
				update_validate_set(msg);
				int round = this.rounds.get(sender.id);
				round++;
				this.rounds.put(sender.id, round);
				this.steps.put(Pair.of(sender.id, rounds.get(sender.id)), 0);		// restart proposing a new value
				done = true;
			}
		}
		else if (msg.getType() == Message.MessageType.ACCEPT) {
			update_validate_set(msg);
			done = true;
		}
		
	}
	
	
	public void update_validate_set(Message msg) {
		System.out.println(msg.getSender().id + " ACCEPTED " + msg.getV());
		
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
}
