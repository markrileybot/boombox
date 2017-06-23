package boombox.android.blespp;

import android.bluetooth.BluetoothGattCharacteristic;
import boombox.android.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mriley on 10/24/16.
 */
public final class GattInputStream extends InputStream {

	private static final Logger log = LoggerFactory.getLogger(GattInputStream.class);
	private static final byte[] SIG_CLOSE = {};

	private final AtomicInteger available = new AtomicInteger();
	private final LinkedBlockingQueue<byte[]> chunks = new LinkedBlockingQueue<>();
	private final Connection connection;
	private BluetoothGattCharacteristic characteristic;

	private boolean trace = log.isTraceEnabled();
	private long timeout;
	private long pollAttempts;
	private int position;
	private byte[] chunk;

	public GattInputStream(Connection connection) {
		this.connection = connection;
	}

	void addData(BluetoothGattCharacteristic characteristic) {
		try {
			// get the value
			byte[] value = characteristic.getValue();
			// mark as read
			characteristic.setValue((byte[]) null);
			if(value.length > 0) {
				int av = available.addAndGet(value.length);
				chunks.put(value);
				if(trace) {
					log.trace("Bytes available: {} ({})", av, value.length);
					log.trace("\t{}", Hex.bytesToHexString(value));
				}
			}
		} catch (InterruptedException ignored) {}
	}

	public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
		this.characteristic = characteristic;
	}

	public void setTimeout(long timeout) {
		this.timeout = 100;
		this.pollAttempts = Math.max(10, timeout / this.timeout);
	}

	@Override
	public int read() throws IOException {
		byte[] chunk = getChunk(true);
		int r = (chunk[position++] & 0xff);
		available.addAndGet(-1);
		return r;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = 0;
		while (read < len) {
			byte[] chunk = getChunk(read == 0);
			if(chunk == null) {
				break;
			}
			int numToCopy = Math.min(chunkRemaining(), len - read);
			System.arraycopy(chunk, position, b, off, numToCopy);
			position += numToCopy;
			read += numToCopy;
			off += numToCopy;
		}
		available.addAndGet(-read);
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		int skipped = 0;
		while (skipped < n) {
			byte[] chunk = getChunk(skipped == 0);
			if(chunk == null) {
				break;
			}
			int numToSkip = (int) Math.min(chunkRemaining(), n - skipped);
			position += numToSkip;
			skipped += numToSkip;
		}
		available.addAndGet(-skipped);
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return available.get();
	}

	@Override
	public void close() {
		try {
			chunks.put(SIG_CLOSE);
		} catch (InterruptedException ignored) {
		}
	}

	public void clear() {
		chunks.clear();
		chunk = null;
		position = 0;
		available.set(0);
	}

	@Override
	public void mark(int readlimit) {
		super.mark(readlimit);
	}

	@Override
	public void reset() throws IOException {
		super.reset();
	}

	@Override
	public boolean markSupported() {
		return super.markSupported();
	}

	private int chunkRemaining() {
		return chunk == null ? 0 : chunk.length - position;
	}

	private byte[] getChunk(boolean wait) {
		long remaining = pollAttempts;
		while (chunkRemaining() == 0) {
			chunk = null;
			position = 0;
			try {
				if(wait) {
					chunk = chunks.poll(timeout, TimeUnit.MILLISECONDS);
				} else {
					chunk = chunks.poll();
				}
			} catch (InterruptedException ignored) {}
			if (chunk == SIG_CLOSE) {
				chunk = null;
				Errors.closed();
			} else if(!wait) {
				break;
			} else if(chunk == null && --remaining == 0) {
				Errors.timeout();
			}
		}
		return chunk;
	}
}
