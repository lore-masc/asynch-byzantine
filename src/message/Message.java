package message;

import bconsensus.Process;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY, ACCEPT};
	protected MessageType type;
	protected int v;
	protected int n_echo;
	protected int n_ready;
	protected boolean accepted;
	public Process to;
	
	public Message(Process to, MessageType type, int v) {
		this.to = to;
		this.type = type;
		this.v = v;
		this.accepted = false;
		this.n_echo = 0;
		this.n_ready = 0;
	}
	
	public MessageType getType() {
		return this.type;
	}
	
	public int getV() {
		return this.v;
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
	
	public void accept() {
		this.accepted = true;
	}
	
	public boolean getAcceptState() {
		return this.accepted;
	}

}
