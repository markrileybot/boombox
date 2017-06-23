package boombox.android.blespp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import boombox.android.blespp.Connection.State;
import boombox.android.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by mriley on 10/24/16.
 */
public final class GattOutputStream extends OutputStream {

	private static final Logger log = LoggerFactory.getLogger(GattOutputStream.class);

	private final Connection connection;
	private int mtu;

	GattOutputStream(Connection connection) {
		this.connection = connection;
	}

	public void setMtu(int mtu) {
		this.mtu = mtu;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[]{(byte) b});
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len > mtu) {
			byte[] tmpBuf = new byte[mtu];
			while (len > 0) {
				int l = Math.min(len, mtu);
				System.arraycopy(b, off, tmpBuf, 0, l);
				writeInternal(tmpBuf, 0, l);
				off += l;
				len -= l;
			}
		} else {
			writeInternal(b, off, len);
		}
	}

	private void writeInternal(byte[] b, int off, int len) {
		byte[] newVal = b;
		if (off != 0 || b.length != len) {
			newVal = new byte[len];
			System.arraycopy(b, off, newVal, 0, len);
		}

		BluetoothGatt gatt = connection.getBluetoothGatt();

		connection.awaitStateAndMark(State.OPEN, State.BUSY);
		BluetoothGattCharacteristic buffer = connection.getBuffer();
		buffer.setValue(newVal);
		log.trace("Bytes sent: {}", len);
		log.trace("\t{}", Hex.bytesToHexString(newVal, off, len));
		while (!gatt.writeCharacteristic(buffer)) {
			connection.awaitStateAndMark(State.OPEN, State.BUSY);
		}
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() {
	}
}
