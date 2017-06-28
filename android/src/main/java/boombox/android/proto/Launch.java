package boombox.android.proto;

import boombox.android.proto.Message.Type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Launch extends Payload<Launch> {

	private final Map<Byte, SequenceItem> sequenceMap = new LinkedHashMap<>(1);
	private final List<SequenceItem> sequence = new ArrayList<>(1);

	public Launch() {
		super(Type.LAUNCH);
	}

	public boolean isEmpty() {
		return sequence.isEmpty();
	}

	public SequenceItem get(int pos) {
		return sequence.get(pos);
	}

	public SequenceItem get(byte tube) {
		return sequenceMap.get(tube);
	}

	public int size() {
		return sequence.size();
	}

	public Iterable<SequenceItem> getSequence() {
		return sequence;
	}

	public Launch addLaunchTube(LaunchTube launchTube) {
		if (!sequenceMap.containsKey(launchTube.getPosition())) {
			SequenceItem sequenceItem = new SequenceItem((short) 0, launchTube.getPosition());
			sequenceMap.put(launchTube.getPosition(), sequenceItem);
			sequence.add(sequenceItem);
		}
		return this;
	}

	public Launch removeLaunchTube(LaunchTube launchTube) {
		SequenceItem item;
		if ((item = sequenceMap.remove(launchTube.getPosition())) != null) {
			sequence.remove(item);
		}
		return this;
	}

	public long getIntervalBefore(SequenceItem item) {
		long ret = 0;
		for (SequenceItem sequenceItem : sequence) {
			if (sequenceItem.getTube() == item.getTube()) {
				break;
			}
			ret += sequenceItem.getInterval();
		}
		return ret;
	}

	@Override
	public String toString() {
		return "Launch{" +
				"sequence=" + sequence +
				'}';
	}

	@Override
	public Launch write(DataOutput buffer) throws IOException {
		buffer.writeShort(sequence.size());
		for (SequenceItem sequenceItem : sequence) {
			sequenceItem.write(buffer);
		}
		return this;
	}

	@Override
	public Launch read(DataInput buffer) throws IOException {
		int size = buffer.readShort();
		sequence.clear();
		sequenceMap.clear();
		for (int i = 0; i < size; i++) {
			SequenceItem item = new SequenceItem().read(buffer);
			sequenceMap.put(item.getTube(), item);
			sequence.add(item);
		}
		return this;
	}
}
