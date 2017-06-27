package boombox.android.proto;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Externalizable<T extends Externalizable> {

	T write(DataOutput buffer) throws IOException;

	T read(DataInput buffer) throws IOException;
}
