package message;

public class Message {
	public enum MessageType {INITIAL, ECHO, READY, ACCEPT};
	protected MessageType type;
	protected int v;
	protected int n_echo;
	protected int n_ready;
	
	public Message(MessageType type, int v) {
		this.type = type;
		this.v = v;
		this.n_echo = 0;
		this.n_ready = 0;
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
