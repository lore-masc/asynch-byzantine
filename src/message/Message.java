package message;

import bconsensus.Process;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY, ACCEPT};
	protected MessageType type;
	protected int v;
	protected int round;
	protected int n_echo;
	protected int n_ready;
	public Process to;
	
	public Message(Process to, MessageType type, int v, int round) {
		this.to = to;
		this.type = type;
		this.v = v;
		this.round = round;
		this.n_echo = 0;
		this.n_ready = 0;
	}
	
	public MessageType getType() {
		return this.type;
	}
	
	public int getV() {
		return this.v;
	}
	
	public int getRound() {
		return this.round;
	}
	
	public int getEcho() {
		return this.n_echo;
	}
	
	public int getReady() {
		return this.n_ready;
	}
	
	public int keepEcho() {
		return ++this.n_echo;
	}
	
	public int keepReady() {
		return ++this.n_ready;
	}

}
