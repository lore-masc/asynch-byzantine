package message;

import bconsensus.Process;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY, ACCEPT};
	protected MessageType type;
	protected int v;
	protected int round;
	public Process to;
	
	public Message(Process to, MessageType type, int v, int round) {
		this.to = to;
		this.type = type;
		this.v = v;
		this.round = round;
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

}
