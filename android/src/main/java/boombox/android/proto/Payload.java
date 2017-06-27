package boombox.android.proto;

import boombox.android.proto.Message.Type;

public abstract class Payload<T extends Payload> implements Externalizable<T> {

	private Type type;

	protected Payload(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	void setType(Type type) {
		this.type = type;
	}
}
