package boombox.android.blespp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class Connection implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(Connection.class);

	public enum State {
		CLOSING,
		CLOSED,
		OPENING,
		BONDING,
		DISCOVERING,
		DISCOVERED,
		CONFIG_DESC,
		CONFIG_MTU,
		OPEN, // open/idle
		BUSY;

		public boolean isOpening() {
			switch (this) {
				case OPENING:
				case BONDING:
				case DISCOVERING:
				case DISCOVERED:
				case CONFIG_DESC:
				case CONFIG_MTU:
					return true;
			}
			return false;
		}
	}

	public interface Callback {
		void onStateChanged(Connection connection, State from, State to);
	}

	public static final class Builder {
		private Context context;
		private BluetoothGattCallback gattCallback;
		private Connection.Callback callback;
		private BluetoothDevice device;
		private int mtu = MTU_MAX;
		private long timeout = TimeUnit.SECONDS.toMillis(10);
		private long connectTimeout = timeout;

		public Builder setContext(Context context) {
			this.context = context;
			return this;
		}

		public Builder setGattCallback(BluetoothGattCallback gattCallback) {
			this.gattCallback = gattCallback;
			return this;
		}

		public Builder setCallback(Callback callback) {
			this.callback = callback;
			return this;
		}

		public Builder setDevice(BluetoothDevice device) {
			this.device = device;
			return this;
		}

		public Builder setTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder setMtu(int mtu) {
			this.mtu = mtu;
			return this;
		}

		public Builder setConnectTimeout(long connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		public Connection build() {
			return new Connection(this);
		}
	}

	/**
	 * Maximum BLE MTU
	 */
	public static final int MTU_MAX = 517;
	/**
	 * Default BLE MTU
	 */
	public static final int MTU_DEFAULT = 20;

	/**
	 * SIG UUID of standard Client Characteristic Configuration Descriptor
	 */
	public static final UUID DESC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	/**
	 * SPP over BLE Service UUID (128-bit)
	 */
	public static final UUID SERVICE_UUID = UUID.fromString("e079c6a0-aa8b-11e3-a903-0002a5d5c51b");

	/**
	 * SPPoverBLE Buffer Characteristic UUID (128-bit)
	 */
	public static final UUID BUFFER_CHAR_UUID = UUID.fromString("b38312c0-aa89-11e3-9cef-0002a5d5c51b");

	private final BondStateCallback bondStateCallback = new BondStateCallback();
	private final ConnectionStateCallback connectionStateCallback = new ConnectionStateCallback();
	private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
	private final GattInputStream inputStream = new GattInputStream(this);
	private final GattOutputStream outputStream = new GattOutputStream(this);

	private final Context context;
	private final BluetoothDevice device;
	private final Callback callback;
	private final BluetoothGattCallback gattCallback;
	private final int mtu;
	private final long timeout;
	private final long connectTimeout;
	private final boolean trace;

	private BluetoothGatt bluetoothGatt;
	private BluetoothGattService service;
	private BluetoothGattCharacteristic bufferCharacteristic;

	Connection(Builder builder) {
		this.context = builder.context;
		this.device = builder.device;
		this.callback = builder.callback;
		this.gattCallback = builder.gattCallback;
		this.timeout = builder.timeout;
		this.connectTimeout = builder.connectTimeout;
		this.mtu = builder.mtu;
		this.trace = log.isTraceEnabled();

		inputStream.setTimeout(timeout);
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public Connection open() {
		if(setState(State.CLOSED, State.OPENING)) {
			context.registerReceiver(bondStateCallback, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
			log.info("Open connection {}, type={}, bonded={}", device, device.getType(), device.getBondState());
			bluetoothGatt = device.connectGatt(context, false, connectionStateCallback);
		}
		return this;
	}

	void awaitState(State checkState) {
		long to = timeout;
		if (state.get().isOpening()) {
			to += connectTimeout;
		}
		State s;
		while((s = state.get()) != checkState && s != State.CLOSED && s != State.CLOSING) {
			long start = System.currentTimeMillis();
			synchronized (state) {
				try {
					state.wait(to);
				} catch (InterruptedException ignored) {}
			}
			if((to -= (System.currentTimeMillis() - start)) <= 0) {
				Errors.timeout();
			}
		}
		if(s == State.CLOSED) {
			Errors.closed();
		}
	}

	void awaitStateAndMark(State checkState, State newState) {
		long to = timeout;
		if (state.get().isOpening()) {
			to += connectTimeout;
		}
		long waitSeg = Math.min(10, to);
		State s;
		while (!setState(checkState, newState)) {
			if ((s = state.get()) == State.CLOSED || s == State.CLOSING) {
				Errors.closed();
			}
			long start = System.currentTimeMillis();
			synchronized (state) {
				try {
					state.wait(waitSeg);
				} catch (InterruptedException ignored) {}
			}
			if ((to -= (System.currentTimeMillis() - start)) <= 0) {
				Errors.timeout();
			}
		}
	}

	BluetoothGatt getBluetoothGatt() {
		return bluetoothGatt;
	}

	BluetoothGattCharacteristic getBuffer() {
		return bufferCharacteristic;
	}

	public State getState() {
		return state.get();
	}

	public GattOutputStream getOutputStream() {
		return outputStream;
	}

	public GattInputStream getInputStream() {
		return inputStream;
	}

	@Override
	public void close() {
		if(state.get() != State.CLOSED) {
			setState(State.CLOSING);
			try {
				context.unregisterReceiver(bondStateCallback);
			} catch (Exception ignored) {
			}
			if (bluetoothGatt != null) {
				bluetoothGatt.disconnect();
				bluetoothGatt.close();
			}
			outputStream.close();
			inputStream.close();
			setState(State.CLOSED);
			log.debug("Closed");
		}
	}

	private void setState(State s) {
		setState(null, s);
	}

	private boolean setState(State old, State s) {
		if(old == null) {
			old = state.get();
		}
		if(old != s && state.compareAndSet(old, s)) {
			if(trace) log.trace("{} -> {}", old, s);
			callback.onStateChanged(this, old, s);

			if(s == State.OPEN || s == State.CLOSED) {
				synchronized (state) {
					state.notifyAll();
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Connection{" +
				"state=" + state +
				", device=" + device +
				'}';
	}

	private final class BondStateCallback extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(d != null && d.getAddress().equals(device.getAddress())) {
					if(trace) log.trace("New bond state {}", newState);
					if(newState == BluetoothDevice.BOND_BONDED) {
						if (setState(State.BONDING, State.DISCOVERING)) {
							bluetoothGatt.discoverServices();
						}
					}
				}
			}
		}
	}

	private final class ConnectionStateCallback extends BluetoothGattCallback {
		/**
		 * Callback indicating when GATT client has connected/disconnected to/from a remote
		 * GATT server.
		 *
		 * @param gatt GATT client
		 * @param status Status of the connect or disconnect operation.
		 *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
		 * @param newState Returns the new connection state. Can be one of
		 *                  {@link BluetoothProfile#STATE_DISCONNECTED} or
		 *                  {@link BluetoothProfile#STATE_CONNECTED}
		 */
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if(trace) log.trace("onConnectionStateChange({}, {})", GattStatus.getStatus(status), newState);
			if(newState == BluetoothProfile.STATE_CONNECTED) {
				BluetoothDevice device = gatt.getDevice();
				int bondState = device.getBondState();
				if(bondState == BluetoothDevice.BOND_BONDED) {
					if (setState(State.OPENING, State.DISCOVERING)) {
						gatt.discoverServices();
					}
				} else if(bondState == BluetoothDevice.BOND_BONDING) {
					setState(State.OPENING, State.BONDING);
				} else {
					if (setState(State.OPENING, State.BONDING)) {
						try {
							Method createBond = device.getClass().getMethod("createBond", Integer.class);
							createBond.invoke(device, BluetoothDevice.TRANSPORT_LE);
						} catch (Exception e) {
							log.error("Failed to create LE bond to {}", device);
							log.error("Error", e);
							device.createBond();
						}
					}
				}
			} else {
				close();
			}
			if(gattCallback != null) gattCallback.onConnectionStateChange(gatt, status, newState);
		}

		/**
		 * Callback invoked when the list of remote services, characteristics and descriptors
		 * for the remote device have been updated, ie new services have been discovered.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#discoverServices}
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
		 *               has been explored successfully.
		 */
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if(trace) log.trace("onServicesDiscovered({})", GattStatus.getStatus(status));
			service = gatt.getService(SERVICE_UUID);
			if(service == null) {
				log.warn("Failed to discover service {}", SERVICE_UUID);
				close();
				return;
			} else if(!setState(State.DISCOVERING, State.DISCOVERED)) {
				log.warn("Failed to set state to {}", State.DISCOVERED);
			} else {
				bufferCharacteristic = service.getCharacteristic(BUFFER_CHAR_UUID);
				inputStream.setCharacteristic(bufferCharacteristic);
				if(trace) log.trace("Buffer permissions: {}", bufferCharacteristic.getPermissions());
				if(trace) log.trace("Buffer properties: {}", bufferCharacteristic.getProperties());
				if (!bluetoothGatt.setCharacteristicNotification(bufferCharacteristic, true)) {
					close();
					return;
				}

				BluetoothGattDescriptor descriptor = bufferCharacteristic.getDescriptor(DESC_UUID);
				if(trace) log.trace("Descriptor permissions: {}", descriptor.getPermissions());

				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				if(setState(State.DISCOVERED, State.CONFIG_DESC)) {
					if (!gatt.writeDescriptor(descriptor)) {
						close();
						return;
					}
				}
				inputStream.clear();
			}
			if(gattCallback != null) gattCallback.onServicesDiscovered(gatt, status);
		}

		/**
		 * Callback reporting the result of a characteristic read operation.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#readCharacteristic}
		 * @param characteristic Characteristic that was read from the associated
		 *                       remote device.
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
		 *               was completed successfully.
		 */
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if(trace) log.trace("onCharacteristicRead({}, {})", characteristic.getUuid(), GattStatus.getStatus(status));
			inputStream.addData(characteristic);
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onCharacteristicRead(gatt, characteristic, status);
		}

		/**
		 * Callback indicating the result of a characteristic write operation.
		 *
		 * <p>If this callback is invoked while a reliable write transaction is
		 * in progress, the value of the characteristic represents the value
		 * reported by the remote device. An application should compare this
		 * value to the desired value to be written. If the values don't match,
		 * the application must abort the reliable write transaction.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#writeCharacteristic}
		 * @param characteristic Characteristic that was written to the associated
		 *                       remote device.
		 * @param status The result of the write operation
		 *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
		 */
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if(trace) log.trace("onCharacteristicWrite({}, {})", characteristic.getUuid(), GattStatus.getStatus(status));
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onCharacteristicWrite(gatt, characteristic, status);
		}

		/**
		 * Callback triggered as a result of a remote characteristic notification.
		 *
		 * @param gatt GATT client the characteristic is associated with
		 * @param characteristic Characteristic that has been updated as a result
		 *                       of a remote notification event.
		 */
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if(trace) log.trace("onCharacteristicChanged({})", characteristic.getUuid());
			inputStream.addData(characteristic);
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onCharacteristicChanged(gatt, characteristic);
		}

		/**
		 * Callback reporting the result of a descriptor read operation.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#readDescriptor}
		 * @param descriptor Descriptor that was read from the associated
		 *                   remote device.
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
		 *               was completed successfully
		 */
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if(trace) log.trace("onDescriptorRead({}, {})", descriptor.getUuid(), GattStatus.getStatus(status));
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onDescriptorRead(gatt, descriptor, status);
		}

		/**
		 * Callback indicating the result of a descriptor write operation.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#writeDescriptor}
		 * @param descriptor Descriptor that was writte to the associated
		 *                   remote device.
		 * @param status The result of the write operation
		 *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
		 */
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if(trace) log.trace("onDescriptorWrite({}, {})", descriptor.getUuid(), GattStatus.getStatus(status));
			if(setState(State.CONFIG_DESC, State.CONFIG_MTU)) {
				if(!gatt.requestMtu(mtu)) {
					close();
					return;
				}
			}
			if(gattCallback != null) gattCallback.onDescriptorWrite(gatt, descriptor, status);
		}

		/**
		 * Callback invoked when a reliable write transaction has been completed.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#executeReliableWrite}
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the reliable write
		 *               transaction was executed successfully
		 */
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			if(trace) log.trace("onReliableWriteCompleted({})", GattStatus.getStatus(status));
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onReliableWriteCompleted(gatt, status);
		}

		/**
		 * Callback reporting the RSSI for a remote device connection.
		 *
		 * This callback is triggered in response to the
		 * {@link BluetoothGatt#readRemoteRssi} function.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#readRemoteRssi}
		 * @param rssi The RSSI value for the remote device
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the RSSI was read successfully
		 */
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if(trace) log.trace("onReadRemoteRssi({}, {})", rssi, GattStatus.getStatus(status));
			setState(State.BUSY, State.OPEN);
			if(gattCallback != null) gattCallback.onReadRemoteRssi(gatt, rssi, status);
		}

		/**
		 * Callback indicating the MTU for a given device connection has changed.
		 *
		 * This callback is triggered in response to the
		 * {@link BluetoothGatt#requestMtu} function, or in response to a connection
		 * event.
		 *
		 * @param gatt GATT client invoked {@link BluetoothGatt#requestMtu}
		 * @param mtu The new MTU size
		 * @param status {@link BluetoothGatt#GATT_SUCCESS} if the MTU has been changed successfully
		 */
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
			if(trace) log.trace("onMtuChanged({}, {})", mtu, GattStatus.getStatus(status));
			outputStream.setMtu(mtu - 3);
			setState(State.CONFIG_MTU, State.OPEN);
			if(gattCallback != null) gattCallback.onMtuChanged(gatt, mtu, status);
		}
	}
}
