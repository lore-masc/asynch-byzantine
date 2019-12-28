package counter;

import java.util.ArrayList;

public class Counter {
	private ArrayList<Integer> echoCounter;
	private ArrayList<Integer> readyCounter;
	
	public Counter() {
		this.echoCounter = new ArrayList<Integer>();
		this.echoCounter.add(0);
		this.echoCounter.add(0);
		this.readyCounter = new ArrayList<Integer>();
		this.readyCounter.add(0);
		this.readyCounter.add(0);
	}
	
	public ArrayList<Integer> getEchoCounter() {
		return this.echoCounter;
	}
	
	public ArrayList<Integer> getReadyCounter() {
		return this.readyCounter;
	}
	
	public void setEchoCounter(ArrayList<Integer> echoArray) {
		this.echoCounter = echoArray;
	}
	
	public void setReadyCounter(ArrayList<Integer> readyArray) {
		this.readyCounter = readyArray;
	}
	
	public void incrementEchoCounter(int echo) {
		this.echoCounter.set(echo, this.echoCounter.get(echo) + 1);
	}
	
	public void incrementReadyCounter(int ready) {
		this.readyCounter.set(ready, this.readyCounter.get(ready) + 1);
	}
	
	public int mostEchoValueCounter() {
		if(this.echoCounter.get(0) > this.echoCounter.get(1)) {
			return this.echoCounter.get(0);
		}
		return this.echoCounter.get(1);
	}
	
	public int mostReadyValueCounter() {
		if(this.readyCounter.get(0) > this.readyCounter.get(1)) {
			return this.readyCounter.get(0);
		}
		return this.readyCounter.get(1);
	}
	
	public int mostEchoValue() {
		if(this.echoCounter.get(0) > this.echoCounter.get(1)) {
			return 0;
		}
		return 1;
	}
	
	public int mostReadyValue() {
		if(this.readyCounter.get(0) > this.readyCounter.get(1)) {
			return 0;
		}
		return 1;
	}
	
	public int getEcho0() {
		return this.echoCounter.get(0);
	}
	
	public int getEcho1() {
		return this.echoCounter.get(1);
	}
	
	public int getReady0() {
		return this.readyCounter.get(0);
	}
	
	public int getReady1() {
		return this.readyCounter.get(1);
	}
}
