package boombox.android.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Message implements Externalizable<Message> {

	private static final byte MAGIC0 = (byte)11;
	private static final byte MAGIC1 = (byte)4;
	private static final byte MAGIC2 = (byte)0;
	private static final byte MAGIC3 = (byte)4;
	private static final byte[] FOOTER_MAGIC = {MAGIC0,MAGIC1,MAGIC2,MAGIC3};

	public enum Type {
		ERROR,
		LAUNCH,
		PING,
		RESET
	}

	private int sequence;
	private Payload payload;
	private Type type;

	public Message setSequence(int sequence) {
		this.sequence = sequence;
		return this;
	}

	public int getSequence() {
		return sequence;
	}

	public Message setPayload(Payload payload) {
		this.payload = payload;
		return this;
	}

	public Payload getPayload() {
		return payload;
	}

	public Message setType(Type type) {
		this.type = type;
		return this;
	}

	public Type getType() {
		return type;
	}

	@Override
	public Message write(DataOutput buffer) throws IOException {
		if (payload != null) type = payload.getType();
		buffer.writeInt(sequence);
		buffer.write(type.ordinal());
		if (payload != null) {
			payload.write(buffer);
		}
		buffer.write(FOOTER_MAGIC);
		return this;
	}

	@Override
	public Message read(DataInput buffer) throws IOException {
		sequence = buffer.readInt();
		type = Type.values()[buffer.readByte()];
		switch (type) {
			case ERROR:
				break;
			case LAUNCH:
				break;
			case PING:
				break;
			case RESET:
				break;
		}
		return this;
	}
}
