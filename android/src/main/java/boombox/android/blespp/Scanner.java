package boombox.android.blespp;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanFilter.Builder;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.ParcelUuid;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mriley on 10/25/16.
 */
public class Scanner {

	public interface Callback {
		/**
		 * Callback when a BLE advertisement has been found.
		 *
		 * @param callbackType Determines how this callback was triggered. Could be one of
		 *            {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
		 *            {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
		 *            {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
		 * @param result A Bluetooth LE scan result.
		 */
		void onScanResult(Scanner scanner, int callbackType, ScanResult result);

		/**
		 * Callback when scan could not be started.
		 *
		 * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
		 */
		void onScanFailed(Scanner scanner, int errorCode);
	}

	private final CallbackAdapter callback = new CallbackAdapter();
	private final AtomicBoolean scanning = new AtomicBoolean();
	private final Context context;
	private final Callback listener;
	private final BluetoothLeScanner bluetoothLeScanner;

	public Scanner(Context context, Callback listener) {
		this.context = context;
		this.listener = listener;
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothLeScanner = bluetoothManager.getAdapter().getBluetoothLeScanner();
	}

	public boolean isScanning() {
		return scanning.get();
	}

	public Scanner scan() {
		if (scanning.compareAndSet(false, true)) {
			ScanSettings.Builder builder = new ScanSettings.Builder()
					.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
;
			if (VERSION.SDK_INT >= 23) {
				builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
			}

			ScanSettings settings = builder.build();

			ScanFilter filter = new Builder()
					.setServiceUuid(new ParcelUuid(Connection.SERVICE_UUID))
					.build();

			bluetoothLeScanner.startScan(Collections.singletonList(filter), settings, callback);
		}
		return this;
	}

	public void stop() {
		if (scanning.compareAndSet(true, false)) {
			bluetoothLeScanner.stopScan(callback);
		}
	}

	public Connection.Builder buildConnection() {
		return new Connection.Builder().setContext(context);
	}

	private final class CallbackAdapter extends ScanCallback {
		/**
		 * Callback when a BLE advertisement has been found.
		 *
		 * @param callbackType Determines how this callback was triggered. Could be one of
		 *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
		 *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
		 *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
		 * @param result       A Bluetooth LE scan result.
		 */
		public void onScanResult(int callbackType, ScanResult result) {
			listener.onScanResult(Scanner.this, callbackType, result);
		}

		/**
		 * Callback when batch results are delivered.
		 *
		 * @param results List of scan results that are previously scanned.
		 */
		public void onBatchScanResults(List<ScanResult> results) {
			for(ScanResult sr : results) onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, sr);
		}

		/**
		 * Callback when scan could not be started.
		 *
		 * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
		 */
		public void onScanFailed(int errorCode) {
			listener.onScanFailed(Scanner.this, errorCode);
		}
	}
}
