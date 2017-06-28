package boombox.android.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SequenceItem implements Externalizable<SequenceItem> {

	private short interval;
	private byte tube;

	public SequenceItem() {
	}

	public SequenceItem(short interval, byte tube) {
		this.interval = interval;
		this.tube = tube;
	}

	public short getInterval() {
		return interval;
	}

	public SequenceItem setInterval(short interval) {
		this.interval = interval;
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
				"interval=" + interval +
				", tube=" + tube +
				'}';
	}

	@Override
	public SequenceItem write(DataOutput buffer) throws IOException {
		buffer.writeShort(interval);
		buffer.writeShort(tube);
		return this;
	}

	@Override
	public SequenceItem read(DataInput buffer) throws IOException {
		interval = buffer.readShort();
		tube = (byte) buffer.readShort();
		return this;
	}
}
