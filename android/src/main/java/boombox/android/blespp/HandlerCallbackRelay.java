package boombox.android.blespp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mriley on 10/24/16.
 */
public class HandlerCallbackRelay extends BluetoothGattCallback implements Callback {

	private static final Logger log = LoggerFactory.getLogger(HandlerCallbackRelay.class);

	enum MessageType {
		CONNECTION_STATE,
		SERVICES_DISCOVERED,
		CHAR_READ,
		CHAR_WRITE,
		CHAR_CHANGED,
		DESC_READ,
		DESC_WRITE,
		RELIABLE_WRITE_COMPLETE,
		READ_REMOTE_RSSI,
		MTU_CHANGED;

		public static final MessageType[] VALUES = values();
	}

	private final Handler handler;
	private final BluetoothGattCallback gattCallback;
	private BluetoothGatt bluetoothGatt;

	public HandlerCallbackRelay(BluetoothGattCallback gattCallback) {
		this(gattCallback, null);
	}

	public HandlerCallbackRelay(BluetoothGattCallback gattCallback, Handler handler) {
		if(handler == null) {
			if (Looper.myLooper() == null) {
				Looper.prepare();
			}
			handler = new Handler(this);
		}
		this.handler = handler;
		this.gattCallback = gattCallback;
	}

	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.CONNECTION_STATE.ordinal(), status, newState).sendToTarget();
	}

	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.SERVICES_DISCOVERED.ordinal(), status).sendToTarget();
	}

	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.CHAR_READ.ordinal(), status, 0, characteristic).sendToTarget();
	}

	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.CHAR_WRITE.ordinal(), status, 0, characteristic).sendToTarget();
	}

	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.CHAR_CHANGED.ordinal(), 0, 0, characteristic).sendToTarget();
	}

	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.DESC_READ.ordinal(), status, 0, descriptor).sendToTarget();
	}

	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.DESC_WRITE.ordinal(), status, 0, descriptor).sendToTarget();
	}

	public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.RELIABLE_WRITE_COMPLETE.ordinal(), status, 0).sendToTarget();
	}

	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.READ_REMOTE_RSSI.ordinal(), status, rssi).sendToTarget();
	}

	public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
		bluetoothGatt = gatt;
		handler.obtainMessage(MessageType.MTU_CHANGED.ordinal(), status, mtu).sendToTarget();
	}

	@Override
	public boolean handleMessage(Message msg) {
		MessageType mt = MessageType.VALUES[msg.what];
		switch (mt) {
			case CONNECTION_STATE:
				gattCallback.onConnectionStateChange(bluetoothGatt, msg.arg1, msg.arg2);
				break;
			case SERVICES_DISCOVERED:
				gattCallback.onServicesDiscovered(bluetoothGatt, msg.arg1);
				break;
			case CHAR_READ:
				gattCallback.onCharacteristicRead(bluetoothGatt, (BluetoothGattCharacteristic) msg.obj, msg.arg1);
				break;
			case CHAR_WRITE:
				gattCallback.onCharacteristicWrite(bluetoothGatt, (BluetoothGattCharacteristic) msg.obj, msg.arg1);
				break;
			case CHAR_CHANGED:
				gattCallback.onCharacteristicChanged(bluetoothGatt, (BluetoothGattCharacteristic) msg.obj);
				break;
			case DESC_READ:
				gattCallback.onDescriptorRead(bluetoothGatt, (BluetoothGattDescriptor) msg.obj, msg.arg1);
				break;
			case DESC_WRITE:
				gattCallback.onDescriptorWrite(bluetoothGatt, (BluetoothGattDescriptor) msg.obj, msg.arg1);
				break;
			case RELIABLE_WRITE_COMPLETE:
				gattCallback.onReliableWriteCompleted(bluetoothGatt, msg.arg1);
				break;
			case READ_REMOTE_RSSI:
				gattCallback.onReadRemoteRssi(bluetoothGatt, msg.arg2, msg.arg1);
				break;
			case MTU_CHANGED:
				gattCallback.onMtuChanged(bluetoothGatt, msg.arg2, msg.arg1);
				break;
		}
		return true;
	}
}
