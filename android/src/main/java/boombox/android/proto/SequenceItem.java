package boombox.android.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SequenceItem implements Externalizable<SequenceItem> {

	private short delay;
	private byte tube;

	public SequenceItem() {
	}

	public SequenceItem(short delay, byte tube) {
		this.delay = delay;
		this.tube = tube;
	}

	public short getDelay() {
		return delay;
	}

	public SequenceItem setDelay(short delay) {
		this.delay = delay;
		return this;
	}

	public byte getTube() {
		return tube;
	}

	public SequenceItem setTube(byte tube) {
		this.tube = tube;
		return this;
	}

	@Override
	public String toString() {
		return "SequenceItem{" +
				"delay=" + delay +
				", tube=" + tube +
				'}';
	}

	@Override
	public SequenceItem write(DataOutput buffer) throws IOException {
		buffer.writeShort(delay);
		buffer.writeShort(tube);
		return this;
	}

	@Override
	public SequenceItem read(DataInput buffer) throws IOException {
		delay = buffer.readShort();
		tube = (byte) buffer.readShort();
		return this;
	}
}
