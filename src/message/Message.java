package message;

import bconsensus.Process;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY};
	protected MessageType type;
	protected Process sender;
	protected boolean label;
	protected int v;
	protected int round;
	public Process to;
	
	public Message(Process sender, Process to, MessageType type, int v, boolean label, int round) {
		this.sender = sender;
		this.to = to;
		this.type = type;
		this.label = label;
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
	
	public boolean getLabel() {
		return this.label;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean t = false;
		if (o instanceof Message) {
			Message msgo = (Message) o;
			if(this.type.equals(msgo.type) && this.sender.getID() == msgo.sender.getID() && this.v == msgo.v && this.round == msgo.round &&this.label == msgo.label) {
				t = true;
			}
		}
		return t;
	}
}
