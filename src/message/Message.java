package message;

import bconsensus.Process;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY, ACCEPT};
	protected MessageType type;
	protected Process sender;
	protected int v;
	protected int round;
	public Process to;
	
	public Message(Process sender, Process to, MessageType type, int v, int round) {
		this.sender = sender;
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

	public Process getSender() {
		return this.sender;
	}
}
