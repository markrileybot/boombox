package boombox.android.proto;

import boombox.android.proto.Message.Type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Launch extends Payload<Launch> {

	private List<SequenceItem> sequence;

	public Launch() {
		super(Type.LAUNCH);
	}

	public List<SequenceItem> getSequence() {
		return sequence;
	}

	public Launch setSequence(List<SequenceItem> sequence) {
		this.sequence = sequence;
		return this;
	}

	@Override
	public Launch write(DataOutput buffer) throws IOException {
		if (sequence == null) {
			buffer.writeShort(0);
		} else {
			buffer.writeShort(sequence.size());
			for (SequenceItem sequenceItem : sequence) {
				sequenceItem.write(buffer);
			}
		}
		return this;
	}

	@Override
	public Launch read(DataInput buffer) throws IOException {
		int size = buffer.readShort();
		if (sequence != null) sequence.clear();
		for (int i = 0; i < size; i++) {
			if (sequence == null) sequence = new ArrayList<>(size);
			sequence.add(new SequenceItem().read(buffer));
		}
		return this;
	}
}
